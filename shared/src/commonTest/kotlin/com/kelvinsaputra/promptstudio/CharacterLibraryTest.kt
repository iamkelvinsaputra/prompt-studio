package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.*
import com.kelvinsaputra.promptstudio.persistence.*
import kotlin.random.Random
import kotlin.test.*

private class MemoryLibraryStorage(var text: String? = null) : ProjectStorage {
    var writes = 0
    override fun read(): String? = text
    override fun write(json: String) { text = json; writes++ }
}

class CharacterLibraryTest {
    @Test fun quickAndAdvancedAreFilteredViewsOfTheSameCharacter() {
        val editor = EditorViewModel()
        editor.setProject(editor.state.value.project.copy(coreVisualThesis = "A precise test thesis"))
        val before = editor.state.value.project
        editor.setMode(EditorMode.Advanced)
        assertEquals(before, editor.state.value.project)
        editor.selectModule(EditorModule.Contradiction)
        editor.setMode(EditorMode.Quick)
        assertEquals(EditorModule.Output, editor.state.value.module)
        assertEquals(before, editor.state.value.project)
    }

    @Test fun newRenameDuplicateAndDeleteKeepIndependentProjectsAndActiveSelection() {
        val editor = EditorViewModel()
        editor.renameCharacter("Original")
        editor.setProject(editor.state.value.project.copy(face = FaceConfiguration(eyeShape = "sharp eyes")))
        editor.duplicateCharacter()
        assertEquals(2, editor.state.value.library.characters.size)
        assertEquals("Original Copy", editor.state.value.project.name)
        editor.setProject(editor.state.value.project.copy(face = FaceConfiguration(eyeShape = "round eyes")))
        val original = editor.state.value.library.characters.first { it.name == "Original" }
        assertEquals("sharp eyes", original.face.eyeShape)
        editor.deleteCharacter()
        assertEquals(1, editor.state.value.library.characters.size)
        assertEquals("Original", editor.state.value.project.name)
    }

    @Test fun autosaveRestoresLibraryAndMigratesOldSingleProjectStorage() {
        val storage = MemoryLibraryStorage()
        val editor = EditorViewModel(storage = storage)
        editor.renameCharacter("First")
        editor.newCharacter()
        editor.renameCharacter("Second")
        val restored = EditorViewModel(storage = storage)
        assertEquals(listOf("First", "Second"), restored.state.value.library.characters.map { it.name })
        assertEquals("Second", restored.state.value.project.name)

        val v0Storage = MemoryLibraryStorage(ProjectJson.encode(CharacterProject(subject = "Old project")))
        val migrated = EditorViewModel(storage = v0Storage)
        assertEquals("Old project", migrated.state.value.project.subject)
        assertEquals(1, migrated.state.value.library.characters.size)
        assertEquals(0, v0Storage.writes)
    }

    @Test fun randomizeUnlockedHonorsCoreLocksAndNeverChangesAuthoredStrings() {
        val editor = EditorViewModel(EditorRandomizer(Random(44)))
        val seeded = editor.state.value.project.copy(
            identity = IdentityConfiguration(AgeBand.ADULT, "mid-20s", GenderPresentation.FEMALE, BodyType.ATHLETIC, "mischievous"),
            hair = HairConfiguration("black", "blue roots", HairLength.LONG, HairStyle.WAVY, "windblown", "red pin"),
            powerSignature = PowerSignatureConfiguration(symbolicMeaning = "grief", manifestation = "ink rain"),
        )
        editor.setProject(seeded)
        editor.setVariationLocks(setOf(VariationField.Identity, VariationField.Hair, VariationField.Power, VariationField.Face))
        editor.randomizeUnlocked()
        val varied = editor.state.value.project
        assertEquals(seeded.identity, varied.identity)
        assertEquals(seeded.hair, varied.hair)
        assertEquals(seeded.powerSignature, varied.powerSignature)
        assertEquals(seeded.powerSignature.symbolicMeaning, varied.powerSignature.symbolicMeaning)
        assertNotEquals(seeded.expression.preset, varied.expression.preset)
        assertNotEquals(seeded.pose, varied.pose)
    }
}
