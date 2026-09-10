package com.kelvinsaputra.promptstudio.generation.model

import com.kelvinsaputra.promptstudio.domain.CharacterProject
import com.kelvinsaputra.promptstudio.guide.GuideImage
import com.kelvinsaputra.promptstudio.guide.GuideRenderSpec
import kotlinx.serialization.Transient
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable

@Serializable enum class ImageProviderId { OpenAI, Gemini }
@Serializable data class EffectiveOutput(val aspectRatio: String, val size: String)
data class ImageModelDefinition(
    val id: String, val displayName: String, val provider: ImageProviderId,
    val outputs: List<EffectiveOutput>, val customPixelSizes: Boolean = false,
    val supportsVisualGuide: Boolean = false,
) {
    fun outputFor(ratio: String?): EffectiveOutput? {
        val value = ratioValue(ratio) ?: return null
        outputs.firstOrNull { abs(ratioValue(it.aspectRatio)!! - value) < 1e-9 }?.let { return it }
        if (!customPixelSizes) return outputs.minByOrNull { abs(ln(ratioValue(it.aspectRatio)!! / value)) }
        // Stay within the documented non-experimental pixel budget, preferring ~1 megapixel.
        val target = value.coerceIn(1.0 / 3, 3.0)
        var best: EffectiveOutput? = null
        var bestError = Double.POSITIVE_INFINITY
        var bestArea = Double.POSITIVE_INFINITY
        for (height in 16..2560 step 16) {
            val width = (height * target / 16).roundToInt() * 16
            if (!validPixels(width, height)) continue
            val error = abs(ln(width.toDouble() / height / target))
            val area = abs(width.toDouble() * height - 1_048_576)
            if (error < bestError - 1e-9 || (abs(error - bestError) < 1e-9 && area < bestArea)) {
                val divisor = gcd(width, height)
                best = EffectiveOutput("${width / divisor}:${height / divisor}", "${width}x${height}")
                bestError = error; bestArea = area
            }
        }
        return best
    }
    fun supports(output: EffectiveOutput): Boolean {
        if (output in outputs) return true
        if (!customPixelSizes) return false
        val dimensions = output.size.split('x').map { it.toIntOrNull() }
        if (dimensions.size != 2) return false
        val width = dimensions[0] ?: return false
        val height = dimensions[1] ?: return false
        return validPixels(width, height) && ratioValue(output.aspectRatio)?.let { abs(it - width.toDouble() / height) < 1e-9 } == true
    }
}
private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
private fun validPixels(width: Int, height: Int): Boolean = width in 16..2560 && height in 16..2560 &&
    width % 16 == 0 && height % 16 == 0 && width.toDouble() / height in (1.0 / 3)..3.0 &&
    width.toLong() * height in 655_360L..3_686_400L
private fun ratioValue(value: String?): Double? {
    val parts = value?.split(':') ?: return null
    if (parts.size != 2) return null
    val a = parts[0].trim().toDoubleOrNull() ?: return null
    val b = parts[1].trim().toDoubleOrNull() ?: return null
    return (a / b).takeIf { a > 0 && b > 0 && it.isFinite() && it > 0 }
}
/** Explicit supported subset, verified against official docs on 2026-09-09. */
object ImageModels {
    private val openAiOutputs = listOf(
            EffectiveOutput("1:1", "1024x1024"), EffectiveOutput("16:9", "1536x864"),
            EffectiveOutput("9:16", "864x1536"), EffectiveOutput("4:5", "1024x1280"),
            EffectiveOutput("3:2", "1536x1024"), EffectiveOutput("2:3", "1024x1536"),
        )
    val all = listOf(
        ImageModelDefinition("gpt-image-2.5-sunburst", "GPT Image 2.5 Sunburst", ImageProviderId.OpenAI, openAiOutputs, customPixelSizes = true, supportsVisualGuide = true),
        ImageModelDefinition("gpt-image-2", "GPT Image 2", ImageProviderId.OpenAI, openAiOutputs, customPixelSizes = true, supportsVisualGuide = true),
        ImageModelDefinition("gemini-3.1-flash-image", "Gemini 3.1 Flash Image", ImageProviderId.Gemini,
            listOf("1:1", "1:4", "1:8", "2:3", "3:2", "3:4", "4:1", "4:3", "4:5", "5:4", "8:1", "9:16", "16:9", "21:9").map { EffectiveOutput(it, "1K") }, supportsVisualGuide = true),
    )
    fun default(provider: ImageProviderId) = all.first { it.provider == provider }
}
@Serializable data class ImageGenerationRequest(
    val prompt: String, val requestedAspectRatio: String, val model: String, val output: EffectiveOutput,
    val guideSpec: GuideRenderSpec? = null,
    @Transient val referenceGuide: GuideImage? = null,
)
@Serializable data class GenerationVariant(val id: String, val name: String)
@Serializable data class GenerationMetadata(val provider: ImageProviderId, val request: ImageGenerationRequest, val project: CharacterProject, val requestId: String? = null, val outputFormat: String, val quality: String? = null, val variant: GenerationVariant? = null)
/** Byte ownership transfers to the session; identity equality avoids ByteArray data-class surprises. */
class GeneratedImage(val bytes: ByteArray, val mimeType: String, val metadata: GenerationMetadata)
class ProviderImage(val bytes: ByteArray, val mimeType: String, val requestId: String? = null)
enum class GenerationError(val message: String) {
    GuidePreparation("Could not prepare the visual guide. Try again, or turn off Use visual guide and generate without it."),
    GuideUnsupported("A visual guide is unavailable for these choices. Turn off Use visual guide to generate with text only."),
    MissingCredentials("Enter an API key for this provider."),
    Authentication("The provider rejected access. Check your API key and model permissions."),
    RateLimit("Provider quota or rate limit reached. Check billing or wait before trying again."),
    InvalidRequest("The provider could not accept this request. Check the output settings and prompt."),
    Safety("The provider declined this request under its content policy. Your character is unchanged."),
    Network("Could not reach the provider. Check your internet connection."),
    Timeout("Generation timed out. The provider may still charge for the request."),
    MalformedResponse("The provider did not return a usable image."),
    Server("The provider is having trouble. Try again later."),
    Unknown("Generation failed unexpectedly. Try again when ready."),
}
class GenerationFailure(val error: GenerationError) : Exception(error.message)
fun GeneratedImage.suggestedFilename(): String {
    val stem = "${metadata.project.name}-${metadata.provider}-${metadata.request.model}"
        .replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('.', '-').take(160).ifBlank { "prompt-studio" }
    val extension = when (mimeType) { "image/jpeg" -> "jpg"; "image/webp" -> "webp"; else -> "png" }
    return "$stem.$extension"
}
