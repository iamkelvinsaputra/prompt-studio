package com.kelvinsaputra.promptstudio.platform

import androidx.compose.ui.platform.ClipEntry

// Clipboard access is shared through LocalClipboard. Only the native payload constructor differs.
expect fun textClipEntry(text: String): ClipEntry
