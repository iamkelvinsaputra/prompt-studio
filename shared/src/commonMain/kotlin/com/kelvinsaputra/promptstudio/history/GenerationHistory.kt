package com.kelvinsaputra.promptstudio.history

import androidx.compose.runtime.Composable
import com.kelvinsaputra.promptstudio.domain.CharacterProject
import com.kelvinsaputra.promptstudio.generation.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class GenerationRecord(
    val id: String,
    val createdAt: Long,
    val metadata: GenerationMetadata,
    val mimeType: String,
    val imageReference: String,
    val version: Int = 1,
) {
    init {
        require(version == 1) { "Unsupported history version" }
        require(id.matches(Regex("[a-zA-Z0-9-]{1,80}")))
        require(imageReference == "$id.image")
        require(mimeType in listOf("image/png", "image/jpeg", "image/webp"))
        require(metadata.project.version == 1)
    }
    val timeLabel: String get() = Instant.fromEpochMilliseconds(createdAt).toString()
    fun restoredInto(current: CharacterProject) = metadata.project.copy(id = current.id, name = current.name)
    companion object {
        fun capture(image: GeneratedImage): GenerationRecord {
            val id = Uuid.random().toString()
            return GenerationRecord(id, Clock.System.now().toEpochMilliseconds(), image.metadata, image.mimeType, "$id.image")
        }
    }
}
object HistoryJson {
    private val json = Json { encodeDefaults = true; prettyPrint = true }
    fun encode(record: GenerationRecord) = json.encodeToString(record)
    fun decode(text: String) = json.decodeFromString<GenerationRecord>(text)
}
data class HistoryListing(val records: List<GenerationRecord>, val unreadableCount: Int = 0)
fun List<GenerationRecord>.chronological(characterId: String? = null) =
    filter { characterId == null || it.metadata.project.id == characterId }
        .sortedWith(compareByDescending<GenerationRecord> { it.createdAt }.thenBy { it.id })
interface GenerationHistoryStore {
    suspend fun list(): HistoryListing
    suspend fun save(record: GenerationRecord, image: ByteArray)
    suspend fun loadImage(record: GenerationRecord): ByteArray?
    suspend fun delete(record: GenerationRecord)
}
@Composable expect fun rememberGenerationHistoryStore(): GenerationHistoryStore
