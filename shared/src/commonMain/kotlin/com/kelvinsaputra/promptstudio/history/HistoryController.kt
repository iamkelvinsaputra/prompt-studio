package com.kelvinsaputra.promptstudio.history

import com.kelvinsaputra.promptstudio.generation.model.GeneratedImage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class HistoryState(val records: List<GenerationRecord> = emptyList(), val message: String? = null, val loading: Boolean = true)
class HistoryController(private val store: GenerationHistoryStore) {
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(HistoryState())
    val state = mutable.asStateFlow()
    private suspend fun reload() {
        val listing = store.list()
        mutable.value = HistoryState(listing.records.chronological(),
            if (listing.unreadableCount > 0) "${listing.unreadableCount} history entries could not be read. Their files have been preserved." else null, false)
    }
    suspend fun refresh() = mutex.withLock {
        try { reload() } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(loading = false, message = "Could not load history. Stored files have been preserved.") } }
    }
    suspend fun save(image: GeneratedImage) = mutex.withLock {
        val record = GenerationRecord.capture(image)
        store.save(record, image.bytes)
        mutable.update { it.copy(records = (it.records + record).chronological(), loading = false) }
    }
    suspend fun loadImage(record: GenerationRecord) = mutex.withLock { store.loadImage(record) }
    suspend fun delete(record: GenerationRecord) = mutex.withLock {
        try { store.delete(record); reload() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(message = "Could not fully delete this history entry. Retry deletion.") } }
    }
}
