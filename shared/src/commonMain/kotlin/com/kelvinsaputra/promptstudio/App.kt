package com.kelvinsaputra.promptstudio

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import com.kelvinsaputra.promptstudio.credentials.rememberCredentialStore
import com.kelvinsaputra.promptstudio.platform.*
import com.kelvinsaputra.promptstudio.history.*
import com.kelvinsaputra.promptstudio.generation.provider.*
import com.kelvinsaputra.promptstudio.feature.generation.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelvinsaputra.promptstudio.platform.rememberProjectStorage
import com.kelvinsaputra.promptstudio.platform.rememberProjectFileActions
import com.kelvinsaputra.promptstudio.feature.editor.EditorScreen
import com.kelvinsaputra.promptstudio.feature.editor.EditorViewModel

@Composable
fun App() {
    val credentials = rememberCredentialStore()
    val selection = remember { GenerationSelection() }
    val scope = rememberCoroutineScope()
    val backend = rememberGenerationBackend()
    val historyStore = rememberGenerationHistoryStore()
    val history = remember(historyStore) { HistoryController(historyStore) }
    LaunchedEffect(history) { history.refresh() }
    val generation = remember { GenerationController(scope, backend.providers, credentials, history::save) }
    DisposableEffect(backend) { onDispose { generation.cancel(); backend.close(); credentials.clearSession() } }
    val storage = rememberProjectStorage()
    val editor = viewModel { EditorViewModel(storage = storage) }
    val files = rememberProjectFileActions(editor::exportProject, editor::importProject, editor::showMessage)
    val state by editor.state.collectAsStateWithLifecycle()
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF346784), secondary = Color(0xFF526575))) {
        EditorScreen(
            state = state,
            generationContent = { GenerationAndHistory(state.project, generation, credentials, selection, history, editor::restoreConfiguration, editor::showMessage) },
            onImport = files.importProject,
            onExport = files.exportProject,
            onDismissMessage = editor::dismissMessage,
            onRetrySave = editor::retrySave,
            onModule = editor::selectModule,
            onMode = editor::setMode,
            onProject = editor::setProject,
            onCostumeLocks = editor::setCostumeLocks,
            onPoseLocks = editor::setPoseLocks,
            onVariationLocks = editor::setVariationLocks,
            onRandomizeCostume = editor::randomizeCostume,
            onRandomizePose = editor::randomizePose,
            onRandomizeUnlocked = editor::randomizeUnlocked,
            onResetCostume = editor::resetCostume,
            onResetPose = editor::resetPose,
            onResetModule = editor::resetModule,
            onSelectCharacter = editor::selectCharacter,
            onNewCharacter = editor::newCharacter,
            onRenameCharacter = editor::renameCharacter,
            onDuplicateCharacter = editor::duplicateCharacter,
            onDeleteCharacter = editor::deleteCharacter,
        )
    }
}
