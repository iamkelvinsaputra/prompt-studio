package com.kelvinsaputra.promptstudio.credentials
import androidx.compose.runtime.*
/** Native Keychain bridging is deliberately deferred; no command-line key arguments or plaintext files. */
@Composable actual fun rememberCredentialStore(): CredentialStore = remember { SessionCredentialStore() }
