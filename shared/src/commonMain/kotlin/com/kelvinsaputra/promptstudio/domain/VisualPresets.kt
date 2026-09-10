package com.kelvinsaputra.promptstudio.domain

import kotlinx.serialization.Serializable

@Serializable
sealed interface VisualPresetValue {
    fun applyTo(project: CharacterProject): CharacterProject

    @Serializable
    data class Pose(val pose: PoseConfiguration) : VisualPresetValue {
        override fun applyTo(project: CharacterProject) = project.copy(pose = pose)
    }

    /** Layout presets deliberately preserve the destination output format and prompt ownership. */
    @Serializable
    data class Composition(
        val composition: CompositionConfiguration,
        val framing: Framing?,
        val figurePlacement: String,
        val negativeSpace: String,
        val safeArea: String,
        val clockSafe: Boolean,
        val iconSafe: Boolean,
    ) : VisualPresetValue {
        override fun applyTo(project: CharacterProject) = project.copy(composition = composition, output = project.output.copy(
            framing = framing, figurePlacement = figurePlacement, negativeSpace = negativeSpace,
            safeArea = safeArea, clockSafe = clockSafe, iconSafe = iconSafe,
        ))
        companion object {
            fun capture(project: CharacterProject) = Composition(project.composition, project.output.framing,
                project.output.figurePlacement, project.output.negativeSpace, project.output.safeArea, project.output.clockSafe, project.output.iconSafe)
        }
    }
}

@Serializable
data class SavedVisualPreset(val id: String, val name: String, val value: VisualPresetValue) {
    init { require(id.isNotBlank() && name.isNotBlank()) }
    val category get() = when (value) { is VisualPresetValue.Pose -> "Pose"; is VisualPresetValue.Composition -> "Composition" }
}
