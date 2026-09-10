@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package com.kelvinsaputra.promptstudio.history

import androidx.compose.runtime.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.io.encoding.Base64
import kotlinx.serialization.json.*

/** IndexedDB transaction stores metadata and image bytes together. Nothing binary enters localStorage. */
class BrowserGenerationHistoryStore : GenerationHistoryStore {
    private suspend fun operation(action: String, id: String = "", metadata: String = "", image: String = ""): String =
        suspendCancellableCoroutine { continuation ->
            historyOperation(action, id, metadata, image,
                { if (continuation.isActive) continuation.resume(it) },
                { if (continuation.isActive) continuation.resumeWithException(IllegalStateException("Browser history storage unavailable or quota exceeded")) })
        }
    override suspend fun list(): HistoryListing {
        var corrupt = 0
        val records = Json.parseToJsonElement(operation("list")).jsonArray.mapNotNull {
            try { HistoryJson.decode(it.jsonPrimitive.content) } catch (_: Exception) { corrupt++; null }
        }
        return HistoryListing(records.chronological(), corrupt)
    }
    override suspend fun save(record: GenerationRecord, image: ByteArray) { operation("save", record.id, HistoryJson.encode(record), Base64.encode(image)) }
    override suspend fun loadImage(record: GenerationRecord): ByteArray? = operation("image", record.id).takeIf { it.isNotEmpty() }?.let(Base64::decode)
    override suspend fun delete(record: GenerationRecord) { operation("delete", record.id) }
}
@Composable actual fun rememberGenerationHistoryStore(): GenerationHistoryStore = remember { BrowserGenerationHistoryStore() }
private fun historyOperation(action: String, id: String, metadata: String, image: String, success: (String) -> Unit, failure: () -> Unit): Unit = js("""{
    const open = indexedDB.open('prompt-studio-history', 1);
    open.onupgradeneeded = () => { open.result.createObjectStore('metadata'); open.result.createObjectStore('images'); };
    open.onerror = () => failure(); open.onblocked = () => failure();
    open.onsuccess = () => {
        const db = open.result;
        try {
            const tx = db.transaction(['metadata','images'], action === 'save' || action === 'delete' ? 'readwrite' : 'readonly');
            let result = '';
            tx.oncomplete = () => { db.close(); success(result); };
            tx.onerror = () => { db.close(); failure(); }; tx.onabort = () => { db.close(); failure(); };
            if (action === 'save') {
                tx.objectStore('images').add(Uint8Array.from(atob(image), c => c.charCodeAt(0)), id);
                tx.objectStore('metadata').add(metadata, id);
            } else if (action === 'delete') { tx.objectStore('images').delete(id); tx.objectStore('metadata').delete(id); }
            else if (action === 'list') { tx.objectStore('metadata').getAll().onsuccess = e => { result = JSON.stringify(e.target.result); }; }
            else { tx.objectStore('images').get(id).onsuccess = e => {
                const bytes = e.target.result; if (!bytes) return;
                let binary = ''; for (let i = 0; i < bytes.length; i += 8192) binary += String.fromCharCode(...bytes.subarray(i, i + 8192));
                result = btoa(binary);
            }; }
        } catch(e) { db.close(); failure(); }
    };
}""")
