package com.kelvinsaputra.promptstudio.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.credentials.CredentialStore
import com.kelvinsaputra.promptstudio.domain.CharacterProject
import com.kelvinsaputra.promptstudio.feature.generation.*
import com.kelvinsaputra.promptstudio.generation.model.*
import com.kelvinsaputra.promptstudio.platform.*
import kotlinx.coroutines.*

@Composable
fun GenerationAndHistory(project: CharacterProject, generation: GenerationController, credentials: CredentialStore,
    selection: GenerationSelection, history: HistoryController, restore: (CharacterProject) -> Unit, message: (String) -> Unit) {
    var showHistory by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!showHistory, { showHistory = false }, label = { Text("Generation") })
            FilterChip(showHistory, { showHistory = true }, label = { Text("History") })
        }
        if (showHistory) HistoryPanel(project, history, generation, restore, message)
        else if (platformCapabilities.canGenerateWithByok) GenerationPanel(project, generation, credentials, selection, message)
        else Text("Cloud image generation is unavailable in the web build.\n\nPrompt Studio does not send provider API keys directly from the browser. Use the native app for BYOK generation.")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryPanel(project: CharacterProject, history: HistoryController, generation: GenerationController,
    restore: (CharacterProject) -> Unit, message: (String) -> Unit) {
    val state by history.state.collectAsState()
    val generationState by generation.state.collectAsState()
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<GenerationRecord?>(null) }
    var currentOnly by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboard.current
    val saver = rememberImageSaver(message)
    val record = selected
    if (record == null) {
        Column {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!currentOnly, { currentOnly = false }, label = { Text("All Generations") })
                FilterChip(currentOnly, { currentOnly = true }, label = { Text("Current Character") })
                TextButton(onClick = { scope.launch { history.refresh() } }) { Text("Refresh") }
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            val records = state.records.chronological(if (currentOnly) project.id else null)
            if (state.loading) Text("Loading history…")
            else if (records.isEmpty()) Text("No saved generations yet.")
            // Only visible rows are decoded. Leaving the viewport releases each thumbnail.
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records, key = { it.id }) { entry ->
                    OutlinedCard(onClick = { selected = entry }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HistoryImage(history, entry, thumbnail = true, Modifier.size(72.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.metadata.project.name, style = MaterialTheme.typography.titleSmall)
                                Text("${entry.metadata.provider} · ${entry.metadata.request.model}", style = MaterialTheme.typography.bodySmall)
                                Text(entry.timeLabel, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    } else {
        val supported = ImageModels.all.any { it.id == record.metadata.request.model && it.provider == record.metadata.provider && it.supports(record.metadata.request.output) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = { selected = null }) { Text("← All history") }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text(record.metadata.project.name, style = MaterialTheme.typography.titleLarge)
            Text("Character ID: ${record.metadata.project.id}\n${record.timeLabel}\n${record.metadata.provider} · ${record.metadata.request.model}")
            HistoryImage(history, record, false, Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 520.dp))
            Text("Requested ${record.metadata.request.requestedAspectRatio} → ${record.metadata.request.output.aspectRatio} · ${record.metadata.request.output.size}\n${record.mimeType}${record.metadata.quality?.let { " · $it quality" }.orEmpty()}")
            record.metadata.requestId?.let { Text("Request ID: $it") }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (platformCapabilities.canSaveImage) Button(onClick = { scope.launch {
                    try {
                        val bytes = history.loadImage(record)
                        if (bytes == null) message("Image unavailable") else saver(GeneratedImage(bytes, record.mimeType, record.metadata))
                    } catch (e: CancellationException) { throw e } catch (_: Exception) { message("Could not load the image.") }
                } }) { Text("Save Image") }
                OutlinedButton(onClick = { scope.launch {
                    try { clipboard.copyPromptText(record.metadata.request.prompt); message("Prompt copied") }
                    catch (e: CancellationException) { throw e } catch (_: Exception) { message("Could not copy. Select the prompt text instead.") }
                } }) { Text("Copy Prompt") }
                OutlinedButton(onClick = { confirm = "restore" }) { Text("Restore Configuration") }
                if (platformCapabilities.canGenerateWithByok) OutlinedButton(
                    enabled = supported && generationState.status !is GenerationState.Generating,
                    onClick = { generation.generateAgain(record.metadata); message("Generation requested from the historical snapshot. Open Generation to see progress or enter this provider’s key.") }
                ) { Text("Generate Again") }
                TextButton(onClick = { confirm = "delete" }) { Text("Delete") }
            }
            if (!supported) Text("This historical model/output is no longer supported. Generate Again is unavailable; no replacement has been selected.")
            generationState.status.let { if (it is GenerationState.Error) Text(it.error.message, color = MaterialTheme.colorScheme.error) }
            Text("Compiled prompt snapshot", style = MaterialTheme.typography.titleMedium)
            SelectionContainer { Text(record.metadata.request.prompt) }
        }
        if (confirm != null) AlertDialog(onDismissRequest = { confirm = null },
            title = { Text(if (confirm == "restore") "Restore into ${project.name}?" else "Delete generation?") },
            text = { Text(if (confirm == "restore") "Replace this character’s configuration with the saved snapshot. Its current name and library identity will be preserved." else "Delete this history record and its local image. The character library is unchanged.") },
            confirmButton = { TextButton(onClick = {
                if (confirm == "restore") { restore(record.metadata.project); message("Configuration restored") }
                else scope.launch { history.delete(record); if (history.state.value.records.none { it.id == record.id }) selected = null }
                confirm = null
            }) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } })
    }
}

@Composable
private fun HistoryImage(history: HistoryController, record: GenerationRecord, thumbnail: Boolean, modifier: Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, record.id, thumbnail) {
        try {
            val bytes = history.loadImage(record)
            value = bytes?.let { withContext(Dispatchers.Default) { if (thumbnail) decodeHistoryThumbnail(it) else decodeGeneratedImage(it) } }
        } catch (e: CancellationException) { throw e } catch (_: Exception) { value = null }
    }
    bitmap?.let { Image(it, record.metadata.project.name, modifier, contentScale = ContentScale.Fit) }
        ?: Box(modifier) { Text("Image unavailable", style = MaterialTheme.typography.bodySmall) }
}
