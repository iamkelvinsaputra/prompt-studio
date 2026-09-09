package com.kelvinsaputra.promptstudio.platform

import android.util.AtomicFile
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.kelvinsaputra.promptstudio.persistence.ProjectJson
import com.kelvinsaputra.promptstudio.persistence.ProjectStorage
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.*

class AndroidProjectStorage(file: File) : ProjectStorage {
    private val atomic = AtomicFile(file)
    override fun read(): String? = try {
        atomic.openRead().use { it.projectText() }
    } catch (e: java.io.FileNotFoundException) {
        if (atomic.baseFile.exists()) throw e else null
    }
    override fun write(json: String) {
        val stream = atomic.startWrite()
        try {
            stream.write(json.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (e: Exception) {
            atomic.failWrite(stream)
            throw e
        }
    }
}

private fun InputStream.projectText(): String {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(output.size() + count <= ProjectJson.MAX_BYTES) { "Project exceeds the 1 MB file limit." }
        output.write(buffer, 0, count)
    }
    return output.toByteArray().toString(Charsets.UTF_8)
}

@Composable
actual fun rememberProjectStorage(): ProjectStorage {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidProjectStorage(File(context.noBackupFilesDir, "current-project.json")) }
}

@Composable
actual fun rememberProjectFileActions(
    exportJson: () -> String,
    onImport: (String) -> Unit,
    onMessage: (String) -> Unit,
): ProjectFileActions {
    val resolver = LocalContext.current.applicationContext.contentResolver
    val scope = rememberCoroutineScope()
    val currentExport by rememberUpdatedState(exportJson)
    val currentImport by rememberUpdatedState(onImport)
    val currentMessage by rememberUpdatedState(onMessage)
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    checkNotNull(resolver.openInputStream(uri)).use { it.projectText() }
                }
                currentImport(text)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { currentMessage("Could not read the project file. Choose a readable JSON file under 1 MB.") }
        }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            try {
                // Read the current project after the picker returns, including after activity recreation.
                val text = currentExport()
                withContext(Dispatchers.IO) {
                    checkNotNull(resolver.openOutputStream(uri, "wt")).use { it.write(text.toByteArray(Charsets.UTF_8)) }
                }
                currentMessage("Project exported")
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { currentMessage("Could not export the project. Choose a writable location; projects must be under 1 MB.") }
        }
    }
    return ProjectFileActions(
        importProject = {
            try { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            catch (_: Exception) { currentMessage("No document picker is available.") }
        },
        exportProject = {
            try { currentExport(); exporter.launch("prompt-studio.json") }
            catch (_: Exception) { currentMessage("Could not start export. Check project size and document picker availability.") }
        },
    )
}
