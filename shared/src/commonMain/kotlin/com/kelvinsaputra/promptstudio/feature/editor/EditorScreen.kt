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
import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.platform.textClipEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    state: EditorUiState,
    onModule: (EditorModule) -> Unit,
    onCostume: (CostumeConfiguration) -> Unit,
    onPose: (PoseConfiguration) -> Unit,
    onOutput: (OutputConfiguration) -> Unit,
) {
    val sectionState = rememberSaveableStateHolder()
    val prompt = remember(state.project) { state.compiledPrompt.text }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val validOutput = state.project.output.aspectRatio != null
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).safeDrawingPadding().imePadding().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Prompt Studio", style = MaterialTheme.typography.headlineSmall)
                    Text("Character prompt builder", style = MaterialTheme.typography.bodySmall)
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
                }) { Text("Copy Prompt") }
            }
            if (!validOutput) Text("Enter an aspect ratio in Output before copying.", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            BoxWithConstraints(Modifier.weight(1f)) {
                val wide = maxWidth >= 840.dp
                val showPreview = maxWidth >= 1240.dp && state.module != EditorModule.Prompt
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    if (wide) {
                        Column(Modifier.width(152.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("COMPONENTS", style = MaterialTheme.typography.labelSmall)
                            EditorModule.entries.forEach { module ->
                                FilterChip(state.module == module, { onModule(module) }, label = { Text(module.name) }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                        VerticalDivider()
                    }
                    Column(Modifier.weight(1f)) {
                        if (!wide) {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EditorModule.entries.forEach { module ->
                                    FilterChip(state.module == module, { onModule(module) }, label = { Text(module.name) })
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        if (state.module == EditorModule.Prompt) {
                            PromptPreview(prompt, Modifier.fillMaxSize())
                        } else {
                            sectionState.SaveableStateProvider(state.module.name) {
                                Column(
                                    Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(state.module.name, style = MaterialTheme.typography.titleLarge)
                                    when (state.module) {
                                        EditorModule.Style -> StyleEditor(state.project.style)
                                        EditorModule.Costume -> CostumeEditor(state.project.costume, onCostume)
                                        EditorModule.Pose -> PoseEditor(state.project.pose, onPose)
                                        EditorModule.Output -> OutputEditor(state.project.output, onOutput)
                                        EditorModule.Prompt -> Unit
                                    }
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
