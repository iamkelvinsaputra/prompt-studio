package com.kelvinsaputra.promptstudio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toAwtImage
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.prompt.*
import com.kelvinsaputra.promptstudio.feature.editor.VisualAssemblyEditor
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class VisualAssemblyUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun wideEditorRendersEveryPose() {
        var project by mutableStateOf(CharacterProject())
        rule.setContent {
            MaterialTheme { Surface {
                Column(Modifier.width(900.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    VisualAssemblyEditor(project) { project = it }
                }
            } }
        }
        for (pose in VisualPose.entries) {
            rule.runOnIdle { project = project.withVisualPose(pose) }
            rule.onNodeWithText(pose.label).assertExists()
            rule.onRoot().captureToImage().let { image ->
                ImageIO.write(image.toAwtImage(), "png", File(System.getProperty("java.io.tmpdir"), "visual-assembly-${pose.name}.png"))
            }
        }
    }

    @Test fun adjustmentAndManualControlsKeepOwnershipExplicit() {
        var project by mutableStateOf(CharacterProject())
        rule.setContent {
            MaterialTheme { Surface {
                Column(Modifier.width(360.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    VisualAssemblyEditor(project) { project = it }
                }
            } }
        }
        rule.onNodeWithText("Manual Prompt").assertDoesNotExist()
        rule.onNodeWithText("Describe adjustment").performScrollTo().performClick()
        rule.onNode(hasSetTextAction() and hasText("Describe adjustment"))
            .performScrollTo().performTextInput("Lean slightly toward camera.")
        val visualBefore = project.visualAssembly
        rule.onNodeWithText("Done").performScrollTo().performClick()
        rule.onNodeWithText("Describe adjustment · Active").assertExists()
        rule.onNodeWithText("Advanced").performScrollTo().performClick()
        rule.onNodeWithText("Manual Prompt").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(PromptCompiler().compile(project).text, project.effectivePrompt())
            assertEquals(visualBefore, project.visualAssembly)
        }
        rule.onNode(hasSetTextAction() and hasText("Manual prompt text"))
            .performScrollTo().performTextReplacement("My exact manual prompt")
        rule.onRoot().captureToImage().let { image ->
            ImageIO.write(image.toAwtImage(), "png", File(System.getProperty("java.io.tmpdir"), "visual-assembly-manual.png"))
        }
        rule.onNodeWithText("Sitting").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("My exact manual prompt", project.effectivePrompt()) }
        rule.onNodeWithText("Return to automatic").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(PromptMode.Automatic, project.promptAuthoring.mode)
            assertEquals("My exact manual prompt", project.promptAuthoring.manualDraft)
            assertEquals(PromptCompiler().compile(project).text, project.effectivePrompt())
        }
        rule.onNodeWithText("Describe adjustment · Active").performScrollTo().performClick()
        rule.onNodeWithText("Clear adjustment").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("", project.promptAuthoring.adjustmentText) }
    }

    @Test fun narrowEditorDisclosesControlsAndUpdatesTheSameProject() {
        var project by mutableStateOf(CharacterProject())
        rule.setContent {
            MaterialTheme { Surface {
                Column(Modifier.width(360.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    VisualAssemblyEditor(project) { project = it }
                }
            } }
        }
        rule.onRoot().captureToImage().let { image ->
            ImageIO.write(image.toAwtImage(), "png", File(System.getProperty("java.io.tmpdir"), "visual-assembly-mobile.png"))
        }
        rule.onNodeWithText("Facing").assertDoesNotExist()
        rule.onNodeWithText("Sitting").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(VisualPose.SITTING, project.visualAssembly.posePreset) }
        rule.onNodeWithText("Edit · Quick adjustments").performScrollTo().performClick()
        rule.onNodeWithText("Side", substring = false).performScrollTo().performClick()
        rule.onNodeWithText("On shoulder", substring = false).performScrollTo().performClick()
        rule.onNodeWithText("Close", substring = false).performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(GuideFacing.SIDE, project.pose.guideFacing)
            assertEquals(GuideProp.SHOULDER, project.pose.guideProp)
            assertEquals(Framing.BUST_UP, project.output.framing)
        }
        rule.onNodeWithText(project.visualAssembly.summary).assertExists()
        rule.onNodeWithContentDescription("Structural silhouette: ${project.visualAssembly.summary}").performScrollTo()
        rule.onRoot().captureToImage().let { image ->
            ImageIO.write(image.toAwtImage(), "png", File(System.getProperty("java.io.tmpdir"), "visual-assembly-close.png"))
        }
    }
}
