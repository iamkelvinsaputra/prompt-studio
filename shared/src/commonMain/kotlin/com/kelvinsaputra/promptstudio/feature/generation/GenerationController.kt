package com.kelvinsaputra.promptstudio.feature.generation

import com.kelvinsaputra.promptstudio.credentials.CredentialStore
import com.kelvinsaputra.promptstudio.domain.CharacterProject
import com.kelvinsaputra.promptstudio.domain.visualAssembly
import com.kelvinsaputra.promptstudio.guide.*
import com.kelvinsaputra.promptstudio.generation.model.*
import com.kelvinsaputra.promptstudio.generation.provider.ImageGenerationProvider
import com.kelvinsaputra.promptstudio.persistence.ProjectJson
import com.kelvinsaputra.promptstudio.prompt.effectivePrompt
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed interface GenerationState {
    data object Idle : GenerationState
    data class Generating(val metadata: GenerationMetadata) : GenerationState
    data object Success : GenerationState
    data class Error(val error: GenerationError) : GenerationState
    data object Cancelled : GenerationState
}
data class GenerationUiState(val status: GenerationState = GenerationState.Idle, val latest: GeneratedImage? = null, val attemptedProvider: ImageProviderId? = null, val historyWarning: String? = null)
/** Main-thread events, owned by the application's structured coroutine scope. */
class GenerationController(
    private val scope: CoroutineScope,
    private val providers: List<ImageGenerationProvider>,
    private val credentials: CredentialStore,
    private val saveHistory: suspend (GeneratedImage) -> Unit = {},
    private val guideRenderer: GuideRenderer = LocalGuideRenderer,
) {
    private val mutable = MutableStateFlow(GenerationUiState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    fun supportsVisualGuide(model: ImageModelDefinition): Boolean =
        model.supportsVisualGuide && providers.any { it.id == model.provider && it.supportsVisualGuide }

    fun generateCurrent(project: CharacterProject, model: ImageModelDefinition, useVisualGuide: Boolean = false) {
        if (mutable.value.status is GenerationState.Generating) return
        val output = model.outputFor(project.output.aspectRatio)
        if (output == null) { fail(GenerationError.InvalidRequest); return }
        // Serialization detaches every nested list/set, including caller-owned mutable collections.
        val snapshot = try { ProjectJson.decode(ProjectJson.encode(project)) } catch (_: Exception) { fail(GenerationError.InvalidRequest); return }
        val guideSpec = if (useVisualGuide) {
            if (!supportsVisualGuide(model)) { fail(GenerationError.GuideUnsupported); return }
            GuideRenderSpec.from(snapshot.visualAssembly, snapshot.output.aspectRatio)
                ?: run { fail(GenerationError.GuideUnsupported); return }
        } else null
        val request = ImageGenerationRequest(snapshot.effectivePrompt(), snapshot.output.aspectRatio!!, model.id, output, guideSpec)
        if (request.prompt.isBlank()) { fail(GenerationError.InvalidRequest); return }
        start(GenerationMetadata(model.provider, request, snapshot, outputFormat = if (model.provider == ImageProviderId.OpenAI) "png" else "provider-selected", quality = if (model.provider == ImageProviderId.OpenAI) "medium" else null))
    }
    fun regenerate() { state.value.latest?.metadata?.let { start(it.copy(requestId = null)) } }
    fun generateAgain(metadata: GenerationMetadata) = start(metadata.copy(requestId = null))
    private fun start(metadata: GenerationMetadata) {
        if (ImageModels.all.none { it.id == metadata.request.model && it.provider == metadata.provider && it.supports(metadata.request.output) }) {
            fail(GenerationError.InvalidRequest); return
        }
        if (mutable.value.status is GenerationState.Generating) return
        val provider = providers.firstOrNull { it.id == metadata.provider }
        if (provider == null) { fail(GenerationError.InvalidRequest); return }
        if (metadata.request.guideSpec != null && (!provider.supportsVisualGuide || ImageModels.all.none { it.id == metadata.request.model && it.supportsVisualGuide })) {
            fail(GenerationError.GuideUnsupported); return
        }
        mutable.update { it.copy(attemptedProvider = metadata.provider) }
        val key = try { credentials.get(metadata.provider) } catch (_: Exception) { null }
        if (key == null) { fail(GenerationError.MissingCredentials); return }
        mutable.update { it.copy(status = GenerationState.Generating(metadata)) }
        job = scope.launch {
            try {
                val guide = metadata.request.guideSpec?.let { spec ->
                    try { guideRenderer.render(spec) }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { throw GenerationFailure(GenerationError.GuidePreparation) }
                }
                ensureActive()
                val result = provider.generate(metadata.request.copy(referenceGuide = guide), key)
                ensureActive()
                val image = GeneratedImage(result.bytes, result.mimeType, metadata.copy(requestId = result.requestId, outputFormat = result.mimeType.substringAfter('/')))
                mutable.value = GenerationUiState(GenerationState.Success, image, metadata.provider)
                // Success is already visible. A storage error must never become a provider error.
                withContext(NonCancellable) {
                    try { saveHistory(image) }
                    catch (_: Exception) {
                        mutable.update { current -> if (current.latest === image) current.copy(historyWarning =
                            "Image generated successfully, but could not be saved to history. Save Image to keep a copy.") else current }
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: GenerationFailure) { ensureActive(); fail(e.error) }
            catch (_: Exception) { ensureActive(); fail(GenerationError.Unknown) }
        }
    }
    fun cancel() {
        if (mutable.value.status is GenerationState.Generating) {
            job?.cancel(); job = null
            mutable.update { it.copy(status = GenerationState.Cancelled) }
        }
    }
    private fun fail(error: GenerationError) { mutable.update { it.copy(status = GenerationState.Error(error)) } }
}
