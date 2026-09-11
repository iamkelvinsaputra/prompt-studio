package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.CharacterProject

/** Detailed navigation reuses the same style controls and ownership as the guided step. */
@Composable
fun StyleEditor(project: CharacterProject, onProject: (CharacterProject) -> Unit) {
    CurrentStyleSample(project, Modifier.fillMaxWidth().height(240.dp))
    ArtStyleChoices(project, onProject)
}
