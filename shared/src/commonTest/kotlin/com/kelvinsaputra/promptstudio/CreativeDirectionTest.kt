package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.EditorViewModel
import com.kelvinsaputra.promptstudio.persistence.*
import com.kelvinsaputra.promptstudio.prompt.*
import kotlin.test.*

class CreativeDirectionTest {
    private fun fresh() = CharacterLibrary.newCharacter("study", "Study").withStudioDefaults().copy(
        identity = IdentityConfiguration(characterName = "Aika", ageBand = AgeBand.LATE_TEEN, genderPresentation = GenderPresentation.FEMALE),
        profile = CharacterProfile(enabled = true, eyeColor = "blue-gray", skinTone = "fair neutral", jawShape = "defined", distinguishingFeatures = "freckles"),
        face = FaceConfiguration(eyeShape = "narrow", facialStructure = "oval"),
        hair = HairConfiguration(baseColor = "blue-black", length = HairLength.LONG, style = HairStyle.LOOSELY_TIED),
    )
    @Test fun identityRemainsCanonicalAcrossSceneChanges() {
        val p = fresh()
        val altered = lightingPresets.last().applyTo(p).withEnvironment(EnvironmentCategory.URBAN).withVisualPose(VisualPose.SITTING)
            .copy(expression = ExpressionConfiguration(preset = ExpressionPreset.PLAYFUL), colorDirection = palettePresets.last().direction())
        assertEquals(compileCharacterIdentity(p), compileCharacterIdentity(altered))
        val text = PromptCompiler().compile(altered).text
        assertTrue(text.startsWith("CHARACTER IDENTITY"))
        assertEquals(1, Regex("blue-black").findAll(text).count())
        assertContains(text, "Aika is an original late-teen female character")
        assertContains(text, "eye color: blue-gray")
    }
    @Test fun identityLocksProtectPresetsRandomizationAndDirectEdits() {
        val p = fresh().copy(profile = fresh().profile.copy(lockedTraits = IdentityTrait.entries.toSet()))
        val library = CharacterLibrary(p.id, listOf(p))
        val changed = p.copy(face = FaceConfiguration(facialStructure = "square"), hair = HairConfiguration(baseColor = "red"),
            body = BodyConfiguration(build = Build.STURDY), profile = p.profile.copy(eyeColor = "green", skinTone = "dark", distinguishingFeatures = "scar"))
        val protected = library.replaceActive(changed).active
        assertEquals(p.face, protected.face); assertEquals(p.hair, protected.hair); assertEquals(p.body, protected.body); assertEquals(p.profile, protected.profile)
        val unlocked = library.replaceActive(p.copy(profile = p.profile.copy(lockedTraits = emptySet())))
        assertEquals("red", unlocked.replaceActive(unlocked.active.copy(hair = changed.hair)).active.hair.baseColor)
    }
    @Test fun variantsCaptureSceneAndKeepIndependentEnvironmentsColorsAndEffects() {
        val p = fresh().withEnvironment(EnvironmentCategory.URBAN)
        var library = CharacterLibrary(p.id, listOf(p)).addVariant("Forest", OutputType.PORTRAIT)
        val alternative = library.activeVariants.activeId
        library = library.replaceActive(library.active.withEnvironment(EnvironmentCategory.NATURE).copy(colorDirection = palettePresets.first().direction(), effects = EffectsConfiguration(items = listOf(EffectItem(EffectFamily.WIND, "Gentle wind")))))
        assertEquals(EnvironmentCategory.URBAN, library.selectVariant(PRIMARY_VARIANT).active.environment.category)
        assertTrue(library.selectVariant(PRIMARY_VARIANT).active.effects.items.isEmpty())
        assertEquals(EnvironmentCategory.NATURE, library.selectVariant(alternative).active.environment.category)
        library = library.replaceActive(library.active.withStylePreset(StyleLook.MANGA).copy(costume = library.active.costume.copy(outerwear = Outerwear.COAT)))
        assertEquals(StyleLook.MANGA, library.active.artStyle.preset)
        assertNotEquals(StyleLook.MANGA, library.selectVariant(PRIMARY_VARIANT).active.artStyle.preset)
        assertEquals(p.costume, library.selectVariant(PRIMARY_VARIANT).active.costume)
        assertEquals(compileCharacterIdentity(p), compileCharacterIdentity(library.active))
        assertEquals(library, CharacterLibraryJson.decode(CharacterLibraryJson.encode(library)))
    }
    @Test fun confirmedHistoryRestoreRestoresSnapshotEvenOverCurrentLocks() {
        val editor = EditorViewModel()
        val p = fresh().copy(profile = fresh().profile.copy(lockedTraits = IdentityTrait.entries.toSet()))
        editor.setProject(p)
        val snapshot = fresh().copy(hair = HairConfiguration(baseColor = "auburn"))
        editor.restoreConfiguration(snapshot)
        assertEquals(snapshot.hair, editor.state.value.project.hair)
        assertEquals(compileCharacterIdentity(snapshot), compileCharacterIdentity(editor.state.value.project))
    }
    @Test fun palettesAreValidatedRoleOrderedAndIndependentOfStyle() {
        assertNull(normalizeHex("#12")); assertNull(normalizeHex("GGHHII")); assertEquals("#ABCDEF", normalizeHex("abcdef"))
        assertFailsWith<IllegalArgumentException> { PaletteColor(ColorRole.ACCENT, "wrong") }
        assertFailsWith<IllegalArgumentException> { ColorDirection(saturation = 101) }
        val colors = listOf(PaletteColor(ColorRole.ACCENT, "#abcdef"), PaletteColor(ColorRole.PRIMARY, "#123456"))
        assertEquals(compileColorDirection(ColorDirection(colors = colors)), compileColorDirection(ColorDirection(colors = colors.reversed())))
        val p = fresh().copy(colorDirection = palettePresets.first().direction())
        assertEquals(p.colorDirection, p.withStylePreset(StyleLook.MANGA).colorDirection)
        assertTrue(p.colorDirection.withColor(ColorRole.ACCENT, null).colors.none { it.role == ColorRole.ACCENT })
    }
    @Test fun richerConfigurationAndCreationProgressRoundTripExactly() {
        val p = fresh().copy(creationStep = 7, colorDirection = palettePresets.first().direction(), effects = EffectsConfiguration(items = listOf(EffectItem(EffectFamily.ATMOSPHERIC, "Haze"))))
        assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
        assertEquals(PromptCompiler().compile(p), PromptCompiler().compile(ProjectJson.decode(ProjectJson.encode(p))))
        val old = CharacterProject()
        assertNull(ProjectJson.decode(ProjectJson.encode(old)).creationStep)
        assertFalse(ProjectJson.decode(ProjectJson.encode(old)).profile.enabled)
        val editor = EditorViewModel(); editor.createProject("Study", OutputType.PORTRAIT)
        assertEquals(0, editor.state.value.project.creationStep)
        assertNotNull(editor.state.value.project.output.aspectRatio)
    }
    @Test fun templateLibrariesAreDistinctAndCoverEveryCollection() {
        characterGenders.forEach { gender -> assertEquals(OutfitLibrary.categories.toSet(), OutfitLibrary.entries.filter { it.gender == gender }.map { it.category }.toSet()) }
        OutfitLibrary.categories.forEach { category ->
            val outfits = OutfitLibrary.entries.filter { it.category == category }
            assertNotEquals(outfits[0].costume, outfits[1].costume)
        }
        lightingPresets.forEach { preset -> val p = preset.applyTo(fresh()); assertEquals(preset.direction, p.lighting.lightDirection); assertContains(p.effectivePrompt(), preset.quality) }
    }
}
