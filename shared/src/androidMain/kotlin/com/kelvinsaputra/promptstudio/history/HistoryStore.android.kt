package com.kelvinsaputra.promptstudio.history
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.io.File
@Composable actual fun rememberGenerationHistoryStore(): GenerationHistoryStore {
    val context = LocalContext.current.applicationContext
    return remember(context) { FileGenerationHistoryStore(File(context.noBackupFilesDir, "history")) }
}
