@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.CharacterProject
import com.kelvinsaputra.promptstudio.generation.model.*
import com.kelvinsaputra.promptstudio.history.*
import com.kelvinsaputra.promptstudio.platform.AppleProjectStorage
import com.kelvinsaputra.promptstudio.persistence.ProjectJson
import platform.Foundation.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class AppleStorageTest {
    @Test fun projectAndHistorySurviveReopeningAndMissingImageIsSafe() = runTest {
        val directory = "${NSTemporaryDirectory()}prompt-studio-test-${NSUUID().UUIDString}"
        NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null)
        try {
            val path = "$directory/project.json"
            AppleProjectStorage(path).write(ProjectJson.encode(CharacterProject(name = "Apple fixture")))
            assertEquals("Apple fixture", ProjectJson.decode(AppleProjectStorage(path).read()!!).name)
            val record = GenerationRecord.capture(GeneratedImage(byteArrayOf(1), "image/png", GenerationMetadata(
                ImageProviderId.OpenAI, ImageGenerationRequest("fixture", "1:1", "gpt-image-2", EffectiveOutput("1:1", "1024x1024")), CharacterProject(), outputFormat = "png")))
            val history = AppleGenerationHistoryStore("$directory/history")
            history.save(record, byteArrayOf(1,2,3))
            val reopened = AppleGenerationHistoryStore("$directory/history")
            assertEquals(listOf(record), reopened.list().records)
            assertContentEquals(byteArrayOf(1,2,3), reopened.loadImage(record))
            NSFileManager.defaultManager.removeItemAtPath("$directory/history/${record.imageReference}", null)
            assertNull(reopened.loadImage(record)); assertEquals(listOf(record), reopened.list().records)
            reopened.delete(record); assertTrue(reopened.list().records.isEmpty())
        } finally { NSFileManager.defaultManager.removeItemAtPath(directory, null) }
    }
}
