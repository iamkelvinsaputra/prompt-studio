package com.kelvinsaputra.promptstudio.feature.editor

import androidx.lifecycle.ViewModel
import com.kelvinsaputra.promptstudio.persistence.*
import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.prompt.PromptCompiler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class EditorModule { Style, Costume, Pose, Output, Prompt }

data class EditorUiState(
    val project: CharacterProject = CharacterProject(),
    val module: EditorModule = EditorModule.Costume,
    val costumeLocks: Set<CostumeField> = emptySet(),
    val poseLocks: Set<PoseField> = emptySet(),
    val message: String? = null,
    val saveError: String? = null,
) {
    val compiledPrompt get() = PromptCompiler().compile(project)
}

class EditorViewModel(
    private val randomizer: EditorRandomizer = EditorRandomizer(),
    private val storage: ProjectStorage? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(restore())
    val state = mutableState.asStateFlow()

    fun selectModule(module: EditorModule) { mutableState.update { it.copy(module = module) } }
    fun setCostume(costume: CostumeConfiguration) = edit { copy(costume = costume) }
    fun setPose(pose: PoseConfiguration) = edit { copy(pose = pose) }
    fun setOutput(output: OutputConfiguration) = edit { copy(output = output) }

    fun setCostumeLocks(locks: Set<CostumeField>) { mutableState.update { it.copy(costumeLocks = locks.toSet()) } }
    fun setPoseLocks(locks: Set<PoseField>) { mutableState.update { it.copy(poseLocks = locks.toSet()) } }

    // UI events run on the main thread. Evaluate randomness once, outside a retryable update lambda.
    fun randomizeCostume() {
        val current = state.value
        setCostume(randomizer.costume(current.project.costume, current.costumeLocks))
    }
    fun randomizePose() {
        val current = state.value
        setPose(randomizer.pose(current.project.pose, current.poseLocks))
    }

    // Reset explicitly restores the entire module; session lock choices remain intact.
    fun resetCostume() = setCostume(CostumeConfiguration())
    fun resetPose() = setPose(PoseConfiguration())

    private fun edit(transform: CharacterProject.() -> CharacterProject) {
        val next = state.value.project.transform()
        if (next == state.value.project) return
        mutableState.update { it.copy(project = next) }
        save(next)
    }

    private fun restore(): EditorUiState = try {
        val saved = storage?.read()
        EditorUiState(project = saved?.let(ProjectJson::decode) ?: CharacterProject())
    } catch (_: Exception) {
        // Do not overwrite a corrupt/incompatible file just by launching the app.
        EditorUiState(message = "Could not restore the saved project. Demo loaded; the saved file is unchanged until you edit.")
    }

    private fun save(project: CharacterProject) {
        if (storage == null) return
        try {
            storage.write(ProjectJson.encode(project))
            mutableState.update { it.copy(saveError = null) }
        } catch (_: Exception) {
            mutableState.update { it.copy(saveError = "Changes are not saved locally. Export a backup or retry saving.") }
        }
    }

    fun retrySave() = save(state.value.project)
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
        // Locks and navigation remain session state; only the authored project is replaced.
        mutableState.update { it.copy(project = project) }
        save(project)
        showMessage(if (state.value.saveError == null) "Project imported" else "Project imported, but local saving failed. Export a backup.")
    }
}
