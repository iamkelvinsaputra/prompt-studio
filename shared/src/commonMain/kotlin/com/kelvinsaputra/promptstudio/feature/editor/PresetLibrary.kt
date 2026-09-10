package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*

@Composable
fun PresetLibrary(state: EditorUiState, editor: EditorViewModel) {
    var saving by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<SavedVisualPreset?>(null) }
    saving?.let { category ->
        SimpleNameDialog("Save $category preset", "", { saving = null }) { editor.savePreset(it, category == "composition"); saving = null }
    }
    deleting?.let { preset ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Remove ${preset.name}?") },
            text = { Text("Saved projects keep their applied settings.") },
            confirmButton = { TextButton(onClick = { editor.deletePreset(preset.id); deleting = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Your presets", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { saving = "pose" }) { Text("Save pose") }
            TextButton(onClick = { saving = "composition" }) { Text("Save composition") }
        }
        if (state.library.presets.isEmpty()) Text("Save a pose or composition to reuse in any project.", style = MaterialTheme.typography.bodySmall)
        else {
            Text("Recently saved or used", style = MaterialTheme.typography.bodySmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.library.presets, key = { it.id }) { preset ->
                    val preview = preset.value.applyTo(state.project)
                    Column(Modifier.width(156.dp)) {
                        OutlinedCard(onClick = { editor.applyPreset(preset.id) }, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Apply ${preset.category} preset ${preset.name}" }) {
                            Column(Modifier.padding(8.dp)) {
                                SilhouettePreview(preview.visualAssembly, Modifier.fillMaxWidth().height(116.dp), preview.output.aspectRatio, thumbnail = true)
                                Text(preset.name, style = MaterialTheme.typography.labelLarge)
                                Text(preset.category, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        TextButton(onClick = { deleting = preset }, modifier = Modifier.semantics { contentDescription = "Remove preset ${preset.name}" }) { Text("Remove") }
                    }
                }
            }
        }
    }
}
