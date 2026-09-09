package com.kelvinsaputra.promptstudio.platform

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.kelvinsaputra.promptstudio.generation.model.*
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlinx.coroutines.*

actual fun decodeGeneratedImage(bytes: ByteArray): ImageBitmap = org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
@Composable actual fun rememberImageSaver(onMessage: (String) -> Unit): (GeneratedImage) -> Unit {
    val scope = rememberCoroutineScope()
    val message by rememberUpdatedState(onMessage)
    return { image -> scope.launch {
        val dialog = FileDialog(Frame.getFrames().firstOrNull { it.isVisible }, "Save Image", FileDialog.SAVE)
        try {
            dialog.file = image.suggestedFilename()
            dialog.isVisible = true
            val file = dialog.file?.let { File(dialog.directory, it) }
            if (file != null) {
                withContext(Dispatchers.IO) { saveNewImage(file, image.bytes) }
                message("Image saved")
            }
        } catch (e: CancellationException) { throw e }
        catch (_: java.nio.file.FileAlreadyExistsException) { message("That file already exists. Choose a new filename.") }
        catch (_: Exception) { message("Could not save the image. Choose a writable location.") }
        finally { dialog.dispose() }
    } }
}

internal fun saveNewImage(file: File, bytes: ByteArray) {
    Files.write(file.toPath(), bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
}
