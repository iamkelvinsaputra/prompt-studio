package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*

@Composable
fun PoseEditor(value: PoseConfiguration, onChange: (PoseConfiguration) -> Unit) {
    Text("Choose one value per category; use notes for a custom pose.")
    OptionField("Base pose", value.basePose, BasePose.entries) { onChange(value.copy(basePose = it)) }
    OptionField("Weight distribution", value.weight, Weight.entries) { onChange(value.copy(weight = it)) }
    TextField("Leg action", value.legAction) { onChange(value.copy(legAction = it)) }
    OptionField("Torso action", value.torso, Torso.entries) { onChange(value.copy(torso = it)) }
    OptionField("Arm action", value.arms, Arms.entries) { onChange(value.copy(arms = it)) }
    OptionField("Head angle", value.head, Head.entries) { onChange(value.copy(head = it)) }
    OptionField("Gaze", value.gaze, Gaze.entries) { onChange(value.copy(gaze = it)) }
    OptionField("Energy", value.energy, Energy.entries) { onChange(value.copy(energy = it)) }
    TextField("Motion direction", value.motionDirection) { onChange(value.copy(motionDirection = it)) }
    TextField("Custom pose notes", value.customNotes, multiline = true) { onChange(value.copy(customNotes = it)) }
}
