package com.kelvinsaputra.promptstudio.domain

import kotlinx.serialization.Serializable

/** Navigation only. All selections live in CharacterProject, never in a wizard draft. */
enum class GuideStep(val title: String, val question: String) {
    Gender("Gender", "Who are you imagining?"), Age("Age", "Choose an adult age band"),
    Pose("Pose", "Find the gesture"), Gaze("Gaze", "Where is their attention?"),
    Framing("Framing", "How much do we see?"), Position("Position", "Place your subject"),
    Composition("Composition", "Balance the image"), ArtStyle("Art Style", "Choose a look"),
}

data class GuidedNavigation(val step: GuideStep? = null, val onboarding: Boolean = false) {
    fun edit(step: GuideStep) = GuidedNavigation(step)
    fun forward() = if (!onboarding || step == GuideStep.entries.last()) GuidedNavigation()
        else copy(step = GuideStep.entries[(step?.ordinal ?: -1) + 1])
    fun back() = if (!onboarding || step == GuideStep.Gender) GuidedNavigation()
        else copy(step = GuideStep.entries[(step?.ordinal ?: 1) - 1])
    companion object { fun newProject() = GuidedNavigation(GuideStep.Gender, true) }
}

@Serializable
enum class GuideComposition(val label: String, val wording: String) {
    HERO("Centered Hero", "a clear central focal hierarchy with quiet supporting shapes"),
    THIRDS("Rule of Thirds", "balance supporting shapes along the frame thirds"),
    NEGATIVE_SPACE("Negative Space", "broad quiet space opposite the subject; sparse supporting detail"),
    SYMMETRICAL("Symmetrical", "mirror the supporting shapes across the frame center"),
    DIAGONAL("Diagonal", "a rising diagonal flow through the supporting shapes"),
    OFF_CENTER("Off-Center", "an asymmetric balance of large and small supporting shapes"),
}

/** Bounded rotations relative to a named pose; no screen or bitmap coordinates. */
@Serializable
data class PoseAdjustments(
    val owner: BasePose? = null,
    val leftShoulder: Float = 0f,
    val rightShoulder: Float = 0f,
    val leftElbow: Float = 0f,
    val rightElbow: Float = 0f,
) {
    init { require(listOf(leftShoulder, rightShoulder, leftElbow, rightElbow).all { it.isFinite() && it in -45f..45f }) }
    val isAdjusted get() = listOf(leftShoulder, rightShoulder, leftElbow, rightElbow).any { it != 0f }
}
val PoseConfiguration.effectiveAdjustments get() = guideAdjustments.takeIf { it.owner == basePose } ?: PoseAdjustments()
fun CharacterProject.withPoseAdjustments(value: PoseAdjustments) = copy(pose = pose.copy(guideAdjustments = value.copy(owner = pose.basePose)))
fun CharacterProject.withGuideGender(value: GenderPresentation): CharacterProject {
    require(value == GenderPresentation.MALE || value == GenderPresentation.FEMALE)
    return copy(identity = identity.copy(genderPresentation = value))
}
val guidedAgeBands = listOf(AgeBand.YOUNG_ADULT, AgeBand.ADULT, AgeBand.MIDDLE_AGED, AgeBand.MATURE)
fun AgeBand.guideLabel() = when (this) {
    AgeBand.YOUNG_ADULT -> "Young Adult"; AgeBand.ADULT -> "Adult"; AgeBand.MIDDLE_AGED -> "Mature"
    AgeBand.MATURE -> "Older Adult"; else -> wording
}
fun CharacterProject.withGuideAge(value: AgeBand): CharacterProject {
    require(value in guidedAgeBands)
    return copy(identity = identity.copy(ageBand = value, ageRange = ""))
}
val guidedGazes = listOf(Gaze.TOWARD_VIEWER, Gaze.LEFT, Gaze.RIGHT, Gaze.UPWARD, Gaze.DOWNWARD, Gaze.OFF_SCREEN)
fun Gaze.guideLabel() = when (this) {
    Gaze.TOWARD_VIEWER -> "Camera"; Gaze.LEFT -> "Left"; Gaze.RIGHT -> "Right"; Gaze.UPWARD -> "Up"
    Gaze.DOWNWARD -> "Down"; Gaze.OFF_SCREEN -> "Away"; else -> wording
}
fun CharacterProject.withGuideGaze(value: Gaze) = copy(gazeDirection = gazeDirection.copy(
    gazeTarget = value, headDirection = when (value) {
        Gaze.UPWARD -> Head.CHIN_UP; Gaze.DOWNWARD -> Head.CHIN_DOWN; Gaze.OFF_SCREEN -> Head.TURNED_AWAY
        else -> Head.LEVEL
    },
))
val guidedFramings = listOf(Framing.FULL_BODY, Framing.THREE_QUARTER, Framing.WAIST_UP, Framing.BUST_UP, Framing.CLOSE_UP)
fun Framing.guideLabel() = when (this) {
    Framing.FULL_BODY -> "Full Body"; Framing.THREE_QUARTER -> "Three-Quarter Body"; Framing.WAIST_UP -> "Waist Up"
    Framing.BUST_UP -> "Bust"; Framing.CLOSE_UP -> "Close Up"; else -> "Thigh Up"
}
fun CharacterProject.withGuideFraming(value: Framing) = copy(output = output.copy(framing = value))
fun CharacterProject.withGuidePosition(value: VisualPlacement) = copy(output = output.copy(figurePlacement = value.wording))
fun CharacterProject.withGuideComposition(value: GuideComposition) = copy(composition = composition.copy(guidePreset = value))
fun CharacterProject.withGuidedDefaults() = resetVisualAssembly()
    .withGuideGender(GenderPresentation.FEMALE).withGuideAge(AgeBand.ADULT).withGuideGaze(Gaze.TOWARD_VIEWER)
    .withGuideComposition(GuideComposition.HERO).copy(artStyle = ArtStyleConfiguration(preset = StyleLook.ANIME_INK))

/** The same derived labels power Summary and screen readers. Authored custom values remain visible. */
fun CharacterProject.guideSummary(step: GuideStep): String = when (step) {
    GuideStep.Gender -> when (identity.genderPresentation) { GenderPresentation.MALE -> "Man"; GenderPresentation.FEMALE -> "Woman"; else -> identity.genderPresentation?.wording ?: "Unspecified" }
    GuideStep.Age -> identity.ageBand?.guideLabel() ?: "Unspecified"
    GuideStep.Pose -> (visualAssembly.posePreset?.label ?: pose.basePose?.wording ?: "Unspecified") + if (pose.effectiveAdjustments.isAdjusted) " · Adjusted" else ""
    GuideStep.Gaze -> visualAssembly.gaze?.guideLabel() ?: "Unspecified"
    GuideStep.Framing -> output.framing?.guideLabel() ?: "Unspecified"
    GuideStep.Position -> visualAssembly.placementPreset?.label ?: output.figurePlacement.ifBlank { "Unspecified" }
    GuideStep.Composition -> composition.guidePreset?.label ?: composition.directionalFlow.ifBlank { "Custom / unspecified" }
    GuideStep.ArtStyle -> artStyleSummary
}
