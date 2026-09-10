package com.kelvinsaputra.promptstudio.domain

import kotlinx.serialization.Serializable

@Serializable
enum class PromptMode { Automatic, Manual }

/** Authored inputs only. Compiled output is derived and is never stored here. */
@Serializable
data class PromptAuthoring(
    val adjustmentText: String = "",
    val mode: PromptMode = PromptMode.Automatic,
    val manualDraft: String? = null,
) {
    init { require(mode != PromptMode.Manual || manualDraft != null) { "Manual mode requires a draft." } }
}
