package com.kelvinsaputra.promptstudio.domain

import kotlinx.serialization.Serializable

@Serializable
enum class GuideFacing(val label: String) { FRONT("Front"), THREE_QUARTER("3/4 view"), SIDE("Side") }
@Serializable
enum class GuideProp(val label: String) { NONE("No prop"), DOWN("Held down"), SHOULDER("On shoulder") }

enum class VisualPose(val label: String, val base: BasePose) {
    NEUTRAL("Neutral", BasePose.RELAXED_STANDING),
    RELAXED("Relaxed", BasePose.CONTRAPPOSTO),
    ACTION("Action", BasePose.READY_STANCE),
    SITTING("Sitting", BasePose.SITTING_ON_LEDGE);
}

enum class VisualPlacement(val label: String, val wording: String, val x: Float, val y: Float) {
    CENTER("Center", "center", .5f, .5f),
    LOWER("Lower", "lower center", .5f, .61f),
    LEFT("Left", "slightly left of center", .32f, .5f),
    RIGHT("Right", "slightly right of center", .68f, .5f);
}

/** Read-only projection: pose/output remain authoritative, including unsupported authored values. */
data class VisualAssemblyState(
    val basePose: BasePose?,
    val facing: GuideFacing,
    val framing: Framing?,
    val propPlacement: GuideProp,
    val figurePlacement: String,
) {
    val posePreset: VisualPose? get() = VisualPose.entries.firstOrNull { it.base == basePose }
    val placementPreset: VisualPlacement? get() = VisualPlacement.entries.firstOrNull { it.wording == figurePlacement }
    val summary: String get() = listOf(
        basePose?.wording ?: "Pose unspecified", facing.label,
        framing?.wording ?: "Framing unspecified", propPlacement.label,
        figurePlacement.ifBlank { "Placement unspecified" },
    ).joinToString(" · ")
    val hasApproximation: Boolean get() = posePreset == null || placementPreset == null || framing == null
}

val CharacterProject.visualAssembly: VisualAssemblyState get() = VisualAssemblyState(
    pose.basePose, pose.guideFacing, output.framing, pose.guideProp, output.figurePlacement,
)

/** Replace only mechanics owned by a pose preset; preserve head, gaze and authored notes. */
fun CharacterProject.withVisualPose(preset: VisualPose): CharacterProject {
    val mechanics = when (preset) {
        VisualPose.NEUTRAL -> pose.copy(weight = Weight.EVENLY_DISTRIBUTED, legAction = "feet comfortably apart", torso = Torso.UPRIGHT, arms = null, energy = Energy.CALM, motionDirection = "")
        VisualPose.RELAXED -> pose.copy(weight = Weight.ON_RIGHT_LEG, legAction = "front leg relaxed and slightly forward", torso = Torso.SLIGHT_TWIST, arms = Arms.HAND_NEAR_BELT, energy = Energy.POISED, motionDirection = "subtle diagonal tension through the torso")
        VisualPose.ACTION -> pose.copy(weight = Weight.EVENLY_DISTRIBUTED, legAction = "wide stance with bent knees", torso = Torso.FORWARD_LEAN, arms = Arms.HAND_EXTENDED, energy = Energy.POISED, motionDirection = "forward diagonal")
        VisualPose.SITTING -> pose.copy(weight = Weight.SEATED_WEIGHT, legAction = "knees bent with feet below the ledge", torso = Torso.UPRIGHT, arms = null, energy = Energy.CALM, motionDirection = "")
    }
    return copy(pose = mechanics.copy(basePose = preset.base))
}

fun CharacterProject.resetVisualAssembly(): CharacterProject = withVisualPose(VisualPose.NEUTRAL).let {
    it.copy(pose = it.pose.copy(guideFacing = GuideFacing.FRONT, guideProp = GuideProp.NONE),
        output = it.output.copy(framing = Framing.FULL_BODY, figurePlacement = VisualPlacement.CENTER.wording))
}
