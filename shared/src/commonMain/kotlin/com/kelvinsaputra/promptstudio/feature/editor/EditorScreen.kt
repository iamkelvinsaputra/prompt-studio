package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.CharacterProject
import com.kelvinsaputra.promptstudio.platform.textClipEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    state: EditorUiState,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onDismissMessage: () -> Unit,
    onRetrySave: () -> Unit,
    onModule: (EditorModule) -> Unit,
    onMode: (EditorMode) -> Unit,
    onProject: (CharacterProject) -> Unit,
    onCostumeLocks: (Set<CostumeField>) -> Unit,
    onPoseLocks: (Set<PoseField>) -> Unit,
    onVariationLocks: (Set<VariationField>) -> Unit,
    onRandomizeCostume: () -> Unit,
    onRandomizePose: () -> Unit,
    onRandomizeUnlocked: () -> Unit,
    onResetCostume: () -> Unit,
    onResetPose: () -> Unit,
    onResetModule: (EditorModule) -> Unit,
    onSelectCharacter: (String) -> Unit,
    onNewCharacter: () -> Unit,
    onRenameCharacter: (String) -> Unit,
    onDuplicateCharacter: () -> Unit,
    onDeleteCharacter: () -> Unit,
) {
    val sectionState = rememberSaveableStateHolder()
    val prompt = remember(state.project) { state.compiledPrompt.text }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var projectMenu by remember { mutableStateOf(false) }
    var characterMenu by remember { mutableStateOf(false) }
    var renameDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); onDismissMessage() }
    }
    val validOutput = state.project.output.aspectRatio != null
    val visibleModules = if (state.mode == EditorMode.Quick) quickModules else EditorModule.entries.filter { it != EditorModule.Prompt }

    if (renameDialog) NameDialog(state.project.name, onDismiss = { renameDialog = false }) {
        onRenameCharacter(it); renameDialog = false
    }
    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("Delete ${state.project.name}?") },
            text = { Text(if (state.library.characters.size == 1) "A new empty character will replace the final library entry." else "This removes the character from this local library.") },
            confirmButton = { TextButton(onClick = { onDeleteCharacter(); deleteDialog = false }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("Cancel") } },
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).safeDrawingPadding().imePadding().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Prompt Studio", style = MaterialTheme.typography.headlineSmall)
                    Text("Offline character prompt authoring", style = MaterialTheme.typography.bodySmall)
                }
                Box {
                    TextButton(onClick = { characterMenu = true }) { Text("${state.project.name} ▾") }
                    CharacterMenu(
                        expanded = characterMenu, state = state,
                        onDismiss = { characterMenu = false }, onSelect = { onSelectCharacter(it); characterMenu = false },
                        onNew = { onNewCharacter(); characterMenu = false },
                        onRename = { renameDialog = true; characterMenu = false },
                        onDuplicate = { onDuplicateCharacter(); characterMenu = false },
                        onDelete = { deleteDialog = true; characterMenu = false },
                    )
                }
                Box {
                    TextButton(onClick = { projectMenu = true }) { Text("Project ▾") }
                    DropdownMenu(projectMenu, onDismissRequest = { projectMenu = false }) {
                        DropdownMenuItem(text = { Text("Import Character") }, onClick = { projectMenu = false; onImport() })
                        DropdownMenuItem(text = { Text("Export Character") }, onClick = { projectMenu = false; onExport() })
                    }
                }
                Button(enabled = validOutput, onClick = {
                    scope.launch {
                        try {
                            clipboard.setClipEntry(textClipEntry(prompt))
                            snackbar.showSnackbar("Prompt copied")
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            snackbar.showSnackbar("Could not copy. Try again or select the preview text.")
                        }
                    }
                }) { Text("Copy") }
            }
            state.saveError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onRetrySave) { Text("Retry Save") }
            }
            if (!validOutput) Text("Enter an aspect ratio in Output before copying.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            ModeAndVariationControls(state, onMode, onVariationLocks, onRandomizeUnlocked)
            Spacer(Modifier.height(12.dp))

            BoxWithConstraints(Modifier.weight(1f)) {
                val showLibraryPane = maxWidth >= 1080.dp
                val showModulesPane = maxWidth >= 820.dp
                val showPreview = maxWidth >= 1450.dp && state.module != EditorModule.Prompt
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (showLibraryPane) {
                        CharacterLibraryPane(state, onSelectCharacter, onNewCharacter, { renameDialog = true }, onDuplicateCharacter, { deleteDialog = true })
                        VerticalDivider()
                    }
                    if (showModulesPane) {
                        ModuleNavigation(state.mode, state.module, visibleModules, onModule, Modifier.width(168.dp))
                        VerticalDivider()
                    }
                    Column(Modifier.weight(1f)) {
                        if (!showModulesPane) {
                            ModuleChipBar(state.module, visibleModules, onModule)
                            Spacer(Modifier.height(10.dp))
                        }
                        if (state.module == EditorModule.Prompt) {
                            PromptPreview(prompt, Modifier.fillMaxSize())
                        } else {
                            sectionState.SaveableStateProvider(state.module.name) {
                                Column(
                                    Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(state.module.title, style = MaterialTheme.typography.titleLarge)
                                        if (state.module != EditorModule.Style) TextButton(onClick = { onResetModule(state.module) }) { Text("Reset") }
                                    }
                                    CharacterComponentEditor(
                                        state.module, state.project, onProject, state.costumeLocks, onCostumeLocks,
                                        state.poseLocks, onPoseLocks, onRandomizeCostume, onRandomizePose, onResetCostume, onResetPose,
                                    )
                                    TextButton(onClick = { onModule(EditorModule.Prompt) }) { Text("View live prompt →") }
                                }
                            }
                        }
                    }
                    if (showPreview) {
                        VerticalDivider()
                        PromptPreview(prompt, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModeAndVariationControls(
    state: EditorUiState,
    onMode: (EditorMode) -> Unit,
    onLocks: (Set<VariationField>) -> Unit,
    onRandomize: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(state.mode == EditorMode.Quick, { onMode(EditorMode.Quick) }, label = { Text("Quick") })
        FilterChip(state.mode == EditorMode.Advanced, { onMode(EditorMode.Advanced) }, label = { Text("Advanced") })
        Button(onClick = onRandomize) { Text("Randomize Unlocked") }
    }
    Text("Variation locks protect structured character choices; authored text is always preserved.", style = MaterialTheme.typography.bodySmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        VariationField.entries.forEach { field ->
            FilterChip(
                selected = field in state.variationLocks,
                onClick = { onLocks(if (field in state.variationLocks) state.variationLocks - field else state.variationLocks + field) },
                label = { Text("${if (field in state.variationLocks) "🔒 " else ""}${field.label()}") },
            )
        }
    }
}

private fun VariationField.label() = when (this) {
    VariationField.Identity -> "Identity"; VariationField.Body -> "Body"; VariationField.Face -> "Face"
    VariationField.Expression -> "Expression"; VariationField.Hair -> "Hair"; VariationField.Costume -> "Costume"
    VariationField.Power -> "Power"; VariationField.Pose -> "Pose"
    VariationField.Gaze -> "Gaze"; VariationField.Composition -> "Composition"; VariationField.Environment -> "Environment"
    VariationField.Lighting -> "Lighting"; VariationField.AccentColor -> "Accent"; VariationField.SurfaceTexture -> "Surface"
    VariationField.Output -> "Output"
}

@Composable
private fun ModuleNavigation(mode: EditorMode, selected: EditorModule, modules: List<EditorModule>, onModule: (EditorModule) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (mode == EditorMode.Advanced) {
            modules.groupBy { it.group }.forEach { (group, grouped) ->
                Text(group, style = MaterialTheme.typography.labelSmall)
                grouped.forEach { ModuleChip(it, selected, onModule, Modifier.fillMaxWidth()) }
            }
        } else {
            Text("QUICK FILL", style = MaterialTheme.typography.labelSmall)
            modules.forEach { ModuleChip(it, selected, onModule, Modifier.fillMaxWidth()) }
        }
        Text("PREVIEW", style = MaterialTheme.typography.labelSmall)
        ModuleChip(EditorModule.Prompt, selected, onModule, Modifier.fillMaxWidth())
    }
}

@Composable private fun ModuleChip(module: EditorModule, selected: EditorModule, onModule: (EditorModule) -> Unit, modifier: Modifier = Modifier) =
    FilterChip(selected == module, { onModule(module) }, label = { Text(module.title) }, modifier = modifier)

@Composable
private fun ModuleChipBar(selected: EditorModule, modules: List<EditorModule>, onModule: (EditorModule) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        modules.forEach { ModuleChip(it, selected, onModule) }
        ModuleChip(EditorModule.Prompt, selected, onModule)
    }
}

@Composable
private fun CharacterMenu(
    expanded: Boolean, state: EditorUiState, onDismiss: () -> Unit, onSelect: (String) -> Unit,
    onNew: () -> Unit, onRename: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit,
) {
    DropdownMenu(expanded, onDismissRequest = onDismiss) {
        state.library.characters.forEach { character ->
            DropdownMenuItem(text = { Text(if (character.id == state.library.activeCharacterId) "✓ ${character.name}" else character.name) }, onClick = { onSelect(character.id) })
        }
        HorizontalDivider()
        DropdownMenuItem(text = { Text("New Character") }, onClick = onNew)
        DropdownMenuItem(text = { Text("Rename Character") }, onClick = onRename)
        DropdownMenuItem(text = { Text("Duplicate Character") }, onClick = onDuplicate)
        DropdownMenuItem(text = { Text("Delete Character") }, onClick = onDelete)
    }
}

@Composable
private fun CharacterLibraryPane(
    state: EditorUiState, onSelect: (String) -> Unit, onNew: () -> Unit, onRename: () -> Unit,
    onDuplicate: () -> Unit, onDelete: () -> Unit,
) {
    Column(Modifier.width(180.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("CHARACTERS", style = MaterialTheme.typography.labelSmall)
        state.library.characters.forEach { character ->
            FilterChip(character.id == state.library.activeCharacterId, { onSelect(character.id) }, label = { Text(character.name) }, modifier = Modifier.fillMaxWidth())
        }
        Button(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Text("+ New") }
        TextButton(onClick = onRename, modifier = Modifier.fillMaxWidth()) { Text("Rename") }
        TextButton(onClick = onDuplicate, modifier = Modifier.fillMaxWidth()) { Text("Duplicate") }
        TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Delete") }
    }
}

@Composable
private fun NameDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Rename Character") },
        text = { OutlinedTextField(name, { name = it }, label = { Text("Character name") }, singleLine = true) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PromptPreview(prompt: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Live prompt", style = MaterialTheme.typography.titleLarge)
        Text("Updates as you edit · ${prompt.length} characters", style = MaterialTheme.typography.bodySmall)
        Surface(Modifier.weight(1f).fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
            SelectionContainer(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text(prompt, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
