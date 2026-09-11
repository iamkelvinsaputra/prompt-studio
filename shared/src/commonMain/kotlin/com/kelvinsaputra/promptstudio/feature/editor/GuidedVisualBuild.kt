package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.guide.GuideRenderSpec
import com.kelvinsaputra.promptstudio.prompt.effectivePrompt
import org.jetbrains.compose.resources.painterResource

/** Bounded layout: preview and primary action stay visible while decisions scroll. */
@Composable
fun GuidedVisualBuild(
    project: CharacterProject, navigation: GuidedNavigation,
    onProject: (CharacterProject) -> Unit, onNavigate: (GuidedNavigation) -> Unit,
    onGenerate: () -> Unit, onAdvanced: () -> Unit,
    modifier: Modifier = Modifier,
    useVisualGuide: Boolean = true, onVisualGuide: (Boolean) -> Unit = {},
    variantContent: @Composable () -> Unit = {}, presetContent: @Composable () -> Unit = {},
) {
    val step = navigation.step
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            if (step != null) TextButton(onClick = { onNavigate(navigation.back()) }) { Text(if (navigation.onboarding) "← Back" else "← Summary") }
            else Text("Your visual", style = MaterialTheme.typography.headlineSmall)
            Text(if (step == null) project.output.aspectRatio.orEmpty() else if (navigation.onboarding) "Step ${step.ordinal + 1} of 8" else "Edit ${step.title}", style = MaterialTheme.typography.labelLarge)
        }
        if (navigation.onboarding && step != null) LinearProgressIndicator(progress = { (step.ordinal + 1) / 8f }, modifier = Modifier.fillMaxWidth())
        BoxWithConstraints(Modifier.weight(1f)) {
            val wide = maxWidth >= 760.dp
            val previewHeight = (maxHeight * .46f).coerceIn(150.dp, 290.dp)
            val preview: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (step == GuideStep.ArtStyle) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SilhouettePreview(project.visualAssembly, Modifier.weight(1f).fillMaxHeight(), project.output.aspectRatio)
                            Column(Modifier.weight(1f).fillMaxHeight()) {
                                CurrentStyleSample(project, Modifier.weight(1f).fillMaxWidth())
                                Text("Style sample", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    } else SilhouettePreview(project.visualAssembly, Modifier.weight(1f).fillMaxWidth(), project.output.aspectRatio)
                    Text("Visual guide — generation may vary.", style = MaterialTheme.typography.bodySmall)
                    if (project.visualAssembly.hasApproximation) Text("Custom choices use an approximate guide.", style = MaterialTheme.typography.bodySmall)
                }
            }
            val controls: @Composable (Modifier) -> Unit = { m ->
                key(project.id, step) {
                    Column(m.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (step == null) VisualSummary(project, onProject, { onNavigate(navigation.edit(it)) }, onAdvanced,
                            useVisualGuide, onVisualGuide, variantContent, presetContent)
                        else {
                            Text(step.title.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(step.question, style = MaterialTheme.typography.titleLarge)
                            GuideStepChoices(step, project, onProject, compact = !wide)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            if (wide) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                preview(Modifier.weight(1f).fillMaxHeight())
                controls(Modifier.weight(1f).fillMaxHeight())
            } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                preview(Modifier.fillMaxWidth().height(previewHeight))
                controls(Modifier.fillMaxWidth().weight(1f))
            }
        }
        if (project.promptAuthoring.mode == PromptMode.Manual) Text("Global manual prompt active · Visual and style edits remain in the automatic draft.", style = MaterialTheme.typography.bodySmall)
        Button(onClick = { if (step == null) onGenerate() else onNavigate(navigation.forward()) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(if (step == null) "Generate" else if (!navigation.onboarding) "Done" else if (step == GuideStep.ArtStyle) "Review your visual" else "Continue")
        }
    }
}

@Composable
internal fun GuideStepChoices(step: GuideStep, project: CharacterProject, onProject: (CharacterProject) -> Unit, compact: Boolean) {
    val state = project.visualAssembly
    when (step) {
        GuideStep.Gender -> VisualChoices(listOf(GenderPresentation.MALE, GenderPresentation.FEMALE), state.gender,
            { if (it == GenderPresentation.MALE) "Man" else "Woman" }, { state.copy(gender = it) }, compact,
            { onProject(project.withGuideGender(it)) })
        GuideStep.Age -> {
            VisualChoices(guidedAgeBands, state.ageBand, { it.guideLabel() }, { state.copy(ageBand = it, framing = Framing.BUST_UP) }, compact,
                { onProject(project.withGuideAge(it)) })
            Text("Broad adult age intent. The guide suggests maturity; it does not predict an exact age.", style = MaterialTheme.typography.bodySmall)
        }
        GuideStep.Pose -> {
            VisualChoices(VisualPose.entries, state.posePreset, { it.label }, {
                project.withVisualPose(it).visualAssembly.copy(framing = Framing.FULL_BODY, figurePlacement = "center")
            }, compact, { onProject(project.withVisualPose(it)) })
            PoseAdjustmentControls(project, onProject)
        }
        GuideStep.Gaze -> VisualChoices(guidedGazes, state.gaze, { it.guideLabel() }, {
            state.copy(gaze = it, framing = Framing.CLOSE_UP, figurePlacement = "center")
        }, compact, { onProject(project.withGuideGaze(it)) })
        GuideStep.Framing -> VisualChoices(guidedFramings, state.framing, { it.guideLabel() }, { state.copy(framing = it) }, compact,
            { onProject(project.withGuideFraming(it)) }, project.output.aspectRatio)
        GuideStep.Position -> {
            VisualChoices(VisualPlacement.entries, state.placementPreset, { it.label }, { state.copy(figurePlacement = it.wording) }, compact,
                { onProject(project.withGuidePosition(it)) }, project.output.aspectRatio)
            Text("Position moves the subject inside the frame.", style = MaterialTheme.typography.bodySmall)
        }
        GuideStep.Composition -> {
            VisualChoices(GuideComposition.entries, state.composition, { it.label }, { state.copy(composition = it) }, compact,
                { onProject(project.withGuideComposition(it)) }, project.output.aspectRatio)
            Text("Composition balances the supporting shapes around your chosen position.", style = MaterialTheme.typography.bodySmall)
        }
        GuideStep.ArtStyle -> ArtStyleChoices(project, onProject, compact)
    }
}

@Composable
private fun <T> VisualChoices(options: List<T>, selected: T?, label: (T) -> String, preview: (T) -> VisualAssemblyState,
    compact: Boolean, choose: (T) -> Unit, ratio: String? = "4:5") {
    ChoiceLayout(options, compact) { option, modifier ->
        VisualChoiceCard(label(option), option == selected, { choose(option) }, modifier) {
            SilhouettePreview(preview(option), Modifier.fillMaxWidth().height(142.dp), ratio, thumbnail = true)
        }
    }
}

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
        Text((if (selected) "✓ " else "") + label, Modifier.padding(12.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PoseAdjustmentControls(project: CharacterProject, onProject: (CharacterProject) -> Unit) {
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
    if (sample != null) Image(painterResource(VisualAssetCatalog.style(sample)), "${sample.label} benchmark sample", modifier, contentScale = ContentScale.Fit)
    else Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
        Box(contentAlignment = Alignment.Center) { Text("Custom style\nNo preview sample", Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
internal fun ArtStyleChoices(project: CharacterProject, onProject: (CharacterProject) -> Unit, compact: Boolean = false) {
    var edit by rememberSaveable { mutableStateOf(false) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    val style = project.artStyle
    if (style.manualMode) Text("Manual ART STYLE CORE is active. Preset and refinement changes are saved underneath it.", style = MaterialTheme.typography.bodySmall)
    ChoiceLayout(StyleLook.entries, compact) { look, modifier ->
        VisualChoiceCard(look.label, style.preset == look, { onProject(project.withStylePreset(look)) }, modifier) {
            Image(painterResource(VisualAssetCatalog.style(look)), "${look.label} style sample", Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Fit)
        }
    }
    Text("Samples compare the same subject. Refinements affect the prompt; samples stay fixed.", style = MaterialTheme.typography.bodySmall)
    TextButton(onClick = { edit = !edit }) { Text(if (edit) "Hide style refinements" else "Edit Style") }
    if (edit) {
        val a = style.adjustments; val resolved = project.resolvedStyleAdjustments
        StyleChips("Ink", StyleInk.entries, resolved.ink, { it.label }) { onProject(project.withStyleAdjustments(a.copy(ink = it))) }
        StyleChips("Mood", StyleMood.entries, resolved.mood, { it.label }) { onProject(project.withStyleAdjustments(a.copy(mood = it))) }
        StyleChips("Color", StyleColor.entries, resolved.color, { it.label }) { onProject(project.withStyleAdjustments(a.copy(color = it))) }
        TextButton(onClick = { onProject(project.withStyleAdjustments(StyleAdjustments())) }) { Text("Reset style refinements") }
    }
    TextButton(onClick = { advanced = !advanced }) { Text(if (advanced) "Hide Advanced Style" else "Advanced · Custom ART STYLE CORE") }
    if (advanced || style.manualMode) {
        if (style.manualMode) {
            OutlinedTextField(style.manualDraft.orEmpty(), { onProject(project.editManualStyle(it)) }, label = { Text("ART STYLE CORE") },
                minLines = 5, maxLines = 10, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { onProject(project.useStructuredStyle()); advanced = false }) { Text("Use structured style") }
        } else {
            Text("Edit only the style section, starting from your current look. Your preset and refinements remain saved.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { onProject(project.enterManualStyle()) }) { Text("Edit ART STYLE CORE manually") }
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

@Composable
private fun VisualSummary(project: CharacterProject, onProject: (CharacterProject) -> Unit, onEdit: (GuideStep) -> Unit,
    onAdvanced: () -> Unit, useVisualGuide: Boolean, onVisualGuide: (Boolean) -> Unit,
    variantContent: @Composable () -> Unit, presetContent: @Composable () -> Unit) {
    var prompt by rememberSaveable { mutableStateOf(false) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var variants by rememberSaveable { mutableStateOf(false) }
    GuideStep.entries.forEach { step ->
        OutlinedCard(onClick = { onEdit(step) }, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Edit ${step.title}" }) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step == GuideStep.ArtStyle) CurrentStyleSample(project, Modifier.size(64.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(step.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(project.guideSummary(step), style = MaterialTheme.typography.bodyMedium)
                }
                Text("Edit ›", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    val supported = GuideRenderSpec.from(project.visualAssembly, project.output.aspectRatio) != null
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(useVisualGuide && supported, onVisualGuide, enabled = supported, modifier = Modifier.semantics { contentDescription = "Use visual guide" })
        Column {
            Text("Visual Guide", style = MaterialTheme.typography.labelLarge)
            Text(if (!supported) "Custom configuration · text only" else if (useVisualGuide) "Enabled for supported models" else "Off · text only", style = MaterialTheme.typography.bodySmall)
        }
    }
    TextButton(onClick = { prompt = !prompt }) { Text(if (prompt) "Hide prompt" else "Prompt · ${if (project.promptAuthoring.mode == PromptMode.Manual) "Manual" else "Automatically generated"} · View") }
    if (prompt) SelectionContainer { Text(project.effectivePrompt(), style = MaterialTheme.typography.bodySmall) }
    TextButton(onClick = { advanced = !advanced }) { Text(if (advanced) "Hide Advanced Prompt" else "Advanced Prompt") }
    if (advanced) PromptAuthoringControls(project, onProject)
    TextButton(onClick = onAdvanced) { Text("All character details") }
    TextButton(onClick = { variants = !variants }) { Text(if (variants) "Hide variants & saved presets" else "Variants & saved presets") }
    if (variants) { variantContent(); presetContent() }
}
