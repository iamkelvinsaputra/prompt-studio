package com.kelvinsaputra.promptstudio.platform

import androidx.compose.runtime.*
import com.kelvinsaputra.promptstudio.persistence.ProjectJson
import com.kelvinsaputra.promptstudio.persistence.ProjectStorage
import java.awt.FileDialog
import java.awt.Frame
import java.awt.KeyboardFocusManager
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.*

class DesktopProjectStorage(private val file: File = File(
    System.getProperty("user.home"), "Library/Application Support/Prompt Studio/current-project.json",
)) : ProjectStorage {
    override fun read(): String? = if (file.exists()) file.inputStream().use { it.projectText() } else null
    override fun write(json: String) = writeAtomic(file, json)
}

// Same-directory replacement prevents an interrupted save from truncating the previous project.
private fun writeAtomic(file: File, text: String) {
    Files.createDirectories(file.absoluteFile.parentFile.toPath())
    val temporary = Files.createTempFile(file.absoluteFile.parentFile.toPath(), ".prompt-studio-", ".tmp")
    try {
        temporary.toFile().outputStream().use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
            stream.fd.sync()
        }
        Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    } finally {
        Files.deleteIfExists(temporary)
    }
}

private fun InputStream.projectText(): String {
    val bytes = readNBytes(ProjectJson.MAX_BYTES + 1)
    require(bytes.size <= ProjectJson.MAX_BYTES) { "Project exceeds the 1 MB file limit." }
    return bytes.toString(Charsets.UTF_8)
}

@Composable
actual fun rememberProjectStorage(): ProjectStorage = remember { DesktopProjectStorage() }

@Composable
actual fun rememberProjectFileActions(
    exportJson: () -> String,
    onImport: (String) -> Unit,
    onMessage: (String) -> Unit,
): ProjectFileActions {
    val scope = rememberCoroutineScope()
    val currentExport by rememberUpdatedState(exportJson)
    val currentImport by rememberUpdatedState(onImport)
    val currentMessage by rememberUpdatedState(onMessage)
    return remember(scope) {
        ProjectFileActions(
            importProject = {
                scope.launch {
                    try {
                        val file = chooseFile(FileDialog.LOAD)
                        if (file != null) {
                            val text = withContext(Dispatchers.IO) { file.inputStream().use { it.projectText() } }
                            currentImport(text)
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { currentMessage("Could not read the project file. Choose a readable JSON file under 1 MB.") }
                }
            },
            exportProject = {
                scope.launch {
                    try {
                        val text = currentExport()
                        val file = chooseFile(FileDialog.SAVE)
                        if (file != null) {
                            withContext(Dispatchers.IO) { writeAtomic(file, text) }
                            currentMessage("Project exported")
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { currentMessage("Could not export the project. Choose a writable location; projects must be under 1 MB.") }
                }
            },
        )
    }
}

private fun chooseFile(mode: Int): File? {
    val owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow as? Frame
        ?: Frame.getFrames().firstOrNull { it.isVisible }
    val dialog = FileDialog(owner, if (mode == FileDialog.LOAD) "Import Project" else "Export Project", mode)
    try {
        if (mode == FileDialog.SAVE) dialog.file = "prompt-studio.json"
        dialog.isVisible = true
        return dialog.file?.let { File(dialog.directory, it) }
    } finally {
        dialog.dispose()
        owner?.toFront()
    }
}
