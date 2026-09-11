package com.kelvinsaputra.promptstudio.feature.editor

import com.kelvinsaputra.promptstudio.domain.*
import kotlin.random.Random

// These identify randomizable editor fields, not prompt vocabulary or project data.
enum class CostumeField { Silhouette, Outerwear, Innerwear, LowerWear, Legwear, Footwear, Handwear, Utility, Customization }
enum class PoseField { BasePose, Weight, Torso, Arms, Head, Gaze, Energy }
/** Module-level variation locks. Authored strings are never randomized. */
enum class VariationField { Identity, Body, Face, Expression, Hair, Costume, Power, Pose, Gaze, Composition, Environment, Lighting, AccentColor, SurfaceTexture, Output }

/** Plain Kotlin session logic. Inject Random(seed) for repeatable exploration. */
class EditorRandomizer(private val random: Random = Random.Default) {
    /**
     * Creates a coherent variation using only finite, in-domain vocabulary. It intentionally
     * leaves all free-form writing, list ordering, and unmodelled modules untouched.
     */
    fun variation(
        project: CharacterProject,
        locks: Set<VariationField>,
        costumeLocks: Set<CostumeField> = emptySet(),
        poseLocks: Set<PoseField> = emptySet(),
    ): CharacterProject = project.copy(
        identity = if (VariationField.Identity in locks) project.identity else project.identity.copy(
            ageBand = choose(project.identity.ageBand, AgeBand.entries),
            genderPresentation = choose(project.identity.genderPresentation, characterGenders),
            bodyType = choose(project.identity.bodyType, BodyType.entries),
        ),
        body = if (VariationField.Body in locks) project.body else project.body.copy(
            build = choose(project.body.build, Build.entries),
            heightImpression = choose(project.body.heightImpression, HeightImpression.entries),
            athleticLanguage = choose(project.body.athleticLanguage, AthleticLanguage.entries),
        ),
        expression = if (VariationField.Expression in locks) project.expression else project.expression.copy(
            preset = choose(project.expression.preset, ExpressionPreset.entries),
        ),
        hair = if (VariationField.Hair in locks) project.hair else project.hair.copy(
            length = choose(project.hair.length, HairLength.entries),
            style = choose(project.hair.style, HairStyle.entries),
        ),
        costume = if (VariationField.Costume in locks) project.costume else costume(project.costume, costumeLocks),
        pose = if (VariationField.Pose in locks) project.pose else pose(project.pose, poseLocks),
        gazeDirection = if (VariationField.Gaze in locks) project.gazeDirection else project.gazeDirection.copy(
            headDirection = choose(project.gazeDirection.headDirection, Head.entries),
            gazeTarget = choose(project.gazeDirection.gazeTarget, Gaze.entries),
            intensity = choose(project.gazeDirection.intensity, GazeIntensity.entries),
        ),
        output = if (VariationField.Output in locks) project.output else project.output.copy(
            // A random custom output could require an authored ratio, so exploration uses the
            // complete preset set and never makes Copy Prompt invalid.
            type = choose(project.output.type, OutputType.entries.filter { it != OutputType.CUSTOM }),
            framing = if (VariationField.Composition in locks) project.output.framing else choose(project.output.framing, Framing.entries),
        ),
        environment = if (VariationField.Environment in locks) project.environment else project.environment.copy(
            abstractionLevel = choose(project.environment.abstractionLevel, EnvironmentAbstraction.entries),
        ),
        lighting = if (VariationField.Lighting in locks) project.lighting else project.lighting.copy(
            sourceQuality = choose(project.lighting.sourceQuality, LightingSource.entries),
            shadowSoftness = choose(project.lighting.shadowSoftness, ShadowSoftness.entries),
        ),
        colorAccents = if (VariationField.AccentColor in locks) project.colorAccents else project.colorAccents.copy(
            accentColor = choose(project.colorAccents.accentColor, AccentColor.entries),
        ),
        surfaceTexture = if (VariationField.SurfaceTexture in locks) project.surfaceTexture else project.surfaceTexture.copy(
            paperGrain = choose(project.surfaceTexture.paperGrain, TextureLevel.entries),
            watercolorBehavior = choose(project.surfaceTexture.watercolorBehavior, WatercolorBehavior.entries),
            inkTextureBehavior = choose(project.surfaceTexture.inkTextureBehavior, InkTextureBehavior.entries),
        ),
    ).preservingIdentityOf(project)

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
