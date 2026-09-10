package com.kelvinsaputra.promptstudio.history

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One atomic metadata envelope per record; no global index to truncate or lose. */
class FileGenerationHistoryStore(private val directory: File) : GenerationHistoryStore {
    private val mutex = Mutex()
    private fun metadata(record: GenerationRecord) = File(directory, "${record.id}.json")
    private fun image(record: GenerationRecord) = File(directory, record.imageReference)
    private fun atomic(file: File, bytes: ByteArray) {
        check(directory.isDirectory || directory.mkdirs())
        val temp = Files.createTempFile(directory.toPath(), ".pending-", ".tmp")
        try {
            temp.toFile().outputStream().use { it.write(bytes); it.fd.sync() }
            Files.move(temp, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally { Files.deleteIfExists(temp) }
    }
    override suspend fun list() = withContext(Dispatchers.IO) { mutex.withLock {
        if (!directory.exists()) return@withLock HistoryListing(emptyList())
        val files = checkNotNull(directory.listFiles())
        var corrupt = 0
        val records = files.filter { it.extension == "json" }.mapNotNull { file ->
            try { HistoryJson.decode(file.readText()).also { require(file.name == "${it.id}.json") } }
            catch (_: Exception) { corrupt++; null }
        }
        HistoryListing(records.chronological(), corrupt)
    } }
    override suspend fun save(record: GenerationRecord, image: ByteArray) = withContext(Dispatchers.IO) { mutex.withLock {
        check(!metadata(record).exists() && !image(record).exists())
        atomic(image(record), image)
        try { atomic(metadata(record), HistoryJson.encode(record).encodeToByteArray()) }
        catch (e: Exception) { Files.deleteIfExists(image(record).toPath()); throw e }
    } }
    override suspend fun loadImage(record: GenerationRecord): ByteArray? = withContext(Dispatchers.IO) { mutex.withLock {
        image(record).takeIf { it.exists() }?.readBytes()
    } }
    override suspend fun delete(record: GenerationRecord) = withContext(Dispatchers.IO) { mutex.withLock {
        // Retain metadata on image-delete failure so the user can retry. Missing images remain inspectable.
        Files.deleteIfExists(image(record).toPath())
        Files.deleteIfExists(metadata(record).toPath())
        Unit
    } }
}
