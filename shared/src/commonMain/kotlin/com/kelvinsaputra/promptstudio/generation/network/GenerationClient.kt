package com.kelvinsaputra.promptstudio.generation.network

import io.ktor.client.*
import io.ktor.client.plugins.*

object GenerationTimeouts {
    const val REQUEST_MS = 300_000L
    const val CONNECT_MS = 30_000L
}
expect fun generationEngine(): io.ktor.client.engine.HttpClientEngine
fun generationClient() = HttpClient(generationEngine()) {
    expectSuccess = false
    followRedirects = false
    install(HttpTimeout) {
        requestTimeoutMillis = GenerationTimeouts.REQUEST_MS
        socketTimeoutMillis = GenerationTimeouts.REQUEST_MS
        connectTimeoutMillis = GenerationTimeouts.CONNECT_MS
    }
    // No logging or automatic retries: each POST may incur a charge.
}
