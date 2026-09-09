package com.kelvinsaputra.promptstudio.generation.network
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
actual fun generationEngine(): HttpClientEngine = OkHttp.create { config { retryOnConnectionFailure(false); followRedirects(false); followSslRedirects(false) } }
