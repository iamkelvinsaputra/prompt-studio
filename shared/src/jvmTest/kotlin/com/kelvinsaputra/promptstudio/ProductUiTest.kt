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
        fun click(text: String) { onNodeWithText(text, substring = false).performScrollTo().performClick(); waitForIdle() }
        fun next() { onNodeWithText("Generate").assertDoesNotExist(); onNodeWithText("Continue").assertIsDisplayed().performClick(); waitForIdle() }
        setContent {
            val state by editor.state.collectAsState()
            var home by remember { mutableStateOf(true) }
            StudioTheme { Surface(Modifier.requiredSize(width.dp, height.dp)) {
                if (home) ProjectsScreen(state, editor) { home = false }
                else if (state.project.creationStep != null) GuidedCreation(state, editor, { home = true }) { Text("Generation surface") }
                else EditorScreen(state, {}, {}, editor::dismissMessage, editor::retrySave,
                    editor::selectModule, editor::setMode, editor::setProject, editor::setCostumeLocks,
                    editor::setPoseLocks, editor::setVariationLocks, editor::randomizeCostume, editor::randomizePose,
                    editor::randomizeUnlocked, editor::resetCostume, editor::resetPose, editor::resetModule,
                    editor::selectCharacter, editor::newCharacter, editor::renameCharacter, editor::duplicateCharacter, editor::deleteCharacter,
                    generationContent = { Text("Generation surface") }, onProjects = { home = true },
                    variantContent = { VariantBar(state, editor) }, presetContent = { PresetLibrary(state, editor) })
            } }
        }
        onNodeWithText("New project").performClick()
        onNodeWithText("Character name").performTextReplacement("Aika")
        click("Female"); click("Late-teen"); click("Confident")
        capture("character"); next()
        click("Hair & expression")
        click("Blue-black")
        runOnIdle { assertEquals("Blue-black", editor.state.value.project.hair.baseColor) }
        click("Hair & expression")
        onNodeWithText("Use this outfit").performScrollTo().performClick()
        capture("appearance"); next()
        onNodeWithContentDescription("Relaxed", substring = false).performScrollTo().performClick()
        runOnIdle { assertEquals(BasePose.CONTRAPPOSTO, editor.state.value.project.pose.basePose) }
        capture("pose"); next()
        onNodeWithContentDescription("Bust", substring = false).performScrollTo().performClick()
        onNodeWithContentDescription("Eye level", substring = false).performScrollTo().performClick()
        capture("camera"); next()
        onNodeWithContentDescription("Urban", substring = false).performScrollTo().performClick()
        click("Rooftop")
        capture("environment"); next()
        onNodeWithContentDescription("Front-left", substring = false).performScrollTo().performClick()
        capture("lighting"); next()
        onNodeWithContentDescription("Contemporary Anime", substring = false).performScrollTo().performClick()
        capture("style"); next()
        click("Muted cool")
        onNodeWithContentDescription("Edit Accent color").performScrollTo().performClick()
        onNodeWithText("HEX").performTextReplacement("invalid")
        onNodeWithText("Apply color").assertIsNotEnabled()
        onNodeWithText("HEX").performTextReplacement("#ABCDEF")
        onNodeWithText("Apply color").performClick()
        runOnIdle { assertEquals("#ABCDEF", editor.state.value.project.colorDirection.colors.first { it.role == ColorRole.ACCENT }.hex) }
        capture("color"); next()
        click("Wind"); click("Gentle wind")
        capture("effects"); next()
        onNodeWithText("Generate").assertIsDisplayed()
        capture("review")
        click("Edit character")
        onNodeWithText("Character name").performTextReplacement("Aika Ren")
        onNodeWithText("Return to Review").assertIsDisplayed().performClick()
        onNodeWithText("Generate").performClick()
        onNodeWithText("Generation surface").assertIsDisplayed()
        onNodeWithText("Back to Review").performClick()
        onNodeWithText("Open Studio").performClick()
        runOnIdle { assertNull(editor.state.value.project.creationStep); assertEquals("Aika Ren", editor.state.value.project.characterName) }
        onNodeWithText("Project menu").assertExists()
        if (width < 1180) onNodeWithText("Inspect prompt").performClick()
        onNodeWithText("Prompt", substring = false).performClick()
        onNodeWithText("Copy prompt").assertExists()
        onNodeWithText(editor.state.value.effectivePrompt).assertExists()
        capture("inspector")
        if (width < 1180) onNodeWithText("Back to studio").performClick()
        runOnIdle { editor.selectModule(EditorModule.Pose) }
        if (width < 760) click("Presets & variants") else onNodeWithText("Presets & variants").performClick()
        click("Save preset ▾")
        onNodeWithText("Complete preset").performClick()
        onNode(hasSetTextAction() and hasText("Name")).performTextInput("Rooftop")
        onNodeWithText("Save").performClick()
        runOnIdle { assertTrue(editor.state.value.library.presets.single().value is VisualPresetValue.Complete) }
        click("Create variant")
        onNodeWithText("Create").performClick()
        runOnIdle { assertEquals("Aika Ren", editor.state.value.project.characterName); assertEquals("Rooftop", editor.state.value.project.environment.worldContextHint) }
        onNodeWithText("Generate", substring = false).performClick()
        onNodeWithText("Generation surface").assertIsDisplayed()
        capture("generation")
    }
}
