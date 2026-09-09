package com.kelvinsaputra.promptstudio.platform

import android.graphics.BitmapFactory
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.kelvinsaputra.promptstudio.generation.model.*
import kotlinx.coroutines.*

actual fun decodeGeneratedImage(bytes: ByteArray): ImageBitmap = checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)).asImageBitmap()
@Composable actual fun rememberImageSaver(onMessage: (String) -> Unit): (GeneratedImage) -> Unit {
    val resolver = LocalContext.current.applicationContext.contentResolver
    val scope = rememberCoroutineScope()
    val message by rememberUpdatedState(onMessage)
    var pending by remember { mutableStateOf<GeneratedImage?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val image = pending
        pending = null
        val uri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null) {
            if (image == null) message("Image session ended. Generate and save again.")
            else scope.launch {
                try {
                    withContext(Dispatchers.IO) { checkNotNull(resolver.openOutputStream(uri, "w")).use { it.write(image.bytes) } }
                    message("Image saved")
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { message("Could not save the image. Choose a writable location.") }
            }
        }
    }
    return { image ->
        if (pending == null) {
            pending = image
            try { launcher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = image.mimeType; putExtra(Intent.EXTRA_TITLE, image.suggestedFilename())
            }) } catch (_: Exception) { pending = null; message("No document picker is available.") }
        }
    }
}
