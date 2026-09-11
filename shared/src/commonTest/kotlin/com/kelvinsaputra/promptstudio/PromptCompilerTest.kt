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
        val costume = text.substringAfter("COSTUME\n\n").substringBefore("\n\nPOSE")
        assertContains(costume, "- upper layer: poncho")
        assertContains(costume, "- footwear: work boots")
        assertContains(text, "- personal styling: patches, charm")
        assertContains(text, "Additional costume notes:\nReinforced seams.")
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
        listOf("Base outfit:", "Material feel:", "Additional costume notes:", "POSE").forEach {
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

    @Test fun customOutputAcceptsAuthoredRatioTextAndOmitsBlankRatio() {
        for (ratio in listOf("2:3", "1.85:1", "widescreen")) {
            val output = OutputConfiguration(type = OutputType.CUSTOM, customAspectRatio = " $ratio ", customIntent = "book cover")
            assertContains(compiler.compile(CharacterProject(output = output)).text, "Create a book cover in $ratio.")
        }
        val output = OutputConfiguration(type = OutputType.CUSTOM, customAspectRatio = " ", customIntent = " ")
        assertNull(output.aspectRatio)
        assertContains(compiler.compile(CharacterProject(output = output)).text, "Create an illustration.")
    }

    @Test fun customizationLimitIsEnforcedByDomain() {
        assertFailsWith<IllegalArgumentException> {
            CostumeConfiguration(customization = Customization.entries.take(3).toSet())
        }
    }

    @Test fun defaultProjectHasExactlyTheSixV0SectionsInOrder() {
        val text = compiler.compile(CharacterProject()).text
        assertTrue(text.isNotBlank())
        val headings = text.lines().filter { it.isNotBlank() && it.all { c -> c.isUpperCase() || c == ' ' } }
        assertEquals(listOf("ART STYLE CORE", "OUTPUT INTENT", "SUBJECT", "COSTUME", "POSE", "COMPOSITION"), headings)
        assertContains(text, "SUBJECT\n\n${CharacterProject().name}\n${CharacterProject().subject}")
        assertFalse(text.endsWith("\n"))
    }

    @Test fun compilationIsRepeatableAcrossCompilerInstances() {
        val project = CharacterProject()
        val expected = compiler.compile(project)
        repeat(20) { assertEquals(expected, PromptCompiler().compile(project.copy())) }
    }

    @Test fun costumeSentencesNormalizePunctuationAndOmitPunctuationOnlyValues() {
        val text = compiler.compile(CharacterProject(costume = CostumeConfiguration(
            outfitIdentity = "  field uniform. !  ", materialFeel = "canvas...", exposureLevel = " .!? ",
        ))).text
        assertContains(text, "Costume concept: field uniform.")
        assertContains(text, "Material feel: canvas.")
        assertFalse(text.contains("Exposure level:"))
        assertFalse(text.contains(".."))
    }

    @Test fun poseValuesAndNotesStayInPoseSection() {
        val text = compiler.compile(CharacterProject(pose = PoseConfiguration(
            basePose = BasePose.PIVOT, weight = Weight.SUSPENDED_MID_MOTION,
            legAction = "one foot lifted", torso = Torso.SHARP_TWIST, arms = Arms.ONE_ARM_BRACING,
            head = Head.TURNED_BACK, gaze = Gaze.PAST_VIEWER, energy = Energy.EVASIVE,
            motionDirection = "leftward", customNotes = "  Keep the gesture readable.  ",
        ))).text
        assertEquals("""
            - base pose: pivot
            - weight distribution: suspended mid-motion
            - leg arrangement: one foot lifted
            - torso action: sharp twist
            - arm action: one arm bracing
            - body orientation: body facing the viewer
            - primary prop placement: no prop held in the pose
            - head angle: turned back
            - gaze: past viewer
            - overall energy: evasive
            - motion direction: leftward
            Additional pose notes:
            Keep the gesture readable.
        """.trimIndent(), text.substringAfter("POSE\n\n").substringBefore("\n\nCOMPOSITION"))
        assertFalse(text.substringBefore("POSE\n\n").contains("one foot lifted"))
    }

    @Test fun outputMappingsAreIndependentOfCustomRatio() {
        val expected = listOf(
            OutputType.DESKTOP to "Create a desktop wallpaper in 16:9.",
            OutputType.PHONE to "Create a smartphone wallpaper in 9:16.",
            OutputType.SQUARE to "Create a square illustration in 1:1.",
            OutputType.PORTRAIT to "Create a portrait in 4:5.",
        )
        for ((type, sentence) in expected) {
            val text = compiler.compile(CharacterProject(output = OutputConfiguration(type = type, customAspectRatio = "7:2"))).text
            assertEquals(sentence, text.substringAfter("OUTPUT INTENT\n\n").substringBefore("\n\nSUBJECT"))
        }
    }

    @Test fun emptyCompositionIsOmittedAndSafeAreaIsAuthored() {
        val empty = OutputConfiguration(framing = null, figurePlacement = " ", negativeSpace = "", safeArea = "\n", clockSafe = false, iconSafe = false)
        assertFalse(compiler.compile(CharacterProject(output = empty)).text.contains("COMPOSITION"))
        val text = compiler.compile(CharacterProject(output = empty.copy(safeArea = "preserve clean space for phone clock and icons"))).text
        assertTrue(text.endsWith("COMPOSITION\n\n- safe area: preserve clean space for phone clock and icons"))
    }

    @Test fun customizationLimitAlsoAppliesToCopyAndAllowsTwo() {
        val costume = CostumeConfiguration(customization = setOf(Customization.PATCHES, Customization.CHARM))
        assertContains(compiler.compile(CharacterProject(costume = costume)).text, "- personal styling: patches, charm")
        assertFailsWith<IllegalArgumentException> { costume.copy(customization = costume.customization + Customization.PINS) }
    }

    @Test fun customOutputNormalizesTerminalPunctuation() {
        val output = OutputConfiguration(type = OutputType.CUSTOM, customIntent = "book cover!?", customAspectRatio = "3:2")
        assertContains(compiler.compile(CharacterProject(output = output)).text, "Create a book cover in 3:2.")
    }
}
