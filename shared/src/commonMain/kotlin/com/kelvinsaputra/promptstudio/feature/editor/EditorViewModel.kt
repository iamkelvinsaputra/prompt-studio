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
) {
    val compiledPrompt get() = PromptCompiler().compile(project)
}

class EditorViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(EditorUiState())
    val state = mutableState.asStateFlow()

    fun selectModule(module: EditorModule) { mutableState.update { it.copy(module = module) } }
    fun setStyle(style: ArtStylePreset) = edit { copy(style = style) }
    fun setSubject(subject: String) = edit { copy(subject = subject) }
    fun setCostume(costume: CostumeConfiguration) = edit { copy(costume = costume) }
    fun setPose(pose: PoseConfiguration) = edit { copy(pose = pose) }
    fun setOutput(output: OutputConfiguration) = edit { copy(output = output) }

    private fun edit(transform: CharacterProject.() -> CharacterProject) {
        mutableState.update { it.copy(project = it.project.transform()) }
    }
}
