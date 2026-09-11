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
fun CategoryPanel(category: StudioCategory, state: EditorUiState, onProject: (CharacterProject) -> Unit,
    onCostumeLocks: (Set<CostumeField>) -> Unit, onPoseLocks: (Set<PoseField>) -> Unit,
    onRandomizeCostume: () -> Unit, onRandomizePose: () -> Unit, onResetCostume: () -> Unit, onResetPose: () -> Unit) {
    val p = state.project
    val guides = VisualGuideRegistry
    when (category) {
        StudioCategory.Subject -> {
            TextField("Describe your character", p.subject, multiline = true) { onProject(p.copy(subject = it)) }
            OptionChips("Age presentation", AgeBand.entries, p.identity.ageBand) { onProject(p.copy(identity = p.identity.copy(ageBand = it))) }
            OptionChips("Gender presentation", GenderPresentation.entries, p.identity.genderPresentation) { onProject(p.copy(identity = p.identity.copy(genderPresentation = it))) }
            OptionChips("Expression", ExpressionPreset.entries, p.expression.preset) { onProject(p.copy(expression = p.expression.copy(preset = it))) }
            TextField("Hair color", p.hair.baseColor) { onProject(p.copy(hair = p.hair.copy(baseColor = it))) }
            OptionChips("Hair shape", HairStyle.entries, p.hair.style) { onProject(p.copy(hair = p.hair.copy(style = it))) }
        }
        StudioCategory.Appearance -> {
            Text("Start with an outfit", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutfitPresets.entries.forEach { preset -> FilterChip(p.costume.outfitIdentity == preset.costume.outfitIdentity,
                    { onProject(preset.applyTo(p)) }, label = { Text(preset.label) }) }
            }
            TextField("Outfit concept · preset or your own", p.costume.outfitIdentity) { onProject(p.copy(costume = p.costume.copy(outfitIdentity = it))) }
            OptionChips("Silhouette", Silhouette.entries, p.costume.silhouette) { onProject(p.copy(costume = p.costume.copy(silhouette = it))) }
            OptionChips("Outer layer", Outerwear.entries, p.costume.outerwear) { onProject(p.copy(costume = p.costume.copy(outerwear = it))) }
            TextField("Custom outfit details", p.costume.customNotes, multiline = true) { onProject(p.copy(costume = p.costume.copy(customNotes = it))) }
        }
        StudioCategory.Pose -> {
            VisualOptionGrid("Gesture", guides.poses, p.pose.basePose) { base ->
                val preset = VisualPose.entries.firstOrNull { it.base == base }
                onProject(if (preset != null) p.withVisualPose(preset) else p.copy(pose = p.pose.copy(basePose = base, weight = null,
                    torso = null, arms = null, legAction = "", motionDirection = "", guideAdjustments = PoseAdjustments())))
            }
            VisualOptionGrid("Body orientation", guides.facing, p.pose.guideFacing) { onProject(p.copy(pose = p.pose.copy(guideFacing = it))) }
            TextField("Custom pose details", p.pose.customNotes, multiline = true) { onProject(p.copy(pose = p.pose.copy(customNotes = it))) }
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
            VisualOptionGrid("Subject placement", guides.placement, p.visualAssembly.placementPreset) { onProject(p.withGuidePosition(it)) }
            VisualOptionGrid("Composition", guides.composition, p.composition.guidePreset) { onProject(p.withGuideComposition(it)) }
            TextField("Camera angle, perspective & custom composition", p.composition.additionalInstructions, multiline = true) { onProject(p.copy(composition = p.composition.copy(additionalInstructions = it))) }
        }
        StudioCategory.Environment -> {
            TextField("Location or world", p.environment.worldContextHint, multiline = true) { onProject(p.copy(environment = p.environment.copy(worldContextHint = it))) }
            OptionChips("Background treatment", EnvironmentAbstraction.entries, p.environment.abstractionLevel) { onProject(p.copy(environment = p.environment.copy(abstractionLevel = it))) }
            TextField("Weather, time & atmosphere", p.environment.additionalInstructions, multiline = true) { onProject(p.copy(environment = p.environment.copy(additionalInstructions = it))) }
        }
        StudioCategory.Lighting -> {
            OptionChips("Light source", LightingSource.entries, p.lighting.sourceQuality) { onProject(p.copy(lighting = p.lighting.copy(sourceQuality = it))) }
            TextField("Light direction", p.lighting.direction) { onProject(p.copy(lighting = p.lighting.copy(direction = it))) }
            OptionChips("Shadow softness", ShadowSoftness.entries, p.lighting.shadowSoftness) { onProject(p.copy(lighting = p.lighting.copy(shadowSoftness = it))) }
            TextField("Custom lighting", p.lighting.additionalInstructions, multiline = true) { onProject(p.copy(lighting = p.lighting.copy(additionalInstructions = it))) }
        }
        StudioCategory.Style -> ArtStyleChoices(p, onProject)
        StudioCategory.Color -> {
            Text("Accent palette", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AccentColor.entries.forEach { color ->
                    val swatch = when (color) {
                        AccentColor.MUTED_WARM_BEIGE -> 0xFFBEAC92; AccentColor.DULL_CRIMSON -> 0xFF9D5055
                        AccentColor.MUTED_GOLD -> 0xFFB09852; AccentColor.DESATURATED_VIOLET -> 0xFF8C819B; AccentColor.OLIVE_GRAY -> 0xFF848B72
                    }
                    FilterChip(p.colorAccents.accentColor == color, { onProject(p.copy(colorAccents = p.colorAccents.copy(accentColor = if (p.colorAccents.accentColor == color) null else color))) },
                        label = { Text(color.wording) }, leadingIcon = { Box(Modifier.size(22.dp).background(Color(swatch), MaterialTheme.shapes.small)) })
                }
            }
            TextField("Palette direction · any colors", p.colorAccents.additionalInstructions, multiline = true) { onProject(p.copy(colorAccents = p.colorAccents.copy(additionalInstructions = it))) }
            TextField("Where color appears", p.colorAccents.allowedSurfaces) { onProject(p.copy(colorAccents = p.colorAccents.copy(allowedSurfaces = it))) }
            TextField("Saturation", p.colorAccents.saturationCeiling) { onProject(p.copy(colorAccents = p.colorAccents.copy(saturationCeiling = it))) }
        }
        StudioCategory.Effects -> {
            TextField("Signature effect", p.powerSignature.manifestation, multiline = true) { onProject(p.copy(powerSignature = p.powerSignature.copy(manifestation = it))) }
            TextField("Movement", p.powerSignature.motionQuality) { onProject(p.copy(powerSignature = p.powerSignature.copy(motionQuality = it))) }
            TextField("Custom effects & atmosphere", p.powerSignature.additionalInstructions, multiline = true) { onProject(p.copy(powerSignature = p.powerSignature.copy(additionalInstructions = it))) }
        }
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
    if (category != StudioCategory.Negatives) AdvancedSection("Refine ${category.title.lowercase()}") {
        val modules = category.modules.filterNot { (category == StudioCategory.Style && it == EditorModule.Style) || (category == StudioCategory.Output && it == EditorModule.Output) }
        var module by remember(category) { mutableStateOf(modules.first()) }
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
