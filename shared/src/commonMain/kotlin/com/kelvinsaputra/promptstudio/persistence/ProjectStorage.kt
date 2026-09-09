package com.kelvinsaputra.promptstudio.persistence

/** Tiny local file boundary. Implementations replace the previous file atomically. */
interface ProjectStorage {
    fun read(): String?
    fun write(json: String)
}
