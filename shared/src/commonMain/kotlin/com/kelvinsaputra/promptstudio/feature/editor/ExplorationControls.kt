package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun LockableField(label: String, locked: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(Modifier.weight(1f)) { content() }
        IconToggleButton(
            checked = locked, onCheckedChange = { onToggle() },
            modifier = Modifier.semantics { contentDescription = "${if (locked) "Unlock" else "Lock"} $label for randomization" },
        ) { Text(if (locked) "🔒" else "🔓") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExplorationActions(
    module: String,
    allLocked: Boolean,
    onRandomize: () -> Unit,
    onReset: () -> Unit,
    onLockAll: () -> Unit,
    onUnlockAll: () -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onRandomize, enabled = !allLocked) { Text("Randomize $module") }
        OutlinedButton(onClick = onReset) { Text("Reset $module") }
        TextButton(onClick = onLockAll, enabled = !allLocked) { Text("Lock All") }
        TextButton(onClick = onUnlockAll) { Text("Unlock All") }
    }
    Text("Locks protect choices from randomization. Authored text stays unchanged.", style = MaterialTheme.typography.bodySmall)
    Text("Reset restores all demo values, including locked fields and notes. Lock selections stay unchanged.", style = MaterialTheme.typography.bodySmall)
}
