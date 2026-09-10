package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.credentials.*
import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.EditorViewModel
import com.kelvinsaputra.promptstudio.feature.generation.*
import com.kelvinsaputra.promptstudio.generation.model.*
import com.kelvinsaputra.promptstudio.generation.provider.*
import com.kelvinsaputra.promptstudio.history.*
import com.kelvinsaputra.promptstudio.prompt.PromptCompiler
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

private class MemoryHistoryStore : GenerationHistoryStore {
    val records = mutableMapOf<String, GenerationRecord>()
    val images = mutableMapOf<String, ByteArray>()
    var saves = 0
    var failSave = false
    override suspend fun list() = HistoryListing(records.values.toList())
    override suspend fun save(record: GenerationRecord, image: ByteArray) {
        saves++
        if (failSave) error("quota")
        images[record.id] = image.copyOf(); records[record.id] = record
    }
    override suspend fun loadImage(record: GenerationRecord) = images[record.id]
    override suspend fun delete(record: GenerationRecord) { images.remove(record.id); records.remove(record.id) }
}
@OptIn(ExperimentalCoroutinesApi::class)
class GenerationHistoryTest {
    private val model = ImageModels.default(ImageProviderId.OpenAI)
    private fun image(project: CharacterProject = CharacterProject()): GeneratedImage {
        val request = ImageGenerationRequest(PromptCompiler().compile(project).text, project.output.aspectRatio!!, model.id, model.outputFor(project.output.aspectRatio)!!)
        return GeneratedImage(byteArrayOf(1,2,3), "image/png", GenerationMetadata(model.provider, request, project, "request-id", "png", "medium"))
    }
    private class FakeProvider : ImageGenerationProvider {
        override val id = ImageProviderId.OpenAI
        val requests = mutableListOf<ImageGenerationRequest>()
        var response = CompletableDeferred<ProviderImage>()
        override suspend fun generate(request: ImageGenerationRequest, credentials: ProviderCredentials): ProviderImage {
            requests += request
            return response.await()
        }
    }
    private fun credentials() = SessionCredentialStore().apply { set(model.provider, "history-test-secret") }
    @Test fun recordRoundTripPreservesStructuredSnapshotPromptAndSettings() {
        val project = CharacterProject(name = "Historical", personality = PersonalityConfiguration(listOf("quiet", "precise")), face = FaceConfiguration(eyeShape = "sharp"))
        val record = GenerationRecord.capture(image(project))
        val decoded = HistoryJson.decode(HistoryJson.encode(record))
        assertEquals(record, decoded)
        assertEquals(project, decoded.metadata.project)
        assertEquals(PromptCompiler().compile(project).text, decoded.metadata.request.prompt)
        assertEquals("request-id", decoded.metadata.requestId)
        assertEquals("medium", decoded.metadata.quality)
        assertFalse(HistoryJson.encode(record).contains("history-test-secret"))
        assertFalse(HistoryJson.encode(record).contains("apiKey"))
    }
    @Test fun chronologicalSortingAndCharacterFilteringHaveStableTieBreak() {
        val a = GenerationRecord.capture(image()).copy(id = "a", imageReference = "a.image", createdAt = 20)
        val b = a.copy(id = "b", imageReference = "b.image")
        val c = GenerationRecord.capture(image(CharacterProject(id = "other"))).copy(createdAt = 10)
        assertEquals(listOf(a,b,c), listOf(c,b,a).chronological())
        assertEquals(listOf(a,b), listOf(c,b,a).chronological("demo-character"))
    }
    @Test fun restorePreservesSelectedIdentityAndReproducesHistoricalPrompt() {
        val historical = CharacterProject(id = "old", name = "Historical", face = FaceConfiguration(eyeShape = "narrow"))
        val record = GenerationRecord.capture(image(historical))
        val editor = EditorViewModel()
        editor.newCharacter(); editor.renameCharacter("Keep my name")
        val current = editor.state.value.project
        editor.restoreConfiguration(record.metadata.project)
        val restored = editor.state.value.project
        assertEquals(current.id, restored.id); assertEquals(current.name, restored.name)
        assertEquals(record.restoredInto(current), restored)
        assertEquals(record.metadata.request.prompt, PromptCompiler().compile(restored).text)
    }
    @Test fun deletingCharacterLeavesHistoryAndNewIdsNeverReuseDeletedIdentity() = runTest {
        val editor = EditorViewModel(); editor.newCharacter()
        val character = editor.state.value.project.copy(output = OutputConfiguration())
        val store = MemoryHistoryStore(); val history = HistoryController(store)
        history.save(image(character))
        editor.deleteCharacter(); editor.newCharacter()
        assertNotEquals(character.id, editor.state.value.project.id)
        assertEquals(character, store.records.values.single().metadata.project)
    }
    @Test fun deleteRemovesMetadataAndAssociatedArtifact() = runTest {
        val store = MemoryHistoryStore(); val history = HistoryController(store)
        history.save(image()); val record = history.state.value.records.single()
        assertNotNull(history.loadImage(record))
        history.delete(record)
        assertTrue(store.records.isEmpty()); assertTrue(store.images.isEmpty()); assertTrue(history.state.value.records.isEmpty())
    }
    @Test fun missingImageDoesNotPreventMetadataRestoreOrDeletion() = runTest {
        val store = MemoryHistoryStore(); val history = HistoryController(store)
        history.save(image()); val record = history.state.value.records.single(); store.images.clear()
        history.refresh(); assertEquals(record, history.state.value.records.single())
        assertNull(history.loadImage(record)); assertEquals(record.metadata.project.face, record.restoredInto(CharacterProject()).face)
        history.delete(record); assertTrue(store.records.isEmpty())
    }
    @Test fun successfulGenerationSavesExactlyOnceWithPressTimeSnapshot() = runTest {
        val store = MemoryHistoryStore(); val history = HistoryController(store); val provider = FakeProvider()
        val controller = GenerationController(this, listOf(provider), credentials(), history::save)
        val traits = mutableListOf("calm"); val project = CharacterProject(personality = PersonalityConfiguration(traits))
        val expected = PromptCompiler().compile(project).text
        controller.generateCurrent(project, model); controller.generateCurrent(project, model); runCurrent()
        traits[0] = "changed after pressing Generate"
        provider.response.complete(ProviderImage(byteArrayOf(1), "image/png")); advanceUntilIdle()
        assertEquals(GenerationState.Success, controller.state.value.status)
        assertEquals(1, store.saves); assertEquals(1, store.records.size)
        val saved = store.records.values.single()
        assertEquals(listOf("calm"), saved.metadata.project.personality.traits)
        assertEquals(expected, saved.metadata.request.prompt)
    }
    @Test fun failedGenerationDoesNotSaveHistory() = runTest {
        val store = MemoryHistoryStore(); val provider = FakeProvider()
        val controller = GenerationController(this, listOf(provider), credentials(), HistoryController(store)::save)
        controller.generateCurrent(CharacterProject(), model); runCurrent()
        provider.response.completeExceptionally(GenerationFailure(GenerationError.Authentication)); advanceUntilIdle()
        assertEquals(0, store.saves); assertIs<GenerationState.Error>(controller.state.value.status)
    }
    @Test fun cancelledGenerationDoesNotSaveHistory() = runTest {
        val store = MemoryHistoryStore(); val provider = FakeProvider()
        val controller = GenerationController(this, listOf(provider), credentials(), HistoryController(store)::save)
        controller.generateCurrent(CharacterProject(), model); runCurrent(); controller.cancel()
        provider.response.complete(ProviderImage(byteArrayOf(1), "image/png")); advanceUntilIdle()
        assertEquals(0, store.saves); assertEquals(GenerationState.Cancelled, controller.state.value.status)
    }
    @Test fun historyFailureKeepsSuccessfulImageAvailableForManualSave() = runTest {
        val store = MemoryHistoryStore().apply { failSave = true }; val provider = FakeProvider()
        val controller = GenerationController(this, listOf(provider), credentials(), HistoryController(store)::save)
        controller.generateCurrent(CharacterProject(), model); runCurrent()
        provider.response.complete(ProviderImage(byteArrayOf(1), "image/png")); advanceUntilIdle()
        assertEquals(GenerationState.Success, controller.state.value.status)
        assertContentEquals(byteArrayOf(1), controller.state.value.latest!!.bytes)
        assertNotNull(controller.state.value.historyWarning); assertEquals(1, store.saves)
    }
    @Test fun generateAgainUsesHistoricalPromptAndSettingsWithoutEditorState() = runTest {
        val provider = FakeProvider(); val store = MemoryHistoryStore()
        val controller = GenerationController(this, listOf(provider), credentials(), HistoryController(store)::save)
        val historical = image(CharacterProject(name = "Old", output = OutputConfiguration(type = OutputType.SQUARE))).metadata
        val editor = EditorViewModel(); editor.setProject(CharacterProject(subject = "unrelated current editor"))
        controller.generateAgain(historical); runCurrent()
        assertEquals(historical.request, provider.requests.single())
        provider.response.complete(ProviderImage(byteArrayOf(4), "image/png", "new-request")); advanceUntilIdle()
        val saved = store.records.values.single()
        assertEquals(historical.project, saved.metadata.project)
        assertEquals("new-request", saved.metadata.requestId)
        assertNotEquals(editor.state.value.project, saved.metadata.project)
    }
    @Test fun unavailableHistoricalModelNeverFallsBackOrCallsProvider() = runTest {
        val provider = FakeProvider(); val controller = GenerationController(this, listOf(provider), credentials())
        val metadata = image().metadata
        controller.generateAgain(metadata.copy(request = metadata.request.copy(model = "retired-model"))); advanceUntilIdle()
        assertTrue(provider.requests.isEmpty()); assertIs<GenerationState.Error>(controller.state.value.status)
    }
    @Test fun unsupportedHistoryVersionAndPathTraversalAreRejected() {
        val encoded = HistoryJson.encode(GenerationRecord.capture(image()))
        assertFails { HistoryJson.decode(encoded.replace("\"version\": 1", "\"version\": 99")) }
        assertFails { GenerationRecord("../outside", 0, image().metadata, "image/png", "../outside.image") }
    }
}
