package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.guide.*
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

class GuideRendererTest {
    private val assembly = CharacterProject().resetVisualAssembly().withVisualPose(VisualPose.RELAXED).visualAssembly
    private fun spec(state: VisualAssemblyState = assembly, ratio: String = "9:16") = assertNotNull(GuideRenderSpec.from(state, ratio))
    private fun decode(image: GuideImage): BufferedImage = checkNotNull(ImageIO.read(ByteArrayInputStream(image.bytes())))
    private fun pixels(image: BufferedImage) = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
    private fun darkPixels(image: BufferedImage): List<Pair<Int, Int>> = buildList {
        for (y in 0 until image.height) for (x in 0 until image.width) {
            val rgb = image.getRGB(x, y)
            if ((rgb shr 16 and 255) < 85 && (rgb shr 8 and 255) < 90 && (rgb and 255) < 90) add(x to y)
        }
    }

    @Test fun offscreenPngIsDeterministicOpaqueAndUsesRequestedDimensions() = runBlocking {
        for (ratio in listOf("9:16", "16:9", "1:1", "1:8")) {
            val spec = spec(ratio = ratio)
            val first = LocalGuideRenderer.render(spec)
            val second = LocalGuideRenderer.render(spec)
            val bitmap = decode(first)
            assertEquals(spec.width, bitmap.width); assertEquals(spec.height, bitmap.height)
            assertContentEquals(pixels(bitmap), pixels(decode(second)))
            assertTrue(pixels(bitmap).all { (it ushr 24) == 255 })
            assertTrue(darkPixels(bitmap).isNotEmpty())
        }
    }

    @Test fun placementAndCropMoveAndEnlargeTheActualSilhouette() = runBlocking {
        val left = darkPixels(decode(LocalGuideRenderer.render(spec(assembly.copy(figurePlacement = VisualPlacement.LEFT.wording)))))
        val right = darkPixels(decode(LocalGuideRenderer.render(spec(assembly.copy(figurePlacement = VisualPlacement.RIGHT.wording)))))
        assertTrue(right.map { it.first }.average() - left.map { it.first }.average() > 100)
        val center = darkPixels(decode(LocalGuideRenderer.render(spec())))
        val lower = darkPixels(decode(LocalGuideRenderer.render(spec(assembly.copy(figurePlacement = VisualPlacement.LOWER.wording)))))
        assertTrue(lower.map { it.second }.average() - center.map { it.second }.average() > 60)
        val close = darkPixels(decode(LocalGuideRenderer.render(spec(assembly.copy(framing = Framing.BUST_UP)))))
        assertTrue(close.size > center.size * 2)
    }

    @Test fun poseFacingAndPropAffectOutputAndProduceReviewableGuides() = runBlocking {
        val directory = File("build/reports/visual-guides").apply { mkdirs() }
        val base = decode(LocalGuideRenderer.render(spec()))
        val variants = mapOf(
            "relaxed" to assembly,
            "side-shoulder" to assembly.copy(facing = GuideFacing.SIDE, propPlacement = GuideProp.SHOULDER),
            "action-down" to assembly.copy(basePose = VisualPose.ACTION.base, propPlacement = GuideProp.DOWN),
            "seated-lower" to assembly.copy(basePose = VisualPose.SITTING.base, figurePlacement = VisualPlacement.LOWER.wording),
            "close" to assembly.copy(framing = Framing.BUST_UP),
        )
        for ((name, state) in variants) {
            val guide = LocalGuideRenderer.render(spec(state))
            if (state != assembly) assertFalse(pixels(base).contentEquals(pixels(decode(guide))))
            File(directory, "$name.png").writeBytes(guide.bytes())
        }
    }
}
