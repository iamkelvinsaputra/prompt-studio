package com.kelvinsaputra.promptstudio.feature.editor

import androidx.lifecycle.ViewModel
import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.persistence.*
import com.kelvinsaputra.promptstudio.prompt.PromptCompiler
import com.kelvinsaputra.promptstudio.prompt.effectivePrompt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class EditorMode { Quick, Advanced }

/** Navigation groups only; prompt ordering remains entirely in PromptCompiler. */
enum class EditorModule(val title: String, val group: String) {
    VisualBuild("Visual Build", "SCENE"),
    Style("Style", "STYLE"),
    Identity("Identity", "CHARACTER"),
    Role("Role", "CHARACTER"),
    VisualThesis("Visual Thesis", "CHARACTER"),
    Personality("Personality", "CHARACTER"),
    Contradiction("Inner Contradiction", "CHARACTER"),
    Body("Body", "CHARACTER"),
    Face("Face", "APPEARANCE"),
    Expression("Expression", "APPEARANCE"),
    Hair("Hair", "APPEARANCE"),
    Costume("Costume", "APPEARANCE"),
    Accessories("Accessories", "APPEARANCE"),
    ShapeLanguage("Shape Language", "APPEARANCE"),
    Power("Power Signature", "POWER & STORY"),
    Prop("Prop / Tool", "POWER & STORY"),
    Pose("Pose", "SCENE"),
    Gaze("Gaze", "SCENE"),
    Composition("Composition", "SCENE"),
    Environment("Environment", "SCENE"),
    Lighting("Lighting", "SCENE"),
    AccentColor("Color Accents", "SCENE"),
    SurfaceTexture("Surface / Texture", "SCENE"),
    Output("Output", "OUTPUT"),
    Avoid("Avoid", "OUTPUT"),
    PriorityStack("Priority Stack", "OUTPUT"),
    Prompt("Prompt", "PREVIEW"),
}

val quickModules = listOf(
    EditorModule.VisualBuild,
    EditorModule.Output, EditorModule.Identity, EditorModule.Role, EditorModule.VisualThesis,
    EditorModule.Personality, EditorModule.Face, EditorModule.Hair, EditorModule.Costume,
    EditorModule.Power, EditorModule.Pose, EditorModule.Composition, EditorModule.Lighting,
    EditorModule.Avoid,
)

data class EditorUiState(
    val library: CharacterLibrary = CharacterLibrary(),
    val module: EditorModule = EditorModule.VisualBuild,
    val mode: EditorMode = EditorMode.Quick,
    val costumeLocks: Set<CostumeField> = emptySet(),
    val poseLocks: Set<PoseField> = emptySet(),
    val variationLocks: Set<VariationField> = emptySet(),
    val message: String? = null,
    val saveError: String? = null,
    val guided: GuidedNavigation = GuidedNavigation(),
) {
    val project: CharacterProject get() = library.active
    val compiledPrompt get() = PromptCompiler().compile(project)
    val effectivePrompt get() = project.effectivePrompt()
}

class EditorViewModel(
    private val randomizer: EditorRandomizer = EditorRandomizer(),
    private val storage: ProjectStorage? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(restore())
    val state = mutableState.asStateFlow()

    fun selectModule(module: EditorModule) { mutableState.update { it.copy(module = module) } }
    fun setMode(mode: EditorMode) {
        mutableState.update { current ->
            val available = if (mode == EditorMode.Quick) quickModules else EditorModule.entries.toList()
            current.copy(mode = mode, module = current.module.takeIf { it in available } ?: quickModules.first())
        }
    }

    fun restoreConfiguration(snapshot: CharacterProject) = edit { snapshot.copy(id = id, name = name) }

    fun setProject(project: CharacterProject) = edit { project.copy(id = id) }
    fun setCostume(costume: CostumeConfiguration) = edit { copy(costume = costume) }
    fun setPose(pose: PoseConfiguration) = edit { copy(pose = pose) }
    fun setOutput(output: OutputConfiguration) = edit { copy(output = output) }

    fun setCostumeLocks(locks: Set<CostumeField>) { mutableState.update { it.copy(costumeLocks = locks.toSet()) } }
    fun setPoseLocks(locks: Set<PoseField>) { mutableState.update { it.copy(poseLocks = locks.toSet()) } }
    fun setVariationLocks(locks: Set<VariationField>) { mutableState.update { it.copy(variationLocks = locks.toSet()) } }

    fun selectCharacter(id: String) {
        changeLibrary(state.value.library.select(id))
        navigateGuide(GuidedNavigation())
    }
    fun navigateGuide(value: GuidedNavigation) { mutableState.update { it.copy(guided = value, module = EditorModule.VisualBuild) } }
    fun selectVariant(id: String) = changeLibrary(state.value.library.selectVariant(id))
    fun addVariant(name: String, type: OutputType) = changeLibrary(state.value.library.addVariant(name, type))
    fun renameVariant(name: String) = changeLibrary(state.value.library.renameVariant(name))
    fun deleteVariant() = changeLibrary(state.value.library.deleteVariant())
    fun setGeneration(value: GenerationPreferences) = changeLibrary(state.value.library.setGeneration(value))
    fun savePreset(name: String, composition: Boolean) {
        if (name.isBlank()) return
        val library = state.value.library
        val value = if (composition) VisualPresetValue.Composition.capture(library.active) else VisualPresetValue.Pose(library.active.pose)
        changeLibrary(library.copy(presets = listOf(SavedVisualPreset(library.nextId(), name.trim(), value)) + library.presets))
    }
    fun savePreset(name: String, category: String) {
        if (category == "pose" || category == "composition") { savePreset(name, category == "composition"); return }
        if (name.isBlank()) return
        val library = state.value.library
        val p = library.active
        val value = when (category) {
            "style" -> VisualPresetValue.Style(p.style, p.artStyle, p.surfaceTexture)
            "character" -> VisualPresetValue.Character(p)
            "complete" -> VisualPresetValue.Complete(p)
            else -> return
        }
        changeLibrary(library.copy(presets = listOf(SavedVisualPreset(library.nextId(), name.trim(), value)) + library.presets))
    }
    fun applyPreset(id: String) {
        val library = state.value.library
        val preset = library.presets.firstOrNull { it.id == id } ?: return
        changeLibrary(library.replaceActive(preset.value.applyTo(library.active)).copy(presets = listOf(preset) + library.presets.filterNot { it.id == id }))
    }
    fun deletePreset(id: String) = changeLibrary(state.value.library.copy(presets = state.value.library.presets.filterNot { it.id == id }))
    fun createProject(name: String, type: OutputType) {
        if (name.isBlank()) return
        val project = CharacterLibrary.newCharacter(state.value.library.nextId(), name.trim()).withStudioDefaults()
        val library = state.value.library.add(project.copy(output = project.output.copy(type = type)))
        changeLibrary(library.withVariants(library.activeVariants.copy(primaryName = type.name.lowercase().replaceFirstChar { it.uppercase() })))
        navigateGuide(GuidedNavigation.newProject())
    }
    fun newCharacter() = createProject("New Project", OutputType.PHONE)
    fun renameCharacter(name: String) {
        val cleaned = name.trim()
        if (cleaned.isNotEmpty()) edit { copy(name = cleaned) }
    }
    fun duplicateCharacter() = changeLibrary(state.value.library.duplicateActive())
    fun deleteCharacter() = changeLibrary(state.value.library.deleteActive())

    // UI events run on the main thread. Randomness is evaluated outside retryable update lambdas.
    fun randomizeCostume() {
        val current = state.value
        setCostume(randomizer.costume(current.project.costume, current.costumeLocks))
    }
    fun randomizePose() {
        val current = state.value
        setPose(randomizer.pose(current.project.pose, current.poseLocks))
    }
    fun randomizeUnlocked() {
        val current = state.value
        edit {
            randomizer.variation(this, current.variationLocks, current.costumeLocks, current.poseLocks)
        }
    }

    // Reset explicitly restores only the selected component. Session lock choices remain intact.
    fun resetCostume() = setCostume(CostumeConfiguration())
    fun resetPose() = setPose(PoseConfiguration())
    fun resetModule(module: EditorModule = state.value.module) = when (module) {
        EditorModule.VisualBuild -> edit { resetVisualAssembly() }
        EditorModule.Style, EditorModule.Prompt -> Unit
        EditorModule.Identity -> edit { copy(subject = "", identity = IdentityConfiguration()) }
        EditorModule.Role -> edit { copy(role = RoleConfiguration()) }
        EditorModule.VisualThesis -> edit { copy(coreVisualThesis = "") }
        EditorModule.Personality -> edit { copy(personality = PersonalityConfiguration()) }
        EditorModule.Contradiction -> edit { copy(contradiction = ContradictionConfiguration()) }
        EditorModule.Body -> edit { copy(body = BodyConfiguration()) }
        EditorModule.Face -> edit { copy(face = FaceConfiguration()) }
        EditorModule.Expression -> edit { copy(expression = ExpressionConfiguration()) }
        EditorModule.Hair -> edit { copy(hair = HairConfiguration()) }
        EditorModule.Costume -> resetCostume()
        EditorModule.Accessories -> edit { copy(accessories = AccessoriesConfiguration()) }
        EditorModule.ShapeLanguage -> edit { copy(shapeLanguage = ShapeLanguageConfiguration()) }
        EditorModule.Power -> edit { copy(powerSignature = PowerSignatureConfiguration()) }
        EditorModule.Prop -> edit { copy(prop = PropConfiguration()) }
        EditorModule.Pose -> resetPose()
        EditorModule.Gaze -> edit { copy(gazeDirection = GazeConfiguration()) }
        EditorModule.Composition -> edit { copy(composition = CompositionConfiguration()) }
        EditorModule.Environment -> edit { copy(environment = EnvironmentConfiguration()) }
        EditorModule.Lighting -> edit { copy(lighting = LightingConfiguration()) }
        EditorModule.AccentColor -> edit { copy(colorAccents = ColorAccentConfiguration()) }
        EditorModule.SurfaceTexture -> edit { copy(surfaceTexture = SurfaceTextureConfiguration()) }
        EditorModule.Output -> setOutput(OutputConfiguration())
        EditorModule.Avoid -> edit { copy(exclusions = emptyList()) }
        EditorModule.PriorityStack -> edit { copy(priorityStack = emptyList()) }
    }

    private fun edit(transform: CharacterProject.() -> CharacterProject) {
        val before = state.value
        val next = before.project.transform()
        if (next == before.project) return
        val library = before.library.replaceActive(next)
        mutableState.update { it.copy(library = library) }
        save(library)
    }

    private fun changeLibrary(library: CharacterLibrary) {
        if (library == state.value.library) return
        val changedSelection = library.activeCharacterId != state.value.library.activeCharacterId || library.activeVariants.activeId != state.value.library.activeVariants.activeId
        mutableState.update { it.copy(library = library, guided = if (changedSelection) GuidedNavigation() else it.guided,
            module = if (library.activeCharacterId != state.value.library.activeCharacterId) EditorModule.VisualBuild else it.module) }
        save(library)
    }

    private fun restore(): EditorUiState = try {
        val saved = storage?.read()
        val library = when {
            saved == null -> CharacterLibrary(activeCharacterId = "first-study", characters = listOf(CharacterLibrary.newCharacter("first-study", "First study").withStudioDefaults()))
            else -> runCatching { CharacterLibraryJson.decode(saved) }
                .getOrElse { ProjectJson.decode(saved).let { CharacterLibrary(it.id, listOf(it)) } }
        }
        EditorUiState(library = library)
    } catch (_: Exception) {
        // Do not overwrite a corrupt/incompatible file just by launching the app.
        EditorUiState(message = "Could not restore the saved character library. Demo loaded; the saved file is unchanged until you edit.")
    }

    private fun save(library: CharacterLibrary) {
        if (storage == null) return
        try {
            storage.write(CharacterLibraryJson.encode(library))
            mutableState.update { it.copy(saveError = null) }
        } catch (_: Exception) {
            mutableState.update { it.copy(saveError = "Changes are not saved locally. Retry saving; Export Character can keep the active variant.") }
        }
    }

    fun retrySave() = save(state.value.library)
    fun showMessage(message: String) { mutableState.update { it.copy(message = message) } }
    fun dismissMessage() { mutableState.update { it.copy(message = null) } }
    fun exportProject(): String = ProjectJson.encode(state.value.project)

    fun importProject(text: String) {
        val project = try {
            ProjectJson.decode(text)
        } catch (e: ProjectFileException) {
            showMessage("Import failed: ${e.message}")
            return
        }
        // Import replaces the active character and opens Summary; lock choices remain session state.
        val next = state.value.library.replaceActive(project)
        mutableState.update { it.copy(library = next, guided = GuidedNavigation(), module = EditorModule.VisualBuild) }
        save(next)
        showMessage(if (state.value.saveError == null) "Character imported" else "Character imported, but local saving failed. Export a backup.")
    }
}
