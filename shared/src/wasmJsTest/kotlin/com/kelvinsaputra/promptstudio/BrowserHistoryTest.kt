package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.CharacterProject
import com.kelvinsaputra.promptstudio.generation.model.*
import com.kelvinsaputra.promptstudio.history.*
import com.kelvinsaputra.promptstudio.platform.platformCapabilities
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class BrowserHistoryTest {
    @Test fun indexedDbPersistsBinarySeparatelyAndDeletesBothInTransaction() = runTest {
        assertFalse(platformCapabilities.canGenerateWithByok)
        val store = BrowserGenerationHistoryStore()
        val record = GenerationRecord.capture(GeneratedImage(byteArrayOf(1), "image/png", GenerationMetadata(
            ImageProviderId.OpenAI, ImageGenerationRequest("historical fixture", "1:1", "gpt-image-2", EffectiveOutput("1:1", "1024x1024")), CharacterProject(), outputFormat = "png")))
        try {
            store.save(record, byteArrayOf(1,2,3,127,-1))
            val reopened = BrowserGenerationHistoryStore()
            assertTrue(reopened.list().records.any { it == record })
            assertContentEquals(byteArrayOf(1,2,3,127,-1), reopened.loadImage(record))
            assertFails { reopened.save(record, byteArrayOf(9)) }
            assertContentEquals(byteArrayOf(1,2,3,127,-1), reopened.loadImage(record))
            reopened.delete(record)
            assertNull(reopened.loadImage(record))
            assertFalse(reopened.list().records.any { it.id == record.id })
        } finally { store.delete(record) }
    }
}
