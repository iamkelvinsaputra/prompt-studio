package com.kelvinsaputra.promptstudio

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
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
    val storage = rememberProjectStorage()
    val editor = viewModel { EditorViewModel(storage = storage) }
    val files = rememberProjectFileActions(editor::exportProject, editor::importProject, editor::showMessage)
    val state by editor.state.collectAsStateWithLifecycle()
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF346784), secondary = Color(0xFF526575))) {
        EditorScreen(
            state = state,
            onImport = files.importProject,
            onExport = files.exportProject,
            onDismissMessage = editor::dismissMessage,
            onRetrySave = editor::retrySave,
            onModule = editor::selectModule,
            onCostume = editor::setCostume,
            onPose = editor::setPose,
            onOutput = editor::setOutput,
            onCostumeLocks = editor::setCostumeLocks,
            onPoseLocks = editor::setPoseLocks,
            onRandomizeCostume = editor::randomizeCostume,
            onRandomizePose = editor::randomizePose,
            onResetCostume = editor::resetCostume,
            onResetPose = editor::resetPose,
        )
    }
}
