package com.kelvinsaputra.promptstudio.platform

import androidx.compose.runtime.*
import com.kelvinsaputra.promptstudio.generation.network.generationClient
import com.kelvinsaputra.promptstudio.generation.provider.*
actual val platformCapabilities = PlatformCapabilities(canGenerateWithByok = true)
@Composable actual fun rememberGenerationBackend(): GenerationBackend = remember {
    val client = generationClient()
    GenerationBackend(listOf(OpenAiImageGenerationProvider(client), GeminiImageGenerationProvider(client)), client::close)
}
