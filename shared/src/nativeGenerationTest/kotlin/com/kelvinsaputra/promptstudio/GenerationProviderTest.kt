package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.credentials.*
import com.kelvinsaputra.promptstudio.generation.model.*
import com.kelvinsaputra.promptstudio.generation.provider.*
import com.kelvinsaputra.promptstudio.guide.*
import com.kelvinsaputra.promptstudio.domain.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.utils.io.*
import kotlinx.io.readByteArray
import kotlin.io.encoding.Base64
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

private const val png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII="
class GenerationProviderTest {
    private val key = SessionCredentialStore().apply { set(ImageProviderId.OpenAI, "test-secret") }.get(ImageProviderId.OpenAI)!!
    private fun request(id: ImageProviderId): ImageGenerationRequest {
        val model = ImageModels.default(id)
        return ImageGenerationRequest("Exact prompt\nwith punctuation!", "9:16", model.id, model.outputFor("9:16")!!)
    }
    private fun provider(id: ImageProviderId, client: HttpClient): ImageGenerationProvider = when(id) {
        ImageProviderId.OpenAI -> OpenAiImageGenerationProvider(client)
        ImageProviderId.Gemini -> GeminiImageGenerationProvider(client)
    }
    private fun success(id: ImageProviderId) = if (id == ImageProviderId.OpenAI) """{"data":[{"b64_json":"$png"}]}""" else
        """{"responseId":"gemini-request","candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"Here is the image"},{"inlineData":{"mimeType":"image/png","data":"$png"}}]}}]}"""
    @Test fun bothProvidersConstructExactRequestsAndDecodeOwnedBytes() = runTest {
        for (id in ImageProviderId.entries) {
            var calls = 0
            val client = HttpClient(MockEngine { req ->
                calls++
                val root = Json.parseToJsonElement((req.body as TextContent).text).jsonObject
                assertEquals("https", req.url.protocol.name)
                assertNull(req.url.parameters["key"])
                if (id == ImageProviderId.OpenAI) {
                    assertEquals("api.openai.com", req.url.host)
                    assertEquals("/v1/images/generations", req.url.encodedPath)
                    assertTrue(req.headers[HttpHeaders.Authorization] == "Bearer test-secret", "Expected bearer authentication")
                    assertEquals(request(id).prompt, root["prompt"]!!.jsonPrimitive.content)
                    assertEquals("gpt-image-2.5-sunburst", root["model"]!!.jsonPrimitive.content)
                    assertEquals(1, root["n"]!!.jsonPrimitive.int)
                    assertEquals("864x1536", root["size"]!!.jsonPrimitive.content)
                    assertEquals("png", root["output_format"]!!.jsonPrimitive.content)
                    assertFalse("response_format" in root)
                    assertFalse("image" in root || "images" in root)
                } else {
                    assertEquals("generativelanguage.googleapis.com", req.url.host)
                    assertEquals("/v1beta/models/gemini-3.1-flash-image:generateContent", req.url.encodedPath)
                    assertTrue(req.headers["x-goog-api-key"] == "test-secret", "Expected API key header")
                    assertNull(req.headers[HttpHeaders.Authorization])
                    assertEquals(request(id).prompt, root["contents"]!!.jsonArray[0].jsonObject["parts"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content)
                    assertEquals(1, root["contents"]!!.jsonArray[0].jsonObject["parts"]!!.jsonArray.size)
                    val config = root["generationConfig"]!!.jsonObject
                    assertEquals("IMAGE", config["responseModalities"]!!.jsonArray.single().jsonPrimitive.content)
                    assertEquals("9:16", config["imageConfig"]!!.jsonObject["aspectRatio"]!!.jsonPrimitive.content)
                    assertEquals("1K", config["imageConfig"]!!.jsonObject["imageSize"]!!.jsonPrimitive.content)
                    assertEquals(1, config["candidateCount"]!!.jsonPrimitive.int)
                }
                respond(success(id), HttpStatusCode.OK, headersOf("x-request-id", "openai-request"))
            })
            try {
                val result = provider(id, client).generate(request(id), key)
                assertEquals("image/png", result.mimeType)
                assertTrue(result.bytes.size > 50)
                assertEquals(if(id == ImageProviderId.OpenAI) "openai-request" else "gemini-request", result.requestId)
                assertEquals(1, calls)
            } finally { client.close() }
        }
    }
    @Test fun guidedRequestsUseSupportedImageInputsAndPreserveExactPrompt() = runTest {
        for (model in ImageModels.all) {
            val id = model.provider
            val bytes = Base64.decode(png)
            val guide = GuideImage(bytes, 1, 1)
            val input = request(id).copy(model = model.id, referenceGuide = guide)
            val client = HttpClient(MockEngine { req ->
                if (id == ImageProviderId.OpenAI) {
                    assertEquals("/v1/images/edits", req.url.encodedPath)
                    val content = assertIs<MultiPartFormDataContent>(req.body)
                    val channel = ByteChannel(autoFlush = true)
                    val body = coroutineScope {
                        launch { content.writeTo(channel); channel.close() }
                        channel.readRemaining().readByteArray()
                    }
                    val text = body.decodeToString()
                    fun field(name: String): String = text.split("--${content.contentType.parameter("boundary")}")
                        .single { it.substringBefore("\r\n\r\n").contains(Regex("name=\"?$name\"?(?:\\r\\n|;)")) }
                        .substringAfter("\r\n\r\n").removeSuffix("\r\n")
                    assertEquals(input.prompt, field("prompt"))
                    assertEquals(model.id, field("model"))
                    assertEquals(input.output.size, field("size"))
                    assertEquals("medium", field("quality"))
                    assertEquals("png", field("output_format"))
                    assertEquals("1", field("n"))
                    assertContains(text, "name=\"image[]\"")
                    assertContains(text, "filename=\"visual-guide.png\"")
                    assertContains(text, "Content-Type: image/png")
                    assertTrue(body.asList().windowed(bytes.size).any { it == bytes.asList() })
                    assertFalse(text.contains("input_fidelity"))
                } else {
                    val root = Json.parseToJsonElement((req.body as TextContent).text).jsonObject
                    val parts = root["contents"]!!.jsonArray.single().jsonObject["parts"]!!.jsonArray
                    assertEquals(2, parts.size)
                    assertEquals(input.prompt, parts[0].jsonObject["text"]!!.jsonPrimitive.content)
                    val image = parts[1].jsonObject["inlineData"]!!.jsonObject
                    assertEquals("image/png", image["mimeType"]!!.jsonPrimitive.content)
                    assertContentEquals(bytes, Base64.decode(image["data"]!!.jsonPrimitive.content))
                }
                respond(success(id), HttpStatusCode.OK)
            })
            try { assertEquals("image/png", provider(id, client).generate(input, key).mimeType) }
            finally { client.close() }
        }
    }

    @Test fun missingPreparedGuideIsRejectedBeforeNetwork() = runTest {
        for (id in ImageProviderId.entries) {
            var calls = 0
            val client = HttpClient(MockEngine { calls++; respond(success(id)) })
            try {
                val spec = GuideRenderSpec.from(CharacterProject().resetVisualAssembly().visualAssembly, "9:16")!!
                assertEquals(GenerationError.GuidePreparation, assertFailsWith<GenerationFailure> {
                    provider(id, client).generate(request(id).copy(guideSpec = spec), key)
                }.error)
                assertEquals(0, calls)
            } finally { client.close() }
        }
    }
    @Test fun httpFailuresAreSafeAndNeverRetried() = runTest {
        for (id in ImageProviderId.entries) for ((status, expected) in listOf(
            401 to GenerationError.Authentication, 403 to GenerationError.Authentication,
            429 to GenerationError.RateLimit, 400 to GenerationError.InvalidRequest,
            503 to GenerationError.Server, 504 to GenerationError.Timeout,
        )) {
            var calls = 0
            val client = HttpClient(MockEngine { calls++; respond("""{"error":{"message":"test-secret", "status":"ERROR"}}""", HttpStatusCode.fromValue(status)) })
            try {
                val error = assertFailsWith<GenerationFailure> { provider(id, client).generate(request(id), key) }
                assertEquals(expected, error.error)
                assertFalse(error.toString().contains("test-secret"))
                assertNull(error.cause)
                assertEquals(1, calls)
            } finally { client.close() }
        }
    }
    @Test fun safetyAndInvalidGoogleKeyAreDecoded() = runTest {
        for ((id, fixture, expected) in listOf(
            Triple(ImageProviderId.OpenAI, """{"error":{"code":"content_policy_violation"}}""", GenerationError.Safety),
            Triple(ImageProviderId.Gemini, """{"error":{"details":[{"reason":"API_KEY_INVALID"}]}}""", GenerationError.Authentication),
        )) {
            val client = HttpClient(MockEngine { respond(fixture, HttpStatusCode.BadRequest) })
            try { assertEquals(expected, assertFailsWith<GenerationFailure> { provider(id, client).generate(request(id), key) }.error) }
            finally { client.close() }
        }
    }
    @Test fun malformedResponsesAndInvalidBase64AreRejected() = runTest {
        for (id in ImageProviderId.entries) for (fixture in listOf("broken", "[]", "{}", success(id).replace(png, "%%%"), success(id).replace(png, "aGVsbG8="))) {
            val client = HttpClient(MockEngine { respond(fixture) })
            try { assertEquals(GenerationError.MalformedResponse, assertFailsWith<GenerationFailure> { provider(id, client).generate(request(id), key) }.error) }
            finally { client.close() }
        }
    }
    @Test fun geminiSafetyAndTextOnlyResponses() = runTest {
        for (fixture in listOf("""{"promptFeedback":{"blockReason":"SAFETY"}}""", """{"candidates":[{"finishReason":"IMAGE_SAFETY"}]}""")) {
            val client = HttpClient(MockEngine { respond(fixture) })
            try { assertEquals(GenerationError.Safety, assertFailsWith<GenerationFailure> { provider(ImageProviderId.Gemini, client).generate(request(ImageProviderId.Gemini), key) }.error) }
            finally { client.close() }
        }
    }
    @Test fun cancellationPropagatesThroughHttp() = runTest {
        val client = HttpClient(MockEngine { awaitCancellation() })
        try {
            val task = async { provider(ImageProviderId.OpenAI, client).generate(request(ImageProviderId.OpenAI), key) }
            yield(); task.cancel()
            assertFailsWith<CancellationException> { task.await() }
        } finally { client.close() }
    }
    @Test fun networkTimeoutAndUnexpectedFailuresStaySafe() = runTest {
        for (id in ImageProviderId.entries) for ((exception, expected) in listOf(
            kotlinx.io.IOException("test-secret") to GenerationError.Network,
            io.ktor.util.network.UnresolvedAddressException() to GenerationError.Network,
            io.ktor.client.plugins.HttpRequestTimeoutException("https://example.invalid", 300000L) to GenerationError.Timeout,
            IllegalStateException("test-secret") to GenerationError.Unknown,
        )) {
            val client = HttpClient(MockEngine { throw exception })
            try {
                val failure = assertFailsWith<GenerationFailure> { provider(id, client).generate(request(id), key) }
                assertEquals(expected, failure.error)
                assertFalse(failure.toString().contains("test-secret"))
            } finally { client.close() }
        }
    }

    @Test fun everyCatalogModelIsAcceptedAndUnknownModelsNeverReachNetwork() = runTest {
        for (model in ImageModels.all) {
            var calls = 0
            val client = HttpClient(MockEngine { calls++; respond(success(model.provider)) })
            try {
                val request = request(model.provider).copy(model = model.id)
                provider(model.provider, client).generate(request, key)
                assertEquals(GenerationError.InvalidRequest, assertFailsWith<GenerationFailure> {
                    provider(model.provider, client).generate(request.copy(model = "unsupported"), key)
                }.error)
                assertEquals(1, calls)
            } finally { client.close() }
        }
    }

}
