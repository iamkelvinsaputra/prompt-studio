package com.kelvinsaputra.promptstudio.prompt

import com.kelvinsaputra.promptstudio.domain.*

data class CompiledPrompt(val text: String)

class PromptCompiler {
    fun compile(project: CharacterProject): CompiledPrompt = with(project) {
        CompiledPrompt(listOfNotNull(
            section("ART STYLE CORE", style.prompt.trim()),
            section("OUTPUT INTENT", outputIntent(output)),
            section("SUBJECT", subject.trim()),
            section("COSTUME", costumeBlock(costume)),
            section("POSE", poseBlock(pose)),
            section("COMPOSITION", composition(output)),
            section("PRIORITY STACK", "1. subject presence\n2. silhouette\n3. costume design\n4. composition"),
        ).joinToString("\n\n"))
    }

    private fun section(title: String, body: String): String? =
        body.takeIf { it.isNotBlank() }?.let { "$title\n\n$it" }

    private fun bullet(label: String, value: String?): String? =
        value?.trim()?.takeIf { it.isNotEmpty() }?.let { "- $label: $it" }

    private fun sentence(label: String, value: String): String? =
        value.trim().takeIf { it.isNotEmpty() }?.let { "$label: ${it.trimEnd('.', '!', '?')}." }

    private fun costumeBlock(c: CostumeConfiguration): String {
        val layers = listOfNotNull(
            bullet("silhouette", c.silhouette?.wording),
            bullet("upper layer", c.outerwear?.wording),
            bullet("inner layer", c.innerwear?.wording),
            bullet("lower layer", c.lowerWear?.wording),
            bullet("legwear", c.legwear?.wording),
            bullet("footwear", c.footwear?.wording),
            bullet("handwear", c.handwear?.wording),
            bullet("utility/support items", c.utility?.wording),
            bullet("personal styling", Customization.entries.filter { it in c.customization }.joinToString(", ") { it.wording }),
        ).joinToString("\n")
        return listOfNotNull(
            sentence("Costume concept", c.outfitIdentity),
            section("Base outfit:", layers),
            sentence("Material feel", c.materialFeel),
            sentence("Exposure level", c.exposureLevel),
            "Costume requirements:\n- believable seams\n- practical closures\n- realistic fabric thickness\n- readable material differences\n- understandable layering\n- plausible construction",
            section("Additional costume notes:", c.customNotes.trim()),
        ).joinToString("\n\n")
    }

    private fun poseBlock(p: PoseConfiguration): String = listOfNotNull(
        bullet("base pose", p.basePose?.wording),
        bullet("weight distribution", p.weight?.wording),
        bullet("leg arrangement", p.legAction),
        bullet("torso action", p.torso?.wording),
        bullet("arm action", p.arms?.wording),
        bullet("head angle", p.head?.wording),
        bullet("gaze", p.gaze?.wording),
        bullet("overall energy", p.energy?.wording),
        bullet("motion direction", p.motionDirection),
        section("Additional pose notes:", p.customNotes.trim()),
    ).joinToString("\n")

    private fun outputIntent(o: OutputConfiguration): String {
        val intent = if (o.type == OutputType.CUSTOM) o.customIntent.trim().ifEmpty { "illustration" } else o.type.intent
        val ratio = o.aspectRatio?.let { " in $it" }.orEmpty()
        return "Create a ${intent.trimEnd('.')}$ratio."
    }

    private fun composition(o: OutputConfiguration): String = listOfNotNull(
        bullet("framing", o.framing?.wording),
        bullet("figure placement", o.figurePlacement),
        bullet("negative space", o.negativeSpace),
        bullet("safe area", o.safeArea),
        bullet("clock-safe area", "preserve clean space for the clock".takeIf { o.type.isWallpaper && o.clockSafe }),
        bullet("icon-safe area", "preserve clean space for icons".takeIf { o.type.isWallpaper && o.iconSafe }),
    ).joinToString("\n")
}
