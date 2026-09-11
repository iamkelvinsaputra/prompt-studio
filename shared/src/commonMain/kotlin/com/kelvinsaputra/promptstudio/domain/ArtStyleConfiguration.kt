package com.kelvinsaputra.promptstudio.domain

import kotlinx.serialization.Serializable

@Serializable
enum class StyleLook(val label: String, val rendering: String) {
    ANIME("Contemporary Anime", "Contemporary Japanese anime-game illustration with controlled cel shading, precise contours and clean facial construction."),
    ANIME_INK("Anime + Ink Wash", "Contemporary Japanese anime-game illustration with selective sumi-e brushwork and translucent sky-blue watercolor, subtle pigment pooling and tactile washi surface."),
    PAINTERLY("Painterly", "Painterly character illustration with opaque gouache-like color planes, visible confident brushstrokes and softly lost and found edges."),
    WATERCOLOR("Watercolor", "Transparent watercolor character illustration with luminous washes, soft edge diffusion, delicate pigment pooling and tactile paper grain."),
    GAME("Anime Game Illustration", "Polished anime game illustration with layered rendering, readable materials and fine costume detail."),
    CEL("Clean Cel Shading", "Crisp anime contours and flat color regions with clean two-tone cel shadows."),
    PAINTERLY_ANIME("Painterly Anime", "Anime facial construction with layered painterly color and soft brushwork."),
    SEMI_REAL("Semi-Realistic Anime", "Stylized anime features with nuanced anatomy, material rendering and modeled light."),
    MANGA("Manga", "Monochrome manga illustration with expressive ink contours, screentones and clear silhouettes."),
    RETRO("Retro Anime", "Vintage anime illustration with simplified shapes, textured cels and expressive contour work."),
    NINETIES("90s Anime", "Hand-painted cel animation feeling with precise ink lines and softly textured background rendering."),
    WATERCOLOR_ANIME("Watercolor Anime", "Anime character construction with translucent watercolor washes and delicate paper texture."),
    INK_ANIME("Ink Anime", "Anime illustration using expressive black ink lines and restrained ink wash."),
    GOUACHE("Gouache Anime", "Anime forms painted in matte opaque gouache with broad confident brush marks."),
    SOFT("Soft Illustration", "Soft character illustration with gentle edges and delicate texture."),
    GRAPHIC("Graphic Anime", "Bold graphic anime forms with geometric shapes and decisive edge treatment."),
    MINIMAL("Minimal Anime", "Economical anime linework with simple shapes and selective detail."),
    FANTASY("Fantasy Anime", "Anime fantasy illustration with intricate invented materials and decorative craft."),
    SCI_FI("Sci-Fi Anime", "Anime science-fiction illustration with precise engineered forms and clean material definition."),
    CINEMATIC("Cinematic Anime", "Anime illustration with cinematic depth, selective focus and controlled rendering."),
    EDITORIAL("Editorial Anime", "Editorial anime illustration with expressive shapes, thoughtful visual metaphors and print texture."),
    CONCEPT("Anime Concept Art", "Exploratory anime concept illustration with readable design construction and selective finishing."),
    NOVEL("Light Novel Illustration", "Polished character-focused anime illustration with fine linework and delicate shading."),
}
@Serializable
enum class StyleInk(val label: String, val wording: String) {
    NONE("No Ink", "Use color edges without ink outlines."), SUBTLE("Subtle Ink", "Use restrained thin ink contours selectively."),
    STRONG("Strong Ink", "Use expressive black ink with pressure variation, thick-to-thin transitions and controlled dry-brush texture."),
}
@Serializable
enum class StyleMood(val label: String, val wording: String) {
    AIRY("Airy", "Keep an elegant, airy mood."), DRAMATIC("Dramatic", "Create a dramatic, focused mood."),
    MELANCHOLIC("Melancholic", "Keep a quiet, slightly melancholic mood."),
}
@Serializable
enum class StyleColor(val label: String, val wording: String) {
    MUTED("Muted", "Keep colors muted with restrained saturation."), BALANCED("Balanced", "Use balanced natural color saturation."),
    VIVID("Vivid", "Use vivid, deliberate color accents with clear color hierarchy."),
}
@Serializable
data class StyleAdjustments(val ink: StyleInk? = null, val mood: StyleMood? = null, val color: StyleColor? = null)

/** Null preset preserves the exact legacy/imported style. Manual ownership is section-scoped. */
@Serializable
data class ArtStyleConfiguration(
    val preset: StyleLook? = null,
    val adjustments: StyleAdjustments = StyleAdjustments(),
    val manualMode: Boolean = false,
    val manualDraft: String? = null,
    val lineTreatment: String = "",
    val shading: String = "",
    val rendering: String = "",
    val texture: String = "",
    val detail: String = "",
    val edges: String = "",
    val brush: String = "",
) {
    init { require(!manualMode || manualDraft != null) }
}
fun StyleLook.defaults() = when (this) {
    StyleLook.ANIME -> StyleAdjustments(StyleInk.SUBTLE, StyleMood.AIRY, null)
    StyleLook.ANIME_INK -> StyleAdjustments(StyleInk.STRONG, StyleMood.MELANCHOLIC, null)
    StyleLook.PAINTERLY -> StyleAdjustments(StyleInk.NONE, StyleMood.AIRY, null)
    StyleLook.WATERCOLOR -> StyleAdjustments(StyleInk.NONE, StyleMood.AIRY, null)
    else -> StyleAdjustments()
}
val CharacterProject.resolvedStyleAdjustments: StyleAdjustments get() {
    val defaults = artStyle.preset?.defaults() ?: StyleAdjustments()
    return StyleAdjustments(artStyle.adjustments.ink ?: defaults.ink, artStyle.adjustments.mood ?: defaults.mood, artStyle.adjustments.color ?: defaults.color)
}
fun CharacterProject.compileStructuredArtStyle(): String {
    val look = artStyle.preset
    val a = resolvedStyleAdjustments
    return listOfNotNull(
        look?.rendering ?: style.prompt.trim(), a.ink?.wording, a.mood?.wording, a.color?.wording,
        listOf("Line treatment" to artStyle.lineTreatment, "Shading" to artStyle.shading, "Rendering" to artStyle.rendering, "Texture" to artStyle.texture, "Detail" to artStyle.detail, "Edges" to artStyle.edges, "Brush character" to artStyle.brush).filter { it.second.isNotBlank() }.joinToString("\n") { "${it.first}: ${it.second}" }.takeIf { it.isNotBlank() },
        "Maintain believable age-appropriate proportions, clear costume construction, readable materials and a strong silhouette. Avoid exaggerated anatomy, random text and decorative noise.".takeIf { look != null },
    ).joinToString("\n")
}
fun CharacterProject.effectiveArtStyleCore(): String = if (artStyle.manualMode) requireNotNull(artStyle.manualDraft) else compileStructuredArtStyle()
fun CharacterProject.withStylePreset(preset: StyleLook) = copy(artStyle = artStyle.copy(preset = preset, adjustments = StyleAdjustments()))
fun CharacterProject.withStyleAdjustments(value: StyleAdjustments) = copy(artStyle = artStyle.copy(adjustments = value))
fun CharacterProject.enterManualStyle() = if (artStyle.manualMode) this else copy(artStyle = artStyle.copy(manualMode = true, manualDraft = compileStructuredArtStyle()))
fun CharacterProject.editManualStyle(text: String) = if (!artStyle.manualMode) this else copy(artStyle = artStyle.copy(manualDraft = text))
fun CharacterProject.useStructuredStyle() = copy(artStyle = artStyle.copy(manualMode = false))
fun CharacterProject.resumeManualStyle() = if (artStyle.manualDraft == null) this else copy(artStyle = artStyle.copy(manualMode = true))
val CharacterProject.artStyleSummary: String get() = if (artStyle.manualMode) "Custom · Manual ART STYLE CORE" else
    listOfNotNull(artStyle.preset?.label ?: style.name, resolvedStyleAdjustments.let { a -> listOfNotNull(a.ink?.label, a.mood?.label, a.color?.label).joinToString(" · ").takeIf { it.isNotEmpty() } }).joinToString(" · ")
