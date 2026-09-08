package com.kelvinsaputra.promptstudio.domain

data class ArtStylePreset(val id: String, val name: String, val prompt: String)

object ArtStyles {
    val SumiESkyBlue = ArtStylePreset(
        id = "sumi-e-sky-blue-rpg",
        name = "Sumi-e Sky Blue RPG",
        prompt = """
            A premium contemporary Japanese anime-game illustration with elegant, airy, slightly melancholic mood.
            Combine sophisticated anime character rendering with selective Japanese sumi-e brushwork, translucent watercolor, restrained supernatural abstraction, and strong negative space.
            Use expressive black ink brushwork selectively as a major graphic element, with visible pressure variation, thick-to-thin transitions, occasional dry-brush texture, imperfect endings, and controlled negative space.
            Use soft sky-blue watercolor as the primary atmospheric color, luminous and translucent rather than neon, with subtle pigment pooling, soft bleed, and delicate overlap with black ink.
            Maintain premium modern anime-game readability: believable adult proportions, controlled cel shading, restrained painterly softness, clean facial construction, clear costume construction, readable materials, and strong silhouette.
            The final image should feel like polished flagship Japanese RPG key art with subtle tactile paper/washi texture embedded into the image rather than a literal blank sheet.
            Avoid neon cyberpunk aesthetics, generic magic circles, random kanji, fake Japanese text, excessive particles, heavy ink splatter, cluttered backgrounds, exaggerated anatomy, or decorative AI noise.
        """.trimIndent(),
    )
    val presets = listOf(SumiESkyBlue)
}
