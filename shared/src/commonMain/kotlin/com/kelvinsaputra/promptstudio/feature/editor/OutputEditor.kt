package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kelvinsaputra.promptstudio.domain.*

@Composable
fun OutputEditor(value: OutputConfiguration, onChange: (OutputConfiguration) -> Unit) {
    ChoiceField("Output type", value.type, OutputType.entries, { it.label }) { onChange(value.copy(type = it)) }
    if (value.type == OutputType.CUSTOM) {
        TextField("Custom output intent", value.customIntent) { onChange(value.copy(customIntent = it)) }
        OutlinedTextField(
            value = value.customAspectRatio, onValueChange = { onChange(value.copy(customAspectRatio = it)) },
            label = { Text("Aspect ratio") }, supportingText = { Text("Enter a ratio or description, e.g. 3:2 or widescreen. Required before copying.") },
            isError = value.aspectRatio == null, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
    } else {
        Text("Aspect ratio: ${value.aspectRatio}", style = MaterialTheme.typography.titleMedium)
    }
    OptionField("Framing", value.framing, Framing.entries) { onChange(value.copy(framing = it)) }
    TextField("Figure placement", value.figurePlacement) { onChange(value.copy(figurePlacement = it)) }
    TextField("Negative space", value.negativeSpace) { onChange(value.copy(negativeSpace = it)) }
    TextField("Safe area notes", value.safeArea) { onChange(value.copy(safeArea = it)) }
    if (value.type.isWallpaper) {
        ToggleField("Clock-safe area", value.clockSafe) { onChange(value.copy(clockSafe = it)) }
        ToggleField("Icon-safe area", value.iconSafe) { onChange(value.copy(iconSafe = it)) }
    }
}
