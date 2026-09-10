package com.kelvinsaputra.promptstudio.history
import androidx.compose.runtime.*
import java.io.File
@Composable actual fun rememberGenerationHistoryStore(): GenerationHistoryStore = remember {
    FileGenerationHistoryStore(File(System.getProperty("user.home"), "Library/Application Support/Prompt Studio/history"))
}
