package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kelvinsaputra.promptstudio.domain.ArtStylePreset

@Composable
fun StyleEditor(style: ArtStylePreset) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Text(style.name, style = MaterialTheme.typography.titleMedium)
    Text("Built-in · Locked", style = MaterialTheme.typography.labelLarge)
    Text("Anime-game character rendering with selective sumi-e ink, translucent sky-blue watercolor, and strong negative space.")
    TextButton(onClick = { expanded = !expanded }) {
        Text(if (expanded) "Hide Style Prompt" else "View Style Prompt")
    }
    if (expanded) {
        SelectionContainer {
            Text(style.prompt, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
