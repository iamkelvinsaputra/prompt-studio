package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.PromptOption

@Composable
fun <T : PromptOption> OptionField(label: String, value: T?, options: List<T>, onChange: (T?) -> Unit) {
    ChoiceField(label, value, listOf(null) + options, { it?.wording ?: "Not set" }, onChange)
}

@Composable
fun <T> ChoiceField(label: String, value: T, options: List<T>, wording: (T) -> String, onChange: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(wording(value), modifier = Modifier.weight(1f))
                Text("▾")
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 320.dp)) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(wording(option)) }, onClick = {
                        onChange(option)
                        expanded = false
                    })
                }
            }
        }
    }
}

@Composable
fun TextField(label: String, value: String, multiline: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = !multiline, minLines = if (multiline) 3 else 1,
    )
}

@Composable
fun ToggleField(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f).padding(top = 12.dp))
        Switch(checked, onCheckedChange = onChange)
    }
}
