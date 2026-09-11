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
import com.kelvinsaputra.promptstudio.feature.studio.*
import com.kelvinsaputra.promptstudio.prompt.*
import java.io.File
import javax.imageio.ImageIO
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class ProductUiTest {
    @Test fun phoneStudioJourney() = journey(390, 844)
    @Test fun desktopStudioJourney() = journey(1440, 1000)
    @Test fun tabletStudioJourney() = journey(850, 1000)

    private fun journey(width: Int, height: Int) = runDesktopComposeUiTest(width = width, height = height) {
        val editor = EditorViewModel()
        val reports = File("build/reports/studio").apply { mkdirs() }
        fun capture(name: String) { onRoot().captureToImage().let { ImageIO.write(it.toAwtImage(), "png", File(reports, "$name-$width.png")) } }
        fun category(value: StudioCategory) { runOnIdle { editor.selectModule(value.modules.first()) }; waitForIdle() }
        setContent {
            val state by editor.state.collectAsState()
            var home by remember { mutableStateOf(true) }
            StudioTheme { Surface(Modifier.requiredSize(width.dp, height.dp)) {
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
        capture("projects")
        onNodeWithText("New project").performClick()
        onNode(hasSetTextAction()).performTextReplacement("Character study")
        onNodeWithText("Create").performClick()
        onNodeWithText("Describe your character").performTextReplacement("An adult traveling cartographer")
        runOnIdle { assertContains(editor.state.value.effectivePrompt, "traveling cartographer") }
        capture("subject")
        // Exercise the real category navigator before switching with the state holder for deeper checks.
        onAllNodesWithText("Pose & acting", substring = false)[0].performScrollTo().performClick()
        onNodeWithContentDescription("Relaxed", substring = false).performScrollTo().performClick()
        runOnIdle { assertEquals(BasePose.CONTRAPPOSTO, editor.state.value.project.pose.basePose) }
        capture("pose")
        onNodeWithText("Browse all 25").performScrollTo().performClick()
        onNodeWithText("Search Gesture").performTextInput("casual")
        onNodeWithContentDescription("Relaxed", substring = false).assertExists()
        onNodeWithText("Search Gesture").performTextReplacement("kneeling")
        onNodeWithContentDescription("Kneeling", substring = false).performScrollTo().performClick()
        runOnIdle { assertEquals(BasePose.KNEELING, editor.state.value.project.pose.basePose) }
        onNodeWithText("Search Gesture").performTextReplacement("")
        onNodeWithText("Show less").performScrollTo().performClick()
        onNodeWithContentDescription("Sitting", substring = false).performScrollTo().performClick()
        onNodeWithText("Custom pose details").performScrollTo().performTextInput("holding a folded map")
        runOnIdle { assertContains(editor.state.value.effectivePrompt, "holding a folded map") }
        category(StudioCategory.Camera)
        onNodeWithContentDescription("Bust", substring = false).performScrollTo().performClick()
        onNodeWithContentDescription("Left", substring = false).performScrollTo().performClick()
        onNodeWithContentDescription("Diagonal", substring = false).performScrollTo().performClick()
        onNodeWithText("Camera angle, perspective & custom composition").performScrollTo().performTextInput("camera below the subject, looking upward")
        runOnIdle {
            assertEquals(Framing.BUST_UP, editor.state.value.project.output.framing)
            assertContains(editor.state.value.effectivePrompt, "camera below the subject")
        }
        capture("camera")
        category(StudioCategory.Appearance)
        onNodeWithText("Custom outfit details").performScrollTo().performTextInput("linen coat with copper clasps")
        category(StudioCategory.Style)
        onNodeWithContentDescription("Painterly", substring = false).performScrollTo().performClick()
        onNodeWithText("Custom style…").performScrollTo().performClick()
        onNodeWithText("Write your own style").performScrollTo().performClick()
        onNode(hasSetTextAction() and hasText("Style description")).performScrollTo().performTextReplacement("Graphite on warm paper.")
        runOnIdle { assertEquals("Graphite on warm paper.", editor.state.value.project.effectiveArtStyleCore()) }
        capture("style")
        category(StudioCategory.Pose)
        if (width < 760) onNodeWithText("Presets & variants").performScrollTo().performClick() else onNodeWithText("Presets & variants").performClick()
        onNodeWithText("Save complete").performScrollTo().performClick()
        onNode(hasSetTextAction() and hasText("Name")).performTextInput("Mapmaker")
        onNodeWithText("Save", substring = false).performClick()
        runOnIdle { assertTrue(editor.state.value.library.presets.single().value is VisualPresetValue.Complete) }
        onNodeWithText("+ Variant").performScrollTo().performClick()
        onNodeWithText("Create").performClick()
        runOnIdle { assertEquals("16:9", editor.state.value.project.output.aspectRatio) }
        onNodeWithText("Reset category").performScrollTo().performClick()
        runOnIdle {
            assertEquals(BasePose.RELAXED_STANDING, editor.state.value.project.pose.basePose)
            assertEquals("Graphite on warm paper.", editor.state.value.project.effectiveArtStyleCore())
        }
        if (width < 1180) {
            onNodeWithText("Inspect prompt").performClick()
            onNodeWithText("Compiled", substring = false).performClick()
            onNodeWithText(editor.state.value.effectivePrompt, substring = false).assertExists()
            capture("inspector")
        }
        onNodeWithText(if (width < 600) "Copy" else "Copy prompt", substring = false).performClick()
        waitUntil(timeoutMillis = 5000) {
            runCatching { java.awt.Toolkit.getDefaultToolkit().systemClipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor) == editor.state.value.effectivePrompt }.getOrDefault(false)
        }
        onNodeWithText("Generate", substring = false).performClick()
        onNodeWithText("Generation surface").assertIsDisplayed()
        onNodeWithText("Back to studio").performClick()
        capture("final")
    }
}
