package com.kelvinsaputra.promptstudio

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "PromptStudio",
    ) {
        App()
    }
}