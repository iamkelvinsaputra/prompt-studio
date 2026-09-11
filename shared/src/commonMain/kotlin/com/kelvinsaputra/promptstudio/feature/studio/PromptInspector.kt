package com.kelvinsaputra.promptstudio.feature.studio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import com.kelvinsaputra.promptstudio.platform.copyPromptText
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.SilhouettePreview
import com.kelvinsaputra.promptstudio.prompt.PromptCompiler
import com.kelvinsaputra.promptstudio.prompt.effectivePrompt

@Composable
fun PromptInspector(project: CharacterProject, selected: StudioCategory, onCategory: (StudioCategory) -> Unit,
    modifier: Modifier = Modifier) {
    val compiled = remember(project) { PromptCompiler().compile(project) }
    var textMode by remember { mutableStateOf(false) }
    val manual = project.promptAuthoring.mode == PromptMode.Manual
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("PROMPT INSPECTOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!textMode, { textMode = false }, label = { Text("Structure") })
            FilterChip(textMode, { textMode = true }, label = { Text("Prompt") })
        }
        if (manual) Text("Manual prompt active. Building blocks show your automatic draft.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (textMode) {
                val clipboard = androidx.compose.ui.platform.LocalClipboard.current
                val scope = rememberCoroutineScope()
                var feedback by remember { mutableStateOf("") }
                OutlinedButton(onClick = { scope.launch { try { clipboard.copyPromptText(project.effectivePrompt()); feedback = "Prompt copied" } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { feedback = "Could not copy; select the text below." } } }) { Text("Copy prompt") }
                if (feedback.isNotBlank()) Text(feedback, style = MaterialTheme.typography.bodySmall)
                SelectionContainer { Text(project.effectivePrompt(), style = MaterialTheme.typography.bodyMedium) }
            }
            else {
                Text(project.characterName, style = MaterialTheme.typography.titleLarge)
                StudioCategory.entries.forEach { category ->
                    val sections = compiled.sections.filter { it.title in category.sections }
                    if (sections.isNotEmpty()) {
                        Surface(onClick = { onCategory(category) }, shape = MaterialTheme.shapes.small,
                            color = if (category == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(category.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                sections.forEach { section -> Text(section.body, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
        }
        Text("${project.effectivePrompt().length} characters · ${if (manual) "Manual" else "Updates as you choose"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
