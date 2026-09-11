package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.*
import com.kelvinsaputra.promptstudio.feature.studio.*
import com.kelvinsaputra.promptstudio.guide.*
import com.kelvinsaputra.promptstudio.persistence.*
import com.kelvinsaputra.promptstudio.prompt.*
import kotlin.random.Random
import kotlin.test.*

class StudioDomainTest {
    private fun fresh() = CharacterLibrary.newCharacter("study", "Study").withStudioDefaults()
    @Test fun neutralStartIsUsableAndStyleIndependent() {
        val p = fresh()
        assertNotNull(p.output.aspectRatio)
        assertNotNull(GuideRenderSpec.from(p.visualAssembly, p.output.aspectRatio))
        val text = p.effectivePrompt()
        for (style in listOf("sumi-e", "watercolor", "sky-blue", "female", "antihero")) assertFalse(text.contains(style))
        assertEquals(text, ProjectJson.decode(ProjectJson.encode(p)).effectivePrompt())
    }
    @Test fun cameraCustomValuesAndNegativeDeduplicationReachCompiler() {
        val p = fresh().copy(composition = CompositionConfiguration(cameraAngle = CameraAngle.LOW, additionalInstructions = "Keep the distant arch visible!"),
            exclusions = listOf("extra fingers", " ", "extra fingers"))
        val text = p.effectivePrompt()
        assertContains(text, CameraAngle.LOW.wording)
        assertContains(text, "Keep the distant arch visible!")
        assertEquals(1, Regex("extra fingers").findAll(text).count())
        assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
    }
    @Test fun everyExistingModuleHasAnArtistFacingCategory() {
        val modules = StudioCategory.entries.flatMap { it.modules }
        assertEquals(modules.size, modules.distinct().size)
        assertEquals(EditorModule.entries.toSet() - setOf(EditorModule.VisualBuild, EditorModule.Prompt), modules.toSet())
    }
    @Test fun categoryResetAndRandomizationPreserveUnrelatedStateAndCustomWords() {
        val p = fresh().copy(costume = CostumeConfiguration(customNotes = "hand sewn"),
            composition = CompositionConfiguration(cameraAngle = CameraAngle.LOW, additionalInstructions = "keep the arch"),
            colorAccents = ColorAccentConfiguration(additionalInstructions = "ochre and teal"))
        val camera = p.randomizeStudioCategory(StudioCategory.Camera, Random(2))
        assertEquals(p.output.type, camera.output.type)
        assertEquals(p.costume, camera.costume)
        assertEquals(p.composition.additionalInstructions, camera.composition.additionalInstructions)
        assertEquals(p.composition.cameraAngle, camera.composition.cameraAngle)
        val reset = p.resetStudioCategory(StudioCategory.Appearance)
        assertEquals(p.composition, reset.composition)
        assertEquals(p.artStyle, reset.artStyle)
        assertEquals(fresh().costume, reset.costume)
    }
    @Test fun newPresetKindsPersistAndRespectScopeAndIdentity() {
        val p = fresh().withStylePreset(StyleLook.PAINTERLY).withVisualPose(VisualPose.SITTING)
        val target = fresh().copy(id = "target", name = "Destination")
        val kinds = listOf(VisualPresetValue.Style(p.style, p.artStyle, p.surfaceTexture), VisualPresetValue.Character(p), VisualPresetValue.Complete(p))
        val library = CharacterLibrary(p.id, listOf(p), presets = kinds.mapIndexed { i, value -> SavedVisualPreset("$i", "Preset $i", value) })
        val restored = CharacterLibraryJson.decode(CharacterLibraryJson.encode(library))
        assertEquals(library, restored)
        kinds.forEach { assertEquals(target.id, it.applyTo(target).id); assertEquals(target.name, it.applyTo(target).name) }
        assertEquals(target.pose, kinds[0].applyTo(target).pose)
        assertEquals(target.artStyle, kinds[1].applyTo(target).artStyle)
        assertEquals(p.effectivePrompt(), kinds[2].applyTo(target).effectivePrompt())
    }
    @Test fun assetKeysAreUniqueAndSearchUsesMetadataWithoutChangingPromptWording() {
        assertEquals(VisualGuideRegistry.all.size, VisualGuideRegistry.all.map { it.key }.distinct().size)
        val pose = VisualGuideRegistry.poses.first { it.value == BasePose.CONTRAPPOSTO }
        val asset = VisualGuideAsset(label = "Easy stance", aliases = listOf("casual"), tags = listOf("standing"))
        assertTrue(pose.matches("casual standing", asset)); assertFalse(pose.matches("flying", asset))
        assertEquals(BasePose.CONTRAPPOSTO.wording, pose.promptValue)
        assertEquals(BasePose.entries.toSet(), VisualGuideRegistry.poses.map { it.value }.toSet())
    }
    @Test fun switchingVariantsKeepsTheActiveCreativeCategory() {
        val editor = EditorViewModel()
        editor.selectModule(EditorModule.Pose)
        editor.addVariant("Landscape", OutputType.DESKTOP)
        assertEquals(EditorModule.Pose, editor.state.value.module)
    }
}
