package com.kelvinsaputra.promptstudio.guide

import com.kelvinsaputra.promptstudio.domain.*
import kotlinx.serialization.Serializable

/** Artwork metadata may change without changing option identity or prompt vocabulary. */
@Serializable
data class VisualGuideAsset(
    val image: String? = null,
    val thumbnail: String? = null,
    val label: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
    val order: Int = 0,
)

data class VisualGuideOption<T>(val id: String, val category: String, val value: T, val label: String, val promptValue: String) {
    val key get() = "$category/$id"
    fun matches(query: String, asset: VisualGuideAsset?): Boolean = query.trim().split(Regex("\\s+")).all { word ->
        listOf(label, promptValue, asset?.label.orEmpty(), asset?.description.orEmpty())
            .plus(asset?.tags.orEmpty()).plus(asset?.aliases.orEmpty()).any { it.contains(word, ignoreCase = true) }
    }
}

/** Options are derived from the same typed values the compiler consumes, never copied into JSON. */
object VisualGuideRegistry {
    private fun <T : Enum<T>> options(category: String, values: List<T>, label: (T) -> String, prompt: (T) -> String = label) =
        values.map { VisualGuideOption(it.name.lowercase().replace('_', '-'), category, it, label(it), prompt(it)) }
    val poses = options("pose", BasePose.entries, { value -> VisualPose.entries.firstOrNull { it.base == value }?.label ?: value.wording.replaceFirstChar { it.uppercase() } }, { it.wording })
    val camera = options("camera-angle", CameraAngle.entries, { it.label }, { it.wording })
    val facing = options("body-orientation", GuideFacing.entries, { it.label }, { it.wording })
    val framing = options("framing", Framing.entries, { it.guideLabel() }, { it.wording })
    val placement = options("placement", VisualPlacement.entries, { it.label }, { it.wording })
    val composition = options("composition", GuideComposition.entries, { it.label }, { it.wording })
    val gaze = options("gaze", Gaze.entries, { it.guideLabel() }, { it.wording })
    val head = options("head-direction", Head.entries, { it.wording.replaceFirstChar { it.uppercase() } }, { it.wording })
    val arms = options("arm-position", Arms.entries, { it.wording.replaceFirstChar { it.uppercase() } }, { it.wording })
    val prop = options("prop-placement", GuideProp.entries, { it.label }, { it.wording })
    val lighting = options("lighting-direction", LightDirection.entries, { it.label }, { it.wording })
    val environment = options("environment", EnvironmentCategory.entries, { it.label })
    val all get() = poses + camera + facing + framing + placement + composition + gaze + head + arms + prop + lighting + environment
}
