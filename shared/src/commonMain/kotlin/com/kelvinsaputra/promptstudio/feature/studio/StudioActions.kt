package com.kelvinsaputra.promptstudio.feature.studio

import com.kelvinsaputra.promptstudio.domain.*
import kotlin.random.Random

fun CharacterProject.resetStudioCategory(category: StudioCategory): CharacterProject {
    val neutral = CharacterLibrary.newCharacter(id, name).withStudioDefaults()
    return when (category) {
        StudioCategory.Subject -> copy(subject = neutral.subject, identity = neutral.identity, expression = neutral.expression, hair = neutral.hair, face = neutral.face, body = neutral.body, role = neutral.role, personality = neutral.personality, contradiction = neutral.contradiction, coreVisualThesis = "")
        StudioCategory.Appearance -> copy(costume = neutral.costume, accessories = neutral.accessories, shapeLanguage = neutral.shapeLanguage, prop = neutral.prop)
        StudioCategory.Pose -> copy(pose = neutral.pose, gazeDirection = neutral.gazeDirection)
        StudioCategory.Camera -> copy(composition = neutral.composition, output = output.copy(framing = neutral.output.framing, figurePlacement = neutral.output.figurePlacement, negativeSpace = "", safeArea = ""))
        StudioCategory.Environment -> copy(environment = neutral.environment)
        StudioCategory.Lighting -> copy(lighting = neutral.lighting)
        StudioCategory.Style -> copy(style = neutral.style, artStyle = neutral.artStyle, surfaceTexture = neutral.surfaceTexture)
        StudioCategory.Color -> copy(colorAccents = neutral.colorAccents)
        StudioCategory.Effects -> copy(powerSignature = neutral.powerSignature)
        StudioCategory.Output -> copy(output = neutral.output.copy(framing = output.framing, figurePlacement = output.figurePlacement, negativeSpace = output.negativeSpace), priorityStack = emptyList(), promptAuthoring = PromptAuthoring())
        StudioCategory.Negatives -> copy(exclusions = emptyList())
    }
}

fun CharacterProject.randomizeStudioCategory(category: StudioCategory, random: Random = Random.Default): CharacterProject = when (category) {
    StudioCategory.Camera -> copy(output = output.copy(framing = Framing.entries.filterNot { it == output.framing }.random(random),
        figurePlacement = VisualPlacement.entries.filterNot { it.wording == output.figurePlacement }.random(random).wording),
        composition = composition.copy(guidePreset = GuideComposition.entries.filterNot { it == composition.guidePreset }.random(random)))
    StudioCategory.Color -> copy(colorAccents = colorAccents.copy(accentColor = AccentColor.entries.filterNot { it == colorAccents.accentColor }.random(random)))
    else -> this
}
