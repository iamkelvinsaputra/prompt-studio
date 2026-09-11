package com.kelvinsaputra.promptstudio.guide

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import org.jetbrains.compose.resources.decodeToSvgPainter

internal actual fun decodeGuideSvg(bytes: ByteArray, density: Density): Painter? = bytes.decodeToSvgPainter(density)
