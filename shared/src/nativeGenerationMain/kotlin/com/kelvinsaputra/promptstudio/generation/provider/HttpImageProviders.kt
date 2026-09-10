package com.kelvinsaputra.promptstudio.generation.provider

import com.kelvinsaputra.promptstudio.credentials.ProviderCredentials
import com.kelvinsaputra.promptstudio.generation.model.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64

private val json = Json { ignoreUnknownKeys = true }
private fun JsonObject.obj(key: String) = this[key] as? JsonObject
private fun JsonObject.array(key: String) = this[key] as? JsonArray
private fun JsonObject.string(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull

abstract class HttpImageProvider(protected val client: HttpClient) : ImageGenerationProvider {
    protected suspend fun execute(request: ImageGenerationRequest, block: suspend () -> ProviderImage): ProviderImage {
        if (ImageModels.all.none { it.provider == id && it.id == request.model && it.supports(request.output) })
            throw GenerationFailure(GenerationError.InvalidRequest)
        try { return block() }
        catch (e: CancellationException) { throw e }
        catch (e: GenerationFailure) { throw e }
        catch (_: HttpRequestTimeoutException) { throw GenerationFailure(GenerationError.Timeout) }
        catch (_: io.ktor.client.network.sockets.SocketTimeoutException) { throw GenerationFailure(GenerationError.Timeout) }
        catch (_: io.ktor.client.network.sockets.ConnectTimeoutException) { throw GenerationFailure(GenerationError.Timeout) }
        catch (_: io.ktor.util.network.UnresolvedAddressException) { throw GenerationFailure(GenerationError.Network) }
        catch (_: IOException) { throw GenerationFailure(GenerationError.Network) }
        catch (_: Exception) { throw GenerationFailure(GenerationError.Unknown) }
    }
    protected suspend fun body(response: HttpResponse): JsonObject {
        // Never propagate provider text or HTTP exception details, which may echo credentials.
        val text = response.bodyAsText()
        val root = try { json.parseToJsonElement(text) as? JsonObject } catch (_: IllegalArgumentException) { null }
        if (response.status.value !in 200..299) {
            val error = root?.obj("error")
            val code = error?.string("code") ?: error?.string("status")
            val reason = error?.array("details")?.mapNotNull { (it as? JsonObject)?.string("reason") }.orEmpty()
            throw GenerationFailure(when {
                code in setOf("content_policy_violation", "moderation_blocked") -> GenerationError.Safety
                "API_KEY_INVALID" in reason || code == "invalid_api_key" || response.status.value in listOf(401, 403) -> GenerationError.Authentication
                response.status.value == 429 -> GenerationError.RateLimit
                response.status.value == 408 || response.status.value == 504 -> GenerationError.Timeout
                response.status.value >= 500 -> GenerationError.Server
                response.status.value in 400..499 -> GenerationError.InvalidRequest
                else -> GenerationError.Server
            })
        }
        return root ?: throw GenerationFailure(GenerationError.MalformedResponse)
    }
    protected fun decode(data: String?, mime: String?, requestId: String?): ProviderImage {
        if (data == null || data.length > 48 * 1024 * 1024) throw GenerationFailure(GenerationError.MalformedResponse)
        val bytes = try { Base64.decode(data) } catch (_: IllegalArgumentException) { throw GenerationFailure(GenerationError.MalformedResponse) }
        val actual = when {
            bytes.size >= 8 && bytes.take(8) == listOf(137,80,78,71,13,10,26,10).map { it.toByte() } -> "image/png"
            bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte() -> "image/jpeg"
            bytes.size >= 12 && bytes.copyOfRange(0,4).decodeToString() == "RIFF" && bytes.copyOfRange(8,12).decodeToString() == "WEBP" -> "image/webp"
            else -> throw GenerationFailure(GenerationError.MalformedResponse)
        }
        if (mime != actual) throw GenerationFailure(GenerationError.MalformedResponse)
        return ProviderImage(bytes, actual, requestId)
    }
}
class OpenAiImageGenerationProvider(client: HttpClient) : HttpImageProvider(client) {
    override val id = ImageProviderId.OpenAI
    override suspend fun generate(request: ImageGenerationRequest, credentials: ProviderCredentials) = execute(request) {
        val response = client.post("https://api.openai.com/v1/images/generations") {
            bearerAuth(credentials.apiKey)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("model", request.model); put("prompt", request.prompt); put("n", 1)
                put("size", request.output.size); put("output_format", "png"); put("quality", "medium")
            }.toString())
        }
        val root = body(response)
        val images = root.array("data")
        if (images?.size != 1) throw GenerationFailure(GenerationError.MalformedResponse)
        decode((images[0] as? JsonObject)?.string("b64_json"), "image/png", response.headers["x-request-id"])
    }
}
class GeminiImageGenerationProvider(client: HttpClient) : HttpImageProvider(client) {
    override val id = ImageProviderId.Gemini
    override suspend fun generate(request: ImageGenerationRequest, credentials: ProviderCredentials) = execute(request) {
        val response = client.post("https://generativelanguage.googleapis.com/v1beta/models/${request.model}:generateContent") {
            header("x-goog-api-key", credentials.apiKey)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                putJsonArray("contents") { addJsonObject { putJsonArray("parts") { addJsonObject { put("text", request.prompt) } } } }
                putJsonObject("generationConfig") {
                    put("candidateCount", 1)
                    putJsonArray("responseModalities") { add("IMAGE") }
                    putJsonObject("imageConfig") { put("aspectRatio", request.output.aspectRatio); put("imageSize", request.output.size) }
                }
            }.toString())
        }
        val root = body(response)
        if (root.obj("promptFeedback")?.string("blockReason")?.let { it != "BLOCK_REASON_UNSPECIFIED" } == true)
            throw GenerationFailure(GenerationError.Safety)
        val candidate = root.array("candidates")?.firstOrNull() as? JsonObject
        if (candidate?.string("finishReason") in setOf("SAFETY", "IMAGE_SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "RECITATION", "IMAGE_RECITATION"))
            throw GenerationFailure(GenerationError.Safety)
        val images = candidate?.obj("content")?.array("parts")?.mapNotNull { part ->
            (part as? JsonObject)?.takeUnless { (it["thought"] as? JsonPrimitive)?.booleanOrNull == true }?.obj("inlineData")
        }.orEmpty()
        if (images.size != 1) throw GenerationFailure(GenerationError.MalformedResponse)
        decode(images[0].string("data"), images[0].string("mimeType"), root.string("responseId"))
    }
}
