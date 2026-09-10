package com.kelvinsaputra.promptstudio.platform
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.*
actual fun decodeHistoryThumbnail(bytes: ByteArray): ImageBitmap {
    val original = Image.makeFromEncoded(bytes)
    try {
        val scale = 144f / maxOf(original.width, original.height)
        val width = (original.width * scale).toInt().coerceAtLeast(1)
        val height = (original.height * scale).toInt().coerceAtLeast(1)
        val surface = Surface.makeRasterN32Premul(width, height)
        try {
            surface.canvas.drawImageRect(original, Rect.makeWH(width.toFloat(), height.toFloat()))
            return surface.makeImageSnapshot().toComposeImageBitmap()
        } finally { surface.close() }
    } finally { original.close() }
}
