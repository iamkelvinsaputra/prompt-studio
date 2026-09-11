package com.kelvinsaputra.promptstudio.domain

import com.kelvinsaputra.promptstudio.generation.model.ImageModels
import com.kelvinsaputra.promptstudio.generation.model.ImageProviderId
import kotlinx.serialization.Serializable

@Serializable
data class GenerationPreferences(
    val provider: ImageProviderId = ImageProviderId.OpenAI,
    val modelId: String = ImageModels.default(provider).id,
    val useVisualGuide: Boolean = true,
)

/** Canonical character identity stays shared; illustration choices belong to each variant. Null additions inherit legacy shared settings. */
@Serializable
data class VariantScene(
    val pose: PoseConfiguration,
    val gaze: GazeConfiguration,
    val composition: CompositionConfiguration,
    val output: OutputConfiguration,
    val promptAuthoring: PromptAuthoring,
    val environment: EnvironmentConfiguration? = null,
    val lighting: LightingConfiguration? = null,
    val colorDirection: ColorDirection? = null,
    val effects: EffectsConfiguration? = null,
    val colorAccents: ColorAccentConfiguration? = null,
    val powerSignature: PowerSignatureConfiguration? = null,
    val expression: ExpressionConfiguration? = null,
    val costume: CostumeConfiguration? = null,
    val accessories: AccessoriesConfiguration? = null,
    val prop: PropConfiguration? = null,
    val style: ArtStylePreset? = null,
    val artStyle: ArtStyleConfiguration? = null,
    val surfaceTexture: SurfaceTextureConfiguration? = null,
    val exclusions: List<String>? = null,
    val priorityStack: List<String>? = null,

) {
    fun applyTo(project: CharacterProject) = project.copy(
        environment = environment ?: project.environment, lighting = lighting ?: project.lighting,
        colorDirection = colorDirection ?: project.colorDirection, effects = effects ?: project.effects,
        colorAccents = colorAccents ?: project.colorAccents, powerSignature = powerSignature ?: project.powerSignature, expression = expression ?: project.expression,
        costume = costume ?: project.costume,
        accessories = accessories ?: project.accessories,
        prop = prop ?: project.prop,
        style = style ?: project.style,
        artStyle = artStyle ?: project.artStyle,
        surfaceTexture = surfaceTexture ?: project.surfaceTexture,
        exclusions = exclusions ?: project.exclusions,
        priorityStack = priorityStack ?: project.priorityStack,
        pose = pose, gazeDirection = gaze, composition = composition, output = output, promptAuthoring = promptAuthoring,
    )
    companion object {
        fun capture(project: CharacterProject) = VariantScene(project.pose, project.gazeDirection, project.composition, project.output, project.promptAuthoring, project.environment, project.lighting, project.colorDirection, project.effects, project.colorAccents, project.powerSignature, project.expression, project.costume, project.accessories, project.prop, project.style, project.artStyle, project.surfaceTexture, project.exclusions, project.priorityStack)
    }
}

@Serializable
data class ProjectVariant(
    val id: String,
    val name: String,
    val scene: VariantScene,
    val generation: GenerationPreferences = GenerationPreferences(),
) {
    init { require(id.isNotBlank() && id != PRIMARY_VARIANT && name.isNotBlank()) }
}

const val PRIMARY_VARIANT = "primary"

/** The original scene remains in CharacterProject for portable JSON compatibility. No shared character copies. */
@Serializable
data class ProjectVariants(
    val projectId: String,
    val activeId: String = PRIMARY_VARIANT,
    val primaryName: String = "Original",
    val primaryGeneration: GenerationPreferences = GenerationPreferences(),
    val alternatives: List<ProjectVariant> = emptyList(),
) {
    init {
        require(projectId.isNotBlank() && primaryName.isNotBlank())
        require(alternatives.map { it.id }.distinct().size == alternatives.size)
        require(activeId == PRIMARY_VARIANT || alternatives.any { it.id == activeId })
    }
    val selected get() = alternatives.firstOrNull { it.id == activeId }
    val activeName get() = selected?.name ?: primaryName
    val generation get() = selected?.generation ?: primaryGeneration
}
