package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*

@Composable
fun PoseEditor(
    value: PoseConfiguration,
    onChange: (PoseConfiguration) -> Unit,
    locks: Set<PoseField>,
    onLocks: (Set<PoseField>) -> Unit,
    onRandomize: () -> Unit,
    onReset: () -> Unit,
) {
    ExplorationActions("Pose", locks.size == PoseField.entries.size, onRandomize, onReset,
        onLockAll = { onLocks(PoseField.entries.toSet()) }, onUnlockAll = { onLocks(emptySet()) })
    Text("Choose one value per category; use notes for a custom pose.")
    LockableField("Base pose", PoseField.BasePose in locks, {
        onLocks(if (PoseField.BasePose in locks) locks - PoseField.BasePose else locks + PoseField.BasePose)
    }) {
        OptionField("Base pose", value.basePose, BasePose.entries) { onChange(value.copy(basePose = it)) }
    }
    LockableField("Weight distribution", PoseField.Weight in locks, {
        onLocks(if (PoseField.Weight in locks) locks - PoseField.Weight else locks + PoseField.Weight)
    }) {
        OptionField("Weight distribution", value.weight, Weight.entries) { onChange(value.copy(weight = it)) }
    }
    TextField("Leg action", value.legAction) { onChange(value.copy(legAction = it)) }
    LockableField("Torso action", PoseField.Torso in locks, {
        onLocks(if (PoseField.Torso in locks) locks - PoseField.Torso else locks + PoseField.Torso)
    }) {
        OptionField("Torso action", value.torso, Torso.entries) { onChange(value.copy(torso = it)) }
    }
    LockableField("Arm action", PoseField.Arms in locks, {
        onLocks(if (PoseField.Arms in locks) locks - PoseField.Arms else locks + PoseField.Arms)
    }) {
        OptionField("Arm action", value.arms, Arms.entries) { onChange(value.copy(arms = it)) }
    }
    LockableField("Head angle", PoseField.Head in locks, {
        onLocks(if (PoseField.Head in locks) locks - PoseField.Head else locks + PoseField.Head)
    }) {
        OptionField("Head angle", value.head, Head.entries) { onChange(value.copy(head = it)) }
    }
    LockableField("Gaze", PoseField.Gaze in locks, {
        onLocks(if (PoseField.Gaze in locks) locks - PoseField.Gaze else locks + PoseField.Gaze)
    }) {
        OptionField("Gaze", value.gaze, Gaze.entries) { onChange(value.copy(gaze = it)) }
    }
    LockableField("Energy", PoseField.Energy in locks, {
        onLocks(if (PoseField.Energy in locks) locks - PoseField.Energy else locks + PoseField.Energy)
    }) {
        OptionField("Energy", value.energy, Energy.entries) { onChange(value.copy(energy = it)) }
    }
    TextField("Motion direction", value.motionDirection) { onChange(value.copy(motionDirection = it)) }
    TextField("Custom pose notes", value.customNotes, multiline = true) { onChange(value.copy(customNotes = it)) }
}
