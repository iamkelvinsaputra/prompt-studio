package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*

@Composable
fun ProjectsScreen(state: EditorUiState, editor: EditorViewModel, onOpen: () -> Unit) {
    var creating by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf<String?>(null) }
    var target by remember { mutableStateOf<CharacterProject?>(null) }
    if (creating) OutputNameDialog("New project", "New Project", { creating = false }) { name, type ->
        editor.createProject(name, type); creating = false; onOpen()
    }
    target?.let { project ->
        if (action == "rename") SimpleNameDialog("Rename project", project.name, { target = null }) {
            editor.selectCharacter(project.id); editor.renameCharacter(it); target = null
        }
        if (action == "delete") AlertDialog(onDismissRequest = { target = null },
            title = { Text("Delete ${project.name}?") },
            text = { Text("Remove this project and its variants from this device. Saved generations remain in history. The last project is replaced with a blank project.") },
            confirmButton = { TextButton(onClick = { editor.selectCharacter(project.id); editor.deleteCharacter(); target = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { target = null }) { Text("Cancel") } })
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Prompt Studio", style = MaterialTheme.typography.headlineMedium)
            Text("Projects", style = MaterialTheme.typography.titleLarge)
            Text("Recent projects · saved on this device", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { creating = true }) { Text("New project") }
            state.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = editor::retrySave) { Text("Retry Save") } }
            state.message?.let { Text(it); TextButton(onClick = editor::dismissMessage) { Text("Dismiss") } }
            LazyVerticalGrid(GridCells.Adaptive(260.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.library.recentProjects, key = { it.id }) { project ->
                    OutlinedCard(onClick = { editor.selectCharacter(project.id); onOpen() }) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SilhouettePreview(project.visualAssembly, Modifier.fillMaxWidth().height(140.dp), project.output.aspectRatio, thumbnail = true)
                            Text(project.name, style = MaterialTheme.typography.titleMedium)
                            val count = 1 + (state.library.variants.firstOrNull { it.projectId == project.id }?.alternatives?.size ?: 0)
                            Text("$count ${if (count == 1) "variant" else "variants"} · ${project.output.aspectRatio ?: "Choose an output"}")
                            Row {
                                TextButton(onClick = { target = project; action = "rename" }) { Text("Rename") }
                                TextButton(onClick = { editor.selectCharacter(project.id); editor.duplicateCharacter() }) { Text("Duplicate") }
                                TextButton(onClick = { target = project; action = "delete" }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VariantBar(state: EditorUiState, editor: EditorViewModel) {
    var dialog by remember { mutableStateOf<String?>(null) }
    val variants = state.library.activeVariants
    if (dialog == "add") OutputNameDialog("New variant", "Desktop", { dialog = null }, OutputType.DESKTOP) { name, type ->
        editor.addVariant(name, type); dialog = null
    }
    if (dialog == "rename") SimpleNameDialog("Rename variant", variants.activeName, { dialog = null }) { editor.renameVariant(it); dialog = null }
    if (dialog == "delete") AlertDialog(onDismissRequest = { dialog = null }, title = { Text("Delete ${variants.activeName}?") },
        text = { Text("Remove this variant. Its saved generations remain in history.") },
        confirmButton = { TextButton(onClick = { editor.deleteVariant(); dialog = null }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } })
    var menu by remember { mutableStateOf(false) }
    var switching by remember { mutableStateOf(false) }
    val base = state.library.characters.first { it.id == state.project.id }
    val choices = listOf(PRIMARY_VARIANT to "${variants.primaryName} ${base.output.aspectRatio.orEmpty()}") +
        variants.alternatives.map { it.id to "${it.name} ${it.scene.output.aspectRatio.orEmpty()}" }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val compact = maxWidth < 600.dp
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (compact) Box(Modifier.weight(1f)) {
            TextButton(onClick = { switching = true }, modifier = Modifier.fillMaxWidth()) {
                Text("${choices.first { it.first == variants.activeId }.second} ▾", maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            DropdownMenu(switching, onDismissRequest = { switching = false }) {
                choices.forEach { (id, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { editor.selectVariant(id); switching = false })
                }
            }
        } else {
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            choices.forEach { (id, label) ->
                FilterChip(variants.activeId == id, { editor.selectVariant(id) }, label = { Text(label) })
            }
        }
        }
        TextButton(onClick = { dialog = "add" }) { Text("+ Variant") }
        Box {
            TextButton(onClick = { menu = true }) { Text("Edit") }
            DropdownMenu(menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Rename variant") }, onClick = { menu = false; dialog = "rename" })
                DropdownMenuItem(text = { Text("Delete variant") }, enabled = variants.activeId != PRIMARY_VARIANT, onClick = { menu = false; dialog = "delete" })
            }
        }
    }
    }
}

@Composable
internal fun SimpleNameDialog(title: String, initial: String, dismiss: () -> Unit, save: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) },
        text = { OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { save(name.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OutputNameDialog(title: String, initial: String, dismiss: () -> Unit, initialType: OutputType = OutputType.PHONE, save: (String, OutputType) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    var type by remember { mutableStateOf(initialType) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = {
        Column {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(OutputType.PHONE, OutputType.DESKTOP, OutputType.PORTRAIT, OutputType.SQUARE).forEach { option ->
                    FilterChip(type == option, { type = option }, label = { Text("${option.name.lowercase().replaceFirstChar { it.uppercase() }} ${option.ratio}") })
                }
            }
        }
    }, confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { save(name.trim(), type) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}
