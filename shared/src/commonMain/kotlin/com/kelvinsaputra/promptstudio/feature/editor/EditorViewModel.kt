package com.kelvinsaputra.promptstudio.feature.editor

import androidx.lifecycle.ViewModel
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
) {
    val compiledPrompt get() = PromptCompiler().compile(project)
}

class EditorViewModel(private val randomizer: EditorRandomizer = EditorRandomizer()) : ViewModel() {
    private val mutableState = MutableStateFlow(EditorUiState())
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
        mutableState.update { it.copy(project = it.project.transform()) }
    }
}
