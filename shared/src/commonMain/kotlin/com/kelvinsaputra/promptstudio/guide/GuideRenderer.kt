package com.kelvinsaputra.promptstudio.guide

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.kelvinsaputra.promptstudio.domain.VisualAssemblyState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/** Saved with generation metadata, never with character JSON or PNG bytes. */
@Serializable
data class GuideRenderSpec(val assembly: VisualAssemblyState, val width: Int, val height: Int, val version: Int = 1) {
    init {
        require(version == 1) { "Unsupported guide renderer version" }
        require(width in 1..1024 && height in 1..1024)
    }
    companion object {
        /** Unsupported/custom poses remain text-only rather than sending a misleading approximation. */
        fun from(assembly: VisualAssemblyState, aspectRatio: String?): GuideRenderSpec? {
            if (assembly.hasApproximation) return null
            val parts = aspectRatio?.split(':') ?: return null
            if (parts.size != 2) return null
            val a = parts[0].trim().toDoubleOrNull() ?: return null
            val b = parts[1].trim().toDoubleOrNull() ?: return null
            val ratio = a / b
            if (a <= 0 || b <= 0 || !ratio.isFinite() || ratio !in (1.0 / 32)..32.0) return null
            return if (ratio >= 1) GuideRenderSpec(assembly, 768, (768 / ratio).roundToInt())
                else GuideRenderSpec(assembly, (768 * ratio).roundToInt(), 768)
        }
    }
}

/** Owns encoded bytes. Provider conversion and serialization are outside the renderer. */
class GuideImage(bytes: ByteArray, val width: Int, val height: Int) {
    private val png = bytes.copyOf()
    val mimeType = "image/png"
    fun bytes(): ByteArray = png.copyOf()
    init { require(bytes.isNotEmpty() && bytes.size <= 4 * 1024 * 1024 && width in 1..1024 && height in 1..1024) }
}

fun interface GuideRenderer { suspend fun render(spec: GuideRenderSpec): GuideImage }

/** Draws directly into a bitmap; no composition, visible UI, screenshot or file is involved. */
object LocalGuideRenderer : GuideRenderer {
    override suspend fun render(spec: GuideRenderSpec): GuideImage = withContext(Dispatchers.Default) {
        val bitmap = ImageBitmap(spec.width, spec.height)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(spec.width.toFloat(), spec.height.toFloat())) {
            drawSilhouette(spec.assembly, "${spec.width}:${spec.height}", thumbnail = false, edgeToEdge = true)
        }
        GuideImage(encodeGuidePng(bitmap), spec.width, spec.height)
    }
}

internal expect fun encodeGuidePng(bitmap: ImageBitmap): ByteArray
