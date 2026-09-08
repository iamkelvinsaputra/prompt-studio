package com.kelvinsaputra.promptstudio

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelvinsaputra.promptstudio.feature.editor.EditorScreen
import com.kelvinsaputra.promptstudio.feature.editor.EditorViewModel

@Composable
fun App() {
    val editor = viewModel { EditorViewModel() }
    val state by editor.state.collectAsStateWithLifecycle()
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF346784), secondary = Color(0xFF526575))) {
        EditorScreen(state, editor::selectModule, editor::setCostume, editor::setPose, editor::setOutput)
    }
}
