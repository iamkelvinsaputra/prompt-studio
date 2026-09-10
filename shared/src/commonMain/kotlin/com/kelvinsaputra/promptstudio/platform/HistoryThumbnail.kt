package com.kelvinsaputra.promptstudio.platform
import androidx.compose.ui.graphics.ImageBitmap
expect fun decodeHistoryThumbnail(bytes: ByteArray): ImageBitmap
