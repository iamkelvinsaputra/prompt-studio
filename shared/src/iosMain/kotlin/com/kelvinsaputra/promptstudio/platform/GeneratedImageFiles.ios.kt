@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.kelvinsaputra.promptstudio.platform
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import com.kelvinsaputra.promptstudio.generation.model.*
import platform.Foundation.*
import kotlinx.coroutines.*
actual fun decodeGeneratedImage(bytes: ByteArray): ImageBitmap = org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
@OptIn(ExperimentalComposeUiApi::class)
actual fun textClipEntry(text: String): ClipEntry = ClipEntry.withPlainText(text)
@Composable actual fun rememberImageSaver(onMessage: (String) -> Unit): (GeneratedImage) -> Unit {
    val scope = rememberCoroutineScope()
    val message by rememberUpdatedState(onMessage)
    return { image -> scope.launch {
        try {
            val path = "${NSTemporaryDirectory()}${NSUUID().UUIDString}-${image.suggestedFilename()}"
            withContext(Dispatchers.IO) { atomicWrite(path, image.bytes) }
            shareFile(path, message)
        } catch (e: CancellationException) { throw e } catch (_: Exception) { message("Could not export image.") }
    } }
}
