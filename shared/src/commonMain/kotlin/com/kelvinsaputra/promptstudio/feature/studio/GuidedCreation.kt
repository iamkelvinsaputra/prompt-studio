package com.kelvinsaputra.promptstudio.feature.studio

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.*

val creationCategories = listOf(StudioCategory.Subject, StudioCategory.Appearance, StudioCategory.Pose, StudioCategory.Camera,
    StudioCategory.Environment, StudioCategory.Lighting, StudioCategory.Style, StudioCategory.Color, StudioCategory.Effects)
private val creationQuestions = listOf("Who is your character?", "What do they look like?", "How are they carrying themselves?", "How do you see them?", "Where are they?", "How are they lit?", "How should the illustration look?", "What colors tell their story?", "Anything extra?", "Your character, ready to create.")
fun creationLabel(step: Int) = if (step == 9) "Review" else if (step == 0) "Character" else creationCategories[step].title

@Composable
fun GuidedCreation(state: EditorUiState, editor: EditorViewModel, onProjects: () -> Unit, generationContent: @Composable () -> Unit) {
    val p = state.project
    val step = p.creationStep ?: 9
    var returnToReview by rememberSaveable(p.id) { mutableStateOf(false) }
    var generating by rememberSaveable(p.id) { mutableStateOf(false) }
    val go: (Int) -> Unit = { editor.setProject(p.copy(creationStep = it)) }
    VisualGuideAssets {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.safeDrawingPadding().imePadding()) {
                val compact = maxWidth < 600.dp
                Column(Modifier.fillMaxSize()) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onProjects) { Text("Projects") }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text("CREATE CHARACTER · ${creationLabel(step).uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            }
                            Text("${step + 1} / 10", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    LinearProgressIndicator(progress = { (step + 1) / 10f }, modifier = Modifier.fillMaxWidth())
                    if (generating) {
                        TextButton(onClick = { generating = false }) { Text("Back to Review") }
                        Box(Modifier.weight(1f).padding(16.dp)) { generationContent() }
                        TextButton(onClick = { editor.setProject(p.copy(creationStep = null)); editor.selectModule(EditorModule.Identity) }) { Text("Open Studio") }
                    } else {
                        if (!compact) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
                            (0..9).forEach { index -> TextButton(enabled = index <= step || returnToReview, onClick = { go(index) }) { Text(creationLabel(index), color = if (index == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) } }
                        }
                        state.saveError?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error); TextButton(onClick = editor::retrySave) { Text("Retry Save") } }
                        state.message?.let { Text(it, Modifier.padding(16.dp)); TextButton(onClick = editor::dismissMessage) { Text("Dismiss") } }
                        key(p.id, step) {
                            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = if (compact) 20.dp else 40.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Column(Modifier.widthIn(max = 840.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                    Text("STEP ${step + 1} OF 10 · ${creationLabel(step).uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(creationQuestions[step], style = MaterialTheme.typography.headlineLarge)
                                    if (step < 9) {
                                        Text("Selected · ${creationCategories[step].summary(p)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        CategoryPanel(creationCategories[step], state, editor::setProject, editor::setCostumeLocks, editor::setPoseLocks,
                                            editor::randomizeCostume, editor::randomizePose, editor::resetCostume, editor::resetPose)
                                    } else {
                                        Text(p.characterName, style = MaterialTheme.typography.headlineMedium)
                                        Text("Review your choices. You can change any section before generating.", style = MaterialTheme.typography.bodyMedium)
                                        ReviewSections(p) { category -> returnToReview = true; go(creationCategories.indexOf(category).coerceAtLeast(0)) }
                                        AdvancedSection("Output & negative guidance") {
                                            CategoryPanel(StudioCategory.Output, state, editor::setProject, editor::setCostumeLocks, editor::setPoseLocks, editor::randomizeCostume, editor::randomizePose, editor::resetCostume, editor::resetPose)
                                            BoundedTextListEditor("Exclusion", p.exclusions, 15) { editor.setProject(p.copy(exclusions = it)) }
                                        }
                                    }
                                }
                            }
                        }
                        Surface(shadowElevation = 3.dp) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = if (compact) 16.dp else 40.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TextButton(onClick = { if (step == 0) onProjects() else go(step - 1) }) { Text("Back") }
                                Spacer(Modifier.weight(1f))
                                if (step == 9) {
                                    TextButton(onClick = { editor.setProject(p.copy(creationStep = null)); editor.selectModule(EditorModule.Identity) }) { Text("Open Studio") }
                                    Button(onClick = { generating = true }, enabled = p.output.aspectRatio != null) { Text("Generate") }
                                } else Button(onClick = {
                                    if (returnToReview) { returnToReview = false; go(9) } else go(step + 1)
                                }) { Text(if (returnToReview) "Return to Review" else "Continue") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewSections(p: CharacterProject, edit: (StudioCategory) -> Unit) {
    creationCategories.forEach { category ->
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (category == StudioCategory.Subject) "Character" else category.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { edit(category) }) { Text("Edit ${if (category == StudioCategory.Subject) "character" else category.title.lowercase()}") }
                }
                Text(reviewSummary(category, p), style = MaterialTheme.typography.bodyMedium)
                if (category == StudioCategory.Color && p.colorDirection.colors.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    p.colorDirection.colors.forEach { color -> Box(Modifier.size(28.dp).background(androidx.compose.ui.graphics.Color(0xFF000000L or color.hex.removePrefix("#").toLong(16)), MaterialTheme.shapes.small)) }
                }
            }
        }
    }
}
fun reviewSummary(category: StudioCategory, p: CharacterProject): String = when(category) {
    StudioCategory.Subject -> listOf(category.summary(p), p.identity.bodyType?.wording.orEmpty(), p.face.facialStructure, p.profile.eyeColor.takeIf { it.isNotBlank() }?.plus(" eyes").orEmpty(), p.profile.skinTone, p.profile.distinguishingFeatures, p.expression.preset?.wording.orEmpty(), p.identity.coreVibe).filter { it.isNotBlank() }.joinToString(" · ")
    StudioCategory.Appearance -> listOf(listOfNotNull(p.hair.baseColor.takeIf { it.isNotBlank() }, p.hair.length?.wording, p.hair.style?.wording).joinToString(" "), listOfNotNull(p.costume.outerwear?.wording, p.costume.innerwear?.wording, p.costume.lowerWear?.wording, p.costume.footwear?.wording).joinToString(" · ").ifBlank { category.summary(p) }, p.accessories.items.joinToString(" · ")).filter { it.isNotBlank() }.joinToString("\n")
    StudioCategory.Camera -> listOfNotNull(p.output.framing?.wording, p.composition.cameraAngle?.label, p.output.aspectRatio).joinToString(" · ")
    StudioCategory.Environment -> listOf(category.summary(p), p.environment.time, p.environment.weather, p.environment.mood, p.environment.additionalInstructions).filter { it.isNotBlank() }.joinToString(" · ")
    else -> category.summary(p)
}
