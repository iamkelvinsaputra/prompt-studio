package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceLayout(options: List<T>, compact: Boolean, content: @Composable (T, Modifier) -> Unit) {
    if (compact) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { content(it, Modifier.width(154.dp)) }
    } else BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = (maxWidth - 12.dp) / 2
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { content(it, Modifier.width(width)) }
        }
    }
}

@Composable
private fun VisualChoiceCard(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = modifier.semantics(mergeDescendants = true) {
        this.selected = selected; role = Role.RadioButton; contentDescription = label
    }, border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)) {
        content()
        Text(label, Modifier.padding(12.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun PoseAdjustmentControls(project: CharacterProject, onProject: (CharacterProject) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = !open }) { Text(if (open) "Hide pose adjustment" else "Adjust Pose") }
    if (!open) return
    Text("Fine-tune the arms. Choosing a pose resets these adjustments.", style = MaterialTheme.typography.bodySmall)
    if (project.visualAssembly.posePreset == null) {
        Text("Choose a visual pose to adjust its guide."); return
    }
    val a = project.pose.effectiveAdjustments
    RotationControl("Left shoulder", a.leftShoulder) { onProject(project.withPoseAdjustments(a.copy(leftShoulder = it))) }
    RotationControl("Right shoulder", a.rightShoulder) { onProject(project.withPoseAdjustments(a.copy(rightShoulder = it))) }
    RotationControl("Left elbow", a.leftElbow) { onProject(project.withPoseAdjustments(a.copy(leftElbow = it))) }
    RotationControl("Right elbow", a.rightElbow) { onProject(project.withPoseAdjustments(a.copy(rightElbow = it))) }
    TextButton(onClick = { onProject(project.withPoseAdjustments(PoseAdjustments())) }) { Text("Reset adjustments") }
}
@Composable
private fun RotationControl(label: String, value: Float, onChange: (Float) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    Slider(value, onChange, valueRange = -45f..45f, steps = 17,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = label })
}

@Composable
internal fun CurrentStyleSample(project: CharacterProject, modifier: Modifier = Modifier) {
    val sample = VisualAssetCatalog.sample(project)
    if (sample != null && VisualAssetCatalog.style(sample) != null) Image(painterResource(requireNotNull(VisualAssetCatalog.style(sample))), "${sample.label} benchmark sample", modifier, contentScale = ContentScale.Fit)
    else Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
        Box(contentAlignment = Alignment.Center) { Text("Custom style\nNo preview sample", Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
internal fun ArtStyleChoices(project: CharacterProject, onProject: (CharacterProject) -> Unit, compact: Boolean = false) {
    var edit by rememberSaveable { mutableStateOf(false) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    val style = project.artStyle
    if (style.manualMode) Text("Your custom style is active. Switch to structured style to use the library choices.", style = MaterialTheme.typography.bodySmall)
    var all by remember { mutableStateOf(false) }
    val choices = if (all) StyleLook.entries else (listOf(StyleLook.ANIME, StyleLook.GAME, StyleLook.CEL, StyleLook.PAINTERLY_ANIME, StyleLook.WATERCOLOR_ANIME, StyleLook.MANGA) + listOfNotNull(style.preset)).distinct()
    ChoiceLayout(choices, compact) { look, modifier ->
        VisualChoiceCard(look.label, style.preset == look, { onProject(project.withStylePreset(look).useStructuredStyle()) }, modifier) {
            Text(look.rendering, Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
    TextButton(onClick = { all = !all }) { Text(if (all) "Show fewer styles" else "Browse all ${StyleLook.entries.size} styles") }
    TextButton(onClick = { edit = !edit }) { Text(if (edit) "Hide style refinements" else "Refine style") }
    if (edit) {
        val a = style.adjustments; val resolved = project.resolvedStyleAdjustments
        StyleChips("Ink", StyleInk.entries, resolved.ink, { it.label }) { onProject(project.withStyleAdjustments(a.copy(ink = it))) }
        StyleChips("Mood", StyleMood.entries, resolved.mood, { it.label }) { onProject(project.withStyleAdjustments(a.copy(mood = it))) }
        Text("Set the palette in Color. These controls describe rendering only.", style = MaterialTheme.typography.bodySmall)
        com.kelvinsaputra.promptstudio.feature.studio.Choices("Line treatment", listOf("Fine", "Bold", "Broken", "No outlines"), style.lineTreatment) { onProject(project.copy(artStyle = style.copy(lineTreatment = it))) }
        com.kelvinsaputra.promptstudio.feature.studio.Choices("Shading", listOf("Cel", "Soft gradients", "Crosshatching", "Flat"), style.shading) { onProject(project.copy(artStyle = style.copy(shading = it))) }
        com.kelvinsaputra.promptstudio.feature.studio.Choices("Rendering", listOf("Clean", "Painterly", "Sketch-like"), style.rendering) { onProject(project.copy(artStyle = style.copy(rendering = it))) }
        com.kelvinsaputra.promptstudio.feature.studio.Choices("Texture", listOf("Smooth", "Paper grain", "Dry brush"), style.texture) { onProject(project.copy(artStyle = style.copy(texture = it))) }
        com.kelvinsaputra.promptstudio.feature.studio.Choices("Detail", listOf("Minimal", "Selective", "Intricate"), style.detail) { onProject(project.copy(artStyle = style.copy(detail = it))) }
        com.kelvinsaputra.promptstudio.feature.studio.Choices("Edge treatment", listOf("Crisp", "Soft", "Lost and found"), style.edges) { onProject(project.copy(artStyle = style.copy(edges = it))) }
        com.kelvinsaputra.promptstudio.feature.studio.Choices("Brush character", listOf("Smooth", "Textured", "Expressive"), style.brush) { onProject(project.copy(artStyle = style.copy(brush = it))) }
        TextButton(onClick = { onProject(project.withStyleAdjustments(StyleAdjustments())) }) { Text("Reset style refinements") }
    }
    TextButton(onClick = { advanced = !advanced }) { Text(if (advanced) "Hide custom style" else "Custom style…") }
    if (advanced || style.manualMode) {
        if (style.manualMode) {
            OutlinedTextField(style.manualDraft.orEmpty(), { onProject(project.editManualStyle(it)) }, label = { Text("Style description") },
                minLines = 5, maxLines = 10, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { onProject(project.useStructuredStyle()); advanced = false }) { Text("Use structured style") }
        } else {
            Text("Edit only the style section, starting from your current look. Your preset and refinements remain saved.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { onProject(project.enterManualStyle()) }) { Text("Write your own style") }
            if (style.manualDraft != null) TextButton(onClick = { onProject(project.resumeManualStyle()) }) { Text("Resume saved style draft") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> StyleChips(title: String, options: List<T>, selected: T?, label: (T) -> String, choose: (T) -> Unit) {
    Text(title, style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { FilterChip(it == selected, { choose(it) }, label = { Text(label(it)) }) }
    }
}

