package com.kelvinsaputra.promptstudio.platform

import androidx.compose.runtime.Composable
import com.kelvinsaputra.promptstudio.persistence.ProjectStorage

@Composable
expect fun rememberProjectStorage(): ProjectStorage

class ProjectFileActions(val importProject: () -> Unit, val exportProject: () -> Unit)

// Only native pickers and byte I/O differ; validation and state replacement remain shared.
@Composable
expect fun rememberProjectFileActions(
    exportJson: () -> String,
    onImport: (String) -> Unit,
    onMessage: (String) -> Unit,
): ProjectFileActions
