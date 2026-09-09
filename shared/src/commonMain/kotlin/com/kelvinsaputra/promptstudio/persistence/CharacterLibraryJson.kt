package com.kelvinsaputra.promptstudio.persistence

import com.kelvinsaputra.promptstudio.domain.CharacterLibrary
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/** Separate autosave envelope; importing and exporting still uses one portable CharacterProject. */
object CharacterLibraryJson {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    fun encode(library: CharacterLibrary): String = json.encodeToString(library).also {
        if (it.encodeToByteArray().size > ProjectJson.MAX_BYTES) {
            throw ProjectFileException("Character library exceeds the 1 MB file limit.")
        }
    }

    fun decode(text: String): CharacterLibrary {
        if (text.encodeToByteArray().size > ProjectJson.MAX_BYTES) {
            throw ProjectFileException("Character library exceeds the 1 MB file limit.")
        }
        try {
            val root = json.parseToJsonElement(text) as? JsonObject
                ?: throw ProjectFileException("Not a Prompt Studio character library.")
            if (!root.keys.containsAll(listOf("activeCharacterId", "characters"))) {
                throw ProjectFileException("Character library is missing required sections.")
            }
            return json.decodeFromJsonElement(root)
        } catch (e: ProjectFileException) {
            throw e
        } catch (_: IllegalArgumentException) {
            throw ProjectFileException("Invalid character library JSON.")
        }
    }
}
