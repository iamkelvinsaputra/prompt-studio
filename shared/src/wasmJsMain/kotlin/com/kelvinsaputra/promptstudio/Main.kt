package com.kelvinsaputra.promptstudio
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
@OptIn(ExperimentalComposeUiApi::class)
fun main() { ComposeViewport { App() } }
actual fun getPlatform(): Platform = object : Platform { override val name = "Web / Wasm" }
