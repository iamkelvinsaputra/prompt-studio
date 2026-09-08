package com.kelvinsaputra.promptstudio.feature.editor

import com.kelvinsaputra.promptstudio.domain.*
import kotlin.random.Random

// These identify randomizable editor fields, not prompt vocabulary or project data.
enum class CostumeField { Silhouette, Outerwear, Innerwear, LowerWear, Legwear, Footwear, Handwear, Utility, Customization }
enum class PoseField { BasePose, Weight, Torso, Arms, Head, Gaze, Energy }

/** Plain Kotlin session logic. Inject Random(seed) for repeatable exploration. */
class EditorRandomizer(private val random: Random = Random.Default) {
    fun costume(value: CostumeConfiguration, locks: Set<CostumeField>): CostumeConfiguration = value.copy(
        silhouette = if (CostumeField.Silhouette in locks) value.silhouette else choose(value.silhouette, Silhouette.entries),
        outerwear = if (CostumeField.Outerwear in locks) value.outerwear else choose(value.outerwear, Outerwear.entries),
        innerwear = if (CostumeField.Innerwear in locks) value.innerwear else choose(value.innerwear, Innerwear.entries),
        lowerWear = if (CostumeField.LowerWear in locks) value.lowerWear else choose(value.lowerWear, LowerWear.entries),
        legwear = if (CostumeField.Legwear in locks) value.legwear else choose(value.legwear, Legwear.entries),
        footwear = if (CostumeField.Footwear in locks) value.footwear else choose(value.footwear, Footwear.entries),
        handwear = if (CostumeField.Handwear in locks) value.handwear else choose(value.handwear, Handwear.entries),
        utility = if (CostumeField.Utility in locks) value.utility else choose(value.utility, Utility.entries),
        customization = if (CostumeField.Customization in locks) value.customization else customization(value.customization),
    )

    fun pose(value: PoseConfiguration, locks: Set<PoseField>): PoseConfiguration {
        val base = if (PoseField.BasePose in locks) value.basePose else choose(value.basePose, BasePose.entries)
        return value.copy(
            basePose = base,
            weight = if (PoseField.Weight in locks) value.weight else choose(value.weight, weightsFor(base)),
            torso = if (PoseField.Torso in locks) value.torso else choose(value.torso, Torso.entries),
            arms = if (PoseField.Arms in locks) value.arms else choose(value.arms, Arms.entries),
            head = if (PoseField.Head in locks) value.head else choose(value.head, Head.entries),
            gaze = if (PoseField.Gaze in locks) value.gaze else choose(value.gaze, Gaze.entries),
            energy = if (PoseField.Energy in locks) value.energy else choose(value.energy, Energy.entries),
        )
    }

    // Avoid obvious seated/standing contradictions without introducing a pose engine.
    // Explicit locks and authored text always take priority over these suggestions.
    private fun weightsFor(base: BasePose?): List<Weight> = when (base) {
        BasePose.SITTING_ON_LEDGE, BasePose.SLOUCHED_SEAT -> listOf(Weight.SEATED_WEIGHT)
        BasePose.RELAXED_STANDING, BasePose.ASYMMETRICAL_STANDING, BasePose.CONTRAPPOSTO,
        BasePose.LEANING, BasePose.READY_STANCE -> listOf(Weight.ON_LEFT_LEG, Weight.ON_RIGHT_LEG, Weight.EVENLY_DISTRIBUTED)
        else -> Weight.entries.filter { it != Weight.SEATED_WEIGHT }
    }

    private fun <T> choose(current: T?, options: List<T>): T {
        val alternatives = options.filter { it != current }
        return (alternatives.ifEmpty { options }).random(random)
    }

    private fun customization(current: Set<Customization>): Set<Customization> {
        // All valid sets of zero, one, or two markers; excluding the current set guarantees variation.
        val options = buildList {
            add(emptySet())
            Customization.entries.forEachIndexed { index, first ->
                add(setOf(first))
                Customization.entries.drop(index + 1).forEach { second -> add(setOf(first, second)) }
            }
        }
        return choose(current, options)
    }
}
