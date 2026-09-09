package com.kelvinsaputra.promptstudio.prompt

import com.kelvinsaputra.promptstudio.domain.*

data class CompiledPrompt(val text: String)

/** Pure, deterministic rendering of the modular cheatsheet into readable prompt text. */
class PromptCompiler {
    fun compile(project: CharacterProject): CompiledPrompt = with(project) {
        CompiledPrompt(listOfNotNull(
            section("ART STYLE CORE", style.prompt.trim()),
            section("OUTPUT INTENT", outputIntent(output)),
            section("SUBJECT", subjectBlock(identity, subject)),
            section("ROLE", roleBlock(role)),
            section("CORE VISUAL THESIS", sentenceContent(coreVisualThesis)),
            section("PERSONALITY READ", personalityBlock(personality)),
            section("INNER CONTRADICTION", contradictionBlock(contradiction)),
            section("BODY", bodyBlock(body)),
            section("FACE", faceBlock(face)),
            section("EXPRESSION", expressionBlock(expression)),
            section("HAIR", hairBlock(hair)),
            section("COSTUME", costumeBlock(costume)),
            section("ACCESSORIES", simpleListBlock(accessories.items, accessories.additionalInstructions)),
            section("SHAPE LANGUAGE", shapeLanguageBlock(shapeLanguage)),
            section("SUPERNATURAL SIGNATURE", powerBlock(powerSignature)),
            section("PROP / WEAPON / TOOL", propBlock(prop)),
            section("POSE", poseBlock(pose, gazeDirection.hasContent())),
            section("GAZE / HEAD DIRECTION", gazeBlock(gazeDirection)),
            section("COMPOSITION", composition(output, composition)),
            section("ENVIRONMENT / BACKGROUND", environmentBlock(environment)),
            section("LIGHTING", lightingBlock(lighting)),
            section("ACCENT COLOR POLICY", colorAccentBlock(colorAccents)),
            section("SURFACE / TEXTURE", surfaceBlock(surfaceTexture)),
            section("PRIORITY STACK", priorityBlock(priorityStack)),
            section("AVOID", listBullets(exclusions)),
        ).joinToString("\n\n"))
    }

    private fun section(title: String, body: String): String? =
        body.takeIf { it.isNotBlank() }?.let { "$title\n\n$it" }

    private fun subsection(title: String, body: String): String? =
        body.takeIf { it.isNotBlank() }?.let { "$title\n$it" }

    private fun clean(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }

    private fun bullet(label: String, value: String?): String? = clean(value)?.let { "- $label: $it" }
    private fun optionBullet(label: String, value: PromptOption?): String? = bullet(label, value?.wording)
    private fun listBullets(values: List<String>): String = values.mapNotNull(::clean).joinToString("\n") { "- $it" }
    private fun additional(value: String): String? = subsection("Additional instructions:", clean(value).orEmpty())

    // Normalize only authored sentence endings; preserve punctuation inside the text.
    private fun sentenceContent(value: String): String = value.trim().trimEnd { it.isWhitespace() || it in ".!?" }
    private fun sentence(label: String, value: String): String? =
        sentenceContent(value).takeIf { it.isNotEmpty() }?.let { "$label: $it." }

    private fun IdentityConfiguration.hasContent(): Boolean =
        ageBand != null || ageRange.isNotBlank() || genderPresentation != null || bodyType != null ||
            coreVibe.isNotBlank() || additionalInstructions.isNotBlank()
    private fun GazeConfiguration.hasContent(): Boolean =
        headDirection != null || gazeTarget != null || intensity != null || additionalInstructions.isNotBlank()

    private fun subjectBlock(identity: IdentityConfiguration, legacySubject: String): String {
        if (!identity.hasContent()) return legacySubject.trim()
        val descriptor = listOfNotNull(identity.ageBand?.wording, identity.genderPresentation?.wording)
            .joinToString(" ").let { if (it.isBlank()) "An original character" else "An original $it character" }
        val details = listOfNotNull(
            clean(identity.ageRange)?.let { "in their $it" },
            identity.bodyType?.wording?.let { "with a $it body type" },
            clean(identity.coreVibe)?.let { "and a $it presence" },
        )
        val main = if (details.isEmpty()) "$descriptor." else "$descriptor ${details.joinToString(", ")}."
        // A migrated V0 demo sentence must not echo beside the structured identity. Any other
        // authored subject text remains an intentional escape hatch for custom identity detail.
        val legacyNote = clean(legacySubject)?.takeUnless { it == CharacterProject().subject }
        return listOfNotNull(main, subsection("Additional subject notes:", legacyNote.orEmpty()), additional(identity.additionalInstructions)).joinToString("\n\n")
    }

    private fun roleBlock(role: RoleConfiguration): String = listOfNotNull(
        sentence("Role", role.role), sentence("Organization / faction / context", role.organizationOrContext),
        sentence("Behavioral archetype", role.behavioralArchetype), additional(role.additionalInstructions),
    ).joinToString("\n")

    private fun personalityBlock(value: PersonalityConfiguration): String = listOfNotNull(
        listBullets(value.traits).takeIf { it.isNotBlank() }?.let { "At first glance the character should feel:\n$it" },
        additional(value.additionalInstructions),
    ).joinToString("\n\n")

    private fun contradictionBlock(value: ContradictionConfiguration): String = listOfNotNull(
        if (clean(value.outwardQuality) != null && clean(value.inwardQuality) != null)
            "Underlying contradiction: outwardly ${clean(value.outwardQuality)}, but inwardly ${clean(value.inwardQuality)}." else null,
        sentence("Secondary nuance", value.secondaryNuance), additional(value.additionalInstructions),
    ).joinToString("\n")

    private fun bodyBlock(value: BodyConfiguration): String = listOfNotNull(
        sentence("Age read", value.ageRead), optionBullet("build", value.build), bullet("proportions", value.proportions),
        optionBullet("height impression", value.heightImpression), optionBullet("athletic language", value.athleticLanguage),
        additional(value.additionalInstructions),
    ).joinToString("\n")

    private fun faceBlock(value: FaceConfiguration): String = listOfNotNull(
        bullet("eye shape", value.eyeShape), bullet("eyebrow behavior", value.eyebrowBehavior),
        bullet("mouth / smile character", value.mouthOrSmile), bullet("makeup / grooming", value.makeupOrGrooming),
        bullet("facial structure", value.facialStructure), bullet("maturity note", value.maturityNote),
        additional(value.additionalInstructions),
    ).joinToString("\n")

    private fun expressionBlock(value: ExpressionConfiguration): String = listOfNotNull(
        optionBullet("expression preset", value.preset),
        sentence("Default expression", value.defaultExpression), sentence("Subtext", value.emotionalSubtext),
        sentence("Alternate emotional range", value.alternateRange), additional(value.additionalInstructions),
    ).joinToString("\n")

    private fun hairBlock(value: HairConfiguration): String = listOfNotNull(
        bullet("base color", value.baseColor), bullet("roots", value.roots), optionBullet("length", value.length),
        optionBullet("style", value.style), bullet("behavior", value.movementBehavior),
        bullet("accent treatment", value.accentTreatment), additional(value.additionalInstructions),
    ).joinToString("\n")

    private fun costumeBlock(c: CostumeConfiguration): String {
        val layers = listOfNotNull(
            optionBullet("silhouette", c.silhouette), optionBullet("upper layer", c.outerwear),
            optionBullet("inner layer", c.innerwear), optionBullet("lower layer", c.lowerWear),
            optionBullet("legwear", c.legwear), optionBullet("footwear", c.footwear),
            optionBullet("handwear", c.handwear), optionBullet("utility/support items", c.utility),
            bullet("personal styling", Customization.entries.filter { it in c.customization }.joinToString(", ") { it.wording }),
        ).joinToString("\n")
        val configured = c.outfitIdentity.isNotBlank() || layers.isNotBlank() || c.materialFeel.isNotBlank() ||
            c.exposureLevel.isNotBlank() || c.customNotes.isNotBlank()
        return listOfNotNull(
            sentence("Costume concept", c.outfitIdentity), subsection("Base outfit:", layers),
            sentence("Material feel", c.materialFeel), sentence("Exposure level", c.exposureLevel),
            "Costume requirements:\n- believable seams\n- practical closures\n- realistic fabric thickness\n- readable material differences\n- understandable layering\n- plausible construction".takeIf { configured },
            subsection("Additional costume notes:", c.customNotes.trim()),
        ).joinToString("\n\n")
    }

    private fun simpleListBlock(items: List<String>, notes: String): String = listOfNotNull(
        listBullets(items).takeIf { it.isNotBlank() }, additional(notes)
    ).joinToString("\n\n")

    private fun shapeLanguageBlock(value: ShapeLanguageConfiguration): String = listOfNotNull(
        listBullets(value.principles).takeIf { it.isNotBlank() }?.let { "Use shape language based on:\n$it" },
        additional(value.additionalInstructions),
    ).joinToString("\n\n")

    private fun powerBlock(value: PowerSignatureConfiguration): String = listOfNotNull(
        sentence("Represents", value.symbolicMeaning), bullet("manifests as", value.manifestation),
        bullet("shape logic", value.shapeLogic), bullet("motion quality", value.motionQuality),
        bullet("color / material behavior", value.colorMaterialBehavior), sentence("Restraint note", value.restraint),
        additional(value.additionalInstructions),
    ).joinToString("\n")

    private fun propBlock(value: PropConfiguration): String = listOfNotNull(
        sentence("Primary prop", value.primaryProp), sentence("Primary function", value.primaryFunction),
        sentence("Primary style logic", value.primaryStyleLogic), sentence("Secondary prop", value.secondaryProp),
        additional(value.additionalInstructions),
    ).joinToString("\n")

    private fun poseBlock(p: PoseConfiguration, hasDedicatedGaze: Boolean): String = listOfNotNull(
        optionBullet("base pose", p.basePose), optionBullet("weight distribution", p.weight), bullet("leg arrangement", p.legAction),
        optionBullet("torso action", p.torso), optionBullet("arm action", p.arms),
        optionBullet("head angle", p.head.takeUnless { hasDedicatedGaze }), optionBullet("gaze", p.gaze.takeUnless { hasDedicatedGaze }),
        optionBullet("overall energy", p.energy), bullet("motion direction", p.motionDirection),
        subsection("Additional pose notes:", p.customNotes.trim()),
    ).joinToString("\n")

    private fun gazeBlock(value: GazeConfiguration): String = listOfNotNull(
        optionBullet("head direction", value.headDirection), optionBullet("gaze", value.gazeTarget),
        optionBullet("intensity", value.intensity), additional(value.additionalInstructions),
    ).joinToString("\n")

    private fun composition(output: OutputConfiguration, value: CompositionConfiguration): String = listOfNotNull(
        optionBullet("framing", output.framing), bullet("figure placement", output.figurePlacement),
        bullet("directional flow", value.directionalFlow), bullet("negative space", output.negativeSpace),
        bullet("detail concentration", value.detailConcentration), bullet("readability priority", value.readabilityPriority),
        bullet("safe area", output.safeArea),
        bullet("clock-safe area", "preserve clean space for the clock".takeIf { output.type.isWallpaper && output.clockSafe }),
        bullet("icon-safe area", "preserve clean space for icons".takeIf { output.type.isWallpaper && output.iconSafe }),
        additional(value.additionalInstructions),
    ).joinToString("\n")

    private fun environmentBlock(value: EnvironmentConfiguration): String = listOfNotNull(
        sentence("World / context hint", value.worldContextHint), optionBullet("abstraction level", value.abstractionLevel),
        bullet("depth behavior", value.depthBehavior), bullet("motif / support element", value.motifOrSupportElement),
        additional(value.additionalInstructions),
    ).joinToString("\n")

    private fun lightingBlock(value: LightingConfiguration): String = listOfNotNull(
        optionBullet("source / quality", value.sourceQuality), bullet("direction", value.direction),
        optionBullet("shadow softness", value.shadowSoftness), bullet("mood effect", value.moodEffect),
        bullet("lighting exclusions", value.exclusions), additional(value.additionalInstructions),
    ).joinToString("\n")

    private fun colorAccentBlock(value: ColorAccentConfiguration): String = listOfNotNull(
        optionBullet("accent color", value.accentColor), bullet("allowed surfaces", value.allowedSurfaces),
        bullet("saturation ceiling", value.saturationCeiling), bullet("forbidden surfaces", value.forbiddenSurfaces),
        bullet("emotional role", value.emotionalRole), additional(value.additionalInstructions),
    ).joinToString("\n")

    private fun surfaceBlock(value: SurfaceTextureConfiguration): String = listOfNotNull(
        optionBullet("paper grain", value.paperGrain), optionBullet("watercolor behavior", value.watercolorBehavior),
        optionBullet("ink texture behavior", value.inkTextureBehavior), additional(value.additionalInstructions),
    ).joinToString("\n")

    private fun priorityBlock(values: List<String>): String = values.mapNotNull(::clean)
        .mapIndexed { index, value -> "${index + 1}. $value" }.joinToString("\n")

    private fun outputIntent(o: OutputConfiguration): String {
        val intent = if (o.type == OutputType.CUSTOM) sentenceContent(o.customIntent).ifEmpty { "illustration" } else o.type.intent
        val ratio = o.aspectRatio?.let { " in $it" }.orEmpty()
        val article = if (intent.first().lowercaseChar() in "aeiou") "an" else "a"
        return "Create $article $intent$ratio."
    }
}
