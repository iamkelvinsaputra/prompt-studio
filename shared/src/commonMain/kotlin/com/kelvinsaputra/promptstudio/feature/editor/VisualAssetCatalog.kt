package com.kelvinsaputra.promptstudio.feature.editor

import com.kelvinsaputra.promptstudio.domain.*
import org.jetbrains.compose.resources.DrawableResource
import promptstudio.shared.generated.resources.*

/** Resource names stay at the presentation boundary. Domain/compiler never depend on assets. */
internal object VisualAssetCatalog {
    fun style(look: StyleLook): DrawableResource = when (look) {
        StyleLook.ANIME -> Res.drawable.style_anime
        StyleLook.ANIME_INK -> Res.drawable.style_anime_ink
        StyleLook.PAINTERLY -> Res.drawable.style_painterly
        StyleLook.WATERCOLOR -> Res.drawable.style_watercolor
    }
    fun sample(project: CharacterProject): StyleLook? = if (project.artStyle.manualMode) null else
        project.artStyle.preset ?: StyleLook.ANIME_INK.takeIf { project.style == ArtStyles.SumiESkyBlue }
}
