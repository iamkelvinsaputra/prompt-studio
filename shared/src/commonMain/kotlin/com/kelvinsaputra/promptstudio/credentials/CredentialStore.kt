package com.kelvinsaputra.promptstudio.credentials

import androidx.compose.runtime.Composable
import com.kelvinsaputra.promptstudio.generation.model.ImageProviderId

/** Intentionally not serializable and never part of UI state or metadata. */
class ProviderCredentials internal constructor(internal val apiKey: String) {
    override fun toString() = "ProviderCredentials([redacted])"
}
interface CredentialStore {
    val supportsSecurePersistence: Boolean
    fun get(provider: ImageProviderId): ProviderCredentials?
    fun set(provider: ImageProviderId, key: String, remember: Boolean = false)
    fun remove(provider: ImageProviderId)
    fun clearSession()
}
open class SessionCredentialStore : CredentialStore {
    private val keys = mutableMapOf<ImageProviderId, ProviderCredentials>()
    override val supportsSecurePersistence = false
    override fun get(provider: ImageProviderId) = keys[provider]
    override fun set(provider: ImageProviderId, key: String, remember: Boolean) {
        require(!remember) { "Secure persistence is unavailable." }
        if (key.isBlank()) remove(provider) else keys[provider] = ProviderCredentials(key.trim())
    }
    override fun remove(provider: ImageProviderId) { keys.remove(provider) }
    override fun clearSession() { keys.clear() }
}
@Composable expect fun rememberCredentialStore(): CredentialStore
