package com.kelvinsaputra.promptstudio.guide

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density

/** Android has no bundled SVG decoder; the registry may provide a raster thumbnail. */
internal expect fun decodeGuideSvg(bytes: ByteArray, density: Density): Painter?
