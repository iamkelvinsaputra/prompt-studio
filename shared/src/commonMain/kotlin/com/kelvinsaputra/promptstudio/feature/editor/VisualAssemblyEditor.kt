package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import com.kelvinsaputra.promptstudio.domain.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualAssemblyEditor(project: CharacterProject, onProject: (CharacterProject) -> Unit) {
    val assembly = project.visualAssembly
    var editing by rememberSaveable(project.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Start with a shape", style = MaterialTheme.typography.titleMedium)
        Text("A rough structural guide, not final artwork. Your choices also shape the automatic prompt.", style = MaterialTheme.typography.bodySmall)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val controls: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pose", style = MaterialTheme.typography.titleMedium)
                    FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = 2, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VisualPose.entries.forEach { preset ->
                            val selected = assembly.posePreset == preset
                            Card(onClick = { onProject(project.withVisualPose(preset)) },
                                modifier = Modifier.weight(1f).semantics { this.selected = selected },
                                colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)) {
                                SilhouettePreview(assembly.copy(basePose = preset.base, facing = GuideFacing.FRONT,
                                    framing = Framing.FULL_BODY, propPlacement = GuideProp.NONE, figurePlacement = "center"),
                                    Modifier.fillMaxWidth().height(100.dp), thumbnail = true)
                                Text(preset.label, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }
            if (maxWidth >= 640.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    SilhouettePreview(assembly, Modifier.weight(1f).height(340.dp), project.output.aspectRatio)
                    Box(Modifier.weight(1f)) { controls() }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SilhouettePreview(assembly, Modifier.fillMaxWidth().height(280.dp), project.output.aspectRatio)
                    controls()
                }
            }
        }
        Text(assembly.summary, style = MaterialTheme.typography.bodyMedium)
        if (assembly.hasApproximation) Text("Custom or unspecified choices use a neutral, centered approximation where needed. Detailed pose mechanics and notes are not rendered.", style = MaterialTheme.typography.bodySmall)
        else Text("Preset shapes approximate the pose; detailed mechanics and notes are not rendered.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { editing = !editing }) { Text(if (editing) "Hide quick adjustments" else "Edit · Quick adjustments") }
        if (editing) {
            Choices("Facing", GuideFacing.entries, assembly.facing, { it.label }) { onProject(project.copy(pose = project.pose.copy(guideFacing = it))) }
            Choices("Framing", listOf(Framing.FULL_BODY, Framing.THIGH_UP, Framing.BUST_UP), assembly.framing,
                { when (it) { Framing.FULL_BODY -> "Full"; Framing.THIGH_UP -> "Mid"; else -> "Close" } }) { onProject(project.copy(output = project.output.copy(framing = it))) }
            Choices("Prop", GuideProp.entries, assembly.propPlacement, { it.label }) { onProject(project.copy(pose = project.pose.copy(guideProp = it))) }
            Choices("Composition", VisualPlacement.entries, assembly.placementPreset, { it.label }) { onProject(project.copy(output = project.output.copy(figurePlacement = it.wording))) }
        }
        PromptAuthoringControls(project, onProject)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Choices(title: String, options: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option -> FilterChip(selected == option, { onSelect(option) }, label = { Text(label(option)) }) }
    }
}
