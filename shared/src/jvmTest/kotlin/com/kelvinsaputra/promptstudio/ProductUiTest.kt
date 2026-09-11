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
import com.kelvinsaputra.promptstudio.prompt.*
import java.io.File
import javax.imageio.ImageIO
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class ProductUiTest {
    @Test fun mobileProjectVariantPresetAndGenerateJourney() = journey(360, 780)
    @Test fun desktopProjectVariantPresetAndGenerateJourney() = journey(1280, 900)

    private fun journey(width: Int, height: Int) = runDesktopComposeUiTest(width = width, height = height) {
        val editor = EditorViewModel()
        val reports = File("build/reports/guided-build").apply { mkdirs() }
        fun capture(name: String) { onRoot().captureToImage().let { ImageIO.write(it.toAwtImage(), "png", File(reports, "$name-$width.png")) } }
        fun choose(label: String) { onNodeWithContentDescription(label, substring = false).performScrollTo().performClick() }
        fun next() { onNodeWithText("Continue", substring = false).assertIsDisplayed().performClick() }
        setContent {
            val state by editor.state.collectAsState()
            var home by remember { mutableStateOf(true) }
            MaterialTheme { Surface(Modifier.requiredSize(width.dp, height.dp)) {
                if (home) ProjectsScreen(state, editor) { home = false }
                else EditorScreen(state, {}, {}, editor::dismissMessage, editor::retrySave,
                    editor::selectModule, editor::setMode, editor::setProject, editor::setCostumeLocks,
                    editor::setPoseLocks, editor::setVariationLocks, editor::randomizeCostume, editor::randomizePose,
                    editor::randomizeUnlocked, editor::resetCostume, editor::resetPose, editor::resetModule,
                    editor::selectCharacter, editor::newCharacter, editor::renameCharacter, editor::duplicateCharacter, editor::deleteCharacter,
                    generationContent = { Text("Generation surface") }, onProjects = { home = true },
                    variantContent = { VariantBar(state, editor) }, presetContent = { PresetLibrary(state, editor) },
                    onGuideNavigation = editor::navigateGuide,
                    useVisualGuide = state.library.activeVariants.generation.useVisualGuide,
                    onVisualGuideChange = { editor.setGeneration(state.library.activeVariants.generation.copy(useVisualGuide = it)) })
            } }
        }
        onNodeWithText("New project").performClick()
        onNode(hasSetTextAction()).performTextReplacement("Journey")
        onNodeWithText("Create").performClick()
        onNodeWithText("Step 1 of 8").assertIsDisplayed()
        onNodeWithText("All sections").assertDoesNotExist()
        capture("gender")
        choose("Woman"); next()
        choose("Adult"); next()
        choose("Relaxed")
        capture("pose")
        onNodeWithText("Adjust Pose").performScrollTo().performClick()
        onNodeWithContentDescription("Left elbow").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(20f) }
        runOnIdle { assertTrue(editor.state.value.project.pose.effectiveAdjustments.isAdjusted) }
        next(); choose("Right"); next()
        choose("Full Body"); next()
        choose("Lower Center"); next()
        choose("Negative Space"); next()
        onNodeWithText("Step 8 of 8").assertIsDisplayed()
        capture("style")
        choose("Anime + Ink Wash")
        onNodeWithText("Review your visual").assertIsDisplayed().performClick()
        runOnIdle {
            assertNull(editor.state.value.guided.step)
            assertEquals(VisualPose.RELAXED, editor.state.value.project.visualAssembly.posePreset)
            assertEquals(Gaze.RIGHT, editor.state.value.project.visualAssembly.gaze)
        }
        capture("summary")
        choose("Edit Pose"); choose("Sitting")
        onNodeWithText("Done", substring = false).assertIsDisplayed().performClick()
        onNodeWithText("Your visual").assertIsDisplayed()
        runOnIdle { assertFalse(editor.state.value.project.pose.effectiveAdjustments.isAdjusted) }
        choose("Edit Art Style")
        onNodeWithText("Edit Style", substring = false).performScrollTo().performClick()
        onNodeWithText("Dramatic", substring = false).performScrollTo().performClick()
        onNodeWithText("Vivid", substring = false).performScrollTo().performClick()
        onNodeWithText("Advanced · Custom ART STYLE CORE").performScrollTo().performClick()
        onNodeWithText("Edit ART STYLE CORE manually").performScrollTo().performClick()
        onNode(hasSetTextAction() and hasText("ART STYLE CORE")).performScrollTo().performTextReplacement("My exact style.")
        runOnIdle { assertEquals("My exact style.", editor.state.value.project.effectiveArtStyleCore()) }
        capture("manual-style")
        onNodeWithText("Done", substring = false).performClick()
        onNodeWithText("Custom · Manual ART STYLE CORE").performScrollTo().assertIsDisplayed()
        choose("Edit Art Style")
        onNodeWithText("Use structured style").performScrollTo().performClick()
        onNodeWithText("Done", substring = false).performClick()
        runOnIdle { assertContains(editor.state.value.project.guideSummary(GuideStep.ArtStyle), "Dramatic") }
        onNodeWithText("Variants & saved presets").performScrollTo().performClick()
        onNodeWithText("Save pose").performScrollTo().performClick()
        onNode(hasSetTextAction()).performTextInput("My seated pose")
        onNodeWithText("Save", substring = false).performClick()
        runOnIdle { assertEquals(VisualPose.SITTING.base, (editor.state.value.library.presets.single().value as VisualPresetValue.Pose).pose.basePose) }
        onNodeWithText("+ Variant").performScrollTo().performClick()
        onNodeWithText("Create", substring = false).performClick()
        runOnIdle { assertEquals("16:9", editor.state.value.project.output.aspectRatio) }
        choose("Edit Pose"); choose("Dynamic")
        onNodeWithText("Done", substring = false).performClick()
        onNodeWithText("Variants & saved presets").performScrollTo().performClick()
        // The preset carousel scrolls horizontally; reveal it through the surrounding vertical panel first.
        onNode(hasScrollAction() and SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange))
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) { it(0f, 1000f) }
        onNodeWithContentDescription("Apply Pose preset My seated pose").performScrollTo()
        capture("preset-before")
        onNodeWithContentDescription("Apply Pose preset My seated pose").performClick()
        capture("preset-after")
        runOnIdle {
            assertEquals(VisualPose.SITTING, editor.state.value.project.visualAssembly.posePreset)
            assertEquals("16:9", editor.state.value.project.output.aspectRatio)
        }
        if (width < 600) onNodeWithText("Desktop 16:9 ▾").performScrollTo().performClick()
        onNodeWithText("Phone 9:16").performScrollTo().performClick()
        runOnIdle { assertEquals("9:16", editor.state.value.project.output.aspectRatio) }
        onNodeWithText("Generate", substring = false).assertIsDisplayed().performClick()
        onNodeWithText("Generation surface").assertIsDisplayed()
        onNodeWithText("← Summary").performClick()
        onNodeWithText("← Projects").performClick()
        onNodeWithText("Journey", substring = false).performClick()
        onNodeWithText("Your visual").assertIsDisplayed()
        onNodeWithText("Step 1 of 8").assertDoesNotExist()
    }
}
