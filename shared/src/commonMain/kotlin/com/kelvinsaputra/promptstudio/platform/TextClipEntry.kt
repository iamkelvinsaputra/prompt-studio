package com.kelvinsaputra.promptstudio.platform

import androidx.compose.ui.platform.ClipEntry

// Clipboard access is shared through LocalClipboard. Only the native payload constructor differs.
expect fun textClipEntry(text: String): ClipEntry

/** Some browsers leave clipboard permission requests pending; keep copy failure actionable. */
suspend fun androidx.compose.ui.platform.Clipboard.copyPromptText(text: String) {
    check(kotlinx.coroutines.withTimeoutOrNull(5_000) { setClipEntry(textClipEntry(text)); true } == true) {
        "Clipboard request timed out"
    }
}
