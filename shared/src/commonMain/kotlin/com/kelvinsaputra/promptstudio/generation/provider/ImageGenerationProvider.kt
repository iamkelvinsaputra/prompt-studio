package com.kelvinsaputra.promptstudio.generation.provider

import com.kelvinsaputra.promptstudio.credentials.ProviderCredentials
import com.kelvinsaputra.promptstudio.generation.model.*

interface ImageGenerationProvider {
    val id: ImageProviderId
    val supportsVisualGuide: Boolean get() = false
    suspend fun generate(request: ImageGenerationRequest, credentials: ProviderCredentials): ProviderImage
}
