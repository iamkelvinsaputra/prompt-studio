package com.kelvinsaputra.promptstudio

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.feature.studio.*
import com.kelvinsaputra.promptstudio.guide.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class VisualAssemblyUiTest {
    @Test fun missingArtworkRemainsSelectableByKeyboard() = runDesktopComposeUiTest {
        var selected = false
        setContent { StudioTheme {
            VisualOptionCard(VisualGuideRegistry.poses.first(), false, { selected = true }, Modifier.width(180.dp))
        } }
        onNodeWithText("Text guide").assertExists()
        onNodeWithContentDescription("Neutral").performKeyInput { pressKey(Key.Tab); pressKey(Key.Enter) }
        runOnIdle { assertTrue(selected) }
    }
}
