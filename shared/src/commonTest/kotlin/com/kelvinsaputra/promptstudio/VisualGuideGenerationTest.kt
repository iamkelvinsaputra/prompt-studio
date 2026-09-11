package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.credentials.*
import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.generation.*
import com.kelvinsaputra.promptstudio.generation.model.*
import com.kelvinsaputra.promptstudio.generation.provider.*
import com.kelvinsaputra.promptstudio.guide.*
import com.kelvinsaputra.promptstudio.history.*
import com.kelvinsaputra.promptstudio.persistence.ProjectJson
import com.kelvinsaputra.promptstudio.prompt.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class VisualGuideGenerationTest {
    private val model = ImageModels.default(ImageProviderId.OpenAI)
    private fun project() = CharacterProject().resetVisualAssembly().withVisualPose(VisualPose.RELAXED).let {
        it.copy(pose = it.pose.copy(guideFacing = GuideFacing.THREE_QUARTER, guideProp = GuideProp.SHOULDER),
            output = it.output.copy(figurePlacement = VisualPlacement.LOWER.wording))
    }
    private fun credentials() = SessionCredentialStore().apply { set(model.provider, "test-only-key") }
    private class Provider(override val supportsVisualGuide: Boolean = true) : ImageGenerationProvider {
        override val id = ImageProviderId.OpenAI
        val requests = mutableListOf<ImageGenerationRequest>()
        override suspend fun generate(request: ImageGenerationRequest, credentials: ProviderCredentials): ProviderImage {
            requests += request
            return ProviderImage(byteArrayOf(1, 2, 3), "image/png")
        }
    }
    private class Renderer : GuideRenderer {
        val specs = mutableListOf<GuideRenderSpec>()
        override suspend fun render(spec: GuideRenderSpec): GuideImage {
            specs += spec
            return GuideImage(byteArrayOf(1, 2, 3), spec.width, spec.height)
        }
    }

    @Test fun guideDimensionsAreBoundedAndFollowAuthoredRatios() {
        for ((ratio, width, height) in listOf(Triple("9:16", 432, 768), Triple("16:9", 768, 432), Triple("1:1", 768, 768), Triple("1:8", 96, 768))) {
            val spec = assertNotNull(GuideRenderSpec.from(project().visualAssembly, ratio))
            assertEquals(width, spec.width); assertEquals(height, spec.height)
            assertEquals(spec, GuideRenderSpec.from(project().visualAssembly, ratio))
        }
        for (ratio in listOf(null, "", "0:1", "Infinity:1", "1:0", "1:1000000", "freeform"))
            assertNull(GuideRenderSpec.from(project().visualAssembly, ratio))
        assertNull(GuideRenderSpec.from(project().visualAssembly.copy(basePose = BasePose.KNEELING), "9:16"))
        assertFailsWith<IllegalArgumentException> { GuideRenderSpec(project().visualAssembly, 100000, 100000) }
        assertFailsWith<IllegalArgumentException> { GuideRenderSpec(project().visualAssembly, 432, 768, version = 3) }
    }

    @Test fun legacyRequestsWithoutGuideFieldsRemainTextOnly() {
        val legacy = Json.decodeFromString<ImageGenerationRequest>("""{"prompt":"Legacy prompt","requestedAspectRatio":"1:1","model":"gpt-image-2","output":{"aspectRatio":"1:1","size":"1024x1024"}}""")
        assertNull(legacy.guideSpec)
        assertNull(legacy.referenceGuide)
        assertEquals("Legacy prompt", legacy.prompt)
    }

    @Test fun guideIsOptionalAndManualOrAdjustedTextIsSentUnchanged() = runTest {
        val provider = Provider(); val renderer = Renderer()
        val controller = GenerationController(this, listOf(provider), credentials(), guideRenderer = renderer)
        val automatic = project().copy(promptAuthoring = PromptAuthoring(adjustmentText = "Make the expression mischievous."))
        controller.generateCurrent(automatic, model, useVisualGuide = true); runCurrent()
        val guided = provider.requests.single()
        assertEquals(automatic.effectivePrompt(), guided.prompt)
        assertNotNull(guided.referenceGuide)
        assertEquals(automatic.visualAssembly, renderer.specs.single().assembly)
        controller.generateCurrent(automatic, model, useVisualGuide = false); runCurrent()
        assertNull(provider.requests.last().referenceGuide)
        assertNull(provider.requests.last().guideSpec)
        assertEquals(guided.prompt, provider.requests.last().prompt)
        assertEquals(1, renderer.specs.size)
        val manual = automatic.enterManualPrompt().editManualPrompt("  Exact manual instructions!\n")
        controller.generateCurrent(manual, model, useVisualGuide = true); runCurrent()
        assertEquals(manual.effectivePrompt(), provider.requests.last().prompt)
        assertNotNull(provider.requests.last().referenceGuide)
        assertEquals(renderer.specs.first(), renderer.specs.last())
        assertEquals(GenerationState.Success, controller.state.value.status)
    }

    @Test fun guidedStyleAndGlobalManualOwnershipReachTheExistingGenerationPipeline() = runTest {
        val provider = Provider(); val renderer = Renderer()
        val controller = GenerationController(this, listOf(provider), credentials(), guideRenderer = renderer)
        val guided = project().withGuidedDefaults().withGuidePosition(VisualPlacement.LOWER)
            .withGuideComposition(GuideComposition.NEGATIVE_SPACE).withGuideGaze(Gaze.RIGHT)
            .withStylePreset(StyleLook.WATERCOLOR).withStyleAdjustments(StyleAdjustments(color = StyleColor.VIVID))
        controller.generateCurrent(guided, model, useVisualGuide = true); runCurrent()
        assertEquals(guided.effectivePrompt(), provider.requests.last().prompt)
        assertContains(provider.requests.last().prompt, StyleColor.VIVID.wording)
        assertEquals(guided.visualAssembly, renderer.specs.last().assembly)
        assertEquals(2, renderer.specs.last().version)
        val styleManual = guided.enterManualStyle().editManualStyle("Only my style rendering.")
        controller.generateCurrent(styleManual, model, useVisualGuide = true); runCurrent()
        assertContains(provider.requests.last().prompt, "ART STYLE CORE\n\nOnly my style rendering.")
        assertEquals(guided.visualAssembly, renderer.specs.last().assembly)
        val global = styleManual.enterManualPrompt().editManualPrompt("Entire prompt verbatim.")
        controller.generateCurrent(global, model, useVisualGuide = true); runCurrent()
        assertEquals("Entire prompt verbatim.", provider.requests.last().prompt)
        assertEquals(styleManual.artStyle, controller.state.value.latest?.metadata?.project?.artStyle)
    }

    @Test fun unsupportedGuideNeverRendersOrReachesProviderButTextStillWorks() = runTest {
        val provider = Provider(false); val renderer = Renderer()
        val controller = GenerationController(this, listOf(provider), credentials(), guideRenderer = renderer)
        assertFalse(controller.supportsVisualGuide(model))
        controller.generateCurrent(project(), model, useVisualGuide = true); runCurrent()
        assertEquals(GenerationState.Error(GenerationError.GuideUnsupported), controller.state.value.status)
        assertTrue(provider.requests.isEmpty()); assertTrue(renderer.specs.isEmpty())
        controller.generateCurrent(project(), model, useVisualGuide = false); runCurrent()
        assertNull(provider.requests.single().referenceGuide)
        assertEquals(GenerationState.Success, controller.state.value.status)
    }

    @Test fun failedGuideIsExplicitAndUserCanRetryWithoutIt() = runTest {
        val provider = Provider()
        val controller = GenerationController(this, listOf(provider), credentials(), guideRenderer = GuideRenderer { error("Rendering failed") })
        controller.generateCurrent(project(), model, useVisualGuide = true); runCurrent()
        assertEquals(GenerationState.Error(GenerationError.GuidePreparation), controller.state.value.status)
        assertTrue(provider.requests.isEmpty())
        controller.generateCurrent(project(), model, useVisualGuide = false); runCurrent()
        assertEquals(GenerationState.Success, controller.state.value.status)
        assertNull(provider.requests.single().referenceGuide)
    }

    @Test fun snapshotSurvivesCancellationAndDuplicateGenerateDuringPreparation() = runTest {
        val provider = Provider()
        val started = mutableListOf<GuideRenderSpec>()
        val resume = CompletableDeferred<Unit>()
        val controller = GenerationController(this, listOf(provider), credentials(), guideRenderer = GuideRenderer { spec ->
            started += spec; resume.await(); GuideImage(byteArrayOf(1), spec.width, spec.height)
        })
        val original = project()
        controller.generateCurrent(original, model, useVisualGuide = true); runCurrent()
        controller.generateCurrent(original.withVisualPose(VisualPose.SITTING), model, useVisualGuide = true); runCurrent()
        assertEquals(listOf(original.visualAssembly), started.map { it.assembly })
        controller.cancel(); resume.complete(Unit); runCurrent()
        assertEquals(GenerationState.Cancelled, controller.state.value.status)
        assertTrue(provider.requests.isEmpty())
    }

    @Test fun historySavesOnlyRenderSpecAndRegeneratesWithTheSameGuide() = runTest {
        val provider = Provider(); val renderer = Renderer()
        val controller = GenerationController(this, listOf(provider), credentials(), guideRenderer = renderer)
        val project = project()
        val variant = GenerationVariant("desktop-variant", "Desktop")
        controller.generateCurrent(project, model, useVisualGuide = true, variant = variant); runCurrent()
        val latest = assertNotNull(controller.state.value.latest)
        val encoded = HistoryJson.encode(GenerationRecord.capture(latest))
        assertFalse(encoded.contains("referenceGuide"))
        assertNull(latest.metadata.request.referenceGuide)
        assertFalse(ProjectJson.encode(project).contains("guideSpec"))
        val restored = HistoryJson.decode(encoded)
        assertEquals(variant, restored.metadata.variant)
        assertEquals(project.effectivePrompt(), restored.metadata.request.prompt)
        val edited = project.withVisualPose(VisualPose.ACTION).enterManualPrompt().editManualPrompt("Different text")
        assertNotEquals(edited.effectivePrompt(), restored.metadata.request.prompt)
        controller.generateAgain(restored.metadata); runCurrent()
        assertEquals(variant, controller.state.value.latest?.metadata?.variant)
        assertEquals(renderer.specs.first(), renderer.specs.last())
        assertEquals(provider.requests.first().prompt, provider.requests.last().prompt)
        assertNotNull(provider.requests.last().referenceGuide)
        assertContentEquals(provider.requests.first().referenceGuide!!.bytes(), provider.requests.last().referenceGuide!!.bytes())
    }
}
