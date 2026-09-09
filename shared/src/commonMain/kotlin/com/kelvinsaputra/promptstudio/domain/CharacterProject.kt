package com.kelvinsaputra.promptstudio.domain

import kotlinx.serialization.Serializable

@Serializable
data class CostumeConfiguration(
    val outfitIdentity: String = "altered institutional field uniform with punk personalization",
    val silhouette: Silhouette? = Silhouette.FITTED,
    val outerwear: Outerwear? = Outerwear.CROPPED_JACKET,
    val innerwear: Innerwear? = Innerwear.FITTED_TOP,
    val lowerWear: LowerWear? = LowerWear.SHORTS,
    val legwear: Legwear? = Legwear.UTILITY_TIGHTS,
    val footwear: Footwear? = Footwear.COMBAT_BOOTS,
    val handwear: Handwear? = Handwear.FINGERLESS_GLOVES,
    val utility: Utility? = Utility.COMPACT_BELT,
    val customization: Set<Customization> = setOf(Customization.STITCHED_REPAIR),
    val materialFeel: String = "matte technical fabric with canvas and leather accents",
    val exposureLevel: String = "controlled, not overly revealing",
    val customNotes: String = "",
) {
    init { require(customization.size <= 2) { "Choose at most two customization markers." } }
}

@Serializable
data class PoseConfiguration(
    val basePose: BasePose? = BasePose.ASYMMETRICAL_STANDING,
    val weight: Weight? = Weight.ON_RIGHT_LEG,
    val legAction: String = "front leg relaxed and slightly forward",
    val torso: Torso? = Torso.SLIGHT_TWIST,
    val arms: Arms? = Arms.HAND_NEAR_BELT,
    val head: Head? = Head.SLIGHT_TILT,
    val gaze: Gaze? = Gaze.TOWARD_VIEWER,
    val energy: Energy? = Energy.COCKY,
    val motionDirection: String = "subtle diagonal tension through the torso",
    val customNotes: String = "",
)

@Serializable
enum class OutputType(val label: String, val intent: String, val ratio: String?) {
    DESKTOP("Desktop wallpaper", "desktop wallpaper", "16:9"),
    PHONE("Phone wallpaper", "smartphone wallpaper", "9:16"),
    SQUARE("Square", "square illustration", "1:1"),
    PORTRAIT("Portrait", "portrait", "4:5"),
    CUSTOM("Custom", "illustration", null);

    val isWallpaper get() = this == DESKTOP || this == PHONE
}

@Serializable
data class OutputConfiguration(
    val type: OutputType = OutputType.PHONE,
    val customAspectRatio: String = "3:2",
    val customIntent: String = "illustration",
    val framing: Framing? = Framing.FULL_BODY,
    val figurePlacement: String = "slightly right of center",
    val negativeSpace: String = "upper-left area",
    val safeArea: String = "",
    val clockSafe: Boolean = true,
    val iconSafe: Boolean = true,
) {
    // Prompt-authoring text, not image dimensions: custom ratios need no numeric parser.
    val aspectRatio: String? get() = type.ratio ?: customAspectRatio.trim().takeIf { it.isNotEmpty() }
}

@Serializable
data class CharacterProject(
    val version: Int = 1,
    val style: ArtStylePreset = ArtStyles.SumiESkyBlue,
    val subject: String = "An original adult female antihero in her mid-20s with an athletic, agile build and a mischievous, dangerous presence.",
    val costume: CostumeConfiguration = CostumeConfiguration(),
    val pose: PoseConfiguration = PoseConfiguration(),
    val output: OutputConfiguration = OutputConfiguration(),
)
