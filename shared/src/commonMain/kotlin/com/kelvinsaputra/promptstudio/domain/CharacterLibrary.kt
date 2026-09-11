package com.kelvinsaputra.promptstudio.domain

import kotlinx.serialization.Serializable

/** A small, local-only index. The characters themselves remain portable CharacterProject JSON. */
@Serializable
data class CharacterLibrary(
    val activeCharacterId: String = CharacterProject().id,
    val characters: List<CharacterProject> = listOf(CharacterProject()),
    val variants: List<ProjectVariants> = emptyList(),
    val recentProjectIds: List<String> = emptyList(),
    val schemaVersion: Int = 2,
    val presets: List<SavedVisualPreset> = emptyList(),
) {
    init {
        require(characters.isNotEmpty()) { "A character library needs one active character." }
        require(characters.map { it.id }.distinct().size == characters.size) { "Character ids must be unique." }
        require(characters.any { it.id == activeCharacterId }) { "Active character is missing from the library." }
        require(schemaVersion == 2) { "Unsupported library version." }
        require(presets.map { it.id }.distinct().size == presets.size)
        require(variants.map { it.projectId }.distinct().size == variants.size)
        require(variants.all { entry -> characters.any { it.id == entry.projectId } })
    }

    val activeVariants get() = variants.firstOrNull { it.projectId == activeCharacterId } ?: ProjectVariants(activeCharacterId)
    val active: CharacterProject get() = project(activeCharacterId)
    fun project(id: String): CharacterProject {
        val base = characters.first { it.id == id }
        return variants.firstOrNull { it.projectId == id }?.selected?.scene?.applyTo(base) ?: base
    }
    val recentProjects get() = (recentProjectIds + characters.map { it.id }).distinct().mapNotNull { id ->
        characters.firstOrNull { it.id == id }?.let { project(id) }
    }

    fun replaceActive(project: CharacterProject, preserveIdentityLocks: Boolean = true): CharacterLibrary {
        val base = characters.first { it.id == activeCharacterId }
        val state = activeVariants
        val protected = if (preserveIdentityLocks) project.preservingIdentityOf(active) else project
        val updated = if (state.activeId == PRIMARY_VARIANT) protected else VariantScene.capture(base).applyTo(protected)
        val library = copy(characters = characters.map { if (it.id == activeCharacterId) updated.copy(id = activeCharacterId) else it },
            recentProjectIds = listOf(activeCharacterId) + recentProjectIds.filterNot { it == activeCharacterId })
        return if (state.activeId == PRIMARY_VARIANT) library else library.withVariants(state.copy(
            alternatives = state.alternatives.map { if (it.id == state.activeId) it.copy(scene = VariantScene.capture(protected)) else it },
        ))
    }

    fun withVariants(value: ProjectVariants): CharacterLibrary = copy(variants = variants.filterNot { it.projectId == value.projectId } + value)
    fun selectVariant(id: String): CharacterLibrary {
        val value = activeVariants
        return if (id == PRIMARY_VARIANT || value.alternatives.any { it.id == id }) withVariants(value.copy(activeId = id)) else this
    }
    fun addVariant(name: String, type: OutputType): CharacterLibrary {
        if (name.isBlank()) return this
        val scene = VariantScene.capture(active.copy(output = active.output.copy(type = type)))
        val added = ProjectVariant(nextId(), name.trim(), scene, activeVariants.generation)
        return withVariants(activeVariants.copy(activeId = added.id, alternatives = activeVariants.alternatives + added))
    }
    fun renameVariant(name: String): CharacterLibrary {
        if (name.isBlank()) return this
        val value = activeVariants
        return withVariants(if (value.activeId == PRIMARY_VARIANT) value.copy(primaryName = name.trim()) else value.copy(
            alternatives = value.alternatives.map { if (it.id == value.activeId) it.copy(name = name.trim()) else it }))
    }
    fun deleteVariant(): CharacterLibrary {
        val value = activeVariants
        if (value.activeId == PRIMARY_VARIANT) return this
        return withVariants(value.copy(activeId = PRIMARY_VARIANT, alternatives = value.alternatives.filterNot { it.id == value.activeId }))
    }
    fun setGeneration(value: GenerationPreferences): CharacterLibrary {
        val state = activeVariants
        return withVariants(if (state.activeId == PRIMARY_VARIANT) state.copy(primaryGeneration = value) else state.copy(
            alternatives = state.alternatives.map { if (it.id == state.activeId) it.copy(generation = value) else it }))
    }

    fun select(id: String): CharacterLibrary = if (characters.any { it.id == id }) copy(activeCharacterId = id,
        recentProjectIds = listOf(id) + recentProjectIds.filterNot { it == id }) else this

    // Never reuse deleted identities: history keeps references after character deletion.
    fun nextId(): String = kotlin.uuid.Uuid.random().toString()

    fun add(project: CharacterProject): CharacterLibrary {
        val added = project.copy(id = nextId())
        return copy(activeCharacterId = added.id, characters = characters + added, recentProjectIds = listOf(added.id) + recentProjectIds)
    }

    fun duplicateActive(): CharacterLibrary {
        val duplicate = characters.first { it.id == activeCharacterId }.copy(id = nextId(), name = "${active.name} Copy")
        val state = activeVariants
        val alternatives = state.alternatives.map { it.copy(id = nextId()) }
        val selected = state.alternatives.indexOfFirst { it.id == state.activeId }
        return copy(activeCharacterId = duplicate.id, characters = characters + duplicate,
            recentProjectIds = listOf(duplicate.id) + recentProjectIds,
            variants = variants + state.copy(projectId = duplicate.id, alternatives = alternatives,
                activeId = alternatives.getOrNull(selected)?.id ?: PRIMARY_VARIANT))
    }

    /** Keep a usable empty project after deleting the final entry. */
    fun deleteActive(): CharacterLibrary {
        if (characters.size == 1) {
            val replacement = newCharacter(nextId()).withStudioDefaults()
            return copy(activeCharacterId = replacement.id, characters = listOf(replacement), variants = emptyList(), recentProjectIds = emptyList())
        }
        val retained = characters.filterNot { it.id == activeCharacterId }
        return copy(activeCharacterId = retained.first().id, characters = retained,
            variants = variants.filterNot { it.projectId == activeCharacterId }, recentProjectIds = recentProjectIds.filterNot { it == activeCharacterId })
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
