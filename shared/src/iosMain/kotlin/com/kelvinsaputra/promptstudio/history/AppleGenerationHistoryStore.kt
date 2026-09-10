@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.kelvinsaputra.promptstudio.history
import androidx.compose.runtime.*
import com.kelvinsaputra.promptstudio.platform.*
import platform.Foundation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppleGenerationHistoryStore(private val directory: String = "${appDirectory()}/history") : GenerationHistoryStore {
    private val mutex = Mutex()
    override suspend fun list(): HistoryListing = withContext(Dispatchers.IO) { mutex.withLock {
        if (!NSFileManager.defaultManager.fileExistsAtPath(directory)) return@withLock HistoryListing(emptyList())
        val names = checkNotNull(NSFileManager.defaultManager.contentsOfDirectoryAtPath(directory, null))
        var corrupt = 0
        val records = names.filterIsInstance<String>().filter { it.endsWith(".json") }.mapNotNull { name ->
            try { HistoryJson.decode(checkNotNull(readBytes("$directory/$name")).decodeToString()).also { require(name == "${it.id}.json") } }
            catch (_: Exception) { corrupt++; null }
        }
        HistoryListing(records.chronological(), corrupt)
    } }
    override suspend fun save(record: GenerationRecord, image: ByteArray) = withContext(Dispatchers.IO) { mutex.withLock {
        check(NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null))
        val path = "$directory/${record.imageReference}"
        check(!NSFileManager.defaultManager.fileExistsAtPath(path) &&
            !NSFileManager.defaultManager.fileExistsAtPath("$directory/${record.id}.json"))
        atomicWrite(path, image)
        try { atomicWrite("$directory/${record.id}.json", HistoryJson.encode(record).encodeToByteArray()) }
        catch (e: Exception) { removeFile(path); throw e }
    } }
    override suspend fun loadImage(record: GenerationRecord): ByteArray? = withContext(Dispatchers.IO) { mutex.withLock { readBytes("$directory/${record.imageReference}") } }
    override suspend fun delete(record: GenerationRecord) = withContext(Dispatchers.IO) { mutex.withLock {
        removeFile("$directory/${record.imageReference}")
        removeFile("$directory/${record.id}.json")
    } }
}
@Composable actual fun rememberGenerationHistoryStore(): GenerationHistoryStore = remember { AppleGenerationHistoryStore() }
