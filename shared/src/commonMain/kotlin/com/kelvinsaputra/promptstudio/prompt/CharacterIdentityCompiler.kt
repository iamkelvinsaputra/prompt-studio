package com.kelvinsaputra.promptstudio.prompt

import com.kelvinsaputra.promptstudio.domain.*

/** Canonical order and wording. Scene, expression, costume and style never affect this result. */
fun compileCharacterIdentity(p: CharacterProject): String = with(p) {
    val traits = listOf(
        "body type" to identity.bodyType?.wording.orEmpty(), "build" to body.build?.wording.orEmpty(),
        "age detail" to identity.ageRange, "body age read" to body.ageRead,
        "proportions" to body.proportions, "height" to body.heightImpression?.wording.orEmpty(),
        "physical character" to body.athleticLanguage?.wording.orEmpty(),
        "appearance" to profile.broadAppearance, "skin tone" to profile.skinTone,
        "face shape" to face.facialStructure, "jaw" to profile.jawShape,
        "eyes" to face.eyeShape, "eye color" to profile.eyeColor, "eyebrows" to face.eyebrowBehavior,
        "nose" to profile.nose, "mouth" to face.mouthOrSmile, "grooming" to face.makeupOrGrooming,
        "facial maturity" to face.maturityNote, "hair color" to hair.baseColor, "hair roots" to hair.roots,
        "hair length" to hair.length?.wording.orEmpty(), "hair style" to hair.style?.wording.orEmpty(),
        "hair texture" to profile.hairTexture, "hair accents" to hair.accentTreatment,
        "distinguishing features" to profile.distinguishingFeatures,
        "visual attitude" to identity.coreVibe,
        "identity notes" to identity.additionalInstructions, "body notes" to body.additionalInstructions,
        "face notes" to face.additionalInstructions, "hair notes" to hair.additionalInstructions,
    ).filter { it.second.isNotBlank() }.joinToString("\n") { "- ${it.first}: ${it.second.trim()}" }
    val presentation = listOfNotNull(identity.ageBand?.wording, identity.genderPresentation?.wording).joinToString(" ")
    listOf("$characterName is an original ${presentation.takeIf { it.isNotBlank() }?.plus(" ").orEmpty()}character.", traits,
        subject.trim().takeUnless { it == CharacterProject().subject && identity.ageBand != null }.orEmpty()).filter { it.isNotBlank() }.joinToString("\n")
}
fun compileColorDirection(value: ColorDirection): String = buildList {
    if (value.description.isNotBlank()) add(value.description.trim())
    value.colors.sortedBy { it.role.ordinal }.forEach { add("${it.role.label}: ${normalizeHex(it.hex)}") }
    listOf("Saturation" to value.saturation, "Contrast" to value.contrast, "Temperature (cool to warm)" to value.temperature, "Brightness" to value.brightness)
        .forEach { (label, amount) -> if (amount != null) add("$label: $amount/100") }
}.joinToString("\n")
fun compileEffects(value: EffectsConfiguration): String = buildList {
    value.items.sortedWith(compareBy({ it.family.ordinal }, { it.name })).distinctBy { it.name }.forEach { add("${value.intensity.label} ${it.name.lowercase()}") }
    if (value.items.isNotEmpty() && value.direction.isNotBlank()) add("Direction: ${value.direction.trim()}")
    if (value.customDescription.isNotBlank()) add(value.customDescription.trim())
}.joinToString("\n")
