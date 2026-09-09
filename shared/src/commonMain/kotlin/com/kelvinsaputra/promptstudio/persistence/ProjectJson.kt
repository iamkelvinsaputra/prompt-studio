package com.kelvinsaputra.promptstudio.persistence

import com.kelvinsaputra.promptstudio.domain.CharacterProject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

class ProjectFileException(message: String) : IllegalArgumentException(message)

/** One file format for autosave, import, and export. No editor state or compiled text. */
object ProjectJson {
    const val VERSION = 1
    const val MAX_BYTES = 1_048_576
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    fun encode(project: CharacterProject): String {
        require(project.version == VERSION) { "Unsupported project version." }
        return json.encodeToString(project).also {
            if (it.encodeToByteArray().size > MAX_BYTES) throw ProjectFileException("Project exceeds the 1 MB file limit.")
        }
    }

    fun decode(text: String): CharacterProject {
        if (text.encodeToByteArray().size > MAX_BYTES) throw ProjectFileException("Project exceeds the 1 MB file limit.")
        try {
            val root = json.parseToJsonElement(text) as? JsonObject
                ?: throw ProjectFileException("Not a Prompt Studio project object.")
            val version = root["version"] as? JsonPrimitive
            if (version == null || version.isString || version.intOrNull == null) {
                throw ProjectFileException("Project version is missing or invalid.")
            }
            if (version.intOrNull != VERSION) throw ProjectFileException("Unsupported project version ${version.content}. This app supports version $VERSION.")
            if (!root.keys.containsAll(listOf("style", "subject", "costume", "pose", "output"))) {
                throw ProjectFileException("Project is missing required sections.")
            }
            // Unknown enum values, wrong types, and domain invariants (including the marker limit)
            // fail here. Do not coerce unknown selections into defaults.
            return json.decodeFromJsonElement<CharacterProject>(root)
        } catch (e: ProjectFileException) {
            throw e
        } catch (_: IllegalArgumentException) {
            throw ProjectFileException("Invalid project JSON or configuration.")
        }
    }
}
