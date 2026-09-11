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
import com.kelvinsaputra.promptstudio.feature.editor.ProjectsScreen
import com.kelvinsaputra.promptstudio.feature.editor.VariantBar
import com.kelvinsaputra.promptstudio.feature.editor.PresetLibrary
import com.kelvinsaputra.promptstudio.generation.model.GenerationVariant

@Composable
fun App() {
    val credentials = rememberCredentialStore()
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
    var projects by remember { mutableStateOf(true) }
    val variants = state.library.activeVariants
    val selection = remember(state.project.id, variants.activeId, variants.generation) { GenerationSelection(variants.generation, editor::setGeneration) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF346784), secondary = Color(0xFF526575))) {
        if (projects) {
            ProjectsScreen(state, editor) { projects = false; editor.selectModule(com.kelvinsaputra.promptstudio.feature.editor.EditorModule.VisualBuild) }
            return@MaterialTheme
        }
        EditorScreen(
            state = state,
            onProjects = { projects = true },
            onGuideNavigation = editor::navigateGuide,
            useVisualGuide = variants.generation.useVisualGuide,
            onVisualGuideChange = { editor.setGeneration(variants.generation.copy(useVisualGuide = it)) },
            variantContent = { VariantBar(state, editor) },
            presetContent = { PresetLibrary(state, editor) },
            generationContent = { GenerationAndHistory(state.project, generation, credentials, selection, history, editor::restoreConfiguration, editor::showMessage, GenerationVariant(variants.activeId, variants.activeName)) },
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
            onNewCharacter = { editor.createProject("New Project", com.kelvinsaputra.promptstudio.domain.OutputType.PHONE) },
            onRenameCharacter = editor::renameCharacter,
            onDuplicateCharacter = editor::duplicateCharacter,
            onDeleteCharacter = editor::deleteCharacter,
        )
    }
}
