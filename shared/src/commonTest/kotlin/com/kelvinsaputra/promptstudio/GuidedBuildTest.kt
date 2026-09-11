package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.EditorViewModel
import com.kelvinsaputra.promptstudio.feature.editor.EditorModule
import com.kelvinsaputra.promptstudio.guide.GuideRenderSpec
import com.kelvinsaputra.promptstudio.persistence.*
import com.kelvinsaputra.promptstudio.prompt.*
import kotlinx.serialization.json.*
import kotlin.test.*

class GuidedBuildTest {
    private fun fresh() = CharacterLibrary.newCharacter("guide").withGuidedDefaults().copy(output = OutputConfiguration(type = OutputType.PORTRAIT, figurePlacement = "center"))
    private fun core(p: CharacterProject) = PromptCompiler().compile(p).sections.firstOrNull { it.title == "ART STYLE CORE" }?.body

    @Test fun guidedSelectionsProjectIntoCanonicalStateAndCompiler() {
        val p = fresh().withGuideGender(GenderPresentation.MALE).withGuideAge(AgeBand.MATURE)
            .withVisualPose(VisualPose.CROUCHING).withGuideGaze(Gaze.RIGHT).withGuideFraming(Framing.WAIST_UP)
            .withGuidePosition(VisualPlacement.LOWER).withGuideComposition(GuideComposition.NEGATIVE_SPACE)
        with(p.visualAssembly) {
            assertEquals(GenderPresentation.MALE, gender); assertEquals(AgeBand.MATURE, ageBand)
            assertEquals(VisualPose.CROUCHING, posePreset); assertEquals(Gaze.RIGHT, gaze)
            assertEquals(Framing.WAIST_UP, framing); assertEquals(VisualPlacement.LOWER, placementPreset)
            assertEquals(GuideComposition.NEGATIVE_SPACE, composition)
        }
        val prompt = p.effectivePrompt()
        for (wording in listOf("male", "mature", "crouching", "looking right", "waist-up", "lower center", "broad quiet space")) assertContains(prompt, wording)
        assertEquals(p.visualAssembly, GuideRenderSpec.from(p.visualAssembly, p.output.aspectRatio)?.assembly)
    }

    @Test fun onboardingBackAndForwardPreserveSelectionsAndFinishAtSummary() {
        val editor = EditorViewModel(); editor.createProject("New", OutputType.PHONE)
        assertEquals(GuidedNavigation.newProject(), editor.state.value.guided)
        assertNull(editor.state.value.project.identity.ageBand) // New studio defaults make no age assumption.
        editor.setProject(editor.state.value.project.withGuideGender(GenderPresentation.MALE))
        editor.navigateGuide(editor.state.value.guided.forward())
        editor.setProject(editor.state.value.project.withGuideAge(AgeBand.YOUNG_ADULT))
        editor.navigateGuide(editor.state.value.guided.back())
        assertEquals(GuideStep.Gender, editor.state.value.guided.step)
        val snapshot = editor.state.value.project
        repeat(8) { editor.navigateGuide(editor.state.value.guided.forward()) }
        assertEquals(GuidedNavigation(), editor.state.value.guided)
        assertEquals(snapshot, editor.state.value.project)
    }

    @Test fun everyDashboardEditReturnsDirectlyToSummaryAndSharesTheProject() {
        val editor = EditorViewModel(); editor.createProject("New", OutputType.SQUARE)
        editor.navigateGuide(GuidedNavigation())
        GuideStep.entries.forEach { step ->
            editor.navigateGuide(editor.state.value.guided.edit(step))
            assertFalse(editor.state.value.guided.onboarding)
            editor.setProject(editor.state.value.project.withGuideGaze(Gaze.UPWARD))
            editor.navigateGuide(editor.state.value.guided.forward())
            assertEquals(GuidedNavigation(), editor.state.value.guided)
            assertEquals(Gaze.UPWARD, editor.state.value.project.visualAssembly.gaze)
        }
    }

    @Test fun existingProjectsImportsVariantsAndRestartsDoNotStartOnboarding() {
        val storage = object : ProjectStorage {
            var text: String? = null
            override fun read() = text
            override fun write(json: String) { text = json }
        }
        val editor = EditorViewModel(storage = storage)
        assertNull(editor.state.value.guided.step)
        editor.createProject("Saved", OutputType.PHONE)
        val snapshot = editor.state.value.project
        val restored = EditorViewModel(storage = storage)
        assertNull(restored.state.value.guided.step)
        assertEquals(snapshot, restored.state.value.project)
        editor.selectCharacter(snapshot.id)
        assertNull(editor.state.value.guided.step)
        editor.navigateGuide(GuidedNavigation.newProject())
        editor.importProject(ProjectJson.encode(snapshot))
        assertNull(editor.state.value.guided.step)
        editor.addVariant("Other", OutputType.DESKTOP)
        assertNull(editor.state.value.guided.step)
    }

    @Test fun projectEntryFromDetailedEditorAlsoRoutesToGuidedBuild() {
        val editor = EditorViewModel()
        val existingId = editor.state.value.project.id
        editor.selectModule(EditorModule.Costume)
        editor.newCharacter()
        assertEquals(EditorModule.VisualBuild, editor.state.value.module)
        assertEquals(GuideStep.Gender, editor.state.value.guided.step)
        editor.selectModule(EditorModule.Style)
        editor.selectCharacter(existingId)
        assertEquals(EditorModule.VisualBuild, editor.state.value.module)
        assertNull(editor.state.value.guided.step)
        editor.selectModule(EditorModule.Identity)
        editor.importProject(ProjectJson.encode(editor.state.value.project))
        assertEquals(EditorModule.VisualBuild, editor.state.value.module)
        assertNull(editor.state.value.guided.step)
    }

    @Test fun positionAndCompositionNeverMutateEachOther() {
        val p = fresh().withGuidePosition(VisualPlacement.LEFT)
        val balanced = p.withGuideComposition(GuideComposition.SYMMETRICAL)
        assertEquals(p.output, balanced.output)
        assertEquals(VisualPlacement.LEFT, balanced.visualAssembly.placementPreset)
        assertEquals(balanced.composition, balanced.withGuidePosition(VisualPlacement.RIGHT).composition)
    }

    @Test fun poseOverridesBelongToPresetAndCannotLeakAcrossPresetChanges() {
        val adjusted = fresh().withVisualPose(VisualPose.RELAXED).withPoseAdjustments(PoseAdjustments(leftElbow = 20f))
        assertTrue(adjusted.visualAssembly.adjustments.isAdjusted)
        assertContains(adjusted.effectivePrompt(), "left elbow rotated 20 degrees")
        assertEquals(adjusted.pose.guideAdjustments, adjusted.withGuideGaze(Gaze.LEFT).pose.guideAdjustments)
        assertFalse(adjusted.withVisualPose(VisualPose.SITTING).visualAssembly.adjustments.isAdjusted)
        val detailed = adjusted.copy(pose = adjusted.pose.copy(basePose = BasePose.KNEELING))
        assertFalse(detailed.visualAssembly.adjustments.isAdjusted)
        assertFalse(detailed.effectivePrompt().contains("left elbow rotated"))
        assertFailsWith<IllegalArgumentException> { PoseAdjustments(leftElbow = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { PoseAdjustments(leftElbow = 100f) }
    }

    @Test fun eachStylePresetAndRefinementCompilesDeterministically() {
        for (look in StyleLook.entries) {
            val p = fresh().withStylePreset(look)
            assertEquals(p.compileStructuredArtStyle(), core(p))
            assertEquals(PromptCompiler().compile(p), PromptCompiler().compile(p))
            val refined = p.withStyleAdjustments(StyleAdjustments(StyleInk.STRONG, StyleMood.DRAMATIC, StyleColor.VIVID))
            assertContains(core(refined).orEmpty(), StyleInk.STRONG.wording)
            assertContains(core(refined).orEmpty(), StyleMood.DRAMATIC.wording)
            assertContains(core(refined).orEmpty(), StyleColor.VIVID.wording)
            assertFalse(core(refined).orEmpty().contains(StyleColor.MUTED.wording))
        }
    }

    @Test fun styleManualOwnershipPreservesStructuredInputsAndDraft() {
        val p = fresh().withStylePreset(StyleLook.PAINTERLY).withStyleAdjustments(StyleAdjustments(mood = StyleMood.DRAMATIC))
        val manual = p.enterManualStyle()
        assertEquals(p.compileStructuredArtStyle(), manual.artStyle.manualDraft)
        val edited = manual.editManualStyle("My style only.")
        val underneath = edited.withStylePreset(StyleLook.WATERCOLOR).withStyleAdjustments(StyleAdjustments(color = StyleColor.VIVID))
        assertEquals("My style only.", core(underneath))
        assertEquals(StyleLook.WATERCOLOR, underneath.artStyle.preset)
        assertEquals(StyleColor.VIVID, underneath.artStyle.adjustments.color)
        val structured = underneath.useStructuredStyle()
        assertEquals(structured.compileStructuredArtStyle(), core(structured))
        assertEquals("My style only.", structured.artStyle.manualDraft)
        assertEquals("My style only.", core(structured.resumeManualStyle()))
        assertEquals(structured.compileStructuredArtStyle(), structured.enterManualStyle().artStyle.manualDraft)
        assertEquals("Custom · Manual ART STYLE CORE", underneath.guideSummary(GuideStep.ArtStyle))
    }

    @Test fun globalManualTakesPrecedenceWithoutMutatingStyleState() {
        val style = fresh().enterManualStyle().editManualStyle("Custom rendering.")
        val global = style.enterManualPrompt().editManualPrompt("Exact global text")
        val updated = global.withStylePreset(StyleLook.ANIME).editManualStyle("Different style.")
        assertEquals("Exact global text", updated.effectivePrompt())
        assertEquals("Different style.", core(updated))
        assertEquals(updated.artStyle, updated.useAutomaticPrompt().artStyle)
        assertContains(updated.useAutomaticPrompt().effectivePrompt(), "Different style.")
        assertEquals("Exact global text", updated.useStructuredStyle().effectivePrompt())
    }

    @Test fun legacyStyleIsUntouchedAndNewStateRoundTripsThroughProjectAndVariants() {
        val legacy = CharacterProject(style = ArtStylePreset("imported", "Original", "Preserve original style."))
        val objectWithoutNewField = Json.parseToJsonElement(ProjectJson.encode(legacy)).jsonObject.filterKeys { it != "artStyle" }
        val decoded = ProjectJson.decode(JsonObject(objectWithoutNewField).toString())
        assertEquals("Preserve original style.", core(decoded))
        val p = fresh().withGuideComposition(GuideComposition.DIAGONAL).withVisualPose(VisualPose.ACTION)
            .withPoseAdjustments(PoseAdjustments(rightShoulder = -10f)).withStylePreset(StyleLook.ANIME_INK)
            .enterManualStyle().editManualStyle("A saved style.")
        assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
        var library = CharacterLibrary(p.id, listOf(p)).addVariant("Landscape", OutputType.DESKTOP)
        library = library.replaceActive(library.active.withStylePreset(StyleLook.PAINTERLY).withGuidePosition(VisualPlacement.LEFT))
        assertEquals(library, CharacterLibraryJson.decode(CharacterLibraryJson.encode(library)))
        assertEquals(StyleLook.PAINTERLY, library.characters.single().artStyle.preset)
        assertEquals(VisualPlacement.LEFT, library.active.visualAssembly.placementPreset)
        assertEquals(VisualPlacement.CENTER, library.characters.single().visualAssembly.placementPreset)
    }

    @Test fun guidedGenderAndAgeOptionsAreConstrainedWithoutBreakingLegacyImports() {
        assertEquals(4, guidedAgeBands.size)
        assertFalse(AgeBand.LATE_TEEN in guidedAgeBands)
        assertFailsWith<IllegalArgumentException> { fresh().withGuideGender(GenderPresentation.ANDROGYNOUS) }
        assertFailsWith<IllegalArgumentException> { fresh().withGuideAge(AgeBand.LATE_TEEN) }
        val legacy = CharacterProject(identity = IdentityConfiguration(genderPresentation = GenderPresentation.ANDROGYNOUS))
        assertEquals(legacy, ProjectJson.decode(ProjectJson.encode(legacy)))
    }
}
