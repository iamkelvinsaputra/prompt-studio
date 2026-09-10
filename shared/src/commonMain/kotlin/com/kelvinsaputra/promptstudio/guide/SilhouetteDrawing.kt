package com.kelvinsaputra.promptstudio.guide

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.*
import com.kelvinsaputra.promptstudio.domain.*
import kotlin.math.min

/** Deterministic rendering, independent of remembered UI state and providers. Coordinates use a 200 × 300 body space. */
internal fun DrawScope.drawSilhouette(state: VisualAssemblyState, aspectRatio: String?, thumbnail: Boolean, edgeToEdge: Boolean = false) {
    val parts = aspectRatio?.split(':')
    val numerator = parts?.getOrNull(0)?.trim()?.toFloatOrNull()
    val denominator = parts?.getOrNull(1)?.trim()?.toFloatOrNull()
    val ratio = if (parts?.size == 2 && numerator != null && denominator != null && numerator > 0 && denominator > 0 && (numerator / denominator).isFinite()) numerator / denominator else .8f
    val inset = if (edgeToEdge) 0f else if (thumbnail) 4f else 12f
    val w = min(size.width - inset * 2, (size.height - inset * 2) * ratio).coerceAtLeast(1f)
    val h = (w / ratio).coerceAtLeast(1f)
    val left = (size.width - w) / 2
    val top = (size.height - h) / 2
    drawRect(Color(0xFFECEDE9), Offset(left, top), Size(w, h))
    clipRect(left, top, left + w, top + h) {
        if (!thumbnail && !edgeToEdge) {
            for (fraction in listOf(1f / 3, 2f / 3)) {
                drawLine(Color(0xFFD5D8D2), Offset(left + w * fraction, top), Offset(left + w * fraction, top + h), 1f)
                drawLine(Color(0xFFD5D8D2), Offset(left, top + h * fraction), Offset(left + w, top + h * fraction), 1f)
            }
        }
        val placement = state.placementPreset ?: VisualPlacement.CENTER
        val zoom = when (state.framing) { Framing.BUST_UP -> 2.6f; Framing.THIGH_UP -> 1.45f; Framing.THREE_QUARTER -> 1.15f; else -> 1f }
        val scale = min(w / 210f, h / 310f) * zoom
        val centerY = if (zoom > 1f) top + h * .12f + 130f * scale + (placement.y - .5f) * h else top + h * placement.y
        translate(left + w * placement.x - 100f * scale, centerY - 150f * scale) {
            scale(scale, scale, Offset.Zero) { drawBody(state) }
        }
    }
    drawRect(Color(0xFF92988E), Offset(left, top), Size(w, h), style = Stroke(1.5f))
}

private fun DrawScope.drawBody(state: VisualAssemblyState) {
    val ink = Color(0xFF303835)
    val prop = Color(0xFF79827B)
    val pose = state.posePreset ?: VisualPose.NEUTRAL
    val hipX = if (pose == VisualPose.RELAXED) 110f else 100f
    val hipY = if (pose == VisualPose.SITTING) 168f else 172f
    val shoulderX = if (pose == VisualPose.ACTION) 113f else 100f
    val spread = when (state.facing) { GuideFacing.FRONT -> 25f; GuideFacing.THREE_QUARTER -> 19f; GuideFacing.SIDE -> 11f }
    fun limb(start: Offset, middle: Offset, end: Offset? = null, width: Float = 15f, color: Color = ink) {
        listOfNotNull(start, middle, end).zipWithNext().forEach { (a, b) -> drawLine(color, a, b, width, StrokeCap.Round) }
    }
    if (pose == VisualPose.SITTING) drawRect(Color(0xFFCDD2CA), Offset(62f, 178f), Size(78f, 98f))
    val headX = shoulderX + if (state.facing == GuideFacing.FRONT) 0f else 5f
    drawOval(ink, Offset(headX - 16f, 28f), Size(32f, 43f))
    if (state.facing != GuideFacing.FRONT) {
        drawPath(Path().apply { moveTo(headX + 12f, 42f); lineTo(headX + 23f, 52f); lineTo(headX + 12f, 56f); close() }, ink)
    }
    limb(Offset(shoulderX, 62f), Offset(shoulderX, 86f), width = 14f)
    drawPath(Path().apply {
        moveTo(shoulderX - spread, 82f); lineTo(shoulderX + spread, 82f)
        lineTo(hipX + 17f, hipY); lineTo(hipX - 17f, hipY); close()
    }, ink)
    when (pose) {
        VisualPose.SITTING -> {
            limb(Offset(hipX - 10, hipY), Offset(66f, 192f), Offset(75f, 266f), width = 20f)
            limb(Offset(hipX + 10, hipY), Offset(141f, 191f), Offset(135f, 265f), width = 20f)
        }
        VisualPose.ACTION -> {
            limb(Offset(hipX - 10, hipY), Offset(61f, 212f), Offset(40f, 276f), width = 20f)
            limb(Offset(hipX + 10, hipY), Offset(149f, 216f), Offset(161f, 276f), width = 20f)
        }
        else -> {
            limb(Offset(hipX - 10, hipY), Offset(88f, 222f), Offset(81f, 277f), width = 20f)
            limb(Offset(hipX + 10, hipY), Offset(122f, 224f), Offset(125f, 277f), width = 20f)
        }
    }
    limb(Offset(shoulderX - spread, 89f), Offset(65f, 127f), Offset(if (pose == VisualPose.ACTION) 43f else 76f, if (pose == VisualPose.ACTION) 110f else 174f))
    val hand = when (state.propPlacement) {
        GuideProp.SHOULDER -> Offset(142f, 82f)
        else -> Offset(143f, 170f)
    }
    limb(Offset(shoulderX + spread, 89f), Offset(149f, 127f), hand)
    when (state.propPlacement) {
        GuideProp.NONE -> Unit
        GuideProp.DOWN -> limb(hand, Offset(161f, 268f), width = 10f, color = prop)
        GuideProp.SHOULDER -> limb(Offset(64f, 70f), Offset(177f, 91f), width = 10f, color = prop)
    }
}
