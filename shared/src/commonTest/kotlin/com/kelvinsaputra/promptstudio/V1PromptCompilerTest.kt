package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.persistence.ProjectJson
import com.kelvinsaputra.promptstudio.prompt.PromptCompiler
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.Json
import kotlin.test.*

class V1PromptCompilerTest {
    private val compiler = PromptCompiler()

    private fun completeProject() = CharacterProject(
        id = "v1", name = "Iona",
        subject = "",
        identity = IdentityConfiguration(AgeBand.ADULT, "late 20s", GenderPresentation.ANDROGYNOUS, BodyType.WIRY, "quietly dangerous"),
        role = RoleConfiguration("relic courier", "the flooded archive", "reluctant protector"),
        coreVisualThesis = "Ceremonial discipline interrupted by salt-worn field gear",
        personality = PersonalityConfiguration(listOf("watchful", "wry", "patient", "protective", "unsettling")),
        contradiction = ContradictionConfiguration("composed", "grieving", "never sentimental"),
        body = BodyConfiguration("mature young adult", Build.WIRY, "long-limbed but believable", HeightImpression.TALL, AthleticLanguage.AGILE),
        face = FaceConfiguration("narrow, observant eyes", "controlled brows", "rare crooked smile", "minimal ink liner", "strong jaw", "adult and weathered"),
        expression = ExpressionConfiguration(ExpressionPreset.AMUSED, "faint amusement", "guarded concern", "a sudden serious read"),
        hair = HairConfiguration("black", "silver at the roots", HairLength.SHOULDER_LENGTH, HairStyle.ASYMMETRICAL, "moves in the sea wind", "one muted crimson clip"),
        accessories = AccessoriesConfiguration(listOf("cracked signet ring", "small compass")),
        shapeLanguage = ShapeLanguageConfiguration(listOf("asymmetry", "diagonal motion", "interrupted curves", "slight visual imbalance")),
        powerSignature = PowerSignatureConfiguration("memory held in water", "translucent ink ribbons", "broken diagonals", "slow tidal pull", "sky-blue and black pigment", "keep effects sparse"),
        prop = PropConfiguration("folding archive spear", "retrieves submerged records", "practical brass and canvas", "waterproof folio"),
        gazeDirection = GazeConfiguration(Head.TURNED_BACK, Gaze.PAST_VIEWER, GazeIntensity.CHALLENGING),
        composition = CompositionConfiguration("left-to-right tidal diagonal", "face and hand", "clean silhouette first"),
        environment = EnvironmentConfiguration("a flooded archive", EnvironmentAbstraction.ATMOSPHERIC, "two soft depth layers", "faint shelves and watercolor drift"),
        lighting = LightingConfiguration(LightingSource.SOFT_SIDE_LIGHT, "from upper left", ShadowSoftness.VERY_SOFT, "quiet tension", "no flashy rim light"),
        colorAccents = ColorAccentConfiguration(AccentColor.DULL_CRIMSON, "clip and inner lining", "muted", "large background fields", "a restrained warning"),
        surfaceTexture = SurfaceTextureConfiguration(TextureLevel.SUBTLE, WatercolorBehavior.DELICATE_DIFFUSION, InkTextureBehavior.CONTROLLED_DRY_BRUSH),
        exclusions = listOf("extra weapons", "crowded background", "neon effects"),
        priorityStack = listOf("face and personality", "silhouette", "costume construction", "power signature"),
    )

    @Test fun fullV1ProjectCompilesEverySectionInCheatsheetOrder() {
        val text = compiler.compile(completeProject()).text
        val headings = text.lines().filter { it.isNotBlank() && it.all { c -> c.isUpperCase() || c == ' ' || c == '/' } }
        assertEquals(listOf(
            "ART STYLE CORE", "OUTPUT INTENT", "SUBJECT", "ROLE", "CORE VISUAL THESIS", "PERSONALITY READ",
            "INNER CONTRADICTION", "BODY", "FACE", "EXPRESSION", "HAIR", "COSTUME", "ACCESSORIES",
            "SHAPE LANGUAGE", "SUPERNATURAL SIGNATURE", "PROP / WEAPON / TOOL", "POSE", "GAZE / HEAD DIRECTION",
            "COMPOSITION", "ENVIRONMENT / BACKGROUND", "LIGHTING", "ACCENT COLOR POLICY", "SURFACE / TEXTURE",
            "PRIORITY STACK", "AVOID",
        ), headings)
        assertEquals(compiler.compile(completeProject()), compiler.compile(completeProject()))
        assertEquals(1, Regex("ART STYLE CORE").findAll(text).count())
        assertFalse(text.contains("null"))
        assertContains(text, "1. face and personality")
    }

    @Test fun V1LimitsRejectOverflowAndPriorityOrderIsNotSorted() {
        assertFailsWith<IllegalArgumentException> { PersonalityConfiguration(List(8) { "trait $it" }) }
        assertFailsWith<IllegalArgumentException> { AccessoriesConfiguration(List(4) { "item $it" }) }
        assertFailsWith<IllegalArgumentException> { ShapeLanguageConfiguration(List(7) { "shape $it" }) }
        assertFailsWith<IllegalArgumentException> { CharacterProject(exclusions = List(16) { "avoid $it" }) }
        assertFailsWith<IllegalArgumentException> { CharacterProject(priorityStack = List(6) { "priority $it" }) }
        val prompt = compiler.compile(CharacterProject(priorityStack = listOf("third", "first", "second"))).text
        assertTrue(prompt.indexOf("1. third") < prompt.indexOf("2. first"))
    }

    @Test fun emptyV1ModulesDisappearWithoutDamagingV0Sections() {
        val text = compiler.compile(CharacterProject(subject = "", costume = CostumeConfiguration(
            outfitIdentity = "", silhouette = null, outerwear = null, innerwear = null, lowerWear = null,
            legwear = null, footwear = null, handwear = null, utility = null, customization = emptySet(), materialFeel = "", exposureLevel = "",
        ), pose = PoseConfiguration(basePose = null, weight = null, legAction = "", torso = null, arms = null, head = null, gaze = null, energy = null, motionDirection = ""),
            output = OutputConfiguration(framing = null, figurePlacement = "", negativeSpace = "", safeArea = "", clockSafe = false, iconSafe = false),
        )).text
        assertEquals(listOf("ART STYLE CORE", "OUTPUT INTENT", "SUBJECT"), text.lines().filter { it.isNotBlank() && it.all { c -> c.isUpperCase() || c == ' ' } })
    }

    @Test fun V0SerializedProjectDecodesIntoExpandedDefaults() {
        val full = Json.parseToJsonElement(ProjectJson.encode(CharacterProject())).jsonObject
        val v0Keys = setOf("version", "style", "subject", "costume", "pose", "output")
        val v0 = JsonObject(full.filterKeys { it in v0Keys })
        val decoded = ProjectJson.decode(v0.toString())
        assertEquals(CharacterProject().subject, decoded.subject)
        assertEquals(IdentityConfiguration(), decoded.identity)
        assertEquals("demo-character", decoded.id)
        assertEquals("Antihero", decoded.name)
    }

    @Test fun expandedProjectSerializationKeepsCompiledPrompt() {
        val project = completeProject()
        val decoded = ProjectJson.decode(ProjectJson.encode(project))
        assertEquals(project, decoded)
        assertEquals(compiler.compile(project), compiler.compile(decoded))
    }
}
