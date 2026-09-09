package com.kelvinsaputra.promptstudio.domain

import kotlinx.serialization.Serializable

/** A small, local-only index. The characters themselves remain portable CharacterProject JSON. */
@Serializable
data class CharacterLibrary(
    val activeCharacterId: String = CharacterProject().id,
    val characters: List<CharacterProject> = listOf(CharacterProject()),
) {
    init {
        require(characters.isNotEmpty()) { "A character library needs one active character." }
        require(characters.map { it.id }.distinct().size == characters.size) { "Character ids must be unique." }
        require(characters.any { it.id == activeCharacterId }) { "Active character is missing from the library." }
    }

    val active: CharacterProject get() = characters.first { it.id == activeCharacterId }

    fun replaceActive(project: CharacterProject): CharacterLibrary = copy(
        characters = characters.map { if (it.id == activeCharacterId) project.copy(id = activeCharacterId) else it },
    )

    fun select(id: String): CharacterLibrary = if (characters.any { it.id == id }) copy(activeCharacterId = id) else this

    fun nextId(): String {
        var candidate = 1
        val ids = characters.map { it.id }.toSet()
        while ("character-$candidate" in ids) candidate++
        return "character-$candidate"
    }

    fun add(project: CharacterProject): CharacterLibrary {
        val added = project.copy(id = nextId())
        return copy(activeCharacterId = added.id, characters = characters + added)
    }

    fun duplicateActive(): CharacterLibrary {
        val duplicate = active.copy(id = nextId(), name = "${active.name} Copy")
        return copy(activeCharacterId = duplicate.id, characters = characters + duplicate)
    }

    /** Keep a usable empty project after deleting the final entry. */
    fun deleteActive(): CharacterLibrary {
        if (characters.size == 1) {
            val replacement = newCharacter(nextId())
            return CharacterLibrary(replacement.id, listOf(replacement))
        }
        val retained = characters.filterNot { it.id == activeCharacterId }
        return copy(activeCharacterId = retained.first().id, characters = retained)
    }

    companion object {
        fun newCharacter(id: String, name: String = "New Character") = CharacterProject(
            id = id,
            name = name,
            subject = "",
            costume = CostumeConfiguration(
                outfitIdentity = "", silhouette = null, outerwear = null, innerwear = null, lowerWear = null,
                legwear = null, footwear = null, handwear = null, utility = null, customization = emptySet(),
                materialFeel = "", exposureLevel = "",
            ),
            pose = PoseConfiguration(
                basePose = null, weight = null, legAction = "", torso = null, arms = null, head = null,
                gaze = null, energy = null, motionDirection = "",
            ),
            output = OutputConfiguration(
                type = OutputType.CUSTOM, customAspectRatio = "", customIntent = "illustration", framing = null,
                figurePlacement = "", negativeSpace = "", clockSafe = false, iconSafe = false,
            ),
        )
    }
}
