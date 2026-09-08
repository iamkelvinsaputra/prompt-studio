package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.*
import com.kelvinsaputra.promptstudio.prompt.PromptCompiler
import kotlin.random.Random
import kotlin.test.*

class EditorRandomizerTest {
    @Test fun unlockedCostumeChoicesChangeAndAuthoredTextSurvives() {
        val original = CostumeConfiguration(outfitIdentity = "My outfit", materialFeel = "Canvas", exposureLevel = "Covered", customNotes = "Keep the lining")
        val result = EditorRandomizer(Random(42)).costume(original, emptySet())
        assertNotEquals(original.silhouette, result.silhouette)
        assertNotEquals(original.outerwear, result.outerwear)
        assertNotEquals(original.innerwear, result.innerwear)
        assertNotEquals(original.lowerWear, result.lowerWear)
        assertNotEquals(original.legwear, result.legwear)
        assertNotEquals(original.footwear, result.footwear)
        assertNotEquals(original.handwear, result.handwear)
        assertNotEquals(original.utility, result.utility)
        assertNotEquals(original.customization, result.customization)
        assertEquals(original.outfitIdentity, result.outfitIdentity)
        assertEquals(original.materialFeel, result.materialFeel)
        assertEquals(original.exposureLevel, result.exposureLevel)
        assertEquals(original.customNotes, result.customNotes)
    }
    @Test fun everyCostumeLockPreservesItsFieldWhileOtherChoicesVary() {
        val original = CostumeConfiguration()
        for (field in CostumeField.entries) {
            val result = EditorRandomizer(Random(7)).costume(original, setOf(field))
            when (field) {
                CostumeField.Silhouette -> assertEquals(original.silhouette, result.silhouette)
                CostumeField.Outerwear -> assertEquals(original.outerwear, result.outerwear)
                CostumeField.Innerwear -> assertEquals(original.innerwear, result.innerwear)
                CostumeField.LowerWear -> assertEquals(original.lowerWear, result.lowerWear)
                CostumeField.Legwear -> assertEquals(original.legwear, result.legwear)
                CostumeField.Footwear -> assertEquals(original.footwear, result.footwear)
                CostumeField.Handwear -> assertEquals(original.handwear, result.handwear)
                CostumeField.Utility -> assertEquals(original.utility, result.utility)
                CostumeField.Customization -> assertEquals(original.customization, result.customization)
            }
            assertNotEquals(original, result)
        }
    }
    @Test fun everyPoseLockPreservesItsFieldWhileOtherChoicesVary() {
        val original = PoseConfiguration()
        for (field in PoseField.entries) {
            val result = EditorRandomizer(Random(7)).pose(original, setOf(field))
            when (field) {
                PoseField.BasePose -> assertEquals(original.basePose, result.basePose)
                PoseField.Weight -> assertEquals(original.weight, result.weight)
                PoseField.Torso -> assertEquals(original.torso, result.torso)
                PoseField.Arms -> assertEquals(original.arms, result.arms)
                PoseField.Head -> assertEquals(original.head, result.head)
                PoseField.Gaze -> assertEquals(original.gaze, result.gaze)
                PoseField.Energy -> assertEquals(original.energy, result.energy)
            }
            assertNotEquals(original, result)
        }
    }
    @Test fun allLocksPreserveEntireModulesIncludingUnsetValues() {
        val randomizer = EditorRandomizer(Random(123))
        val costume = CostumeConfiguration(silhouette = null, customization = emptySet())
        val pose = PoseConfiguration(basePose = null, weight = null)
        assertEquals(costume, randomizer.costume(costume, CostumeField.entries.toSet()))
        assertEquals(pose, randomizer.pose(pose, PoseField.entries.toSet()))
    }

    @Test fun poseRandomizationPreservesAuthoredTextAndChangesChoices() {
        val original = PoseConfiguration(legAction = "Custom stance", motionDirection = "Left", customNotes = "Keep gesture")
        val result = EditorRandomizer(Random(42)).pose(original, emptySet())
        assertNotEquals(original.basePose, result.basePose)
        assertNotEquals(original.energy, result.energy)
        assertEquals(original.legAction, result.legAction)
        assertEquals(original.motionDirection, result.motionDirection)
        assertEquals(original.customNotes, result.customNotes)
    }

    @Test fun repeatedRandomizationAlwaysRespectsCustomizationLimit() {
        val randomizer = EditorRandomizer(Random(16))
        var current = CostumeConfiguration()
        repeat(500) {
            val next = randomizer.costume(current, emptySet())
            assertTrue(next.customization.size <= 2)
            assertTrue(next.customization.all { it in Customization.entries })
            assertNotEquals(current.customization, next.customization)
            current = next
        }
    }

    @Test fun seatedAndStandingWeightSuggestionsUseExistingVocabulary() {
        val randomizer = EditorRandomizer(Random(1))
        repeat(20) {
            val seated = randomizer.pose(PoseConfiguration(basePose = BasePose.SITTING_ON_LEDGE), setOf(PoseField.BasePose))
            assertEquals(Weight.SEATED_WEIGHT, seated.weight)
            val standing = randomizer.pose(PoseConfiguration(basePose = BasePose.RELAXED_STANDING), setOf(PoseField.BasePose))
            assertTrue(standing.weight in listOf(Weight.ON_LEFT_LEG, Weight.ON_RIGHT_LEG, Weight.EVENLY_DISTRIBUTED))
        }
        val locked = PoseConfiguration(basePose = BasePose.SITTING_ON_LEDGE, weight = Weight.ON_LEFT_LEG)
        assertEquals(locked.weight, randomizer.pose(locked, setOf(PoseField.BasePose, PoseField.Weight)).weight)
    }

    @Test fun sameSeedAndEventsProduceSameProjectsAndDeterministicPrompts() {
        val first = EditorViewModel(EditorRandomizer(Random(99)))
        val second = EditorViewModel(EditorRandomizer(Random(99)))
        repeat(10) {
            first.randomizeCostume(); first.randomizePose()
            second.randomizeCostume(); second.randomizePose()
            assertEquals(first.state.value.project, second.state.value.project)
            val project = first.state.value.project
            assertEquals(PromptCompiler().compile(project), PromptCompiler().compile(project))
        }
    }
}
