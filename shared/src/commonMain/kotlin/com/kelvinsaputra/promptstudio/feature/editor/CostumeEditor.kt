package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CostumeEditor(
    value: CostumeConfiguration,
    onChange: (CostumeConfiguration) -> Unit,
    locks: Set<CostumeField>,
    onLocks: (Set<CostumeField>) -> Unit,
    onRandomize: () -> Unit,
    onReset: () -> Unit,
) {
    ExplorationActions("Costume", locks.size == CostumeField.entries.size, onRandomize, onReset,
        onLockAll = { onLocks(CostumeField.entries.toSet()) }, onUnlockAll = { onLocks(emptySet()) })
    Text("Choose one item per category. Leave a category unset to describe it in custom notes.")
    TextField("Outfit identity", value.outfitIdentity) { onChange(value.copy(outfitIdentity = it)) }
    LockableField("Silhouette", CostumeField.Silhouette in locks, {
        onLocks(if (CostumeField.Silhouette in locks) locks - CostumeField.Silhouette else locks + CostumeField.Silhouette)
    }) {
        OptionField("Silhouette", value.silhouette, Silhouette.entries) { onChange(value.copy(silhouette = it)) }
    }
    LockableField("Upper outerwear", CostumeField.Outerwear in locks, {
        onLocks(if (CostumeField.Outerwear in locks) locks - CostumeField.Outerwear else locks + CostumeField.Outerwear)
    }) {
        OptionField("Upper outerwear", value.outerwear, Outerwear.entries) { onChange(value.copy(outerwear = it)) }
    }
    LockableField("Innerwear", CostumeField.Innerwear in locks, {
        onLocks(if (CostumeField.Innerwear in locks) locks - CostumeField.Innerwear else locks + CostumeField.Innerwear)
    }) {
        OptionField("Innerwear", value.innerwear, Innerwear.entries) { onChange(value.copy(innerwear = it)) }
    }
    LockableField("Lower wear", CostumeField.LowerWear in locks, {
        onLocks(if (CostumeField.LowerWear in locks) locks - CostumeField.LowerWear else locks + CostumeField.LowerWear)
    }) {
        OptionField("Lower wear", value.lowerWear, LowerWear.entries) { onChange(value.copy(lowerWear = it)) }
    }
    LockableField("Legwear", CostumeField.Legwear in locks, {
        onLocks(if (CostumeField.Legwear in locks) locks - CostumeField.Legwear else locks + CostumeField.Legwear)
    }) {
        OptionField("Legwear", value.legwear, Legwear.entries) { onChange(value.copy(legwear = it)) }
    }
    LockableField("Footwear", CostumeField.Footwear in locks, {
        onLocks(if (CostumeField.Footwear in locks) locks - CostumeField.Footwear else locks + CostumeField.Footwear)
    }) {
        OptionField("Footwear", value.footwear, Footwear.entries) { onChange(value.copy(footwear = it)) }
    }
    LockableField("Handwear", CostumeField.Handwear in locks, {
        onLocks(if (CostumeField.Handwear in locks) locks - CostumeField.Handwear else locks + CostumeField.Handwear)
    }) {
        OptionField("Handwear", value.handwear, Handwear.entries) { onChange(value.copy(handwear = it)) }
    }
    LockableField("Utility / support", CostumeField.Utility in locks, {
        onLocks(if (CostumeField.Utility in locks) locks - CostumeField.Utility else locks + CostumeField.Utility)
    }) {
        OptionField("Utility / support", value.utility, Utility.entries) { onChange(value.copy(utility = it)) }
    }
    LockableField("Personal customization", CostumeField.Customization in locks, {
        onLocks(if (CostumeField.Customization in locks) locks - CostumeField.Customization else locks + CostumeField.Customization)
    }) {
        Column {
            Text("Personal customization (${value.customization.size}/2)", style = MaterialTheme.typography.titleSmall)
            Text("Maximum 2. Remove a selection to choose another.", style = MaterialTheme.typography.bodySmall)
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
        }
    }
    TextField("Material feel", value.materialFeel) { onChange(value.copy(materialFeel = it)) }
    TextField("Exposure level", value.exposureLevel) { onChange(value.copy(exposureLevel = it)) }
    TextField("Custom costume notes", value.customNotes, multiline = true) { onChange(value.copy(customNotes = it)) }
}
