package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.studio.*
import com.kelvinsaputra.promptstudio.guide.GuideRenderSpec
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
    val project = state.project
    val category = StudioCategory.from(state.module)
    val sectionState = rememberSaveableStateHolder()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(false) }
    var view by remember(project.id, state.library.activeVariants.activeId) { mutableStateOf("editor") }
    var library by remember { mutableStateOf(false) }
    val choose: (StudioCategory) -> Unit = { onModule(it.modules.first()); view = "editor" }
    val canCopy = state.effectivePrompt.isNotBlank() && (project.promptAuthoring.mode == PromptMode.Manual || project.output.aspectRatio != null)
    val copy: () -> Unit = {
        scope.launch {
            try { clipboard.copyPromptText(state.effectivePrompt); snackbar.showSnackbar("Prompt copied") }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { snackbar.showSnackbar("Could not copy. Select the compiled prompt to copy it manually.") }
        }
    }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); onDismissMessage() } }
    if (rename) SimpleNameDialog("Rename project", project.name, { rename = false }) { onRenameCharacter(it); rename = false }
    if (delete) AlertDialog(onDismissRequest = { delete = false }, title = { Text("Delete ${project.name}?") },
        text = { Text("This removes the project and its variants. Saved generations remain in history.") },
        confirmButton = { TextButton(onClick = { onDeleteCharacter(); delete = false }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { delete = false }) { Text("Cancel") } })
    if (reset) AlertDialog(onDismissRequest = { reset = false }, title = { Text("Reset this configuration?") },
        text = { Text("Restore a neutral starting point for this variant. Save a preset first if you want to reuse your current choices.") },
        confirmButton = { TextButton(onClick = { onProject(CharacterLibrary.newCharacter(project.id, project.name).withStudioDefaults()); reset = false }) { Text("Reset all") } },
        dismissButton = { TextButton(onClick = { reset = false }) { Text("Cancel") } })
    VisualGuideAssets {
    Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = 76.dp)) }) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).safeDrawingPadding().imePadding()) {
            val desktop = maxWidth >= 1180.dp
            val rail = maxWidth >= 760.dp
            val compact = maxWidth < 600.dp
            Column(Modifier.fillMaxSize()) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = if (compact) 16.dp else 24.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            onProjects?.let { TextButton(onClick = it) { Text("Projects") } }
                            Column(Modifier.weight(1f)) {
                                Text("PROMPT STUDIO", style = MaterialTheme.typography.labelSmall)
                                Text(project.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                            }
                            Box {
                                TextButton(onClick = { menu = true }) { Text("Project menu") }
                                DropdownMenu(menu, { menu = false }) {
                                    state.library.characters.forEach { character -> DropdownMenuItem(text = { Text(character.name) }, onClick = { onSelectCharacter(character.id); menu = false }) }
                                    HorizontalDivider()
                                    DropdownMenuItem(text = { Text("New project") }, onClick = { onNewCharacter(); menu = false })
                                    DropdownMenuItem(text = { Text("Rename project") }, onClick = { rename = true; menu = false })
                                    DropdownMenuItem(text = { Text("Duplicate project") }, onClick = { onDuplicateCharacter(); menu = false })
                                    DropdownMenuItem(text = { Text("Import Character") }, onClick = { onImport(); menu = false })
                                    DropdownMenuItem(text = { Text("Export Character") }, onClick = { onExport(); menu = false })
                                    DropdownMenuItem(text = { Text("Reset all") }, onClick = { reset = true; menu = false })
                                    DropdownMenuItem(text = { Text("Delete project") }, onClick = { delete = true; menu = false })
                                }
                            }
                            if (!compact && view != "generate") {
                                Button(onClick = { view = "generate" }) { Text("Generate") }
                            }
                        }
                        if (project.promptAuthoring.mode != PromptMode.Manual && project.output.aspectRatio == null) Text("Choose an aspect ratio in Output before copying.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        state.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = onRetrySave) { Text("Retry Save") } }
                    }
                }
                HorizontalDivider()
                if (view == "generate") {
                    TextButton(onClick = { view = "editor" }) { Text("Back to studio") }
                    val supported = GuideRenderSpec.from(project.visualAssembly, project.output.aspectRatio) != null
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(useVisualGuide && supported, onVisualGuideChange, enabled = supported)
                        Text(if (supported) "Include structural guide for supported models" else "Custom configuration · text only", style = MaterialTheme.typography.bodySmall)
                    }
                    Box(Modifier.weight(1f).padding(16.dp)) { generationContent() }
                } else {
                    if (!rail && view == "editor") Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StudioCategory.entries.forEach { item -> FilterChip(category == item, { choose(item) }, label = { Text(item.title) }) }
                    }
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        if (rail) StudioSidebar(category, state, choose, { library = !library }, Modifier.width(210.dp).fillMaxHeight())
                        if (view == "prompt") {
                            Column(Modifier.weight(1f).padding(20.dp)) {
                                TextButton(onClick = { view = "editor" }) { Text("Back to studio") }
                                PromptInspector(project, category, choose, Modifier.weight(1f))
                            }
                        } else {
                            sectionState.SaveableStateProvider("${project.id}-${category.name}") {
                                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(if (compact) 18.dp else 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${(category.ordinal + 1).toString().padStart(2, '0')} / CREATIVE DIRECTION", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        if (!rail) TextButton(onClick = { library = !library }) { Text("Presets & variants") }
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(category.title, style = MaterialTheme.typography.headlineLarge)
                                        Text(category.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    AdvancedSection("Section actions") {
                                        TextButton(onClick = { onProject(project.resetStudioCategory(category)) }) { Text("Reset category") }
                                        if (category == StudioCategory.Pose) TextButton(onClick = onRandomizePose, enabled = VariationField.Pose !in state.variationLocks) { Text("Randomize pose") }
                                        if (category == StudioCategory.Appearance) TextButton(onClick = onRandomizeCostume, enabled = VariationField.Costume !in state.variationLocks) { Text("Randomize outfit") }
                                        if (category == StudioCategory.Camera || category == StudioCategory.Color) TextButton(enabled = (if (category == StudioCategory.Camera) VariationField.Composition else VariationField.AccentColor) !in state.variationLocks, onClick = { onProject(project.randomizeStudioCategory(category)) }) { Text("Randomize ${if (category == StudioCategory.Camera) "camera" else "colors"}") }
                                    }
                                    if (library) {
                                        variantContent(); presetContent()
                                        AdvancedSection("Variation controls") {
                                            Text("Locks protect categories when exploring. Your custom wording stays in place.", style = MaterialTheme.typography.bodySmall)
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                VariationField.entries.forEach { field -> FilterChip(field in state.variationLocks,
                                                    { onVariationLocks(if (field in state.variationLocks) state.variationLocks - field else state.variationLocks + field) },
                                                    label = { Text("${if (field in state.variationLocks) "Locked · " else ""}${field.name}") }) }
                                            }
                                            OutlinedButton(onClick = onRandomizeUnlocked) { Text("Randomize unlocked") }
                                        }
                                        HorizontalDivider()
                                    }
                                    if (project.promptAuthoring.mode == PromptMode.Manual) Text("Manual prompt active. Your choices update the automatic draft; switch back in Output to use them.", color = MaterialTheme.colorScheme.primary)
                                    CategoryPanel(category, state, onProject, onCostumeLocks, onPoseLocks, onRandomizeCostume, onRandomizePose, onResetCostume, onResetPose)
                                    if (category.ordinal < StudioCategory.entries.lastIndex) OutlinedButton(onClick = { choose(StudioCategory.entries[category.ordinal + 1]) }) { Text("Next · ${StudioCategory.entries[category.ordinal + 1].title}") }
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                        }
                        if (desktop && view == "editor") {
                            VerticalDivider()
                            Surface(Modifier.width(336.dp).fillMaxHeight(), color = MaterialTheme.colorScheme.surface) {
                                PromptInspector(project, category, choose, Modifier.padding(20.dp))
                            }
                        }
                    }
                    if (!desktop) Surface(shadowElevation = 4.dp) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { view = if (view == "prompt") "editor" else "prompt" }, modifier = Modifier.weight(1f)) { Text(if (view == "prompt") "Edit" else "Inspect prompt") }
                            if (compact) {
                                Button(onClick = { view = "generate" }) { Text("Generate") }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun StudioSidebar(selected: StudioCategory, state: EditorUiState, onSelect: (StudioCategory) -> Unit, onLibrary: () -> Unit, modifier: Modifier) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("YOUR BUILDING BLOCKS", Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(StudioCategory.entries) { category ->
                Surface(onClick = { onSelect(category) }, shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().semantics { this.selected = category == selected },
                    color = if (category == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(category.title, style = MaterialTheme.typography.labelLarge, color = if (category == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        Text(category.summary(state.project), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        HorizontalDivider()
        TextButton(onClick = onLibrary, modifier = Modifier.fillMaxWidth()) { Text("Presets & variants") }
        Text("Made of small decisions.", Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
