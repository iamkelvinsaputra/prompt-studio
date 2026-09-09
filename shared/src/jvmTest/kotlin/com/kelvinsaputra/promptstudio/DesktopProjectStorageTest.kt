package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.EditorViewModel
import com.kelvinsaputra.promptstudio.persistence.ProjectJson
import com.kelvinsaputra.promptstudio.persistence.CharacterLibraryJson
import com.kelvinsaputra.promptstudio.platform.DesktopProjectStorage
import java.nio.file.Files
import kotlin.test.*

class DesktopProjectStorageTest {
    @Test fun fileSurvivesNewStorageAndEditorInstances() {
        val directory = Files.createTempDirectory("prompt-studio-test").toFile()
        try {
            val file = directory.resolve("nested/current-project.json")
            val first = EditorViewModel(storage = DesktopProjectStorage(file))
            first.setPose(PoseConfiguration(energy = Energy.POISED))
            first.setCostume(CostumeConfiguration(customNotes = "Portable costume"))
            val second = EditorViewModel(storage = DesktopProjectStorage(file))
            assertEquals(first.state.value.project, second.state.value.project)
            assertEquals(first.state.value.compiledPrompt, second.state.value.compiledPrompt)
            assertEquals(first.state.value.project, CharacterLibraryJson.decode(file.readText()).active)
            assertEquals(listOf("current-project.json"), file.parentFile.listFiles()!!.map { it.name })
        } finally { directory.deleteRecursively() }
    }
}
