package com.kelvinsaputra.promptstudio.prompt

import com.kelvinsaputra.promptstudio.domain.*

/** The only output selection rule. An empty manual draft stays empty; it never falls back to auto. */
fun CharacterProject.effectivePrompt(): String = when (promptAuthoring.mode) {
    PromptMode.Automatic -> PromptCompiler().compile(this).text
    PromptMode.Manual -> requireNotNull(promptAuthoring.manualDraft)
}

/** Every fresh entry starts from current automatic compilation, never from previously edited output. */
fun CharacterProject.enterManualPrompt(): CharacterProject = if (promptAuthoring.mode == PromptMode.Manual) this else
    copy(promptAuthoring = promptAuthoring.copy(mode = PromptMode.Manual, manualDraft = PromptCompiler().compile(this).text))

fun CharacterProject.useAutomaticPrompt(): CharacterProject = copy(promptAuthoring = promptAuthoring.copy(mode = PromptMode.Automatic))

/** Restoring a saved draft is a separate, explicit action. */
fun CharacterProject.resumeManualDraft(): CharacterProject = if (promptAuthoring.manualDraft == null) this else
    copy(promptAuthoring = promptAuthoring.copy(mode = PromptMode.Manual))

fun CharacterProject.editManualPrompt(text: String): CharacterProject = if (promptAuthoring.mode != PromptMode.Manual) this else
    copy(promptAuthoring = promptAuthoring.copy(manualDraft = text))
