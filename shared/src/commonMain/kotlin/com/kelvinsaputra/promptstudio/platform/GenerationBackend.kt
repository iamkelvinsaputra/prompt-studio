package com.kelvinsaputra.promptstudio.platform

import androidx.compose.runtime.Composable
import com.kelvinsaputra.promptstudio.generation.provider.ImageGenerationProvider

data class PlatformCapabilities(val canGenerateWithByok: Boolean, val canSaveImage: Boolean = true)
expect val platformCapabilities: PlatformCapabilities
class GenerationBackend(val providers: List<ImageGenerationProvider>, val close: () -> Unit = {})
@Composable expect fun rememberGenerationBackend(): GenerationBackend
