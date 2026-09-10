package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.*
import com.kelvinsaputra.promptstudio.persistence.ProjectJson
import com.kelvinsaputra.promptstudio.prompt.PromptCompiler
import kotlin.test.*

class VisualAssemblyTest {
    @Test fun presetsKeepAuthoredContentAndReplaceIncompatibleMechanics() {
        val original = CharacterProject(pose = PoseConfiguration(customNotes = "Keep a quiet mood"))
        val seated = original.withVisualPose(VisualPose.SITTING)
        assertEquals(Weight.SEATED_WEIGHT, seated.pose.weight)
        assertEquals(VisualPose.SITTING, seated.visualAssembly.posePreset)
        assertEquals(original.pose.customNotes, seated.pose.customNotes)
        assertEquals(original.pose.head, seated.pose.head)
        assertEquals(original.costume, seated.costume)
        assertEquals(original.output, seated.output)
        assertContains(PromptCompiler().compile(seated).text, "sitting on ledge")
        assertFalse(seated.pose.legAction.contains("front leg relaxed"))
    }

    @Test fun guideChoicesRoundTripAndChangeCompiledPrompt() {
        val original = CharacterProject()
        val guide = original.copy(pose = original.pose.copy(guideFacing = GuideFacing.SIDE, guideProp = GuideProp.SHOULDER))
        assertEquals(guide, ProjectJson.decode(ProjectJson.encode(guide)))
        assertNotEquals(PromptCompiler().compile(original), PromptCompiler().compile(guide))
        assertContains(guide.visualAssembly.summary, "Side")
        assertContains(guide.visualAssembly.summary, "On shoulder")
    }

    @Test fun oldJsonDefaultsGuideAndCustomChoicesRemainExplicit() {
        val json = ProjectJson.encode(CharacterProject()).replace("    \"guideFacing\": \"FRONT\",\n", "").replace("    \"guideProp\": \"NONE\",\n", "")
        val project = ProjectJson.decode(json)
        assertEquals(GuideFacing.FRONT, project.pose.guideFacing)
        val custom = project.copy(pose = project.pose.copy(basePose = BasePose.KNEELING), output = project.output.copy(figurePlacement = "near a distant doorway"))
        assertNull(custom.visualAssembly.posePreset)
        assertNull(custom.visualAssembly.placementPreset)
        assertTrue(custom.visualAssembly.hasApproximation)
        assertContains(custom.visualAssembly.summary, "kneeling")
        assertContains(custom.visualAssembly.summary, "near a distant doorway")
    }

    @Test fun navigationAndCharacterSwitchesUseProjectAsOnlySourceOfTruth() {
        val editor = EditorViewModel()
        assertEquals(EditorModule.VisualBuild, editor.state.value.module)
        val id = editor.state.value.project.id
        editor.setProject(editor.state.value.project.withVisualPose(VisualPose.ACTION))
        editor.newCharacter()
        editor.selectCharacter(id)
        assertEquals(VisualPose.ACTION, editor.state.value.project.visualAssembly.posePreset)
        editor.setOutput(editor.state.value.project.output.copy(framing = Framing.BUST_UP))
        assertEquals(Framing.BUST_UP, editor.state.value.project.visualAssembly.framing)
        val costume = editor.state.value.project.costume
        editor.resetModule(EditorModule.VisualBuild)
        assertEquals(VisualPose.NEUTRAL, editor.state.value.project.visualAssembly.posePreset)
        assertEquals(costume, editor.state.value.project.costume)
    }
}
