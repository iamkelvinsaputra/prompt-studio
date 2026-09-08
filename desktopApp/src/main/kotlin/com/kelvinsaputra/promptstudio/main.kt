package com.kelvinsaputra.promptstudio

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Prompt Studio",
        state = rememberWindowState(width = 1440.dp, height = 900.dp),
    ) {
        App()
    }
}
