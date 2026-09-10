package com.kelvinsaputra.promptstudio

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.kelvinsaputra.promptstudio.credentials.*
import com.kelvinsaputra.promptstudio.domain.CharacterProject
import com.kelvinsaputra.promptstudio.domain.resetVisualAssembly
import com.kelvinsaputra.promptstudio.guide.*
import com.kelvinsaputra.promptstudio.prompt.*
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
    @Test fun guideDefaultsOnInManualModeAndRetryKeepsChoiceUntilTurnedOff() {
        val credentials = SessionCredentialStore().apply { set(ImageProviderId.OpenAI, "fixture-only-key") }
        val requests = mutableListOf<ImageGenerationRequest>()
        var renderCalls = 0
        val provider = object : ImageGenerationProvider {
            override val id = ImageProviderId.OpenAI
            override val supportsVisualGuide = true
            override suspend fun generate(request: ImageGenerationRequest, credentials: ProviderCredentials): ProviderImage {
                requests += request
                return ProviderImage(bytes, "image/png")
            }
        }
        val project = CharacterProject().resetVisualAssembly().enterManualPrompt().editManualPrompt("My exact manual prompt")
        val selection = GenerationSelection()
        var showPanel by mutableStateOf(true)
        rule.setContent {
            val scope = rememberCoroutineScope()
            val controller = remember { GenerationController(scope, listOf(provider), credentials, guideRenderer = GuideRenderer { spec ->
                renderCalls++
                if (renderCalls == 1) error("Test render failure")
                LocalGuideRenderer.render(spec)
            }) }
            MaterialTheme { if (showPanel) GenerationPanel(project, controller, credentials, selection, {}) }
        }
        rule.onNodeWithContentDescription("Use visual guide").performScrollTo().assertIsOn()
        rule.onNodeWithText("Generate Current").performScrollTo().performClick()
        rule.waitUntil { rule.onAllNodesWithText(GenerationError.GuidePreparation.message).fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { assertTrue(requests.isEmpty()) }
        rule.onNodeWithText("Try Again · current character").performScrollTo().performClick()
        rule.waitUntil { requests.size == 1 }
        rule.runOnIdle {
            assertEquals(2, renderCalls)
            assertEquals("My exact manual prompt", requests.single().prompt)
            assertNotNull(requests.single().referenceGuide)
        }
        rule.waitUntil { rule.onAllNodesWithText("Visual guide used").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Use visual guide").performScrollTo().performClick().assertIsOff()
        rule.runOnIdle { showPanel = false }
        rule.waitForIdle()
        rule.runOnIdle { showPanel = true }
        rule.onNodeWithContentDescription("Use visual guide").performScrollTo().assertIsOff()
        rule.onNodeWithText("Generate Current").performScrollTo().performClick()
        rule.waitUntil { requests.size == 2 }
        rule.runOnIdle {
            assertEquals(2, renderCalls)
            assertNull(requests.last().referenceGuide)
            assertEquals("My exact manual prompt", requests.last().prompt)
        }
        rule.waitUntil { rule.onAllNodesWithText("Text-only generation").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Gemini", substring = false).performScrollTo().performClick()
        rule.onNodeWithContentDescription("Use visual guide").performScrollTo().assertIsOff().assertIsNotEnabled()
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
