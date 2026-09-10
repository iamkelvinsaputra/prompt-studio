package com.kelvinsaputra.promptstudio
import androidx.compose.ui.window.ComposeUIViewController
fun MainViewController() = ComposeUIViewController { App() }
actual fun getPlatform(): Platform = object : Platform { override val name = "iOS / iPadOS" }
