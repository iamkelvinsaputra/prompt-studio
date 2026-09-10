package com.kelvinsaputra.promptstudio.guide

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

internal actual fun encodeGuidePng(bitmap: ImageBitmap): ByteArray {
    val image = Image.makeFromBitmap(bitmap.asSkiaBitmap())
    try {
        val data = checkNotNull(image.encodeToData(EncodedImageFormat.PNG))
        try { return data.bytes } finally { data.close() }
    } finally { image.close() }
}
