package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.prompt.*

/** Disclosure is UI state; every authored keystroke goes through the existing project update path. */
@Composable
fun PromptAuthoringControls(project: CharacterProject, onProject: (CharacterProject) -> Unit) {
    val authoring = project.promptAuthoring
    val manual = authoring.mode == PromptMode.Manual
    var adjustmentOpen by rememberSaveable(project.id) { mutableStateOf(false) }
    var advancedOpen by rememberSaveable(project.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { adjustmentOpen = !adjustmentOpen }) {
            Text(if (authoring.adjustmentText.isBlank()) "Describe adjustment" else "Describe adjustment · Active")
        }
        if (adjustmentOpen) {
            Text(if (manual) "Changes update your automatic draft. Your manual prompt stays unchanged."
                else "Describe what should change. This takes priority in the prompt and leaves the silhouette unchanged.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(authoring.adjustmentText,
                { onProject(project.copy(promptAuthoring = authoring.copy(adjustmentText = it))) },
                label = { Text("Describe adjustment") }, minLines = 2, maxLines = 5, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { adjustmentOpen = false }) { Text("Done") }
                TextButton(enabled = authoring.adjustmentText.isNotEmpty(), onClick = {
                    onProject(project.copy(promptAuthoring = authoring.copy(adjustmentText = "")))
                }) { Text("Clear adjustment") }
            }
        }
        HorizontalDivider()
        if (manual) Text("Advanced · Manual active", style = MaterialTheme.typography.labelLarge)
        else TextButton(onClick = { advancedOpen = !advancedOpen }) { Text(if (advancedOpen) "Hide Advanced" else "Advanced") }
        if (advancedOpen || manual) {
            if (manual) {
                Text("Manual Prompt", style = MaterialTheme.typography.titleMedium)
                Text("This text is used for Copy and generation. Visual choices and adjustments remain saved separately and cannot overwrite it.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(authoring.manualDraft.orEmpty(), { onProject(project.editManualPrompt(it)) },
                    label = { Text("Manual prompt text") }, minLines = 6, maxLines = 12,
                    modifier = Modifier.fillMaxWidth())
                if (authoring.manualDraft.isNullOrBlank()) Text("Enter prompt text before copying or generating.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { onProject(project.useAutomaticPrompt()); advancedOpen = false }) { Text("Return to automatic") }
            } else {
                Text("Take control of the full prompt. Start from the current compiled prompt; returning to automatic keeps your visual choices and adjustment.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { onProject(project.enterManualPrompt()) }) { Text("Manual Prompt") }
                if (authoring.manualDraft != null) {
                    TextButton(onClick = { onProject(project.resumeManualDraft()) }) { Text("Resume saved manual draft") }
                }
            }
        }
    }
}
