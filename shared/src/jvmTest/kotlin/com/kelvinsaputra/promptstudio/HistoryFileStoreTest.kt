package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.CharacterProject
import com.kelvinsaputra.promptstudio.generation.model.*
import com.kelvinsaputra.promptstudio.history.*
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.io.File
import kotlin.test.*

class HistoryFileStoreTest {
    private fun record() = GenerationRecord.capture(GeneratedImage(byteArrayOf(1), "image/png",
        GenerationMetadata(ImageProviderId.OpenAI, ImageGenerationRequest("exact prompt", "1:1", "gpt-image-2", EffectiveOutput("1:1", "1024x1024")), CharacterProject(), outputFormat = "png")))
    @Test fun restartCorruptionMissingImageAndDeletion() = runTest {
        val directory = Files.createTempDirectory("prompt-history-test").toFile()
        try {
            val record = record(); val store = FileGenerationHistoryStore(directory)
            store.save(record, byteArrayOf(1,2,3))
            File(directory, "corrupt.json").writeText("{invalid")
            val restarted = FileGenerationHistoryStore(directory)
            assertEquals(listOf(record), restarted.list().records); assertEquals(1, restarted.list().unreadableCount)
            assertContentEquals(byteArrayOf(1,2,3), restarted.loadImage(record))
            assertFalse(File(directory, "${record.id}.json").readText().contains("bytes"))
            File(directory, record.imageReference).delete()
            assertNull(restarted.loadImage(record)); assertEquals(listOf(record), restarted.list().records)
            restarted.delete(record); assertTrue(restarted.list().records.isEmpty())
            assertTrue(File(directory, "corrupt.json").exists())
        } finally { directory.deleteRecursively() }
    }
    @Test fun preflightFailureCreatesNoImageAndDuplicateSavePreservesOriginal() = runTest {
        val directory = Files.createTempDirectory("prompt-history-test").toFile()
        try {
            val store = FileGenerationHistoryStore(directory); val saved = record()
            store.save(saved, byteArrayOf(7))
            assertFails { store.save(saved, byteArrayOf(8)) }
            assertContentEquals(byteArrayOf(7), store.loadImage(saved))
            val blocked = record()
            // An occupied metadata destination must never create a dangling image.
            val destination = File(directory, "${blocked.id}.json")
            destination.mkdir(); File(destination, "child").writeText("keep")
            // Preflight rejects existing destinations without creating an image.
            assertFails { store.save(blocked, byteArrayOf(9)) }
            assertFalse(File(directory, blocked.imageReference).exists())
        } finally { directory.deleteRecursively() }
    }
}
