package com.kelvinsaputra.promptstudio.feature.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.*
import com.kelvinsaputra.promptstudio.guide.VisualGuideRegistry

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPanel(category: StudioCategory, state: EditorUiState, onChange: (CharacterProject) -> Unit,
    onCostumeLocks: (Set<CostumeField>) -> Unit, onPoseLocks: (Set<PoseField>) -> Unit,
    onRandomizeCostume: () -> Unit, onRandomizePose: () -> Unit, onResetCostume: () -> Unit, onResetPose: () -> Unit) {
    val p = state.project
    val onProject: (CharacterProject) -> Unit = { updated ->
        onChange(if (updated.profile != p.profile && !updated.profile.enabled) updated.copy(profile = updated.profile.copy(enabled = true)) else updated)
    }
    val guides = VisualGuideRegistry
    when (category) {
        StudioCategory.Subject -> CharacterControls(p, onProject)
        StudioCategory.Appearance -> AppearanceControls(p, onProject)
        StudioCategory.Pose -> {
            VisualOptionGrid("Gesture", guides.poses, p.pose.basePose) { base ->
                val preset = VisualPose.entries.firstOrNull { it.base == base }
                onProject(if (preset != null) p.withVisualPose(preset) else p.copy(pose = p.pose.copy(basePose = base, weight = null,
                    torso = null, arms = null, legAction = "", motionDirection = "", guideAdjustments = PoseAdjustments())))
            }
            AdvancedSection("Body orientation & custom pose") {
            VisualOptionGrid("Body orientation", guides.facing, p.pose.guideFacing) { onProject(p.copy(pose = p.pose.copy(guideFacing = it))) }
            TextField("Custom pose details", p.pose.customNotes, multiline = true) { onProject(p.copy(pose = p.pose.copy(customNotes = it))) }
            }
            AdvancedSection("Head, gaze & hands") {
                VisualOptionGrid("Gaze", guides.gaze, p.visualAssembly.gaze) { onProject(p.withGuideGaze(it)) }
                VisualOptionGrid("Head direction", guides.head, p.gazeDirection.headDirection ?: p.pose.head) { onProject(p.copy(gazeDirection = p.gazeDirection.copy(headDirection = it))) }
                VisualOptionGrid("Arm position", guides.arms, p.pose.arms) { onProject(p.copy(pose = p.pose.copy(arms = it))) }
                VisualOptionGrid("Prop placement", guides.prop, p.pose.guideProp) { onProject(p.copy(pose = p.pose.copy(guideProp = it))) }
                PoseAdjustmentControls(p, onProject)
            }
        }
        StudioCategory.Camera -> {
            VisualOptionGrid("Framing", guides.framing, p.output.framing) { onProject(p.withGuideFraming(it)) }
            VisualOptionGrid("Camera angle", guides.camera, p.composition.cameraAngle) { onProject(p.copy(composition = p.composition.copy(cameraAngle = it))) }
            Choices("Image format", listOf(OutputType.PORTRAIT, OutputType.PHONE, OutputType.DESKTOP, OutputType.SQUARE), p.output.type, { "${it.label} · ${it.ratio}" }) { onProject(p.copy(output = p.output.copy(type = it))) }
            AdvancedSection("Placement & composition") {
            VisualOptionGrid("Subject placement", guides.placement, p.visualAssembly.placementPreset) { onProject(p.withGuidePosition(it)) }
            VisualOptionGrid("Composition", guides.composition, p.composition.guidePreset) { onProject(p.withGuideComposition(it)) }
            TextField("Camera angle, perspective & custom composition", p.composition.additionalInstructions, multiline = true) { onProject(p.copy(composition = p.composition.copy(additionalInstructions = it))) }
            }
        }
        StudioCategory.Environment -> EnvironmentControls(p, onProject)
        StudioCategory.Lighting -> LightingControls(p, onProject)
        StudioCategory.Style -> ArtStyleChoices(p, onProject)
        StudioCategory.Color -> ColorControls(p, onProject)
        StudioCategory.Effects -> EffectsControls(p, onProject)
        StudioCategory.Output -> {
            Text("Choose a format", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutputType.entries.forEach { type -> FilterChip(p.output.type == type, { onProject(p.copy(output = p.output.copy(type = type))) },
                    label = { Text("${type.label}${type.ratio?.let { " · $it" }.orEmpty()}") }) }
            }
            if (p.output.type == OutputType.CUSTOM) {
                TextField("Custom output intent", p.output.customIntent) { onProject(p.copy(output = p.output.copy(customIntent = it))) }
                TextField("Aspect ratio", p.output.customAspectRatio) { onProject(p.copy(output = p.output.copy(customAspectRatio = it))) }
            }
            AdvancedSection("Crop & safe areas") { OutputEditor(p.output) { onProject(p.copy(output = it)) } }
            PromptAuthoringControls(p, onProject)
        }
        StudioCategory.Negatives -> BoundedTextListEditor("Exclusion", p.exclusions, 15) { onProject(p.copy(exclusions = it)) }
    }
    if (category != StudioCategory.Negatives) AdvancedSection("Advanced ${category.title.lowercase()} controls") {
        val modules = category.modules.filterNot { (category == StudioCategory.Style && it == EditorModule.Style) || (category == StudioCategory.Output && it == EditorModule.Output) }
        var module by remember(category) { mutableStateOf(modules.firstOrNull() ?: EditorModule.Style) }
        if (modules.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            modules.forEach { item -> FilterChip(module == item, { module = item }, label = { Text(item.title) }) }
        }
        CharacterComponentEditor(module, p, onProject, state.costumeLocks, onCostumeLocks, state.poseLocks, onPoseLocks,
            onRandomizeCostume, onRandomizePose, { onProject(p.resetStudioCategory(StudioCategory.Appearance)) }, { onProject(p.resetStudioCategory(StudioCategory.Pose)) })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T : PromptOption> OptionChips(title: String, options: List<T>, selected: T?, onSelect: (T?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option -> FilterChip(option == selected, { onSelect(if (selected == option) null else option) }, label = { Text(option.wording.replaceFirstChar { it.uppercase() }) }) }
        }
    }
}
