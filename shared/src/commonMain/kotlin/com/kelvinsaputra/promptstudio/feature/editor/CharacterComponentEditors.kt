package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*

@Composable
fun CharacterComponentEditor(
    module: EditorModule,
    project: CharacterProject,
    onProject: (CharacterProject) -> Unit,
    costumeLocks: Set<CostumeField>,
    onCostumeLocks: (Set<CostumeField>) -> Unit,
    poseLocks: Set<PoseField>,
    onPoseLocks: (Set<PoseField>) -> Unit,
    onRandomizeCostume: () -> Unit,
    onRandomizePose: () -> Unit,
    onResetCostume: () -> Unit,
    onResetPose: () -> Unit,
) {
    when (module) {
        EditorModule.Style -> StyleEditor(project.style)
        EditorModule.Identity -> IdentityEditor(project, { onProject(project.copy(identity = it)) }) { onProject(project.copy(subject = it)) }
        EditorModule.Role -> RoleEditor(project.role) { onProject(project.copy(role = it)) }
        EditorModule.VisualThesis -> TextField("One concise design thesis", project.coreVisualThesis, multiline = true) {
            onProject(project.copy(coreVisualThesis = it))
        }
        EditorModule.Personality -> PersonalityEditor(project.personality) { onProject(project.copy(personality = it)) }
        EditorModule.Contradiction -> ContradictionEditor(project.contradiction) { onProject(project.copy(contradiction = it)) }
        EditorModule.Body -> BodyEditor(project.body) { onProject(project.copy(body = it)) }
        EditorModule.Face -> FaceEditor(project.face) { onProject(project.copy(face = it)) }
        EditorModule.Expression -> ExpressionEditor(project.expression) { onProject(project.copy(expression = it)) }
        EditorModule.Hair -> HairEditor(project.hair) { onProject(project.copy(hair = it)) }
        EditorModule.Costume -> CostumeEditor(project.costume, { onProject(project.copy(costume = it)) }, costumeLocks, onCostumeLocks, onRandomizeCostume, onResetCostume)
        EditorModule.Accessories -> AccessoriesEditor(project.accessories) { onProject(project.copy(accessories = it)) }
        EditorModule.ShapeLanguage -> ShapeLanguageEditor(project.shapeLanguage) { onProject(project.copy(shapeLanguage = it)) }
        EditorModule.Power -> PowerEditor(project.powerSignature) { onProject(project.copy(powerSignature = it)) }
        EditorModule.Prop -> PropEditor(project.prop) { onProject(project.copy(prop = it)) }
        EditorModule.Pose -> PoseEditor(project.pose, { onProject(project.copy(pose = it)) }, poseLocks, onPoseLocks, onRandomizePose, onResetPose)
        EditorModule.Gaze -> GazeEditor(project.gazeDirection) { onProject(project.copy(gazeDirection = it)) }
        EditorModule.Composition -> CompositionEditor(project.composition) { onProject(project.copy(composition = it)) }
        EditorModule.Environment -> EnvironmentEditor(project.environment) { onProject(project.copy(environment = it)) }
        EditorModule.Lighting -> LightingEditor(project.lighting) { onProject(project.copy(lighting = it)) }
        EditorModule.AccentColor -> AccentColorEditor(project.colorAccents) { onProject(project.copy(colorAccents = it)) }
        EditorModule.SurfaceTexture -> SurfaceTextureEditor(project.surfaceTexture) { onProject(project.copy(surfaceTexture = it)) }
        EditorModule.Output -> OutputEditor(project.output) { onProject(project.copy(output = it)) }
        EditorModule.Avoid -> BoundedTextListEditor("Exclusion", project.exclusions, 15, onChange = { onProject(project.copy(exclusions = it)) })
        EditorModule.PriorityStack -> BoundedTextListEditor("Visual priority", project.priorityStack, 5, ordered = true, onChange = { onProject(project.copy(priorityStack = it)) })
        EditorModule.Prompt -> Unit
    }
}

@Composable private fun IdentityEditor(value: CharacterProject, onChange: (IdentityConfiguration) -> Unit, onSubjectChange: (String) -> Unit) {
    val identity = value.identity
    Text("Use structured identity controls. The legacy/free-form subject is retained for V0 imports.", style = MaterialTheme.typography.bodySmall)
    OptionField("Age band", identity.ageBand, AgeBand.entries) { onChange(identity.copy(ageBand = it)) }
    TextField("Age range", identity.ageRange) { onChange(identity.copy(ageRange = it)) }
    OptionField("Gender presentation", identity.genderPresentation, GenderPresentation.entries) { onChange(identity.copy(genderPresentation = it)) }
    OptionField("Body type", identity.bodyType, BodyType.entries) { onChange(identity.copy(bodyType = it)) }
    TextField("Core vibe", identity.coreVibe) { onChange(identity.copy(coreVibe = it)) }
    TextField("Additional identity instructions", identity.additionalInstructions, multiline = true) { onChange(identity.copy(additionalInstructions = it)) }
    TextField("Additional subject notes", value.subject, multiline = true, onChange = onSubjectChange)
}

@Composable private fun RoleEditor(value: RoleConfiguration, onChange: (RoleConfiguration) -> Unit) {
    TextField("Role / job", value.role) { onChange(value.copy(role = it)) }
    TextField("Organization / faction / context", value.organizationOrContext) { onChange(value.copy(organizationOrContext = it)) }
    TextField("Behavioral archetype", value.behavioralArchetype) { onChange(value.copy(behavioralArchetype = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun PersonalityEditor(value: PersonalityConfiguration, onChange: (PersonalityConfiguration) -> Unit) {
    BoundedTextListEditor("Trait", value.traits, 7) { onChange(value.copy(traits = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun ContradictionEditor(value: ContradictionConfiguration, onChange: (ContradictionConfiguration) -> Unit) {
    TextField("Outward quality", value.outwardQuality) { onChange(value.copy(outwardQuality = it)) }
    TextField("Inward quality", value.inwardQuality) { onChange(value.copy(inwardQuality = it)) }
    TextField("Secondary nuance", value.secondaryNuance) { onChange(value.copy(secondaryNuance = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun BodyEditor(value: BodyConfiguration, onChange: (BodyConfiguration) -> Unit) {
    TextField("Age read / range", value.ageRead) { onChange(value.copy(ageRead = it)) }
    OptionField("Build", value.build, Build.entries) { onChange(value.copy(build = it)) }
    TextField("Proportions", value.proportions) { onChange(value.copy(proportions = it)) }
    OptionField("Height impression", value.heightImpression, HeightImpression.entries) { onChange(value.copy(heightImpression = it)) }
    OptionField("Athletic language", value.athleticLanguage, AthleticLanguage.entries) { onChange(value.copy(athleticLanguage = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun FaceEditor(value: FaceConfiguration, onChange: (FaceConfiguration) -> Unit) {
    TextField("Eye shape", value.eyeShape) { onChange(value.copy(eyeShape = it)) }
    TextField("Eyebrow behavior", value.eyebrowBehavior) { onChange(value.copy(eyebrowBehavior = it)) }
    TextField("Mouth / smile", value.mouthOrSmile) { onChange(value.copy(mouthOrSmile = it)) }
    TextField("Makeup / grooming", value.makeupOrGrooming) { onChange(value.copy(makeupOrGrooming = it)) }
    TextField("Nose / jaw / facial structure", value.facialStructure) { onChange(value.copy(facialStructure = it)) }
    TextField("Maturity note", value.maturityNote) { onChange(value.copy(maturityNote = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun ExpressionEditor(value: ExpressionConfiguration, onChange: (ExpressionConfiguration) -> Unit) {
    OptionField("Expression preset", value.preset, ExpressionPreset.entries) { onChange(value.copy(preset = it)) }
    TextField("Default / surface expression", value.defaultExpression) { onChange(value.copy(defaultExpression = it)) }
    TextField("Emotional subtext", value.emotionalSubtext) { onChange(value.copy(emotionalSubtext = it)) }
    TextField("Alternate emotional range", value.alternateRange) { onChange(value.copy(alternateRange = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun HairEditor(value: HairConfiguration, onChange: (HairConfiguration) -> Unit) {
    TextField("Base color", value.baseColor) { onChange(value.copy(baseColor = it)) }
    TextField("Roots", value.roots) { onChange(value.copy(roots = it)) }
    OptionField("Length", value.length, HairLength.entries) { onChange(value.copy(length = it)) }
    OptionField("Style", value.style, HairStyle.entries) { onChange(value.copy(style = it)) }
    TextField("Movement / behavior", value.movementBehavior) { onChange(value.copy(movementBehavior = it)) }
    TextField("Accent treatment / asymmetry", value.accentTreatment) { onChange(value.copy(accentTreatment = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun AccessoriesEditor(value: AccessoriesConfiguration, onChange: (AccessoriesConfiguration) -> Unit) {
    BoundedTextListEditor("Accessory", value.items, 3) { onChange(value.copy(items = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun ShapeLanguageEditor(value: ShapeLanguageConfiguration, onChange: (ShapeLanguageConfiguration) -> Unit) {
    Text("Examples: asymmetry, diagonal motion, interrupted curves, slight visual imbalance.", style = MaterialTheme.typography.bodySmall)
    BoundedTextListEditor("Design principle", value.principles, 6) { onChange(value.copy(principles = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun PowerEditor(value: PowerSignatureConfiguration, onChange: (PowerSignatureConfiguration) -> Unit) {
    TextField("Symbolic meaning", value.symbolicMeaning) { onChange(value.copy(symbolicMeaning = it)) }
    TextField("Manifestation", value.manifestation) { onChange(value.copy(manifestation = it)) }
    TextField("Shape logic", value.shapeLogic) { onChange(value.copy(shapeLogic = it)) }
    TextField("Motion quality", value.motionQuality) { onChange(value.copy(motionQuality = it)) }
    TextField("Color / material behavior", value.colorMaterialBehavior) { onChange(value.copy(colorMaterialBehavior = it)) }
    TextField("Restraint note", value.restraint) { onChange(value.copy(restraint = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun PropEditor(value: PropConfiguration, onChange: (PropConfiguration) -> Unit) {
    TextField("Primary prop / weapon / tool", value.primaryProp) { onChange(value.copy(primaryProp = it)) }
    TextField("Primary function", value.primaryFunction) { onChange(value.copy(primaryFunction = it)) }
    TextField("Primary style logic", value.primaryStyleLogic) { onChange(value.copy(primaryStyleLogic = it)) }
    TextField("Secondary prop (optional)", value.secondaryProp) { onChange(value.copy(secondaryProp = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun GazeEditor(value: GazeConfiguration, onChange: (GazeConfiguration) -> Unit) {
    Text("This overrides Pose’s legacy head/gaze fields when set, so the prompt stays non-duplicated.", style = MaterialTheme.typography.bodySmall)
    OptionField("Head direction", value.headDirection, Head.entries) { onChange(value.copy(headDirection = it)) }
    OptionField("Gaze target", value.gazeTarget, Gaze.entries) { onChange(value.copy(gazeTarget = it)) }
    OptionField("Intensity", value.intensity, GazeIntensity.entries) { onChange(value.copy(intensity = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun CompositionEditor(value: CompositionConfiguration, onChange: (CompositionConfiguration) -> Unit) {
    Text("Framing, figure placement, negative space, and safe area live in Output to avoid duplicated state.", style = MaterialTheme.typography.bodySmall)
    TextField("Directional flow", value.directionalFlow) { onChange(value.copy(directionalFlow = it)) }
    TextField("Detail concentration", value.detailConcentration) { onChange(value.copy(detailConcentration = it)) }
    TextField("Readability priority", value.readabilityPriority) { onChange(value.copy(readabilityPriority = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun EnvironmentEditor(value: EnvironmentConfiguration, onChange: (EnvironmentConfiguration) -> Unit) {
    TextField("World / context hint", value.worldContextHint) { onChange(value.copy(worldContextHint = it)) }
    OptionField("Abstraction level", value.abstractionLevel, EnvironmentAbstraction.entries) { onChange(value.copy(abstractionLevel = it)) }
    TextField("Depth behavior", value.depthBehavior) { onChange(value.copy(depthBehavior = it)) }
    TextField("Motif / support element", value.motifOrSupportElement) { onChange(value.copy(motifOrSupportElement = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun LightingEditor(value: LightingConfiguration, onChange: (LightingConfiguration) -> Unit) {
    OptionField("Source / quality", value.sourceQuality, LightingSource.entries) { onChange(value.copy(sourceQuality = it)) }
    TextField("Direction", value.direction) { onChange(value.copy(direction = it)) }
    OptionField("Shadow softness", value.shadowSoftness, ShadowSoftness.entries) { onChange(value.copy(shadowSoftness = it)) }
    TextField("Mood effect", value.moodEffect) { onChange(value.copy(moodEffect = it)) }
    TextField("Lighting exclusions", value.exclusions) { onChange(value.copy(exclusions = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun AccentColorEditor(value: ColorAccentConfiguration, onChange: (ColorAccentConfiguration) -> Unit) {
    Text("The locked sky-blue watercolor core stays intact; this is only a restrained secondary accent.", style = MaterialTheme.typography.bodySmall)
    OptionField("Accent color", value.accentColor, AccentColor.entries) { onChange(value.copy(accentColor = it)) }
    TextField("Allowed surfaces", value.allowedSurfaces) { onChange(value.copy(allowedSurfaces = it)) }
    TextField("Saturation ceiling", value.saturationCeiling) { onChange(value.copy(saturationCeiling = it)) }
    TextField("Forbidden surfaces", value.forbiddenSurfaces) { onChange(value.copy(forbiddenSurfaces = it)) }
    TextField("Emotional role", value.emotionalRole) { onChange(value.copy(emotionalRole = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun SurfaceTextureEditor(value: SurfaceTextureConfiguration, onChange: (SurfaceTextureConfiguration) -> Unit) {
    OptionField("Paper grain", value.paperGrain, TextureLevel.entries) { onChange(value.copy(paperGrain = it)) }
    OptionField("Watercolor behavior", value.watercolorBehavior, WatercolorBehavior.entries) { onChange(value.copy(watercolorBehavior = it)) }
    OptionField("Ink texture behavior", value.inkTextureBehavior, InkTextureBehavior.entries) { onChange(value.copy(inkTextureBehavior = it)) }
    AdditionalField(value.additionalInstructions) { onChange(value.copy(additionalInstructions = it)) }
}

@Composable private fun AdditionalField(value: String, onChange: (String) -> Unit) =
    TextField("Additional instructions", value, multiline = true, onChange = onChange)

@Composable
fun BoundedTextListEditor(
    itemLabel: String,
    values: List<String>,
    maximum: Int,
    ordered: Boolean = false,
    onChange: (List<String>) -> Unit,
) {
    Text("Add up to $maximum ${itemLabel.lowercase()}${if (maximum == 1) "" else "s"}.", style = MaterialTheme.typography.bodySmall)
    values.forEachIndexed { index, value ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(if (ordered) "${index + 1}. $itemLabel" else "$itemLabel ${index + 1}", value, modifier = Modifier.weight(1f)) { updated ->
                onChange(values.toMutableList().also { it[index] = updated })
            }
            if (ordered && index > 0) TextButton(onClick = {
                onChange(values.toMutableList().also { list -> val moving = list.removeAt(index); list.add(index - 1, moving) })
            }) { Text("↑") }
            if (ordered && index < values.lastIndex) TextButton(onClick = {
                onChange(values.toMutableList().also { list -> val moving = list.removeAt(index); list.add(index + 1, moving) })
            }) { Text("↓") }
            TextButton(onClick = { onChange(values.toMutableList().also { it.removeAt(index) }) }) { Text("Remove") }
        }
    }
    if (values.size < maximum) TextButton(onClick = { onChange(values + "") }) { Text("+ Add $itemLabel") }
}
