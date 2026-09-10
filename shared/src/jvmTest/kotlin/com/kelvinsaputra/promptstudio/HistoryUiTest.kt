package com.kelvinsaputra.promptstudio

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.kelvinsaputra.promptstudio.credentials.*
import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.generation.*
import com.kelvinsaputra.promptstudio.generation.model.*
import com.kelvinsaputra.promptstudio.generation.provider.*
import com.kelvinsaputra.promptstudio.history.*
import kotlinx.coroutines.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files
import kotlin.io.encoding.Base64
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class HistoryUiTest {
    @get:Rule val rule = createComposeRule()
    @Test fun historyDetailRestoreGenerateAgainAndDeleteAreUsable() {
        val directory = Files.createTempDirectory("prompt-history-ui").toFile()
        val bytes = Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=")
        val model = ImageModels.default(ImageProviderId.OpenAI)
        val request = ImageGenerationRequest("Exact historical prompt", "1:1", model.id, model.outputFor("1:1")!!)
        val snapshot = CharacterProject(name = "Historical character", face = FaceConfiguration(eyeShape = "Historical eyes"))
        val metadata = GenerationMetadata(model.provider, request, snapshot, outputFormat = "png")
        val store = FileGenerationHistoryStore(directory)
        val record = GenerationRecord.capture(GeneratedImage(bytes, "image/png", metadata))
        val requests = mutableListOf<ImageGenerationRequest>()
        var restored: CharacterProject? = null
        runBlocking { store.save(record, bytes) }
        try {
            rule.setContent {
                val scope = rememberCoroutineScope()
                val history = remember { HistoryController(store) }
                LaunchedEffect(Unit) { history.refresh() }
                val keys = remember { SessionCredentialStore().apply { set(model.provider, "fixture-only") } }
                val provider = remember { object : ImageGenerationProvider {
                    override val id = model.provider
                    override suspend fun generate(request: ImageGenerationRequest, credentials: ProviderCredentials): ProviderImage {
                        requests += request; return ProviderImage(bytes, "image/png")
                    }
                } }
                val generation = remember { GenerationController(scope, listOf(provider), keys, history::save) }
                MaterialTheme { GenerationAndHistory(CharacterProject(name = "Current character"), generation, keys,
                    remember { GenerationSelection() }, history, { restored = it }, {}) }
            }
            rule.onNodeWithText("History", useUnmergedTree = false).performClick()
            rule.waitUntil { rule.onAllNodesWithText("Historical character").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("Historical character").performClick()
            rule.onNodeWithText("Exact historical prompt").assertExists()
            rule.onNodeWithText("Save Image").assertExists()
            rule.onNodeWithText("Restore Configuration").performScrollTo().performClick()
            rule.onNodeWithText("Restore into Current character?").assertExists()
            rule.onNodeWithText("Confirm").performClick()
            rule.runOnIdle { assertEquals(snapshot, restored) }
            rule.onNodeWithText("Generate Again").performScrollTo().performClick()
            rule.waitUntil { requests.size == 1 }
            rule.runOnIdle { assertEquals(request, requests.single()) }
            rule.onNodeWithText("Delete", useUnmergedTree = false).performScrollTo().performClick()
            rule.onNodeWithText("Delete generation?").assertExists()
            rule.onNodeWithText("Confirm").performClick()
            rule.waitUntil { runBlocking { store.list().records.none { it.id == record.id } } }
            rule.runOnIdle { assertEquals(null, runBlocking { store.loadImage(record) }) }
        } finally { directory.deleteRecursively() }
    }
}
