package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.*
import kotlin.random.Random
import kotlin.test.*

class ExplorationStateTest {
    @Test fun resetCostumeRestoresDefaultsAndPreservesLocksAndOtherModules() {
        val editor = EditorViewModel(EditorRandomizer(Random(12)))
        editor.randomizeCostume()
        editor.randomizePose()
        editor.setCostume(editor.state.value.project.costume.copy(customNotes = "Authored note"))
        editor.setCostumeLocks(CostumeField.entries.toSet())
        editor.selectModule(EditorModule.Costume)
        val before = editor.state.value
        editor.resetCostume()
        val after = editor.state.value
        assertEquals(CostumeConfiguration(), after.project.costume)
        assertEquals(before.costumeLocks, after.costumeLocks)
        assertEquals(before.project.pose, after.project.pose)
        assertEquals(before.project.output, after.project.output)
        assertEquals(before.project.subject, after.project.subject)
        assertEquals(before.project.style, after.project.style)
        assertEquals(before.module, after.module)
    }
    @Test fun resetPoseRestoresDefaultsAndPreservesLocksAndOtherModules() {
        val editor = EditorViewModel(EditorRandomizer(Random(12)))
        editor.randomizeCostume()
        editor.randomizePose()
        editor.setPose(editor.state.value.project.pose.copy(customNotes = "Authored note"))
        editor.setPoseLocks(PoseField.entries.toSet())
        editor.selectModule(EditorModule.Pose)
        val before = editor.state.value
        editor.resetPose()
        val after = editor.state.value
        assertEquals(PoseConfiguration(), after.project.pose)
        assertEquals(before.poseLocks, after.poseLocks)
        assertEquals(before.project.costume, after.project.costume)
        assertEquals(before.project.output, after.project.output)
        assertEquals(before.project.subject, after.project.subject)
        assertEquals(before.project.style, after.project.style)
        assertEquals(before.module, after.module)
    }
    @Test fun locksNeverChangeProjectOrPromptAndRandomizationStaysInItsModule() {
        val editor = EditorViewModel(EditorRandomizer(Random(33)))
        val original = editor.state.value
        editor.setCostumeLocks(setOf(CostumeField.Footwear))
        editor.setPoseLocks(setOf(PoseField.Energy))
        assertEquals(original.project, editor.state.value.project)
        assertEquals(original.compiledPrompt, editor.state.value.compiledPrompt)
        editor.randomizeCostume()
        val costumeChanged = editor.state.value
        assertEquals(original.project.costume.footwear, costumeChanged.project.costume.footwear)
        assertEquals(original.project.pose, costumeChanged.project.pose)
        editor.randomizePose()
        val poseChanged = editor.state.value
        assertEquals(costumeChanged.project.costume, poseChanged.project.costume)
        assertEquals(original.project.pose.energy, poseChanged.project.pose.energy)
        assertEquals(original.project.output, poseChanged.project.output)
    }
}
