package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.*
import kotlin.test.*

class EditorStateTest {
    @Test fun editingOneModulePreservesOthersAndUpdatesPrompt() {
        val editor = EditorViewModel()
        val initial = editor.state.value
        editor.setCostume(initial.project.costume.copy(footwear = Footwear.SANDALS))
        val updated = editor.state.value
        assertEquals(initial.project.pose, updated.project.pose)
        assertEquals(initial.project.output, updated.project.output)
        assertEquals(initial.project.style, updated.project.style)
        assertContains(updated.compiledPrompt.text, "- footwear: sandals")
        assertNotEquals(initial.compiledPrompt, updated.compiledPrompt)
        editor.selectModule(EditorModule.Prompt)
        assertEquals(updated.project, editor.state.value.project)
    }

    @Test fun switchingOutputKeepsCustomSettingsAndDerivesPresetRatio() {
        val editor = EditorViewModel()
        val custom = OutputConfiguration(type = OutputType.CUSTOM, customAspectRatio = "7:4")
        editor.setOutput(custom)
        editor.setOutput(custom.copy(type = OutputType.PHONE))
        assertContains(editor.state.value.compiledPrompt.text, "smartphone wallpaper in 9:16")
        editor.setOutput(editor.state.value.project.output.copy(type = OutputType.CUSTOM))
        assertEquals("7:4", editor.state.value.project.output.aspectRatio)
    }
}
