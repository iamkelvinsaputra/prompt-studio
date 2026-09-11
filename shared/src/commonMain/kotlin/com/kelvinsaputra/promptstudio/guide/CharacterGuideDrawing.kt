package com.kelvinsaputra.promptstudio.guide

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.*
import com.kelvinsaputra.promptstudio.domain.*
import kotlin.math.*

/** V2 preview and export share this exact renderer; inputs are semantic and immutable. */
internal fun DrawScope.drawSilhouette(state: VisualAssemblyState, aspectRatio: String?, thumbnail: Boolean, edgeToEdge: Boolean = false) {
    val parts = aspectRatio?.split(':')
    val ratio = if (parts?.size == 2) (parts[0].toFloatOrNull()?.div(parts[1].toFloatOrNull() ?: 0f)) else null
    val r = ratio?.takeIf { it.isFinite() && it > 0 } ?: .8f
    val inset = if (edgeToEdge) 0f else if (thumbnail) 3f else 8f
    val w = min(size.width - inset * 2, (size.height - inset * 2) * r).coerceAtLeast(1f)
    val h = w / r
    val x = (size.width - w) / 2; val y = (size.height - h) / 2
    drawRect(Color(0xFFF1F0EA), Offset(x, y), Size(w, h))
    clipRect(x, y, x + w, y + h) {
        translate(x, y) { drawBalance(state, w, h) }
        val placement = state.placementPreset ?: VisualPlacement.CENTER
        val zoom = when (state.framing) {
            Framing.THREE_QUARTER -> 1.3f; Framing.THIGH_UP -> 1.5f; Framing.WAIST_UP -> 1.85f
            Framing.BUST_UP -> 2.7f; Framing.CLOSE_UP -> 4.7f; else -> 1f
        }
        val body = GuideAssetCatalog.pose(state.posePreset)
        val headTop = body.shoulder.y - 57f
        val frameScale = min(w / 200f, h / 310f)
        // Full Body remains fully inside the frame even at upper/lower and side placements.
        val fitY = min(placement.y * h / (155f - headTop), (1f - placement.y) * h / 142f) * .94f
        val fitX = min(placement.x, 1f - placement.x) * w / 95f
        val scale = if (zoom == 1f) min(frameScale, min(fitY, fitX)) else frameScale * zoom
        val bodyY = if (zoom > 1f) y + h * .09f - headTop * scale + (placement.y - .5f) * h
            else y + h * placement.y - 150f * scale
        translate(x + w * placement.x - 100f * scale, bodyY) {
            scale(scale, scale, Offset.Zero) { drawMannequin(state, body) }
        }
    }
    drawRect(Color(0xFFC2C7C6), Offset(x, y), Size(w, h), style = Stroke(1f))
}

private fun DrawScope.drawBalance(state: VisualAssemblyState, w: Float, h: Float) {
    val mass = Color(0xFFDDE3DF); val line = Color(0xFFBFCBC7)
    when (state.composition) {
        GuideComposition.HERO -> drawOval(mass, Offset(w * .19f, h * .17f), Size(w * .62f, h * .65f))
        GuideComposition.THIRDS -> for (f in listOf(1f / 3, 2f / 3)) {
            drawLine(line, Offset(w * f, 0f), Offset(w * f, h), 1f)
            drawLine(line, Offset(0f, h * f), Offset(w, h * f), 1f)
        }
        GuideComposition.NEGATIVE_SPACE -> {
            val right = (state.placementPreset?.x ?: .5f) >= .5f
            drawRect(mass, Offset(if (right) w * .76f else 0f, h * .55f), Size(w * .24f, h * .45f))
        }
        GuideComposition.SYMMETRICAL -> {
            drawRect(mass, Offset(w * .08f, h * .25f), Size(w * .14f, h * .65f))
            drawRect(mass, Offset(w * .78f, h * .25f), Size(w * .14f, h * .65f))
        }
        GuideComposition.DIAGONAL -> drawPath(Path().apply {
            moveTo(0f, h * .85f); lineTo(w, h * .25f); lineTo(w, h * .5f); lineTo(0f, h); close()
        }, mass)
        GuideComposition.OFF_CENTER -> {
            drawOval(mass, Offset(-w * .2f, h * .1f), Size(w * .65f, h * .65f))
            drawRect(mass, Offset(w * .8f, h * .72f), Size(w * .2f, h * .25f))
        }
        null -> Unit
    }
}

private fun rotated(point: Offset, pivot: Offset, degrees: Float): Offset {
    val a = degrees * PI.toFloat() / 180f; val d = point - pivot
    return pivot + Offset(d.x * cos(a) - d.y * sin(a), d.x * sin(a) + d.y * cos(a))
}

/** Curved, tapered volumes overlap at joints to preserve a continuous adult form. */
private fun DrawScope.volume(a: Offset, b: Offset, start: Float, end: Float, color: Color) {
    val d = b - a; val length = d.getDistance().coerceAtLeast(1f); val n = Offset(-d.y / length, d.x / length)
    val p = Path().apply {
        val p0 = a + n * start; moveTo(p0.x, p0.y)
        val c1 = a + d * .34f + n * (start * 1.12f); val c2 = a + d * .72f + n * (end * 1.3f); val p1 = b + n * end
        cubicTo(c1.x, c1.y, c2.x, c2.y, p1.x, p1.y)
        val tip = b + d / length * end; val p2 = b - n * end
        quadraticTo(tip.x, tip.y, p2.x, p2.y)
        val c3 = a + d * .7f - n * (end * 1.1f); val c4 = a + d * .3f - n * start; val p3 = a - n * start
        cubicTo(c3.x, c3.y, c4.x, c4.y, p3.x, p3.y)
        val cap = a - d / length * start
        quadraticTo(cap.x, cap.y, p0.x, p0.y); close()
    }
    drawPath(p, color)
    drawLine(Color(0xFFBDCED2).copy(alpha = .15f), a + n * (start * .5f), b + n * (end * .4f), 1.5f, StrokeCap.Round)
}

private fun DrawScope.drawMannequin(state: VisualAssemblyState, asset: GuidePoseAsset) {
    val ink = Color(0xFF35434E); val front = Color(0xFF536673); val light = Color(0xFF718593)
    val woman = state.gender == GenderPresentation.FEMALE
    val maturity = when (state.ageBand) { AgeBand.YOUNG_ADULT -> 0; AgeBand.ADULT -> 1; AgeBand.MIDDLE_AGED -> 2; AgeBand.MATURE -> 3; else -> 1 }
    val mature = maturity >= 2
    val s = asset.shoulder; val hip = asset.hip
    val spread = (if (woman) 21f else 27f) * when (state.facing) { GuideFacing.FRONT -> 1f; GuideFacing.THREE_QUARTER -> .8f; GuideFacing.SIDE -> .5f }
    val pelvis = if (woman) 19f else 17f
    val ls = s + Offset(-spread, asset.tilt); val rs = s + Offset(spread, -asset.tilt)
    if (state.posePreset == VisualPose.SITTING) drawRect(Color(0xFFC7CECA), Offset(52f, 185f), Size(98f, 97f))
    // Hips, thighs, kneecaps, tapered calves and grounded feet.
    listOf(Triple(hip + Offset(-11f, 4f), asset.leftKnee, asset.leftAnkle), Triple(hip + Offset(11f, 4f), asset.rightKnee, asset.rightAnkle)).forEachIndexed { i, (a, k, f) ->
        volume(a, k, if (woman) 12f else 13f, 7f, if (i == 0) ink else front)
        volume(k, f, 7.5f, 4f, if (i == 0) ink else front)
        drawPath(Path().apply {
            moveTo(f.x - 4f, f.y - 3f); quadraticTo(f.x + 4f, f.y - 6f, f.x + 5f, f.y + 1f)
            quadraticTo(f.x + 14f, f.y + 5f, f.x + 12f, f.y + 8f); lineTo(f.x - 6f, f.y + 8f)
            quadraticTo(f.x - 7f, f.y + 3f, f.x - 4f, f.y - 3f); close()
        }, ink)
    }
    drawPath(Path().apply {
        moveTo(ls.x, ls.y - 3f)
        cubicTo(s.x - 12f, s.y - 10f, s.x + 12f, s.y - 10f, rs.x, rs.y - 3f)
        cubicTo(rs.x + 4f, rs.y + 22f, hip.x + 13f, hip.y - 30f, hip.x + pelvis, hip.y - 5f)
        quadraticTo(hip.x + pelvis + 3f, hip.y + 11f, hip.x + 10f, hip.y + 17f)
        quadraticTo(hip.x, hip.y + 20f, hip.x - 13f, hip.y + 15f)
        quadraticTo(hip.x - pelvis - 4f, hip.y + 6f, hip.x - pelvis, hip.y - 5f)
        cubicTo(hip.x - 12f, hip.y - 30f, ls.x - 4f, ls.y + 22f, ls.x, ls.y - 3f); close()
    }, front)
    drawPath(Path().apply {
        moveTo(ls.x + 5f, ls.y + 4f); quadraticTo(s.x, s.y - 2f, rs.x - 4f, rs.y + 4f)
        quadraticTo(s.x + 9f, s.y + 21f, s.x + 2f, s.y + 27f)
        quadraticTo(s.x - 11f, s.y + 20f, ls.x + 5f, ls.y + 4f); close()
    }, light.copy(alpha = .5f))
    val adjustments = state.adjustments.takeIf { it.owner == state.basePose } ?: PoseAdjustments()
    fun arm(start: Offset, elbow: Offset, wrist: Offset, shoulderAngle: Float, elbowAngle: Float, color: Color) {
        val e = rotated(elbow, start, shoulderAngle)
        val hand = rotated(rotated(wrist, start, shoulderAngle), e, elbowAngle)
        volume(start, e, 8f, 5f, color); volume(e, hand, 5.8f, 3.2f, color)
        val tip = hand + (hand - e) / (hand - e).getDistance().coerceAtLeast(1f) * 10f
        volume(hand, tip, 4.2f, 3.2f, color)
    }
    arm(ls, asset.leftElbow, asset.leftWrist, adjustments.leftShoulder, adjustments.leftElbow, ink)
    val hand = if (state.propPlacement == GuideProp.SHOULDER) Offset(rs.x + 12f, rs.y + 2f) else asset.rightWrist
    arm(rs, asset.rightElbow, hand, adjustments.rightShoulder, adjustments.rightElbow, front)
    if (state.propPlacement != GuideProp.NONE) {
        if (state.propPlacement == GuideProp.SHOULDER) drawLine(Color(0xFF939685), Offset(54f, s.y - 15f), Offset(179f, s.y + 8f), 6f, StrokeCap.Round)
        else drawLine(Color(0xFF939685), hand, hand + Offset(15f, 95f), 6f, StrokeCap.Round)
    }
    volume(s + Offset(0f, -21f), s + Offset(0f, -2f), 6f, 8f, front)
    val head = s + Offset(if (mature) 1f else 0f, -39f)
    val gaze = state.gaze
    val angle = when (gaze) { Gaze.UPWARD -> -9f; Gaze.DOWNWARD -> 12f; else -> 0f }
    rotate(angle, head) {
        val direction = when (gaze) { Gaze.LEFT -> -1f; Gaze.RIGHT -> 1f; else -> if (state.facing == GuideFacing.FRONT) 0f else 1f }
        translate(head.x, head.y) {
            drawPath(Path().apply {
                moveTo(-13f, -7f); cubicTo(-13f, -25f, 13f, -25f, 14f, -8f)
                cubicTo(15f, 1f, 10f, 15f, 2f, 19f); quadraticTo(-5f, 20f, -11f, 8f)
                quadraticTo(-15f, 1f, -13f, -7f); close()
            }, front)
            if (maturity == 3) {
                drawLine(Color(0xFFADB9BA), Offset(-11f, -9f), Offset(-10f, 0f), 2f, StrokeCap.Round)
                drawLine(Color(0xFFADB9BA), Offset(12f, -9f), Offset(11f, 0f), 2f, StrokeCap.Round)
            }
            if (gaze != Gaze.OFF_SCREEN) {
                val faceX = direction * 7f
                drawPath(Path().apply {
                    moveTo(faceX - 6f, -11f); quadraticTo(faceX + 5f, -15f, faceX + 8f, -4f)
                    lineTo(faceX + 10f, 3f); lineTo(faceX + 5f, 5f)
                    quadraticTo(faceX + 6f, 14f, faceX - 1f, 15f); quadraticTo(faceX - 7f, 8f, faceX - 6f, -11f); close()
                }, light)
                drawLine(ink, Offset(faceX - 3f, -2f), Offset(faceX + 3f, -2f), 1.3f, StrokeCap.Round)
                drawLine(ink.copy(alpha = .6f), Offset(faceX, 10f), Offset(faceX + 4f, 10f), .8f)
                if (maturity >= 1) drawLine(ink.copy(alpha = .2f), Offset(faceX + 5f, 5f), Offset(faceX + 3f, 8f), .7f)
                if (mature) drawLine(ink.copy(alpha = .45f), Offset(faceX - 4f, 2f), Offset(faceX + 1f, 3f), .8f)
                if (maturity == 3) drawLine(ink.copy(alpha = .4f), Offset(faceX - 4f, -8f), Offset(faceX + 2f, -8f), .7f)
            }
        }
    }
}
