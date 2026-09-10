package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.credentials.*
import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.generation.model.*
import com.kelvinsaputra.promptstudio.generation.provider.*
import com.kelvinsaputra.promptstudio.feature.generation.*
import com.kelvinsaputra.promptstudio.persistence.*
import com.kelvinsaputra.promptstudio.prompt.PromptCompiler
import com.kelvinsaputra.promptstudio.prompt.enterManualPrompt
import com.kelvinsaputra.promptstudio.prompt.editManualPrompt
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class GenerationStateTest {
    private val model = ImageModels.default(ImageProviderId.OpenAI)
    private fun credentials() = SessionCredentialStore().apply { set(ImageProviderId.OpenAI, "test-secret") }
    private class Fake : ImageGenerationProvider {
        override val id = ImageProviderId.OpenAI
        var calls = mutableListOf<ImageGenerationRequest>()
        var response = CompletableDeferred<ProviderImage>()
        override suspend fun generate(request: ImageGenerationRequest, credentials: ProviderCredentials): ProviderImage {
            calls.add(request); return response.await()
        }
        fun succeed() { response.complete(ProviderImage(byteArrayOf(1,2,3), "image/png", "request-id")) }
    }
    @Test fun snapshotDuplicateProtectionRegenerateAndGenerateCurrent() = runTest {
        val fake = Fake()
        val controller = GenerationController(this, listOf(fake), credentials())
        val traits = mutableListOf("calm")
        val original = CharacterProject(personality = PersonalityConfiguration(traits))
        assertEquals(GenerationState.Idle, controller.state.value.status)
        controller.generateCurrent(original, model)
        assertIs<GenerationState.Generating>(controller.state.value.status)
        controller.generateCurrent(original, model)
        runCurrent()
        assertEquals(1, fake.calls.size)
        val expected = PromptCompiler().compile(original).text
        traits[0] = "angry"
        val edited = original.copy(name = "Edited")
        assertEquals(expected, fake.calls.single().prompt)
        fake.succeed(); runCurrent()
        assertEquals(GenerationState.Success, controller.state.value.status)
        val latest = controller.state.value.latest!!
        assertEquals(listOf("calm"), latest.metadata.project.personality.traits)
        assertEquals(expected, latest.metadata.request.prompt)
        assertEquals("request-id", latest.metadata.requestId)
        fake.response = CompletableDeferred()
        controller.regenerate(); runCurrent()
        assertEquals(expected, fake.calls.last().prompt)
        fake.succeed(); runCurrent()
        fake.response = CompletableDeferred()
        controller.generateCurrent(edited, model); runCurrent()
        assertEquals(PromptCompiler().compile(edited).text, fake.calls.last().prompt)
        controller.cancel(); runCurrent()
        assertEquals(GenerationState.Cancelled, controller.state.value.status)
        assertNotNull(controller.state.value.latest)
    }
    @Test fun existingGenerationUsesManualOutputAndRejectsEmptyDraft() = runTest {
        val fake = Fake()
        val controller = GenerationController(this, listOf(fake), credentials())
        val project = CharacterProject().enterManualPrompt().editManualPrompt("Exact manual prompt")
        controller.generateCurrent(project, model); runCurrent()
        assertEquals("Exact manual prompt", fake.calls.single().prompt)
        controller.cancel(); runCurrent()
        controller.generateCurrent(project.editManualPrompt("  "), model); runCurrent()
        assertEquals(GenerationState.Error(GenerationError.InvalidRequest), controller.state.value.status)
        assertEquals(1, fake.calls.size)
    }
    @Test fun missingCredentialsAndProviderErrorsPreserveEditor() = runTest {
        val fake = Fake()
        val keys = SessionCredentialStore()
        val controller = GenerationController(this, listOf(fake), keys)
        val project = CharacterProject()
        controller.generateCurrent(project, model)
        assertEquals(GenerationState.Error(GenerationError.MissingCredentials), controller.state.value.status)
        assertTrue(fake.calls.isEmpty())
        keys.set(ImageProviderId.OpenAI, "test-secret")
        controller.generateCurrent(project, model); runCurrent()
        fake.response.completeExceptionally(GenerationFailure(GenerationError.RateLimit)); runCurrent()
        assertEquals(GenerationState.Error(GenerationError.RateLimit), controller.state.value.status)
        assertEquals(CharacterProject(), project)
    }
    @Test fun cancelThenStartDoesNotAcceptOldCompletion() = runTest {
        val fake = Fake()
        val controller = GenerationController(this, listOf(fake), credentials())
        controller.generateCurrent(CharacterProject(), model); runCurrent()
        val old = fake.response
        controller.cancel()
        fake.response = CompletableDeferred()
        controller.generateCurrent(CharacterProject(name = "New"), model); runCurrent()
        old.complete(ProviderImage(byteArrayOf(0), "image/png")); runCurrent()
        assertIs<GenerationState.Generating>(controller.state.value.status)
        fake.succeed(); runCurrent()
        assertEquals("New", controller.state.value.latest!!.metadata.project.name)
    }
    @Test fun credentialsNeverEnterProjectLibraryOrMetadata() = runTest {
        val keys = credentials()
        assertFalse(keys.get(ImageProviderId.OpenAI).toString().contains("test-secret"))
        val fake = Fake()
        val controller = GenerationController(this, listOf(fake), keys)
        controller.generateCurrent(CharacterProject(), model); runCurrent(); fake.succeed(); runCurrent()
        val metadata = controller.state.value.latest!!.metadata
        for (encoded in listOf(ProjectJson.encode(metadata.project), CharacterLibraryJson.encode(CharacterLibrary()), Json.encodeToString(metadata))) {
            assertFalse(encoded.contains("test-secret")); assertFalse(encoded.contains("apiKey"))
        }
        keys.remove(ImageProviderId.OpenAI)
        assertNull(keys.get(ImageProviderId.OpenAI))
        controller.regenerate()
        assertEquals(GenerationState.Error(GenerationError.MissingCredentials), controller.state.value.status)
        assertNotNull(controller.state.value.latest)
    }
    @Test fun capabilitiesAndFilenameAreHonest() {
        for (id in ImageProviderId.entries) for (ratio in listOf("1:1", "9:16", "16:9", "4:5"))
            assertEquals(ratio, ImageModels.default(id).outputFor(ratio)!!.aspectRatio)
        assertNull(model.outputFor("widescreen"))
        assertNull(model.outputFor("0:1"))
        assertNull(model.outputFor("Infinity:1"))
        assertEquals("37:20", model.outputFor("1.85:1")!!.aspectRatio)
        assertEquals("7:4", model.outputFor("7:4")!!.aspectRatio)
        assertEquals("3:1", model.outputFor("8:1")!!.aspectRatio)
        for (ratio in listOf("7:4", "1.85:1", "8:1", "1:8", "2:2")) assertTrue(model.supports(model.outputFor(ratio)!!))
        val request = ImageGenerationRequest("prompt", "9:16", model.id, model.outputFor("9:16")!!)
        val image = GeneratedImage(byteArrayOf(), "image/jpeg", GenerationMetadata(model.provider, request, CharacterProject(name = "../a/b"), outputFormat = "jpeg"))
        assertFalse(image.suggestedFilename().contains('/'))
        assertTrue(image.suggestedFilename().endsWith(".jpg"))
    }
}
