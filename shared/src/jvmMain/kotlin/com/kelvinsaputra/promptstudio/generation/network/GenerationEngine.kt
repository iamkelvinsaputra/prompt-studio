package com.kelvinsaputra.promptstudio.generation.network
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
actual fun generationEngine(): HttpClientEngine = CIO.create { endpoint { connectAttempts = 1 } }
