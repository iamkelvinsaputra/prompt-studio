package com.kelvinsaputra.promptstudio.domain

import kotlinx.serialization.Serializable

val characterGenders = listOf(GenderPresentation.MALE, GenderPresentation.FEMALE)

@Serializable
enum class IdentityTrait(val label: String) { FACE("Face"), EYES("Eyes"), HAIR("Hair"), SKIN("Skin"), BODY("Body"), FEATURES("Distinctive features") }

/** Complements the existing typed body, face and hair sections; no second copy of their traits. */
@Serializable
data class CharacterProfile(
    val enabled: Boolean = false,
    val skinTone: String = "",
    val broadAppearance: String = "",
    val eyeColor: String = "",
    val jawShape: String = "",
    val nose: String = "",
    val hairTexture: String = "",
    val distinguishingFeatures: String = "",
    val lockedTraits: Set<IdentityTrait> = emptySet(),
)
val CharacterProject.characterName get() = identity.characterName.trim().ifBlank { name }

/** Locks protect edits, presets and randomization alike. Unlock explicitly before redefining a trait. */
fun CharacterProject.preservingIdentityOf(original: CharacterProject): CharacterProject {
    val locks = original.profile.lockedTraits
    return copy(
        identity = identity.copy(bodyType = if (IdentityTrait.BODY in locks) original.identity.bodyType else identity.bodyType),
        body = if (IdentityTrait.BODY in locks) original.body else body,
        face = if (IdentityTrait.FACE in locks) original.face else face.copy(eyeShape = if (IdentityTrait.EYES in locks) original.face.eyeShape else face.eyeShape),
        hair = if (IdentityTrait.HAIR in locks) original.hair else hair,
        profile = profile.copy(
            eyeColor = if (IdentityTrait.EYES in locks) original.profile.eyeColor else profile.eyeColor,
            jawShape = if (IdentityTrait.FACE in locks) original.profile.jawShape else profile.jawShape,
            nose = if (IdentityTrait.FACE in locks) original.profile.nose else profile.nose,
            skinTone = if (IdentityTrait.SKIN in locks) original.profile.skinTone else profile.skinTone,
            broadAppearance = if (IdentityTrait.SKIN in locks) original.profile.broadAppearance else profile.broadAppearance,
            hairTexture = if (IdentityTrait.HAIR in locks) original.profile.hairTexture else profile.hairTexture,
            distinguishingFeatures = if (IdentityTrait.FEATURES in locks) original.profile.distinguishingFeatures else profile.distinguishingFeatures,
        ),
    )
}

fun CharacterProject.withArchetype(label: String): CharacterProject {
    val build = when (label) { "Athletic", "Adventurous" -> BodyType.ATHLETIC; "Elegant", "Refined" -> BodyType.ELEGANT; "Rugged" -> BodyType.STURDY; else -> BodyType.LEAN }
    return copy(identity = identity.copy(bodyType = build, coreVibe = label.lowercase()),
        expression = expression.copy(preset = if (label in listOf("Confident", "Rebellious")) ExpressionPreset.COCKY else ExpressionPreset.CALM))
        .preservingIdentityOf(this)
}
fun archetypes(gender: GenderPresentation?) = if (gender == GenderPresentation.MALE)
    listOf("Lean", "Athletic", "Refined", "Rugged", "Reserved", "Confident", "Professional", "Youthful", "Stoic", "Adventurous")
else listOf("Soft", "Elegant", "Athletic", "Confident", "Rebellious", "Professional", "Cute", "Mature", "Stoic", "Adventurous")

@Serializable
enum class EnvironmentCategory(val label: String, val locations: List<String>) {
    NONE("No environment", emptyList()), MINIMAL("Minimal", listOf("Soft gradient", "Plain backdrop", "Quiet horizon")),
    STUDIO("Studio", listOf("Seamless backdrop", "Paper sweep", "Textured wall")),
    INTERIOR("Interior", listOf("Bedroom", "Cafe", "Library", "Office", "Apartment", "Studio", "Train interior", "Classroom", "Workshop", "Hallway")),
    URBAN("Urban", listOf("City street", "Rooftop", "Alley", "Station", "Cafe exterior", "Residential street", "Downtown", "Industrial district", "Park", "Bridge")),
    NATURE("Nature", listOf("Forest", "Field", "Mountain", "Beach", "River", "Lake", "Garden", "Cliff", "Snow landscape", "Desert")),
    FANTASY("Fantasy", listOf("Ancient courtyard", "Floating garden", "Stone citadel", "Enchanted forest")),
    SCI_FI("Sci-fi", listOf("Orbital station", "Research lab", "Future city", "Spaceport")),
    ABSTRACT("Abstract", listOf("Color fields", "Geometric planes", "Ink drift", "Soft shapes"));
    val outdoors get() = this in listOf(URBAN, NATURE, FANTASY)
}
fun CharacterProject.withEnvironment(category: EnvironmentCategory) = copy(environment = EnvironmentConfiguration(
    category = category, abstractionLevel = when(category) { EnvironmentCategory.NONE -> EnvironmentAbstraction.NONE; EnvironmentCategory.MINIMAL -> EnvironmentAbstraction.MINIMAL; else -> EnvironmentAbstraction.FULL },
    additionalInstructions = environment.additionalInstructions,
))

@Serializable
enum class LightDirection(val label: String, override val wording: String) : PromptOption {
    FRONT("Front", "from directly in front of the character"), FRONT_LEFT("Front-left", "from the front-left"), FRONT_RIGHT("Front-right", "from the front-right"),
    LEFT("Left side", "from the character's left side"), RIGHT("Right side", "from the character's right side"), BACK("Back", "from behind the character"),
    BACK_LEFT("Back-left", "from behind on the left"), BACK_RIGHT("Back-right", "from behind on the right"), TOP("Top", "from overhead"), BELOW("Below", "from below the character");
}
data class LightingPreset(val name: String, val direction: LightDirection, val quality: String, val temperature: String, val contrast: String = "low") {
    fun applyTo(p: CharacterProject) = p.copy(lighting = LightingConfiguration(preset = name, lightDirection = direction, quality = quality, temperature = temperature, contrast = contrast, additionalInstructions = p.lighting.additionalInstructions))
}
val lightingPresets = listOf(
    LightingPreset("Overcast daylight", LightDirection.FRONT_LEFT, "diffused", "neutral"),
    LightingPreset("Golden hour", LightDirection.BACK_LEFT, "soft", "warm"),
    LightingPreset("Soft studio", LightDirection.FRONT, "soft", "neutral"),
    LightingPreset("Window light", LightDirection.LEFT, "soft", "neutral"),
    LightingPreset("Cloudy outdoor", LightDirection.TOP, "diffused", "cool"),
    LightingPreset("Neon night", LightDirection.RIGHT, "hard", "cool", "high"),
    LightingPreset("Dramatic rim", LightDirection.BACK_RIGHT, "dramatic", "neutral", "high"),
    LightingPreset("Cinematic backlight", LightDirection.BACK, "dramatic", "warm", "high"),
    LightingPreset("Moonlight", LightDirection.TOP, "soft", "cool"),
    LightingPreset("Indoor ambient", LightDirection.FRONT_RIGHT, "diffused", "warm"),
)

@Serializable
enum class ColorRole(val label: String) { PRIMARY("Character primary"), SECONDARY("Character secondary"), ACCENT("Accent"), ENVIRONMENT("Environment"), LIGHT("Lighting tint") }
fun normalizeHex(value: String): String? = value.trim().removePrefix("#").takeIf { it.matches(Regex("[0-9a-fA-F]{6}")) }?.let { "#${it.uppercase()}" }
@Serializable
data class PaletteColor(val role: ColorRole, val hex: String) { init { require(normalizeHex(hex) != null) { "Use a six-digit HEX color." } } }
@Serializable
data class ColorDirection(val description: String = "", val colors: List<PaletteColor> = emptyList(), val saturation: Int? = null, val contrast: Int? = null, val temperature: Int? = null, val brightness: Int? = null) {
    init { require(colors.map { it.role }.distinct().size == colors.size); require(listOfNotNull(saturation, contrast, temperature, brightness).all { it in 0..100 }) }
    fun withColor(role: ColorRole, hex: String?) = copy(colors = (colors.filterNot { it.role == role } + listOfNotNull(hex?.let { PaletteColor(role, requireNotNull(normalizeHex(it))) })).sortedBy { it.role.ordinal })
}
data class PalettePreset(val name: String, val hexes: List<String>) {
    fun direction() = ColorDirection(description = name, colors = hexes.mapIndexed { index, hex -> PaletteColor(ColorRole.entries[index], hex) })
}
val palettePresets = listOf(
    PalettePreset("Muted cool", listOf("#7D688C", "#768799", "#C0B6C6", "#E1E4E7")),
    PalettePreset("Warm neutral", listOf("#9D7961", "#D3BEA4", "#B07156", "#F1E8DC")),
    PalettePreset("Pastel", listOf("#B7CBE0", "#D8B7CB", "#EEDCA9", "#F4F0EA")),
    PalettePreset("Monochrome", listOf("#353535", "#787878", "#BFBFBF", "#EEEEEE")),
    PalettePreset("Earth", listOf("#65745A", "#957253", "#C4A35F", "#E4DDCB")),
    PalettePreset("High contrast", listOf("#18232C", "#EEE6D6", "#D5573B", "#F9F4EB")),
    PalettePreset("Jewel tones", listOf("#35588B", "#286B59", "#A14E78", "#222A3A")),
    PalettePreset("Desaturated", listOf("#8C9391", "#ACA5A1", "#807A89", "#DFDEDB")),
    PalettePreset("Soft blue", listOf("#6D93B2", "#BDD2DF", "#E8CCB2", "#EDF2F4")),
    PalettePreset("Autumn", listOf("#9C503A", "#BE8B49", "#6F704E", "#E6D5BC")),
    PalettePreset("Winter", listOf("#526A87", "#ACBDCB", "#D6C5CF", "#EAF0F2")),
    PalettePreset("Night", listOf("#282C49", "#515B7E", "#C39A69", "#171B2B")),
)

@Serializable
enum class EffectFamily(val label: String, val choices: List<String>) {
    WEATHER("Weather", listOf("Rain", "Snow", "Mist", "Fog", "Drizzle", "Storm")),
    WIND("Wind", listOf("Gentle wind", "Moderate wind", "Strong wind")),
    PARTICLES("Particles", listOf("Dust", "Petals", "Leaves", "Snow particles", "Embers", "Sparkles")),
    ATMOSPHERIC("Atmospheric", listOf("Mist", "Haze", "Smoke", "Steam", "Bloom", "Light rays")),
    MOTION("Motion", listOf("Hair movement", "Clothing movement", "Motion blur", "Trailing fabric", "Environmental movement")),
    LIGHT("Light", listOf("Bokeh", "Soft glow", "Lens flare")),
    NATURAL("Natural", listOf("Falling leaves", "Floating petals", "Grass movement", "Water spray")),
    SUPERNATURAL("Supernatural", listOf("Energy", "Aura", "Floating fragments", "Abstract light", "Glowing particles")),
    GRAPHIC("Graphic", listOf("Speed lines", "Halftone", "Geometric accents"));
}
@Serializable
enum class EffectIntensity(val label: String) { SUBTLE("Subtle"), MODERATE("Moderate"), STRONG("Strong") }
@Serializable
data class EffectItem(val family: EffectFamily, val name: String)
@Serializable
data class EffectsConfiguration(val items: List<EffectItem> = emptyList(), val intensity: EffectIntensity = EffectIntensity.SUBTLE, val direction: String = "", val customDescription: String = "") {
    init { require(items.size <= 6) }
}
