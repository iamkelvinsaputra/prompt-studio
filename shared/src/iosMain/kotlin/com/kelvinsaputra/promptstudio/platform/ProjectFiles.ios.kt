@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.kelvinsaputra.promptstudio.platform

import androidx.compose.runtime.*
import com.kelvinsaputra.promptstudio.persistence.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import kotlinx.coroutines.*

class AppleProjectStorage(private val path: String = "${appDirectory()}/current-project.json") : ProjectStorage {
    override fun read() = readBytes(path)?.also { require(it.size <= ProjectJson.MAX_BYTES) }?.decodeToString()
    override fun write(json: String) = atomicWrite(path, json.encodeToByteArray())
}
@Composable actual fun rememberProjectStorage(): ProjectStorage = remember { AppleProjectStorage() }

internal fun presentingController(): UIViewController {
    val scene = UIApplication.sharedApplication.connectedScenes.firstOrNull { (it as? UIWindowScene)?.activationState == UISceneActivationStateForegroundActive } as? UIWindowScene
    var controller = checkNotNull((scene?.windows?.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow)?.rootViewController)
    while (controller.presentedViewController != null) controller = controller.presentedViewController!!
    return controller
}
internal fun shareFile(path: String, onMessage: (String) -> Unit) {
    val controller = presentingController()
    val sheet = UIActivityViewController(listOf(NSURL.fileURLWithPath(path)), null)
    sheet.popoverPresentationController?.sourceView = controller.view
    sheet.popoverPresentationController?.sourceRect = controller.view.bounds
    sheet.completionWithItemsHandler = { _, completed, _, error ->
        runCatching { removeFile(path) }
        if (error != null) onMessage("Could not export file.") else if (completed) onMessage("File exported")
    }
    controller.presentViewController(sheet, true, null)
}
private class ProjectPicker(private val scope: CoroutineScope, private val imported: (String) -> Unit, private val message: (String) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
        scope.launch {
            val access = url.startAccessingSecurityScopedResource()
            try {
                val text = withContext(Dispatchers.IO) {
                    val data = checkNotNull(NSData.dataWithContentsOfURL(url))
                    require(data.length <= ProjectJson.MAX_BYTES.toULong())
                    data.asBytes().decodeToString()
                }
                imported(text)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { message("Could not import. Choose readable project JSON under 1 MB.") }
            finally { if (access) url.stopAccessingSecurityScopedResource() }
        }
    }
}
@Composable actual fun rememberProjectFileActions(exportJson: () -> String, onImport: (String) -> Unit, onMessage: (String) -> Unit): ProjectFileActions {
    val scope = rememberCoroutineScope()
    val export by rememberUpdatedState(exportJson)
    val imported by rememberUpdatedState(onImport)
    val message by rememberUpdatedState(onMessage)
    val delegate = remember { ProjectPicker(scope, { imported(it) }, { message(it) }) }
    return ProjectFileActions(
        {
            try {
                val picker = UIDocumentPickerViewController(documentTypes = listOf("public.json", "public.plain-text"), inMode = UIDocumentPickerMode.UIDocumentPickerModeImport)
                picker.delegate = delegate
                presentingController().presentViewController(picker, true, null)
            } catch (_: Exception) { message("Document picker unavailable.") }
        },
        { scope.launch {
            try {
                val text = export()
                val path = "${NSTemporaryDirectory()}prompt-studio-${NSUUID().UUIDString}.json"
                withContext(Dispatchers.IO) { atomicWrite(path, text.encodeToByteArray()) }
                shareFile(path, message)
            } catch (e: CancellationException) { throw e } catch (_: Exception) { message("Could not export project.") }
        } },
    )
}
