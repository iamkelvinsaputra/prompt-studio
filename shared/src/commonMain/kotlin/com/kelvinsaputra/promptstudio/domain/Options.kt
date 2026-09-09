package com.kelvinsaputra.promptstudio.domain

import kotlinx.serialization.Serializable

interface PromptOption { val wording: String }

@Serializable
enum class AgeBand(override val wording: String) : PromptOption {
    LATE_TEEN("late-teen"),
    YOUNG_ADULT("young adult"),
    ADULT("adult"),
    MIDDLE_AGED("middle-aged"),
    MATURE("mature")
}

@Serializable
enum class GenderPresentation(override val wording: String) : PromptOption {
    FEMALE("female"),
    MALE("male"),
    ANDROGYNOUS("androgynous"),
    NONBINARY_PRESENTING("nonbinary-presenting")
}

@Serializable
enum class BodyType(override val wording: String) : PromptOption {
    LEAN("lean"),
    ATHLETIC("athletic"),
    STURDY("sturdy"),
    ELEGANT("elegant"),
    WIRY("wiry"),
    MUSCULAR_BUT_CONTROLLED("muscular-but-controlled")
}

@Serializable
enum class Build(override val wording: String) : PromptOption {
    LEAN("lean"),
    ATHLETIC("athletic"),
    STURDY("sturdy"),
    ELEGANT("elegant"),
    WIRY("wiry"),
    MUSCULAR_BUT_CONTROLLED("muscular-but-controlled")
}

@Serializable
enum class HeightImpression(override val wording: String) : PromptOption {
    PETITE("petite"),
    COMPACT("compact"),
    AVERAGE("average height"),
    TALL("tall"),
    STATUESQUE("statuesque")
}

@Serializable
enum class AthleticLanguage(override val wording: String) : PromptOption {
    AGILE("agile"),
    TRAINED("trained"),
    DANCER_LIKE("dancer-like"),
    PRACTICAL("practical"),
    POWERFUL("powerful"),
    CONTROLLED("controlled")
}

@Serializable
enum class HairLength(override val wording: String) : PromptOption {
    CROPPED("cropped"),
    SHORT("short"),
    SHOULDER_LENGTH("shoulder-length"),
    LONG("long"),
    VERY_LONG("very long")
}

@Serializable
enum class HairStyle(override val wording: String) : PromptOption {
    STRAIGHT("straight"),
    WAVY("wavy"),
    LOOSELY_TIED("loosely tied"),
    BRAIDED("braided"),
    LAYERED("layered"),
    ASYMMETRICAL("asymmetrical")
}

@Serializable
enum class ExpressionPreset(override val wording: String) : PromptOption {
    CALM("calm"),
    AMUSED("amused"),
    COCKY("cocky"),
    POISED("poised"),
    PREDATORY("predatory"),
    PLAYFUL("playful")
}

@Serializable
enum class GazeIntensity(override val wording: String) : PromptOption {
    SOFT("soft"),
    DIRECT("direct"),
    CHALLENGING("challenging"),
    DETACHED("detached"),
    AMUSED("amused")
}

@Serializable
enum class EnvironmentAbstraction(override val wording: String) : PromptOption {
    MINIMAL("minimal"),
    ATMOSPHERIC("atmospheric"),
    LAYERED_ABSTRACT("layered abstract"),
    FAINT_ARCHITECTURAL("faint architectural"),
    SILHOUETTE_LED("silhouette-led")
}

@Serializable
enum class LightingSource(override val wording: String) : PromptOption {
    SOFT_DIFFUSE_DAYLIGHT("soft diffuse daylight"),
    OVERCAST_LIGHT("overcast light"),
    SOFT_SIDE_LIGHT("soft side light"),
    DIM_INTERIOR_LIGHT("dim interior light")
}

@Serializable
enum class ShadowSoftness(override val wording: String) : PromptOption {
    VERY_SOFT("very soft"),
    MODERATE("moderate")
}

@Serializable
enum class AccentColor(override val wording: String) : PromptOption {
    MUTED_WARM_BEIGE("muted warm beige"),
    DULL_CRIMSON("dull crimson"),
    MUTED_GOLD("muted gold"),
    DESATURATED_VIOLET("desaturated violet"),
    OLIVE_GRAY("olive gray")
}

@Serializable
enum class TextureLevel(override val wording: String) : PromptOption {
    LIGHT("light"),
    SUBTLE("subtle"),
    MODERATE("moderate")
}

@Serializable
enum class WatercolorBehavior(override val wording: String) : PromptOption {
    DELICATE_DIFFUSION("delicate watercolor diffusion"),
    SOFT_BLEED("soft bleed"),
    SUBTLE_PIGMENT_POOLING("subtle pigment pooling")
}

@Serializable
enum class InkTextureBehavior(override val wording: String) : PromptOption {
    CONTROLLED_DRY_BRUSH("controlled dry-brush texture"),
    VISIBLE_PRESSURE_VARIATION("visible pressure variation"),
    IMPERFECT_ENDINGS("imperfect brush endings")
}

@Serializable
enum class Silhouette(override val wording: String) : PromptOption {
    FITTED("fitted"),
    CROPPED("cropped"),
    OVERSIZED("oversized"),
    TAPERED("tapered"),
    LAYERED("layered"),
    ASYMMETRICAL("asymmetrical"),
    LONG_LINE("long-line"),
    COMPACT_TACTICAL("compact tactical")
}

@Serializable
enum class Outerwear(override val wording: String) : PromptOption {
    CROPPED_JACKET("cropped jacket"),
    BOMBER("bomber"),
    FIELD_JACKET("field jacket"),
    BLAZER("blazer"),
    COAT("coat"),
    PONCHO("poncho"),
    SLEEVELESS_VEST("sleeveless vest"),
    HOODED_SHELL("hooded shell")
}

@Serializable
enum class Innerwear(override val wording: String) : PromptOption {
    FITTED_TOP("fitted top"),
    MOCK_NECK("mock neck"),
    SHIRT("shirt"),
    BANDED_WRAP_TOP("banded wrap top"),
    BODYSUIT("bodysuit"),
    KNIT("knit"),
    TACTICAL_UNDERSHIRT("tactical undershirt")
}

@Serializable
enum class LowerWear(override val wording: String) : PromptOption {
    SHORTS("shorts"),
    SLIM_TROUSERS("slim trousers"),
    WIDE_TROUSERS("wide trousers"),
    SKIRT("skirt"),
    ASYMMETRICAL_SKIRT("asymmetrical skirt"),
    TACTICAL_PANTS("tactical pants"),
    PLEATED_HYBRID_BOTTOM("pleated hybrid bottom")
}

@Serializable
enum class Legwear(override val wording: String) : PromptOption {
    UTILITY_TIGHTS("utility tights"),
    STOCKINGS("stockings"),
    BARE_LEGS("bare legs"),
    WRAPS("wraps"),
    COMPRESSION_PANELS("compression panels")
}

@Serializable
enum class Footwear(override val wording: String) : PromptOption {
    COMBAT_BOOTS("combat boots"),
    SNEAKERS("sneakers"),
    LOAFERS("loafers"),
    SANDALS("sandals"),
    HEELED_BOOTS("heeled boots"),
    TABI_INSPIRED_BOOTS("tabi-inspired boots"),
    WORK_BOOTS("work boots")
}

@Serializable
enum class Handwear(override val wording: String) : PromptOption {
    FINGERLESS_GLOVES("fingerless gloves"),
    FULL_GLOVES("full gloves"),
    WRAPS("wraps"),
    BARE_HANDS("bare hands"),
    ARM_GUARDS("arm guards")
}

@Serializable
enum class Utility(override val wording: String) : PromptOption {
    COMPACT_BELT("compact belt"),
    POUCH_SET("pouch set"),
    HARNESS("harness"),
    HOLSTER("holster"),
    SLING_STRAPS("sling straps"),
    BUCKLE_SYSTEM("buckle system")
}

@Serializable
enum class Customization(override val wording: String) : PromptOption {
    PATCHES("patches"),
    PAINT_MARKS("paint marks"),
    MISMATCHED_STRAPS("mismatched straps"),
    ALTERED_HEM("altered hem"),
    STITCHED_REPAIR("stitched repair"),
    PINS("pins"),
    TALISMANS("talismans"),
    CHARM("charm"),
    RIBBON("ribbon"),
    TAG("tag")
}

@Serializable
enum class BasePose(override val wording: String) : PromptOption {
    RELAXED_STANDING("relaxed standing"),
    ASYMMETRICAL_STANDING("asymmetrical standing"),
    CONTRAPPOSTO("contrapposto"),
    LEANING("leaning"),
    SITTING_ON_LEDGE("sitting on ledge"),
    CROUCHING("crouching"),
    PERCHED("perched"),
    KNEELING("kneeling"),
    SLOUCHED_SEAT("slouched seat"),
    TURNING_OVER_SHOULDER("turning over shoulder"),
    WALKING_FORWARD("walking forward"),
    SUDDEN_TURN("sudden turn"),
    PIVOT("pivot"),
    STEPPING_THROUGH_SPACE("stepping through space"),
    WINDUP("windup"),
    RECOVERY_POSE("recovery pose"),
    EVASIVE_TWIST("evasive twist"),
    LANDING("landing"),
    READY_STANCE("ready stance"),
    OFF_BALANCE_RECOIL("off-balance recoil"),
    POWER_CASTING_GESTURE("power-casting gesture"),
    HALF_BODY_TURN("half-body turn"),
    CLOSE_BUST_POSE("close bust pose"),
    OVER_SHOULDER_LOOK("over-shoulder look"),
    HAND_NEAR_FACE_POSE("hand-near-face pose")
}

@Serializable
enum class Weight(override val wording: String) : PromptOption {
    ON_LEFT_LEG("on left leg"),
    ON_RIGHT_LEG("on right leg"),
    EVENLY_DISTRIBUTED("evenly distributed"),
    SEATED_WEIGHT("seated weight"),
    SUSPENDED_MID_MOTION("suspended mid-motion")
}

@Serializable
enum class Torso(override val wording: String) : PromptOption {
    UPRIGHT("upright"),
    SLIGHT_TWIST("slight twist"),
    SHARP_TWIST("sharp twist"),
    FORWARD_LEAN("forward lean"),
    BACKWARD_LEAN("backward lean"),
    CURVED_SLOUCH("curved slouch")
}

@Serializable
enum class Arms(override val wording: String) : PromptOption {
    ONE_HAND_IN_POCKET("one hand in pocket"),
    HAND_NEAR_BELT("hand near belt"),
    HAND_EXTENDED("hand extended"),
    CROSSED_LOOSELY("crossed loosely"),
    ONE_ARM_BRACING("one arm bracing"),
    ONE_ARM_CASTING_POWER("one arm casting power"),
    RELAXED_NATURALLY("relaxed naturally")
}

@Serializable
enum class Head(override val wording: String) : PromptOption {
    LEVEL("level"),
    SLIGHT_TILT("slight tilt"),
    CHIN_UP("chin up"),
    CHIN_DOWN("chin down"),
    TURNED_AWAY("turned away"),
    TURNED_BACK("turned back")
}

@Serializable
enum class Gaze(override val wording: String) : PromptOption {
    TOWARD_VIEWER("toward viewer"),
    PAST_VIEWER("past viewer"),
    DOWNWARD("downward"),
    OFF_SCREEN("off-screen")
}

@Serializable
enum class Energy(override val wording: String) : PromptOption {
    CALM("calm"),
    COCKY("cocky"),
    POISED("poised"),
    PREDATORY("predatory"),
    EVASIVE("evasive"),
    EXPLOSIVE("explosive"),
    PLAYFUL("playful")
}

@Serializable
enum class Framing(override val wording: String) : PromptOption {
    FULL_BODY("full-body"),
    THREE_QUARTER("3/4"),
    THIGH_UP("thigh-up"),
    BUST_UP("bust-up")
}
