package com.kelvinsaputra.promptstudio.generation.network
import io.ktor.client.engine.darwin.Darwin
actual fun generationEngine() = Darwin.create()
