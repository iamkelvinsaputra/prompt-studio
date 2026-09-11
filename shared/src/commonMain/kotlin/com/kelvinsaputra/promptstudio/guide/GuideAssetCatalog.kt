package com.kelvinsaputra.promptstudio.guide

import androidx.compose.ui.geometry.Offset
import com.kelvinsaputra.promptstudio.domain.VisualPose

/** Replaceable, normalized anatomy benchmark. Whole coherent gestures, no sprite limb resources. */
internal data class GuidePoseAsset(
    val shoulder: Offset = Offset(100f, 80f), val hip: Offset = Offset(100f, 154f),
    val tilt: Float = 0f,
    val leftElbow: Offset = Offset(68f, 120f), val leftWrist: Offset = Offset(68f, 160f),
    val rightElbow: Offset = Offset(132f, 120f), val rightWrist: Offset = Offset(132f, 160f),
    val leftKnee: Offset = Offset(88f, 216f), val leftAnkle: Offset = Offset(85f, 274f),
    val rightKnee: Offset = Offset(114f, 216f), val rightAnkle: Offset = Offset(118f, 274f),
)
internal object GuideAssetCatalog {
    fun pose(preset: VisualPose?): GuidePoseAsset = when (preset) {
        VisualPose.RELAXED -> GuidePoseAsset(shoulder = Offset(96f, 81f), hip = Offset(107f, 154f), tilt = -4f,
            rightElbow = Offset(143f, 114f), rightWrist = Offset(121f, 145f),
            leftKnee = Offset(84f, 215f), leftAnkle = Offset(75f, 271f), rightKnee = Offset(114f, 213f), rightAnkle = Offset(116f, 274f))
        VisualPose.CONFIDENT -> GuidePoseAsset(shoulder = Offset(103f, 77f), hip = Offset(94f, 152f), tilt = 3f,
            leftElbow = Offset(56f, 108f), leftWrist = Offset(80f, 145f), rightElbow = Offset(148f, 109f), rightWrist = Offset(112f, 145f),
            leftKnee = Offset(85f, 213f), leftAnkle = Offset(82f, 274f), rightKnee = Offset(126f, 214f), rightAnkle = Offset(143f, 274f))
        VisualPose.ACTION -> GuidePoseAsset(shoulder = Offset(114f, 88f), hip = Offset(97f, 158f), tilt = -8f,
            leftElbow = Offset(64f, 118f), leftWrist = Offset(42f, 93f), rightElbow = Offset(151f, 97f), rightWrist = Offset(173f, 66f),
            leftKnee = Offset(60f, 208f), leftAnkle = Offset(38f, 270f), rightKnee = Offset(148f, 205f), rightAnkle = Offset(166f, 271f))
        VisualPose.SITTING -> GuidePoseAsset(shoulder = Offset(101f, 101f), hip = Offset(101f, 171f),
            leftElbow = Offset(66f, 142f), leftWrist = Offset(72f, 190f), rightElbow = Offset(138f, 142f), rightWrist = Offset(130f, 190f),
            leftKnee = Offset(61f, 206f), leftAnkle = Offset(65f, 274f), rightKnee = Offset(142f, 206f), rightAnkle = Offset(140f, 274f))
        VisualPose.CROUCHING -> GuidePoseAsset(shoulder = Offset(111f, 138f), hip = Offset(103f, 202f), tilt = -6f,
            leftElbow = Offset(67f, 179f), leftWrist = Offset(56f, 238f), rightElbow = Offset(148f, 170f), rightWrist = Offset(137f, 212f),
            leftKnee = Offset(54f, 205f), leftAnkle = Offset(75f, 271f), rightKnee = Offset(151f, 208f), rightAnkle = Offset(132f, 273f))
        else -> GuidePoseAsset()
    }
}
