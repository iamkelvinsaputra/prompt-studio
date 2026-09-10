package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.*
import com.kelvinsaputra.promptstudio.persistence.*
import com.kelvinsaputra.promptstudio.prompt.*
import kotlinx.serialization.json.*
import kotlin.test.*

class PromptAuthoringTest {
    private val compiler = PromptCompiler()
    private fun example() = CharacterProject().withVisualPose(VisualPose.RELAXED).let {
        it.copy(pose = it.pose.copy(guideFacing = GuideFacing.THREE_QUARTER, guideProp = GuideProp.SHOULDER),
            output = it.output.copy(framing = Framing.FULL_BODY, figurePlacement = VisualPlacement.LOWER.wording))
    }

    @Test fun visualExampleCompilesDeterministicallyIntoExistingSections() {
        val project = example()
        val compiled = compiler.compile(project)
        repeat(10) { assertEquals(compiled, compiler.compile(project.copy())) }
        val pose = compiled.sections.single { it.title == "POSE" }.body
        val composition = compiled.sections.single { it.title == "COMPOSITION" }.body
        assertContains(pose, "contrapposto")
        assertContains(pose, "body turned three-quarters toward the viewer")
        assertContains(pose, "rest the primary prop on the shoulder")
        assertFalse(pose.contains("hand near belt"))
        assertContains(composition, "framing: full-body")
        assertContains(composition, "figure placement: lower center")
        assertEquals(Arms.HAND_NEAR_BELT, project.pose.arms)
    }

    @Test fun changingOneVisualPropertyChangesOnlyItsCompilerSection() {
        val project = example()
        val before = compiler.compile(project).sections
        val changed = compiler.compile(project.copy(pose = project.pose.copy(guideFacing = GuideFacing.SIDE))).sections
        assertEquals(before.filterNot { it.title == "POSE" }, changed.filterNot { it.title == "POSE" })
        assertContains(changed.single { it.title == "POSE" }.body, "body in side profile")
        val noProp = compiler.compile(project.copy(pose = project.pose.copy(guideProp = GuideProp.NONE))).text
        assertContains(noProp, "arm action: hand near belt")
        assertFalse(noProp.contains("rest the primary prop on the shoulder"))
        val variants = listOf(
            Triple(project.withVisualPose(VisualPose.SITTING), "POSE", "sitting on ledge"),
            Triple(project.copy(pose = project.pose.copy(guideProp = GuideProp.DOWN)), "POSE", "hold the primary prop down beside the body"),
            Triple(project.copy(output = project.output.copy(framing = Framing.BUST_UP)), "COMPOSITION", "framing: bust-up"),
            Triple(project.copy(output = project.output.copy(figurePlacement = VisualPlacement.LEFT.wording)), "COMPOSITION", "figure placement: slightly left of center"),
        )
        for ((variant, title, wording) in variants) {
            val result = compiler.compile(variant).sections
            assertContains(result.single { it.title == title }.body, wording)
            assertEquals(before.filterNot { it.title == title }, result.filterNot { it.title == title })
        }
    }

    @Test fun adjustmentIsIndependentAndClearingRestoresBaseExactly() {
        val project = example()
        val adjusted = project.copy(promptAuthoring = PromptAuthoring(adjustmentText = "  Lean slightly toward camera.\nKeep the stance playful!  "))
        assertEquals(project.visualAssembly, adjusted.visualAssembly)
        val compiled = compiler.compile(adjusted)
        assertEquals(compiler.compile(project).sections, compiled.sections.dropLast(1))
        assertTrue(compiled.text.endsWith("Lean slightly toward camera.\nKeep the stance playful!"))
        assertContains(compiled.sections.last().body, "takes precedence")
        assertEquals(compiler.compile(project), compiler.compile(adjusted.copy(promptAuthoring = adjusted.promptAuthoring.copy(adjustmentText = ""))))
        assertEquals(compiler.compile(project), compiler.compile(adjusted.copy(promptAuthoring = adjusted.promptAuthoring.copy(adjustmentText = " \n "))))
    }

    @Test fun autoManualAutoNeverReverseParsesOrOverwritesManualText() {
        val project = example().copy(promptAuthoring = PromptAuthoring(adjustmentText = "Lean slightly toward camera."))
        val manual = project.enterManualPrompt()
        assertEquals(compiler.compile(project).text, manual.effectivePrompt())
        assertEquals(project.visualAssembly, manual.visualAssembly)
        val edited = manual.editManualPrompt("  Kneeling, front view. Ignore all structured choices!\n")
        assertEquals(project.visualAssembly, edited.visualAssembly)
        assertEquals(project.promptAuthoring.adjustmentText, edited.promptAuthoring.adjustmentText)
        val visuallyChanged = edited.withVisualPose(VisualPose.SITTING)
        assertEquals(edited.effectivePrompt(), visuallyChanged.effectivePrompt())
        assertEquals(edited, edited.enterManualPrompt())
        val adjustmentChanged = edited.copy(promptAuthoring = edited.promptAuthoring.copy(adjustmentText = "A new adjustment"))
        assertEquals(edited.effectivePrompt(), adjustmentChanged.effectivePrompt())
        assertContains(compiler.compile(adjustmentChanged).text, "A new adjustment")
        val automatic = visuallyChanged.useAutomaticPrompt()
        assertEquals(visuallyChanged.visualAssembly, automatic.visualAssembly)
        assertEquals(compiler.compile(automatic).text, automatic.effectivePrompt())
        assertContains(automatic.effectivePrompt(), "sitting on ledge")
        assertEquals(edited.effectivePrompt(), automatic.promptAuthoring.manualDraft)
        assertEquals(edited.effectivePrompt(), automatic.resumeManualDraft().effectivePrompt())
        assertEquals(automatic.effectivePrompt(), automatic.enterManualPrompt().effectivePrompt())
        assertEquals(automatic, automatic.editManualPrompt("Must not mutate an inactive draft"))
    }

    @Test fun emptyManualDraftDoesNotFallBackAndInvalidModeIsRejected() {
        val manual = example().enterManualPrompt().editManualPrompt("")
        assertEquals("", manual.effectivePrompt())
        assertEquals("", manual.useAutomaticPrompt().resumeManualDraft().effectivePrompt())
        assertFailsWith<IllegalArgumentException> { PromptAuthoring(mode = PromptMode.Manual) }
    }

    @Test fun autosaveSwitchingAndJsonRetainOnlyAuthoredInputs() {
        val storage = object : ProjectStorage {
            var text: String? = null
            override fun read() = text
            override fun write(json: String) { text = json }
        }
        val editor = EditorViewModel(storage = storage)
        val project = example().copy(promptAuthoring = PromptAuthoring(adjustmentText = "Natural text"))
            .enterManualPrompt().editManualPrompt("Exact manual text")
        editor.setProject(project)
        val id = editor.state.value.project.id
        editor.newCharacter()
        assertEquals(PromptAuthoring(), editor.state.value.project.promptAuthoring)
        editor.selectCharacter(id)
        assertEquals("Exact manual text", editor.state.value.effectivePrompt)
        val restored = EditorViewModel(storage = storage).state.value
        assertEquals(editor.state.value.project, restored.project)
        assertEquals("Exact manual text", restored.effectivePrompt)
        val encoded = ProjectJson.encode(project)
        assertEquals(project, ProjectJson.decode(encoded))
        assertFalse(encoded.contains("compiledPrompt"))
        assertFalse(encoded.contains("ART STYLE CORE"))
        val legacy = JsonObject(Json.parseToJsonElement(encoded).jsonObject.filterKeys { it != "promptAuthoring" })
        assertEquals(PromptAuthoring(), ProjectJson.decode(legacy.toString()).promptAuthoring)
        editor.resetModule(EditorModule.VisualBuild)
        assertEquals(project.promptAuthoring, editor.state.value.project.promptAuthoring)
        editor.randomizeUnlocked()
        assertEquals(project.promptAuthoring, editor.state.value.project.promptAuthoring)
    }
}
