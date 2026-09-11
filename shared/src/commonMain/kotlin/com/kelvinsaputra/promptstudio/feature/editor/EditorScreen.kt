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
import com.kelvinsaputra.promptstudio.domain.GuidedNavigation
import com.kelvinsaputra.promptstudio.domain.CharacterProject
import com.kelvinsaputra.promptstudio.domain.PromptMode
import com.kelvinsaputra.promptstudio.prompt.useAutomaticPrompt
import com.kelvinsaputra.promptstudio.platform.copyPromptText
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
    generationContent: @Composable () -> Unit = {},
    onProjects: (() -> Unit)? = null,
    variantContent: @Composable () -> Unit = {},
    presetContent: @Composable () -> Unit = {},
    onGuideNavigation: (GuidedNavigation) -> Unit = {},
    useVisualGuide: Boolean = true,
    onVisualGuideChange: (Boolean) -> Unit = {},
) {
    val sectionState = rememberSaveableStateHolder()
    val prompt = remember(state.project) { state.effectivePrompt }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var projectMenu by remember { mutableStateOf(false) }
    var characterMenu by remember { mutableStateOf(false) }
    var renameDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var generating by remember(state.project.id, state.library.activeVariants.activeId) { mutableStateOf(false) }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); onDismissMessage() }
    }
    val manual = state.project.promptAuthoring.mode == PromptMode.Manual
    val validOutput = manual || state.project.output.aspectRatio != null
    val visibleModules = if (state.mode == EditorMode.Quick) quickModules else EditorModule.entries.filter { it != EditorModule.Prompt }

    if (renameDialog) NameDialog(state.project.name, onDismiss = { renameDialog = false }) {
        onRenameCharacter(it); renameDialog = false
    }
    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("Delete ${state.project.name}?") },
            text = { Text("Remove this project and its variants. Saved generations remain in history. The last project is replaced with a blank project.") },
            confirmButton = { TextButton(onClick = { onDeleteCharacter(); deleteDialog = false }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("Cancel") } },
        )
    }

    // Visual Build has its own focused shell; detailed authoring stays available from Summary.
    if (state.module == EditorModule.VisualBuild) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).safeDrawingPadding().imePadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    onProjects?.let { TextButton(onClick = it) { Text("← Projects") } }
                    Text(state.project.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Box {
                        TextButton(onClick = { projectMenu = true }) { Text("File") }
                        DropdownMenu(projectMenu, onDismissRequest = { projectMenu = false }) {
                            DropdownMenuItem(text = { Text("Import Character") }, onClick = { projectMenu = false; onImport() })
                            DropdownMenuItem(text = { Text("Export Character") }, onClick = { projectMenu = false; onExport() })
                            DropdownMenuItem(text = { Text("Copy prompt") }, onClick = {
                                projectMenu = false
                                scope.launch {
                                    try { clipboard.copyPromptText(prompt); snackbar.showSnackbar("Prompt copied") }
                                    catch (e: CancellationException) { throw e }
                                    catch (_: Exception) { snackbar.showSnackbar("Could not copy prompt") }
                                }
                            })
                        }
                    }
                }
                state.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = onRetrySave) { Text("Retry Save") } }
                if (generating) {
                    TextButton(onClick = { generating = false }) { Text("← Summary") }
                    Box(Modifier.weight(1f)) { generationContent() }
                } else GuidedVisualBuild(state.project, state.guided, onProject, onGuideNavigation,
                    onGenerate = { generating = true }, onAdvanced = { onMode(EditorMode.Advanced); onModule(EditorModule.Identity) },
                    modifier = Modifier.weight(1f).fillMaxWidth(), useVisualGuide = useVisualGuide, onVisualGuide = onVisualGuideChange,
                    variantContent = variantContent, presetContent = presetContent)
            }
        }
        return
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).safeDrawingPadding().imePadding().padding(16.dp)) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    Text("Prompt Studio", style = MaterialTheme.typography.headlineSmall)
                    Text("Visual prompt authoring", style = MaterialTheme.typography.bodySmall)
                }
                onProjects?.let { TextButton(onClick = it) { Text("← Projects") } }
                Box {
                    TextButton(onClick = { characterMenu = true }) { Text("${state.project.name} ▾", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 220.dp)) }
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
                    TextButton(onClick = { projectMenu = true }) { Text("File ▾") }
                    DropdownMenu(projectMenu, onDismissRequest = { projectMenu = false }) {
                        DropdownMenuItem(text = { Text("Import Character") }, onClick = { projectMenu = false; onImport() })
                        DropdownMenuItem(text = { Text("Export Character") }, onClick = { projectMenu = false; onExport() })
                    }
                }
                Button(enabled = validOutput && prompt.isNotBlank(), onClick = {
                    scope.launch {
                        try {
                            clipboard.copyPromptText(prompt)
                            snackbar.showSnackbar("Prompt copied")
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            snackbar.showSnackbar("Could not copy. Try again or select the preview text.")
                        }
                    }
                }) { Text("Copy") }
                Button(onClick = { generating = true }) { Text("Generate") }
            }
            variantContent()
            state.saveError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onRetrySave) { Text("Retry Save") }
            }
            if (!validOutput) Text("Enter an aspect ratio in Output before copying.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (manual) {
                Text("Manual prompt active · Visual edits update only the automatic draft.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { onProject(state.project.useAutomaticPrompt()) }) { Text("Use automatic prompt") }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { onModule(EditorModule.VisualBuild) }) { Text("← Summary") }
            if (!generating) ModeAndVariationControls(state, onMode, onVariationLocks, onRandomizeUnlocked)
            Spacer(Modifier.height(12.dp))

            BoxWithConstraints(Modifier.weight(1f)) {
                if (generating) {
                    Column(Modifier.fillMaxSize()) {
                        TextButton(onClick = { generating = false }) { Text("← Back to editor") }
                        generationContent()
                    }
                    return@BoxWithConstraints
                }
                val showLibraryPane = maxWidth >= 1080.dp
                val showModulesPane = maxWidth >= 820.dp
                val showPreview = maxWidth >= 1200.dp && state.module != EditorModule.Prompt && state.module != EditorModule.VisualBuild
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
                            PromptPreview(prompt, manual, Modifier.fillMaxSize(), generationContent)
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
                                    if (state.module == EditorModule.VisualBuild) presetContent()
                                    TextButton(onClick = { onModule(EditorModule.Prompt) }) { Text(if (manual) "Inspect manual prompt →" else "Compiled prompt →") }
                                }
                            }
                        }
                    }
                    if (showPreview) {
                        VerticalDivider()
                        PromptPreview(prompt, manual, Modifier.weight(1f).fillMaxHeight(), generationContent)
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
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(state.mode == EditorMode.Quick, { onMode(EditorMode.Quick) }, label = { Text("Quick") })
        FilterChip(state.mode == EditorMode.Advanced, { onMode(EditorMode.Advanced) }, label = { Text("All sections") })
        if (state.mode == EditorMode.Advanced) Button(onClick = onRandomize) { Text("Randomize Unlocked") }
    }
    if (state.mode != EditorMode.Advanced) return
    var showLocks by remember { mutableStateOf(false) }
    TextButton(onClick = { showLocks = !showLocks }) { Text("${if (showLocks) "Hide" else "Show"} variation locks · ${state.variationLocks.size} locked") }
    if (!showLocks) return
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
        DropdownMenuItem(text = { Text("New Project") }, onClick = onNew)
        DropdownMenuItem(text = { Text("Rename Project") }, onClick = onRename)
        DropdownMenuItem(text = { Text("Duplicate Project") }, onClick = onDuplicate)
        DropdownMenuItem(text = { Text("Delete Project") }, onClick = onDelete)
    }
}

@Composable
private fun CharacterLibraryPane(
    state: EditorUiState, onSelect: (String) -> Unit, onNew: () -> Unit, onRename: () -> Unit,
    onDuplicate: () -> Unit, onDelete: () -> Unit,
) {
    Column(Modifier.width(180.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("PROJECTS", style = MaterialTheme.typography.labelSmall)
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
        onDismissRequest = onDismiss, title = { Text("Rename Project") },
        text = { OutlinedTextField(name, { name = it }, label = { Text("Project name") }, singleLine = true) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PromptPreview(prompt: String, manual: Boolean, modifier: Modifier = Modifier, generationContent: @Composable () -> Unit) {
    var generate by remember { mutableStateOf(false) }
    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!generate, { generate = false }, label = { Text("Prompt") })
            FilterChip(generate, { generate = true }, label = { Text("Generate Image") })
        }
        if (generate) { generationContent(); return@Column }
        PromptText(prompt, manual, Modifier.weight(1f))
    }
}

@Composable
private fun PromptText(prompt: String, manual: Boolean, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (manual) "Manual prompt" else "Compiled prompt", style = MaterialTheme.typography.titleLarge)
        Text("${if (manual) "Your manual text is active" else "Updates from your choices and adjustment"} · ${prompt.length} characters", style = MaterialTheme.typography.bodySmall)
        Surface(Modifier.weight(1f).fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
            SelectionContainer(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text(prompt, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
