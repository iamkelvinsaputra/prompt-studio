package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.*
import com.kelvinsaputra.promptstudio.persistence.*
import kotlin.random.Random
import kotlin.test.*

private class MemoryProjectStorage(var text: String? = null) : ProjectStorage {
    var writes = 0
    var failWrites = false
    override fun read() = text
    override fun write(json: String) {
        if (failWrites) error("Disk full")
        text = json
        writes++
    }
}

class ProjectPersistenceTest {
    @Test fun editsSaveAndNewEditorRestoresProjectButNotLocks() {
        val storage = MemoryProjectStorage()
        val editor = EditorViewModel(EditorRandomizer(Random(4)), storage)
        editor.setCostumeLocks(setOf(CostumeField.Footwear))
        editor.setPoseLocks(setOf(PoseField.Gaze))
        editor.selectModule(EditorModule.Pose)
        assertEquals(0, storage.writes)
        editor.randomizeCostume()
        editor.randomizePose()
        editor.setOutput(editor.state.value.project.output.copy(type = OutputType.DESKTOP))
        val restored = EditorViewModel(storage = storage).state.value
        assertEquals(editor.state.value.project, restored.project)
        assertEquals(editor.state.value.compiledPrompt, restored.compiledPrompt)
        assertTrue(restored.costumeLocks.isEmpty())
        assertTrue(restored.poseLocks.isEmpty())
        assertEquals(3, storage.writes)
    }

    @Test fun invalidImportDoesNotReplaceOrSaveCurrentProject() {
        val storage = MemoryProjectStorage()
        val editor = EditorViewModel(storage = storage)
        editor.setPose(PoseConfiguration(energy = Energy.PLAYFUL))
        val before = editor.state.value.project
        val saved = storage.text
        editor.importProject("{broken")
        assertEquals(before, editor.state.value.project)
        assertEquals(saved, storage.text)
        assertEquals(1, storage.writes)
        assertContains(editor.state.value.message.orEmpty(), "Import failed")
        editor.importProject(ProjectJson.encode(before).replace("\"version\": 1", "\"version\": 3"))
        assertEquals(before, editor.state.value.project)
        assertEquals(saved, storage.text)
    }

    @Test fun validImportReplacesProjectUpdatesPromptAndPersistsImmediately() {
        val storage = MemoryProjectStorage()
        val editor = EditorViewModel(storage = storage)
        editor.setCostumeLocks(setOf(CostumeField.Silhouette))
        val imported = CharacterProject(costume = CostumeConfiguration(footwear = Footwear.SANDALS))
        editor.importProject(ProjectJson.encode(imported))
        assertEquals(imported, editor.state.value.project)
        assertEquals(imported, ProjectJson.decode(storage.text!!))
        assertContains(editor.state.value.compiledPrompt.text, "- footwear: sandals")
        assertEquals(setOf(CostumeField.Silhouette), editor.state.value.costumeLocks)
        assertEquals(imported, ProjectJson.decode(editor.exportProject()))
    }

    @Test fun missingAndCorruptSavesFallBackWithoutOverwritingTheFile() {
        val missing = MemoryProjectStorage()
        assertEquals(CharacterProject(), EditorViewModel(storage = missing).state.value.project)
        assertEquals(0, missing.writes)
        for (text in listOf("corrupt", "{\"version\":9}")) {
            val storage = MemoryProjectStorage(text)
            val state = EditorViewModel(storage = storage).state.value
            assertEquals(CharacterProject(), state.project)
            assertNotNull(state.message)
            assertEquals(text, storage.text)
            assertEquals(0, storage.writes)
        }
    }

    @Test fun saveFailureKeepsEditsAndCanBeRetried() {
        val storage = MemoryProjectStorage().apply { failWrites = true }
        val editor = EditorViewModel(storage = storage)
        editor.setCostume(CostumeConfiguration(footwear = Footwear.SANDALS))
        assertEquals(Footwear.SANDALS, editor.state.value.project.costume.footwear)
        assertNotNull(editor.state.value.saveError)
        assertNull(storage.text)
        storage.failWrites = false
        editor.retrySave()
        assertNull(editor.state.value.saveError)
        assertEquals(editor.state.value.project, ProjectJson.decode(storage.text!!))
    }

    @Test fun unchangedEditsDoNotWriteAndResetIsSaved() {
        val storage = MemoryProjectStorage()
        val editor = EditorViewModel(storage = storage)
        editor.setCostume(editor.state.value.project.costume)
        assertEquals(0, storage.writes)
        editor.setCostume(CostumeConfiguration(customNotes = "Lining"))
        editor.resetCostume()
        assertEquals(2, storage.writes)
        assertEquals(CharacterProject(), ProjectJson.decode(storage.text!!))
    }
}
