@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package com.kelvinsaputra.promptstudio.platform

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.ClipEntry
import com.kelvinsaputra.promptstudio.credentials.*
import com.kelvinsaputra.promptstudio.persistence.*
import com.kelvinsaputra.promptstudio.generation.model.*
import kotlin.io.encoding.Base64

actual val platformCapabilities = PlatformCapabilities(canGenerateWithByok = false)
@Composable actual fun rememberGenerationBackend() = remember { GenerationBackend(emptyList()) }
@Composable actual fun rememberProjectStorage(): ProjectStorage = remember { object : ProjectStorage {
    override fun read(): String? = readLibrary()
    override fun write(json: String) { writeLibrary(json) }
} }
private fun readLibrary(): String? = js("localStorage.getItem('prompt-studio-library-v1')")
private fun writeLibrary(text: String): Unit = js("localStorage.setItem('prompt-studio-library-v1', text)")
@Composable actual fun rememberProjectFileActions(exportJson: () -> String, onImport: (String) -> Unit, onMessage: (String) -> Unit): ProjectFileActions {
    val export by rememberUpdatedState(exportJson)
    val imported by rememberUpdatedState(onImport)
    val message by rememberUpdatedState(onMessage)
    return ProjectFileActions(
        { try { uploadProject({ imported(it) }, { message("Could not import. Choose a JSON file under 1 MB.") }) } catch (_: Exception) { message("File upload unavailable.") } },
        { try { downloadText(export()); message("Project download started") } catch (_: Exception) { message("Could not export project.") } },
    )
}
private fun uploadProject(success: (String) -> Unit, failure: () -> Unit): Unit = js("""{
    const input = document.createElement('input'); input.type = 'file'; input.accept = '.json,application/json';
    input.onchange = async () => { try { const f = input.files[0]; if (!f) return; if (f.size > 1048576) throw Error(); success(await f.text()); } catch(e) { failure(); } }; input.click();
}""")
private fun downloadText(text: String): Unit = js("""{
    const url = URL.createObjectURL(new Blob([text], {type:'application/json'}));
    const a = document.createElement('a'); a.href=url; a.download='prompt-studio.json'; a.click(); setTimeout(() => URL.revokeObjectURL(url), 30000);
}""")
private fun downloadImage(encoded: String, mime: String, name: String): Unit = js("""{
    const data = Uint8Array.from(atob(encoded), c => c.charCodeAt(0));
    const url = URL.createObjectURL(new Blob([data], {type:mime}));
    const a = document.createElement('a'); a.href=url; a.download=name; a.click(); setTimeout(() => URL.revokeObjectURL(url), 30000);
}""")
@Composable actual fun rememberImageSaver(onMessage: (String) -> Unit): (GeneratedImage) -> Unit = { image ->
    try { downloadImage(Base64.encode(image.bytes), image.mimeType, image.suggestedFilename()); onMessage("Image download started") }
    catch (_: Exception) { onMessage("Could not download image.") }
}
actual fun decodeGeneratedImage(bytes: ByteArray): ImageBitmap = org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
@OptIn(ExperimentalComposeUiApi::class)
actual fun textClipEntry(text: String): ClipEntry {
    check(clipboardAvailable()) { "Clipboard unavailable" }
    return ClipEntry.withPlainText(text)
}
private fun clipboardAvailable(): Boolean = js("Boolean(window.isSecureContext && navigator.clipboard && navigator.clipboard.writeText)")
