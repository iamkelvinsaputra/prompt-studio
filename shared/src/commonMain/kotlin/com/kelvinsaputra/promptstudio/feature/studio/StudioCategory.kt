package com.kelvinsaputra.promptstudio.feature.studio

import com.kelvinsaputra.promptstudio.feature.editor.EditorModule
import com.kelvinsaputra.promptstudio.domain.*

/** Artist-facing navigation; the existing typed domain remains authoritative. */
enum class StudioCategory(val title: String, val description: String, val modules: List<EditorModule>, val sections: Set<String>) {
    Subject("Subject", "Find the person behind the image.", listOf(EditorModule.Identity, EditorModule.Expression, EditorModule.Hair, EditorModule.Face, EditorModule.Body, EditorModule.Role, EditorModule.Personality, EditorModule.Contradiction, EditorModule.VisualThesis), setOf("SUBJECT", "BODY", "FACE", "HAIR", "EXPRESSION", "ROLE", "PERSONALITY READ", "INNER CONTRADICTION", "CORE VISUAL THESIS")),
    Appearance("Appearance", "Build an outfit with a distinct silhouette.", listOf(EditorModule.Costume, EditorModule.Accessories, EditorModule.ShapeLanguage, EditorModule.Prop), setOf("COSTUME", "ACCESSORIES", "SHAPE LANGUAGE", "PROP / WEAPON / TOOL")),
    Pose("Pose & acting", "Find the gesture. Give it intention.", listOf(EditorModule.Pose, EditorModule.Gaze), setOf("POSE", "GAZE / HEAD DIRECTION")),
    Camera("Camera & composition", "Choose what we see, and how it fills the frame.", listOf(EditorModule.Composition), setOf("COMPOSITION")),
    Environment("Environment", "Give your character a place in the world.", listOf(EditorModule.Environment), setOf("ENVIRONMENT / BACKGROUND")),
    Lighting("Lighting", "Shape the scene with light and shadow.", listOf(EditorModule.Lighting), setOf("LIGHTING")),
    Style("Style", "Explore a visual language. Every look is replaceable.", listOf(EditorModule.Style, EditorModule.SurfaceTexture), setOf("ART STYLE CORE", "SURFACE / TEXTURE")),
    Color("Color", "Set the palette and choose where color leads.", listOf(EditorModule.AccentColor), setOf("ACCENT COLOR POLICY")),
    Effects("Effects & atmosphere", "Add movement, atmosphere, or a signature effect.", listOf(EditorModule.Power), setOf("SUPERNATURAL SIGNATURE")),
    Output("Output", "Prepare the image for its final destination.", listOf(EditorModule.Output, EditorModule.PriorityStack), setOf("OUTPUT INTENT", "PRIORITY STACK", "DESCRIBE ADJUSTMENT")),
    Negatives("Negative guidance", "Make clear what should stay out of the image.", listOf(EditorModule.Avoid), setOf("AVOID"));

    companion object { fun from(module: EditorModule) = entries.firstOrNull { module in it.modules } ?: Subject }
}

fun StudioCategory.summary(p: CharacterProject): String = when (this) {
    StudioCategory.Subject -> listOfNotNull(p.identity.ageBand?.wording, p.identity.genderPresentation?.wording).joinToString(" · ").ifBlank { p.subject.take(48).ifBlank { "Your character" } }
    StudioCategory.Appearance -> p.costume.outfitIdentity.ifBlank { p.costume.silhouette?.wording ?: "Choose an outfit" }
    StudioCategory.Pose -> p.visualAssembly.posePreset?.label ?: p.pose.basePose?.wording ?: "Choose a gesture"
    StudioCategory.Camera -> listOfNotNull(p.output.framing?.guideLabel(), p.composition.guidePreset?.label).joinToString(" · ").ifBlank { "Frame your subject" }
    StudioCategory.Environment -> p.environment.worldContextHint.ifBlank { p.environment.abstractionLevel?.wording ?: "Add a setting" }
    StudioCategory.Lighting -> p.lighting.sourceQuality?.wording ?: p.lighting.direction.ifBlank { "Shape the light" }
    StudioCategory.Style -> p.artStyle.preset?.label ?: if (p.artStyle.manualMode) "Custom style" else p.style.name
    StudioCategory.Color -> p.colorAccents.accentColor?.wording ?: "Choose a palette"
    StudioCategory.Effects -> p.powerSignature.manifestation.ifBlank { "Optional" }
    StudioCategory.Output -> "${p.output.type.label} · ${p.output.aspectRatio.orEmpty()}"
    StudioCategory.Negatives -> "${p.exclusions.count { it.isNotBlank() }} exclusions"
}
