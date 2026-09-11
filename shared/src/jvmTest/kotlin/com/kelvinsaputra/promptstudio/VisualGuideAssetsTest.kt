package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.guide.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.*

class VisualGuideAssetsTest {
    /** Explicit utility: ./gradlew :shared:jvmTest --tests '*VisualGuideAssetsTest.exportTemporaryArtwork' */
    @Test fun exportTemporaryArtwork() = runBlocking {
        val directory = File("build/reports/studio-artwork").apply { mkdirs() }
        val base = CharacterLibrary.newCharacter("asset").withStudioDefaults().visualAssembly
        val assets = linkedMapOf<String, VisualGuideAsset>()
        VisualGuideRegistry.all.forEachIndexed { index, option ->
            val state = when (val value = option.value) {
                is BasePose -> if (VisualPose.entries.any { it.base == value }) base.copy(basePose = value) else null
                is GuideFacing -> base.copy(facing = value)
                is Framing -> base.copy(framing = value)
                is VisualPlacement -> base.copy(figurePlacement = value.wording)
                is GuideComposition -> base.copy(composition = value)
                is Gaze -> base.copy(gaze = value, framing = Framing.CLOSE_UP)
                is GuideProp -> base.copy(propPlacement = value)
                else -> null
            }
            val image = state?.let { LocalGuideRenderer.render(GuideRenderSpec(it, 320, 400, version = 2)) }
            if (image != null) File(directory, "${option.key}.png").apply { parentFile.mkdirs(); writeBytes(image.bytes()) }
            val preferred = if (option.value is BasePose) VisualPose.entries.indexOfFirst { it.base == option.value }.takeIf { it >= 0 } ?: 100 + index else index
            assets[option.key] = VisualGuideAsset(image = image?.let { "${option.key}.png" }, description = option.promptValue,
                tags = listOf(option.category), aliases = if (option.value == BasePose.CONTRAPPOSTO) listOf("relaxed", "casual", "standing", "weight shift") else emptyList(), order = preferred)
        }
        File(directory, "registry.json").writeText(Json { prettyPrint = true }.encodeToString(assets))
        assertTrue(assets.values.count { it.image != null } >= 30)
    }
}
