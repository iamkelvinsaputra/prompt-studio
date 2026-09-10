package com.kelvinsaputra.promptstudio.credentials
import androidx.compose.runtime.*
import com.kelvinsaputra.promptstudio.generation.model.ImageProviderId
/** Web cannot accept or retain provider credentials, including in memory. */
@Composable actual fun rememberCredentialStore(): CredentialStore = remember { object : CredentialStore {
    override val supportsSecurePersistence = false
    override fun get(provider: ImageProviderId): ProviderCredentials? = null
    override fun set(provider: ImageProviderId, key: String, remember: Boolean) { error("Browser BYOK is unavailable") }
    override fun remove(provider: ImageProviderId) = Unit
    override fun clearSession() = Unit
} }
