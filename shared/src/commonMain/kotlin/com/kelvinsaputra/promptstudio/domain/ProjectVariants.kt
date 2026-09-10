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

/** Only scene and prompt ownership changes across variants. Character/style/costume stay shared. */
@Serializable
data class VariantScene(
    val pose: PoseConfiguration,
    val gaze: GazeConfiguration,
    val composition: CompositionConfiguration,
    val output: OutputConfiguration,
    val promptAuthoring: PromptAuthoring,
) {
    fun applyTo(project: CharacterProject) = project.copy(
        pose = pose, gazeDirection = gaze, composition = composition, output = output, promptAuthoring = promptAuthoring,
    )
    companion object {
        fun capture(project: CharacterProject) = VariantScene(project.pose, project.gazeDirection, project.composition, project.output, project.promptAuthoring)
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
