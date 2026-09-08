package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.prompt.PromptCompiler
import kotlin.test.*

class PromptCompilerTest {
    private val compiler = PromptCompiler()

    @Test fun configuredCostumeCompilesInStableOrder() {
        val project = CharacterProject(costume = CostumeConfiguration(
            outerwear = Outerwear.PONCHO, footwear = Footwear.WORK_BOOTS,
            customization = setOf(Customization.CHARM, Customization.PATCHES), customNotes = "  Reinforced seams.  ",
        ))
        val text = compiler.compile(project).text
        assertContains(text, "- upper layer: poncho")
        assertContains(text, "- footwear: work boots")
        assertContains(text, "- personal styling: patches, charm")
        assertContains(text, "Additional costume notes:\n\nReinforced seams.")
        assertEquals(compiler.compile(project), compiler.compile(project))
        assertEquals(text, compiler.compile(project.copy(costume = project.costume.copy(
            customization = setOf(Customization.PATCHES, Customization.CHARM),
        ))).text)
    }

    @Test fun emptyOptionalFieldsAreOmitted() {
        val text = compiler.compile(CharacterProject(subject = " ", costume = CostumeConfiguration(
            outfitIdentity = "", silhouette = null, outerwear = null, innerwear = null,
            lowerWear = null, legwear = null, footwear = null, handwear = null, utility = null,
            customization = emptySet(), materialFeel = " ", exposureLevel = "", customNotes = "\n",
        ), pose = PoseConfiguration(null, null, "", null, null, null, null, null, "", ""))).text
        assertFalse(text.contains("null"))
        assertFalse(Regex("(?m)^- [^\\n]*:\\s*$").containsMatchIn(text))
        listOf("Base outfit:", "Material feel:", "Additional costume notes:", "SUBJECT", "POSE").forEach {
            assertFalse(text.contains(it), it)
        }
    }

    @Test fun lockedStyleAppearsExactlyOnce() {
        val text = compiler.compile(CharacterProject()).text
        assertEquals(1, Regex("ART STYLE CORE").findAll(text).count())
        assertEquals(1, Regex(Regex.escape(ArtStyles.SumiESkyBlue.prompt)).findAll(text).count())
    }

    @Test fun poseAndOutputMatchSelections() {
        val project = CharacterProject(pose = PoseConfiguration(basePose = BasePose.CROUCHING, gaze = Gaze.OFF_SCREEN))
        for (type in OutputType.entries.filter { it != OutputType.CUSTOM }) {
            val text = compiler.compile(project.copy(output = OutputConfiguration(type = type))).text
            assertContains(text, "Create a ${type.intent} in ${type.ratio}.")
            assertContains(text, "- base pose: crouching")
            assertContains(text, "- gaze: off-screen")
            assertEquals(type.isWallpaper, text.contains("clock-safe area"))
        }
    }

    @Test fun customRatioValidationHandlesIncompleteInput() {
        assertNull(OutputConfiguration(type = OutputType.CUSTOM, customAspectRatio = "0:9").aspectRatio)
        assertNull(OutputConfiguration(type = OutputType.CUSTOM, customAspectRatio = "16:").aspectRatio)
        val output = OutputConfiguration(type = OutputType.CUSTOM, customAspectRatio = " 2:3 ", customIntent = "book cover")
        assertContains(compiler.compile(CharacterProject(output = output)).text, "Create a book cover in 2:3.")
    }

    @Test fun customizationLimitIsEnforcedByDomain() {
        assertFailsWith<IllegalArgumentException> {
            CostumeConfiguration(customization = Customization.entries.take(3).toSet())
        }
    }
}
