package com.kelvinsaputra.promptstudio.platform
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
actual fun decodeHistoryThumbnail(bytes: ByteArray): ImageBitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val options = BitmapFactory.Options().apply { inSampleSize = (maxOf(bounds.outWidth, bounds.outHeight) / 144).coerceAtLeast(1) }
    return checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)).asImageBitmap()
}
