package com.kelvinsaputra.promptstudio.feature.generation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.credentials.CredentialStore
import com.kelvinsaputra.promptstudio.domain.CharacterProject
import com.kelvinsaputra.promptstudio.domain.GenerationPreferences
import com.kelvinsaputra.promptstudio.domain.visualAssembly
import com.kelvinsaputra.promptstudio.guide.GuideRenderSpec
import com.kelvinsaputra.promptstudio.generation.model.*
import com.kelvinsaputra.promptstudio.platform.*
import com.kelvinsaputra.promptstudio.prompt.effectivePrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GenerationSelection(initial: GenerationPreferences = GenerationPreferences(), private val onChange: (GenerationPreferences) -> Unit = {}) {
    private var value by mutableStateOf(initial)
    val provider get() = value.provider
    val modelAvailable get() = ImageModels.all.any { it.id == value.modelId && it.provider == provider }
    var model: ImageModelDefinition
        get() = ImageModels.all.firstOrNull { it.id == value.modelId && it.provider == provider } ?: ImageModels.default(provider)
        set(model) { update(value.copy(provider = model.provider, modelId = model.id)) }
    var useVisualGuide: Boolean
        get() = value.useVisualGuide
        set(enabled) { update(value.copy(useVisualGuide = enabled)) }
    private fun update(next: GenerationPreferences) { value = next; onChange(next) }
    fun select(id: ImageProviderId) { update(value.copy(provider = id, modelId = ImageModels.default(id).id)) }
}

@Composable
fun GenerationPanel(project: CharacterProject, controller: GenerationController, credentials: CredentialStore, selection: GenerationSelection, onMessage: (String) -> Unit, variant: GenerationVariant? = null) {
    val state by controller.state.collectAsState()
    val provider = selection.provider
    val model = selection.model
    var draftKey by remember(provider) { mutableStateOf("") }
    var rememberKey by remember(provider) { mutableStateOf(false) }
    var keyConfigured by remember(provider) { mutableStateOf(runCatching { credentials.get(provider) != null }.getOrDefault(false)) }
    var editingKey by remember(provider) { mutableStateOf(false) }
    var showPrompt by remember { mutableStateOf(false) }
    var showMetadata by remember { mutableStateOf(false) }
    val saver = rememberImageSaver(onMessage)
    val busy = state.status is GenerationState.Generating
    val effective = if (selection.modelAvailable) model.outputFor(project.output.aspectRatio) else null
    val guideSupported = controller.supportsVisualGuide(model)
    val guideAvailable = GuideRenderSpec.from(project.visualAssembly, project.output.aspectRatio) != null
    val useGuide = selection.useVisualGuide && guideSupported && guideAvailable
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Generate Image", style = MaterialTheme.typography.titleLarge)
        Text("Your provider account pays for each request · 1 image")
        if (!selection.modelAvailable) Text("The saved model is unavailable. Select a model to continue.", color = MaterialTheme.colorScheme.error)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ImageProviderId.entries.forEach { id -> FilterChip(provider == id, { selection.select(id) }, label = { Text(id.name) }) }
        }
        ImageModels.all.filter { it.provider == provider }.forEach { option ->
            FilterChip(selection.modelAvailable && model == option, { selection.model = option }, label = { Text(option.displayName) })
        }
        if (keyConfigured) {
            Text("API key configured · hidden")
            TextButton(onClick = { editingKey = !editingKey; draftKey = "" }) { Text(if (editingKey) "Cancel key replacement" else "Change key") }
            TextButton(onClick = {
                try { credentials.remove(provider); keyConfigured = false; draftKey = "" }
                catch (_: Exception) { onMessage("Could not remove the saved key. Try again.") }
            }) { Text("Remove key") }
        }
        if (!keyConfigured || editingKey) {
        OutlinedTextField(draftKey, { draftKey = it }, label = { Text(if (keyConfigured) "Replacement API key" else "API key") },
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, modifier = Modifier.fillMaxWidth())
        if (credentials.supportsSecurePersistence) {
            Row { Checkbox(rememberKey, { rememberKey = it }); Text("Remember securely on this device", Modifier.padding(top = 12.dp)) }
        } else Text("Session only: keys are forgotten when the app closes.", style = MaterialTheme.typography.bodySmall)
        Button(enabled = draftKey.isNotBlank(), onClick = {
            try { credentials.set(provider, draftKey, rememberKey); draftKey = ""; keyConfigured = true; editingKey = false }
            catch (_: Exception) { onMessage("Could not store the key securely. Try session-only storage.") }
        }) { Text("Use key") }
        }
        Text("Authored ratio: ${project.output.aspectRatio ?: "not set"}")
        Text(if (!selection.modelAvailable) "Choose an available model to restore generation."
            else if (effective == null) "Enter a numeric width:height ratio in Output to generate."
            else "Effective output: ${effective.aspectRatio} · ${effective.size}${if (provider == ImageProviderId.OpenAI) " · PNG · medium quality" else " resolution"}")
        if (effective != null && effective.aspectRatio != project.output.aspectRatio)
            Text("Mapped to the nearest supported ratio. The authored prompt stays unchanged.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { showPrompt = !showPrompt }) { Text(if (showPrompt) "Hide exact prompt" else "Inspect exact prompt to send") }
        if (showPrompt) SelectionContainer { Text(project.effectivePrompt(), style = MaterialTheme.typography.bodySmall) }
        Row {
            Checkbox(checked = useGuide, enabled = guideSupported && guideAvailable && !busy, onCheckedChange = { selection.useVisualGuide = it },
                modifier = Modifier.semantics { contentDescription = "Use visual guide" })
            Text("Use visual guide", Modifier.padding(top = 12.dp))
        }
        Text(when {
            !guideSupported -> "Visual guides are unavailable for this provider. Generation uses text only."
            !guideAvailable -> "Choose a supported pose, framing and placement in Visual Build to use a guide. Generation uses text only."
            useGuide -> "Use the composition in Visual Build as a guide. The result may vary; text adjustments do not change the guide."
            else -> "Generation uses text only."
        }, style = MaterialTheme.typography.bodySmall)
        Button(enabled = !busy && effective != null && draftKey.isBlank() && project.effectivePrompt().isNotBlank(), onClick = { controller.generateCurrent(project, model, useGuide, variant) }) { Text("Generate Current") }
        when (val status = state.status) {
            is GenerationState.Generating -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(if (status.preparingGuide) "Preparing visual guide…" else "Generating ${status.metadata.project.name} with ${status.metadata.request.model}…")
                Text("You may keep editing. This request uses its captured snapshot.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = controller::cancel) { Text("Cancel") }
                if (showPrompt) SelectionContainer { Text(status.metadata.request.prompt, style = MaterialTheme.typography.bodySmall) }
            }
            is GenerationState.Error -> {
                state.attemptedProvider?.let { Text("Request to $it failed", style = MaterialTheme.typography.labelLarge) }
                Text(status.error.message, color = MaterialTheme.colorScheme.error)
                TextButton(enabled = effective != null && draftKey.isBlank() && project.effectivePrompt().isNotBlank(), onClick = { controller.generateCurrent(project, model, useGuide, variant) }) { Text("Try Again · current character") }
            }
            GenerationState.Cancelled -> Text("Cancelled locally. The provider may already have processed or charged for the request.")
            GenerationState.Idle -> Text("Build the composition, then generate your first image.")
            GenerationState.Success -> Unit
        }
        state.historyWarning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.latest?.takeIf { it.metadata.project.id == project.id }?.let { image ->
            HorizontalDivider()
            Text("Latest image · ${image.metadata.project.name}", style = MaterialTheme.typography.titleMedium)
            image.metadata.variant?.let { Text("Variant: ${it.name}", style = MaterialTheme.typography.bodySmall) }
            val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, image) {
                value = withContext(Dispatchers.Default) { runCatching { decodeGeneratedImage(image.bytes) }.getOrNull() }
            }
            bitmap?.let { decoded ->
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val previewHeight = (maxWidth * (decoded.height.toFloat() / decoded.width)).coerceIn(160.dp, 720.dp)
                    Image(decoded, "Generated ${image.metadata.project.name}", Modifier.fillMaxWidth().height(previewHeight), contentScale = ContentScale.Fit)
                }
            }
                ?: Text("Preview unavailable or decoding. You can still save the original image.")
            bitmap?.let { Text("Returned image: ${it.width} × ${it.height} pixels", style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { saver(image) }) { Text("Save Image") }
                OutlinedButton(enabled = !busy, onClick = controller::regenerate) { Text("Regenerate") }
            }
            Text("Regenerate uses this image’s previous character, provider and settings, including its visual guide · new paid request.", style = MaterialTheme.typography.bodySmall)
            Text(if (image.metadata.request.guideSpec != null) "Visual guide used" else "Text-only generation", style = MaterialTheme.typography.bodySmall)
            Text("${image.metadata.provider} · ${image.metadata.request.model}\nRequested ${image.metadata.request.requestedAspectRatio} → ${image.metadata.request.output.aspectRatio} · ${image.metadata.request.output.size}\n${image.mimeType}${image.metadata.quality?.let { " · $it quality" }.orEmpty()}")
            image.metadata.requestId?.let { Text("Request ID: $it", style = MaterialTheme.typography.bodySmall) }
            TextButton(onClick = { showMetadata = !showMetadata }) { Text("Captured prompt and character snapshot") }
            if (showMetadata) SelectionContainer { Text(image.metadata.request.prompt + "\n\n" + com.kelvinsaputra.promptstudio.persistence.ProjectJson.encode(image.metadata.project), style = MaterialTheme.typography.bodySmall) }
        }
    }
}
