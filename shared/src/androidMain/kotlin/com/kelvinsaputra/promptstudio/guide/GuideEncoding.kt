package com.kelvinsaputra.promptstudio.guide

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

internal actual fun encodeGuidePng(bitmap: ImageBitmap): ByteArray = ByteArrayOutputStream().use { stream ->
    check(bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream))
    stream.toByteArray()
}
