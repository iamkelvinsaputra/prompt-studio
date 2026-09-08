package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CostumeEditor(value: CostumeConfiguration, onChange: (CostumeConfiguration) -> Unit) {
    Text("Choose one item per category. Leave a category unset to describe it in custom notes.")
    TextField("Outfit identity", value.outfitIdentity) { onChange(value.copy(outfitIdentity = it)) }
    OptionField("Silhouette", value.silhouette, Silhouette.entries) { onChange(value.copy(silhouette = it)) }
    OptionField("Upper outerwear", value.outerwear, Outerwear.entries) { onChange(value.copy(outerwear = it)) }
    OptionField("Innerwear", value.innerwear, Innerwear.entries) { onChange(value.copy(innerwear = it)) }
    OptionField("Lower wear", value.lowerWear, LowerWear.entries) { onChange(value.copy(lowerWear = it)) }
    OptionField("Legwear", value.legwear, Legwear.entries) { onChange(value.copy(legwear = it)) }
    OptionField("Footwear", value.footwear, Footwear.entries) { onChange(value.copy(footwear = it)) }
    OptionField("Handwear", value.handwear, Handwear.entries) { onChange(value.copy(handwear = it)) }
    OptionField("Utility / support", value.utility, Utility.entries) { onChange(value.copy(utility = it)) }
    Text("Personal customization (${value.customization.size}/2)", style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Customization.entries.forEach { marker ->
            val selected = marker in value.customization
            FilterChip(
                selected = selected,
                enabled = selected || value.customization.size < 2,
                onClick = { onChange(value.copy(customization = if (selected) value.customization - marker else value.customization + marker)) },
                label = { Text(marker.wording) },
            )
        }
    }
    TextField("Material feel", value.materialFeel) { onChange(value.copy(materialFeel = it)) }
    TextField("Exposure level", value.exposureLevel) { onChange(value.copy(exposureLevel = it)) }
    TextField("Custom costume notes", value.customNotes, multiline = true) { onChange(value.copy(customNotes = it)) }
}
