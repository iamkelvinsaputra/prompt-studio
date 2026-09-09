package com.kelvinsaputra.promptstudio

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.kelvinsaputra.promptstudio.credentials.*
import com.kelvinsaputra.promptstudio.domain.CharacterProject
import com.kelvinsaputra.promptstudio.feature.generation.*
import com.kelvinsaputra.promptstudio.generation.model.*
import com.kelvinsaputra.promptstudio.generation.provider.ImageGenerationProvider
import com.kelvinsaputra.promptstudio.platform.*
import kotlinx.coroutines.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files
import kotlin.test.*
import kotlin.io.encoding.Base64

@OptIn(ExperimentalTestApi::class)
class GenerationDesktopTest {
    @get:Rule val rule = createComposeRule()
    private val bytes = Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=")
    @Test fun panelShowsMissingKeyLoadingCancellationErrorAndImage() {
        val credentials = SessionCredentialStore()
        val selection = GenerationSelection()
        var response = CompletableDeferred<ProviderImage>()
        val provider = object : ImageGenerationProvider {
            override val id = ImageProviderId.OpenAI
            override suspend fun generate(request: ImageGenerationRequest, credentials: ProviderCredentials) = response.await()
        }
        rule.setContent {
            val scope = rememberCoroutineScope()
            val controller = remember { GenerationController(scope, listOf(provider), credentials) }
            MaterialTheme { GenerationPanel(CharacterProject(), controller, credentials, selection, {}) }
        }
        rule.onNodeWithText("Generate Current").performScrollTo().performClick()
        rule.onNodeWithText(GenerationError.MissingCredentials.message).assertExists()
        rule.onNodeWithText("API key").performScrollTo().performTextInput("fixture-only-key")
        rule.onNodeWithText("Use key").performScrollTo().performClick()
        rule.onNodeWithText("API key configured · hidden").assertExists()
        rule.onNodeWithText("Generate Current").performScrollTo().performClick()
        rule.onNodeWithText("Cancel").performScrollTo().performClick()
        rule.onNodeWithText("Cancelled locally.", substring = true).assertExists()
        rule.onNodeWithText("Generate Current").performScrollTo().performClick()
        rule.runOnIdle { response.completeExceptionally(GenerationFailure(GenerationError.Authentication)) }
        rule.waitUntil { rule.onAllNodesWithText(GenerationError.Authentication.message).fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { response = CompletableDeferred() }
        rule.onNodeWithText("Try Again · current character").performScrollTo().performClick()
        rule.runOnIdle { response.complete(ProviderImage(bytes, "image/png")) }
        rule.waitUntil { rule.onAllNodesWithText("Save Image").fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil { rule.onAllNodesWithContentDescription("Generated Antihero").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Generated Antihero").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Save Image").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Regenerate").assertExists()
    }
    @Test fun desktopDecodesAndSavesOriginalBytesWithoutOverwrite() {
        val bitmap = decodeGeneratedImage(bytes)
        assertEquals(1, bitmap.width); assertEquals(1, bitmap.height)
        val directory = Files.createTempDirectory("prompt-image-test")
        val file = directory.resolve("image.png").toFile()
        try {
            saveNewImage(file, bytes)
            assertContentEquals(bytes, file.readBytes())
            assertFailsWith<java.nio.file.FileAlreadyExistsException> { saveNewImage(file, byteArrayOf(0)) }
            assertContentEquals(bytes, file.readBytes())
        } finally { file.delete(); directory.toFile().delete() }
    }
}
