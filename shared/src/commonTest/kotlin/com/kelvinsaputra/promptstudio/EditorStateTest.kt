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

    @Test fun clearingCostumeFieldAndChangingPosePreserveOtherEdits() {
        val editor = EditorViewModel()
        editor.setCostume(editor.state.value.project.costume.copy(footwear = null, customNotes = "Canvas lining"))
        editor.selectModule(EditorModule.Pose)
        editor.setPose(editor.state.value.project.pose.copy(energy = Energy.POISED))
        editor.selectModule(EditorModule.Output)
        editor.setOutput(editor.state.value.project.output.copy(type = OutputType.DESKTOP))
        editor.selectModule(EditorModule.Prompt)
        val state = editor.state.value
        assertNull(state.project.costume.footwear)
        assertEquals("Canvas lining", state.project.costume.customNotes)
        assertEquals(Energy.POISED, state.project.pose.energy)
        assertFalse(state.compiledPrompt.text.contains("- footwear:"))
        assertContains(state.compiledPrompt.text, "- overall energy: poised")
        assertContains(state.compiledPrompt.text, "Create a desktop wallpaper in 16:9.")
    }
}
