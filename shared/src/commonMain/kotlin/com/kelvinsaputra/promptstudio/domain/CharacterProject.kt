package com.kelvinsaputra.promptstudio.domain

import kotlinx.serialization.Serializable

/**
 * These configurations are deliberately small prompt-authoring controls, not a simulation of
 * anatomy, fashion, photography, or a game inventory. Open-ended fields are kept where the
 * cheatsheet calls for authored language; bounded lists keep prompts legible.
 */
private fun List<String>.hasAtMost(maximum: Int): Boolean = count { it.trim().isNotEmpty() } <= maximum

@Serializable
data class IdentityConfiguration(
    val ageBand: AgeBand? = null,
    val ageRange: String = "",
    val genderPresentation: GenderPresentation? = null,
    val bodyType: BodyType? = null,
    val coreVibe: String = "",
    val additionalInstructions: String = "",
)

@Serializable
data class RoleConfiguration(
    val role: String = "",
    val organizationOrContext: String = "",
    val behavioralArchetype: String = "",
    val additionalInstructions: String = "",
)

@Serializable
data class PersonalityConfiguration(
    val traits: List<String> = emptyList(),
    val additionalInstructions: String = "",
) {
    init { require(traits.hasAtMost(7)) { "Choose at most seven personality traits." } }
}

@Serializable
data class ContradictionConfiguration(
    val outwardQuality: String = "",
    val inwardQuality: String = "",
    val secondaryNuance: String = "",
    val additionalInstructions: String = "",
)

@Serializable
data class BodyConfiguration(
    val ageRead: String = "",
    val build: Build? = null,
    val proportions: String = "",
    val heightImpression: HeightImpression? = null,
    val athleticLanguage: AthleticLanguage? = null,
    val additionalInstructions: String = "",
)

@Serializable
data class FaceConfiguration(
    val eyeShape: String = "",
    val eyebrowBehavior: String = "",
    val mouthOrSmile: String = "",
    val makeupOrGrooming: String = "",
    val facialStructure: String = "",
    val maturityNote: String = "",
    val additionalInstructions: String = "",
)

@Serializable
data class ExpressionConfiguration(
    val preset: ExpressionPreset? = null,
    val defaultExpression: String = "",
    val emotionalSubtext: String = "",
    val alternateRange: String = "",
    val additionalInstructions: String = "",
)

@Serializable
data class HairConfiguration(
    val baseColor: String = "",
    val roots: String = "",
    val length: HairLength? = null,
    val style: HairStyle? = null,
    val movementBehavior: String = "",
    val accentTreatment: String = "",
    val additionalInstructions: String = "",
)

@Serializable
data class AccessoriesConfiguration(
    val items: List<String> = emptyList(),
    val additionalInstructions: String = "",
) {
    init { require(items.hasAtMost(3)) { "Choose at most three accessories." } }
}

@Serializable
data class ShapeLanguageConfiguration(
    val principles: List<String> = emptyList(),
    val additionalInstructions: String = "",
) {
    init { require(principles.hasAtMost(6)) { "Choose at most six shape-language principles." } }
}

@Serializable
data class PowerSignatureConfiguration(
    val symbolicMeaning: String = "",
    val manifestation: String = "",
    val shapeLogic: String = "",
    val motionQuality: String = "",
    val colorMaterialBehavior: String = "",
    val restraint: String = "",
    val additionalInstructions: String = "",
)

@Serializable
data class PropConfiguration(
    val primaryProp: String = "",
    val primaryFunction: String = "",
    val primaryStyleLogic: String = "",
    val secondaryProp: String = "",
    val additionalInstructions: String = "",
)

@Serializable
data class GazeConfiguration(
    val headDirection: Head? = null,
    val gazeTarget: Gaze? = null,
    val intensity: GazeIntensity? = null,
    val additionalInstructions: String = "",
)

@Serializable
data class CompositionConfiguration(
    val directionalFlow: String = "",
    val detailConcentration: String = "",
    val readabilityPriority: String = "",
    val additionalInstructions: String = "",
)

@Serializable
data class EnvironmentConfiguration(
    val worldContextHint: String = "",
    val abstractionLevel: EnvironmentAbstraction? = null,
    val depthBehavior: String = "",
    val motifOrSupportElement: String = "",
    val additionalInstructions: String = "",
)

@Serializable
data class LightingConfiguration(
    val sourceQuality: LightingSource? = null,
    val direction: String = "",
    val shadowSoftness: ShadowSoftness? = null,
    val moodEffect: String = "",
    val exclusions: String = "",
    val additionalInstructions: String = "",
)

@Serializable
data class ColorAccentConfiguration(
    val accentColor: AccentColor? = null,
    val allowedSurfaces: String = "",
    val saturationCeiling: String = "",
    val forbiddenSurfaces: String = "",
    val emotionalRole: String = "",
    val additionalInstructions: String = "",
)

@Serializable
data class SurfaceTextureConfiguration(
    val paperGrain: TextureLevel? = null,
    val watercolorBehavior: WatercolorBehavior? = null,
    val inkTextureBehavior: InkTextureBehavior? = null,
    val additionalInstructions: String = "",
)

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
    /** Stable local identifier. `demo-character` is also the default for V0 JSON migration. */
    val id: String = "demo-character",
    val name: String = "Antihero",
    val style: ArtStylePreset = ArtStyles.SumiESkyBlue,
    /** Retained as a V0-compatible free-form subject fallback. */
    val subject: String = "An original adult female antihero in her mid-20s with an athletic, agile build and a mischievous, dangerous presence.",
    val identity: IdentityConfiguration = IdentityConfiguration(),
    val role: RoleConfiguration = RoleConfiguration(),
    val coreVisualThesis: String = "",
    val personality: PersonalityConfiguration = PersonalityConfiguration(),
    val contradiction: ContradictionConfiguration = ContradictionConfiguration(),
    val body: BodyConfiguration = BodyConfiguration(),
    val face: FaceConfiguration = FaceConfiguration(),
    val expression: ExpressionConfiguration = ExpressionConfiguration(),
    val hair: HairConfiguration = HairConfiguration(),
    val costume: CostumeConfiguration = CostumeConfiguration(),
    val accessories: AccessoriesConfiguration = AccessoriesConfiguration(),
    val shapeLanguage: ShapeLanguageConfiguration = ShapeLanguageConfiguration(),
    val powerSignature: PowerSignatureConfiguration = PowerSignatureConfiguration(),
    val prop: PropConfiguration = PropConfiguration(),
    val pose: PoseConfiguration = PoseConfiguration(),
    val gazeDirection: GazeConfiguration = GazeConfiguration(),
    val composition: CompositionConfiguration = CompositionConfiguration(),
    val environment: EnvironmentConfiguration = EnvironmentConfiguration(),
    val lighting: LightingConfiguration = LightingConfiguration(),
    val colorAccents: ColorAccentConfiguration = ColorAccentConfiguration(),
    val surfaceTexture: SurfaceTextureConfiguration = SurfaceTextureConfiguration(),
    val output: OutputConfiguration = OutputConfiguration(),
    val exclusions: List<String> = emptyList(),
    val priorityStack: List<String> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "Character id cannot be blank." }
        require(name.isNotBlank()) { "Character name cannot be blank." }
        require(exclusions.hasAtMost(15)) { "Choose at most fifteen exclusions." }
        require(priorityStack.hasAtMost(5)) { "Choose at most five visual priorities." }
    }
}
