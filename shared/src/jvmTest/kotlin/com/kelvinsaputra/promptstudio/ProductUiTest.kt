package com.kelvinsaputra.promptstudio

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.*
import java.io.File
import javax.imageio.ImageIO
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class ProductUiTest {
    @Test fun mobileProjectVariantPresetAndGenerateJourney() = journey(360)
    @Test fun desktopProjectVariantPresetAndGenerateJourney() = journey(1280)

    private fun journey(width: Int) = runDesktopComposeUiTest(width = width, height = 900) {
        val rule = this
        val editor = EditorViewModel()
        rule.setContent {
            val state by editor.state.collectAsState()
            var home by remember { mutableStateOf(true) }
            MaterialTheme { Surface(Modifier.requiredSize(width.dp, 900.dp)) {
                if (home) ProjectsScreen(state, editor) { home = false }
                else EditorScreen(state, {}, {}, editor::dismissMessage, editor::retrySave,
                    editor::selectModule, editor::setMode, editor::setProject, editor::setCostumeLocks,
                    editor::setPoseLocks, editor::setVariationLocks, editor::randomizeCostume, editor::randomizePose,
                    editor::randomizeUnlocked, editor::resetCostume, editor::resetPose, editor::resetModule,
                    editor::selectCharacter, editor::newCharacter, editor::renameCharacter, editor::duplicateCharacter, editor::deleteCharacter,
                    generationContent = { Text("Generation surface") }, onProjects = { home = true },
                    variantContent = { VariantBar(state, editor) }, presetContent = { PresetLibrary(state, editor) })
            } }
        }
        rule.onNodeWithText("New project").performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("Journey")
        rule.onNodeWithText("Create").performClick()
        rule.onNodeWithText("Relaxed").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(VisualPose.RELAXED, editor.state.value.project.visualAssembly.posePreset) }
        rule.onNodeWithText("Save pose").performScrollTo().performClick()
        rule.onNode(hasSetTextAction()).performTextInput("My relaxed pose")
        rule.onNodeWithText("Save", substring = false).performClick()
        rule.onNodeWithText("+ Variant").performClick()
        rule.onNodeWithText("Create").performClick()
        rule.runOnIdle { assertEquals("16:9", editor.state.value.project.output.aspectRatio) }
        rule.onNodeWithText("Action", substring = false).performScrollTo().performClick()
        rule.onNodeWithText("Compiled prompt →").performScrollTo()
        rule.onNodeWithContentDescription("Apply Pose preset My relaxed pose").performScrollTo().assertIsDisplayed().performClick()
        rule.runOnIdle {
            assertEquals(VisualPose.RELAXED, editor.state.value.project.visualAssembly.posePreset)
            assertEquals("16:9", editor.state.value.project.output.aspectRatio)
        }
        if (width < 600) rule.onNodeWithText("Desktop 16:9 ▾").performClick()
        rule.onNodeWithText("Phone 9:16").performClick()
        rule.runOnIdle { assertEquals("9:16", editor.state.value.project.output.aspectRatio) }
        if (width < 600) rule.onNodeWithText("Phone 9:16 ▾").performClick()
        rule.onNodeWithText("Desktop 16:9").performClick()
        rule.runOnIdle { assertEquals("16:9", editor.state.value.project.output.aspectRatio) }
        rule.onNodeWithText("Start with a shape").performScrollTo()
        rule.onRoot().captureToImage().let { ImageIO.write(it.toAwtImage(), "png", File("/tmp", "product-editor-$width.png")) }
        rule.onNodeWithText("Generate", substring = false).performClick()
        rule.onNodeWithText("Generation surface").assertIsDisplayed()
        rule.onNodeWithText("← Back to editor").performClick()
        rule.onNodeWithText("← Projects").performClick()
        rule.onNodeWithText("Journey", substring = false).assertExists()
        rule.onRoot().captureToImage().let { ImageIO.write(it.toAwtImage(), "png", File("/tmp", "product-projects-$width.png")) }
    }
}
