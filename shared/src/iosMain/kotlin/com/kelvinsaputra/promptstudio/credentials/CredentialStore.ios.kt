package com.kelvinsaputra.promptstudio.credentials
import androidx.compose.runtime.*
/** Explicit session-only policy: no key enters preferences, files, backups, or exports. */
@Composable actual fun rememberCredentialStore(): CredentialStore = remember { SessionCredentialStore() }
