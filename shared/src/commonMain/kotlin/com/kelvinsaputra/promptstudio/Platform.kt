package com.kelvinsaputra.promptstudio

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform