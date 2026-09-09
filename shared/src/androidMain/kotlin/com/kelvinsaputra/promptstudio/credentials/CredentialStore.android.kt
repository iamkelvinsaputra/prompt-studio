package com.kelvinsaputra.promptstudio.credentials

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.kelvinsaputra.promptstudio.generation.model.ImageProviderId
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** OS-backed AES-GCM wrapping; encrypted blobs live outside Android backups. */
class AndroidCredentialStore(private val directory: File) : CredentialStore {
    private val session = SessionCredentialStore()
    override val supportsSecurePersistence = true
    private fun file(provider: ImageProviderId) = AtomicFile(File(directory, "credential-${provider.name}"))
    private fun key(provider: ImageProviderId): SecretKey {
        val alias = "prompt-studio-${provider.name}"
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }
    override fun get(provider: ImageProviderId): ProviderCredentials? {
        session.get(provider)?.let { return it }
        val saved = file(provider)
        if (!saved.baseFile.exists()) return null
        val bytes = saved.readFully()
        require(bytes.size > 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(provider), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        val plain = cipher.doFinal(bytes.copyOfRange(12, bytes.size))
        try { session.set(provider, plain.toString(Charsets.UTF_8)) } finally { plain.fill(0) }
        return session.get(provider)
    }
    override fun set(provider: ImageProviderId, key: String, remember: Boolean) {
        if (key.isBlank()) { remove(provider); return }
        if (remember) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key(provider))
            val plain = key.trim().toByteArray(Charsets.UTF_8)
            val encrypted = try { cipher.doFinal(plain) } finally { plain.fill(0) }
            val saved = file(provider)
            val stream = saved.startWrite()
            try { stream.write(cipher.iv); stream.write(encrypted); saved.finishWrite(stream) }
            catch (e: Exception) { saved.failWrite(stream); throw e }
        } else file(provider).delete()
        session.set(provider, key)
    }
    override fun remove(provider: ImageProviderId) {
        file(provider).delete()
        check(!file(provider).baseFile.exists())
        session.remove(provider)
    }
    override fun clearSession() = session.clearSession()
}
@Composable actual fun rememberCredentialStore(): CredentialStore {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidCredentialStore(context.noBackupFilesDir) }
}
