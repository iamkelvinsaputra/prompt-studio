package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.EditorViewModel
import com.kelvinsaputra.promptstudio.generation.model.ImageProviderId
import com.kelvinsaputra.promptstudio.persistence.*
import com.kelvinsaputra.promptstudio.prompt.*
import kotlin.test.*

class ProductizationTest {
    @Test fun savedPresetsAreReusableValuesAndCompositionPreservesOutputAndManualDraft() {
        val editor = EditorViewModel()
        editor.setProject(editor.state.value.project.resetVisualAssembly().withVisualPose(VisualPose.RELAXED))
        editor.savePreset("Relaxed", false)
        val pose = editor.state.value.library.presets.single()
        editor.setProject(editor.state.value.project.copy(output = editor.state.value.project.output.copy(figurePlacement = "lower center")))
        editor.savePreset("Lower", true)
        val composition = editor.state.value.library.presets.first()
        editor.createProject("Another", OutputType.DESKTOP)
        editor.setProject(editor.state.value.project.enterManualPrompt().editManualPrompt("Keep exact text"))
        editor.applyPreset(pose.id)
        assertEquals(VisualPose.RELAXED, editor.state.value.project.visualAssembly.posePreset)
        editor.applyPreset(composition.id)
        assertEquals("lower center", editor.state.value.project.output.figurePlacement)
        assertEquals("16:9", editor.state.value.project.output.aspectRatio)
        assertEquals("Keep exact text", editor.state.value.effectivePrompt)
        assertEquals(composition.id, editor.state.value.library.presets.first().id)
        val restored = CharacterLibraryJson.decode(CharacterLibraryJson.encode(editor.state.value.library))
        assertEquals(editor.state.value.library, restored)
        editor.deletePreset(pose.id)
        assertEquals(VisualPose.RELAXED, editor.state.value.project.visualAssembly.posePreset)
    }

    @Test fun failedAutosaveKeepsAllVariantWorkAndRetryPersistsIt() {
        var failing = true
        var saved: String? = null
        val storage = object : ProjectStorage {
            override fun read() = saved
            override fun write(json: String) { if (failing) error("disk full"); saved = json }
        }
        val editor = EditorViewModel(storage = storage)
        editor.addVariant("Desktop", OutputType.DESKTOP)
        editor.setProject(editor.state.value.project.copy(promptAuthoring = PromptAuthoring(adjustmentText = "Lean toward viewer")))
        assertNotNull(editor.state.value.saveError)
        assertEquals("16:9", editor.state.value.project.output.aspectRatio)
        assertNull(saved)
        failing = false; editor.retrySave()
        assertNull(editor.state.value.saveError)
        assertEquals(editor.state.value.library, EditorViewModel(storage = storage).state.value.library)
    }

    @Test fun variantsShareCharacterAndIsolateSceneAndPromptOwnershipAcrossRestart() {
        val storage = object : ProjectStorage {
            var json: String? = null
            override fun read() = json
            override fun write(json: String) { this.json = json }
        }
        val editor = EditorViewModel(storage = storage)
        editor.setProject(editor.state.value.project.resetVisualAssembly().copy(promptAuthoring = PromptAuthoring(adjustmentText = "Lean forward")))
        val phone = editor.state.value.project
        editor.addVariant("Desktop", OutputType.DESKTOP)
        val desktopId = editor.state.value.library.activeVariants.activeId
        editor.setProject(editor.state.value.project.withVisualPose(VisualPose.ACTION).copy(subject = "Shared identity").enterManualPrompt().editManualPrompt("Exact manual text"))
        val preferences = GenerationPreferences(provider = ImageProviderId.Gemini, useVisualGuide = false)
        editor.setGeneration(preferences)
        val desktop = editor.state.value.project
        editor.selectVariant(PRIMARY_VARIANT)
        assertEquals(phone.pose, editor.state.value.project.pose)
        assertEquals(phone.promptAuthoring, editor.state.value.project.promptAuthoring)
        assertEquals("Shared identity", editor.state.value.project.subject)
        assertTrue(editor.state.value.library.activeVariants.generation.useVisualGuide)
        editor.selectVariant(desktopId)
        val restored = EditorViewModel(storage = storage).state.value
        assertEquals(desktop, restored.project)
        assertEquals("Exact manual text", restored.effectivePrompt)
        assertFalse(restored.library.activeVariants.generation.useVisualGuide)
        assertEquals(preferences, restored.library.activeVariants.generation)
        assertEquals(desktopId, restored.library.activeVariants.activeId)
    }

    @Test fun duplicatesKeepVariantsButUseNewProjectAndVariantIdentities() {
        val original = CharacterLibrary().addVariant("Square", OutputType.SQUARE)
        val duplicate = original.duplicateActive()
        assertNotEquals(original.active.id, duplicate.active.id)
        assertNotEquals(original.activeVariants.activeId, duplicate.activeVariants.activeId)
        assertEquals(original.active.pose, duplicate.active.pose)
        assertEquals(2, duplicate.activeVariants.alternatives.size + 1)
        assertEquals(PRIMARY_VARIANT, duplicate.deleteVariant().activeVariants.activeId)
        assertEquals(original.active.id, duplicate.deleteActive().active.id)
    }

    @Test fun oldSingleProjectWithNonDemoIdAndOldLibraryRestoreWithoutWriting() {
        val project = CharacterProject(id = "old-project", name = "Old")
        val json = ProjectJson.encode(project)
        val storage = object : ProjectStorage {
            override fun read() = json
            override fun write(json: String) = error("Opening must not overwrite")
        }
        assertEquals(project, EditorViewModel(storage = storage).state.value.project)
        val legacy = "{\"activeCharacterId\":\"old-project\",\"characters\":[$json]}"
        assertEquals(project, CharacterLibraryJson.decode(legacy).active)
        assertEquals(PRIMARY_VARIANT, CharacterLibraryJson.decode(legacy).activeVariants.activeId)
    }
}
