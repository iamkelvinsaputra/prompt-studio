package com.kelvinsaputra.promptstudio.feature.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.feature.editor.*
import com.kelvinsaputra.promptstudio.guide.VisualGuideRegistry

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> Choices(title: String, options: List<T>, selected: T?, label: (T) -> String = { it.toString() }, choose: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (title.isNotBlank()) Text(title, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option -> FilterChip(option == selected, { choose(option) }, label = { Text(label(option)) }) }
        }
    }
}

@Composable
fun CharacterControls(p: CharacterProject, change: (CharacterProject) -> Unit) {
    TextField("Character name", p.identity.characterName) { change(p.copy(identity = p.identity.copy(characterName = it), profile = p.profile.copy(enabled = true))) }
    TextButton(onClick = { val names = listOf("Alex", "Morgan", "Robin", "Sam", "Casey", "River"); val next = names[(names.indexOf(p.characterName) + 1) % names.size]; change(p.copy(identity = p.identity.copy(characterName = next))) }) { Text("Use a placeholder name") }
    Choices("Character template", characterGenders, p.identity.genderPresentation, { it.wording.replaceFirstChar { c -> c.uppercase() } }) { change(p.copy(identity = p.identity.copy(genderPresentation = it))) }
    Choices("Age presentation", AgeBand.entries, p.identity.ageBand, { it.wording.replaceFirstChar { c -> c.uppercase() } }) { change(p.copy(identity = p.identity.copy(ageBand = it))) }
    Choices("Starting personality", archetypes(p.identity.genderPresentation), archetypes(p.identity.genderPresentation).firstOrNull { it.equals(p.identity.coreVibe, true) }) { change(p.withArchetype(it)) }
    Text("Starting points only. Every trait can be customized.", style = MaterialTheme.typography.bodySmall)
    AdvancedSection("Character profile & identity lock") {
        Text("Lock traits to keep them stable during edits, presets and variants. Unlock a trait before changing it. Consistent wording helps; generated images can still vary.", style = MaterialTheme.typography.bodySmall)
        IdentityTrait.entries.forEach { trait ->
            ToggleField("Lock ${trait.label.lowercase()}", trait in p.profile.lockedTraits) { enabled -> change(p.copy(profile = p.profile.copy(lockedTraits = if (enabled) p.profile.lockedTraits + trait else p.profile.lockedTraits - trait))) }
        }
        if (IdentityTrait.BODY !in p.profile.lockedTraits) {
            OptionField("Body type", p.identity.bodyType, BodyType.entries) { change(p.copy(identity = p.identity.copy(bodyType = it))) }
            OptionField("Height impression", p.body.heightImpression, HeightImpression.entries) { change(p.copy(body = p.body.copy(heightImpression = it))) }
            TextField("Body proportions", p.body.proportions) { change(p.copy(body = p.body.copy(proportions = it))) }
        }
        if (IdentityTrait.SKIN !in p.profile.lockedTraits) {
            Choices("Skin tone", listOf("Fair neutral", "Light warm", "Medium olive", "Tan", "Deep brown", "Rich dark"), p.profile.skinTone) { change(p.copy(profile = p.profile.copy(skinTone = it))) }
            TextField("Skin tone · customize", p.profile.skinTone) { change(p.copy(profile = p.profile.copy(skinTone = it))) }
            TextField("Broad appearance / ethnicity · optional", p.profile.broadAppearance) { change(p.copy(profile = p.profile.copy(broadAppearance = it))) }
        }
        if (IdentityTrait.FACE !in p.profile.lockedTraits) {
            Choices("Face shape", listOf("Oval", "Round", "Angular", "Heart-shaped", "Long"), p.face.facialStructure) { change(p.copy(face = p.face.copy(facialStructure = it))) }
            TextField("Jaw shape", p.profile.jawShape) { change(p.copy(profile = p.profile.copy(jawShape = it))) }
            TextField("Nose", p.profile.nose) { change(p.copy(profile = p.profile.copy(nose = it))) }
            TextField("Eyebrows", p.face.eyebrowBehavior) { change(p.copy(face = p.face.copy(eyebrowBehavior = it))) }
            TextField("Mouth", p.face.mouthOrSmile) { change(p.copy(face = p.face.copy(mouthOrSmile = it))) }
        }
        if (IdentityTrait.EYES !in p.profile.lockedTraits) {
            TextField("Eye shape", p.face.eyeShape) { change(p.copy(face = p.face.copy(eyeShape = it))) }
            Choices("Eye color", listOf("Brown", "Hazel", "Blue-gray", "Green", "Dark brown"), p.profile.eyeColor) { change(p.copy(profile = p.profile.copy(eyeColor = it))) }
            TextField("Eye color · customize", p.profile.eyeColor) { change(p.copy(profile = p.profile.copy(eyeColor = it))) }
        }
        if (IdentityTrait.FEATURES !in p.profile.lockedTraits) TextField("Distinguishing features", p.profile.distinguishingFeatures) { change(p.copy(profile = p.profile.copy(distinguishingFeatures = it))) }
    }
}

@Composable
fun AppearanceControls(p: CharacterProject, change: (CharacterProject) -> Unit) {
    AdvancedSection("Hair & expression") {
        if (IdentityTrait.HAIR in p.profile.lockedTraits) Text("Hair is locked in Character profile.") else {
            Choices("Hair color", listOf("Black", "Brown", "Blond", "Auburn", "Silver", "Blue-black"), p.hair.baseColor) { change(p.copy(hair = p.hair.copy(baseColor = it))) }
            TextField("Hair color · customize", p.hair.baseColor) { change(p.copy(hair = p.hair.copy(baseColor = it))) }
            OptionField("Length", p.hair.length, HairLength.entries) { change(p.copy(hair = p.hair.copy(length = it))) }
            OptionField("Hair style", p.hair.style, HairStyle.entries) { change(p.copy(hair = p.hair.copy(style = it))) }
            Choices("Hair texture", listOf("Fine", "Straight", "Wavy", "Curly", "Coily", "Thick"), p.profile.hairTexture) { change(p.copy(profile = p.profile.copy(hairTexture = it))) }
        }
        OptionChips("Expression", ExpressionPreset.entries, p.expression.preset) { change(p.copy(expression = p.expression.copy(preset = it))) }
    }
    var mode by remember { mutableStateOf("Templates") }
    var category by remember { mutableStateOf("Casual") }
    Choices("Outfit", listOf("Templates", "Build"), mode) { mode = it }
    if (mode == "Templates") {
        ChoiceField("Collection", category, OutfitLibrary.categories, { it }) { category = it }
        val options = OutfitLibrary.entries.filter { it.category == category && it.gender == (p.identity.genderPresentation.takeIf { g -> g in characterGenders } ?: GenderPresentation.FEMALE) }
        options.forEach { template ->
            OutlinedCard(onClick = { change(template.applyTo(p)) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(template.label.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                    Text("${template.costume.footwear?.wording} · ${template.costume.materialFeel}", style = MaterialTheme.typography.bodySmall)
                    Text(if (p.costume.copy(customNotes = "") == template.costume) "Selected" else "Use this outfit", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    } else {
        OptionField("Outer layer", p.costume.outerwear, Outerwear.entries) { change(p.copy(costume = p.costume.copy(outerwear = it))) }
        OptionField("Upper garment", p.costume.innerwear, Innerwear.entries) { change(p.copy(costume = p.costume.copy(innerwear = it))) }
        OptionField("Lower garment", p.costume.lowerWear, LowerWear.entries) { change(p.copy(costume = p.costume.copy(lowerWear = it))) }
        OptionField("Footwear", p.costume.footwear, Footwear.entries) { change(p.copy(costume = p.costume.copy(footwear = it))) }
        OptionField("Fit", p.costume.silhouette, Silhouette.entries) { change(p.copy(costume = p.costume.copy(silhouette = it))) }
        Choices("Materials", listOf("Cotton", "Linen", "Wool", "Denim", "Matte technical fabric", "Leather accents"), p.costume.materialFeel) { change(p.copy(costume = p.costume.copy(materialFeel = it))) }
        TextField("Materials · customize", p.costume.materialFeel) { change(p.copy(costume = p.costume.copy(materialFeel = it))) }
        Choices("Layering", listOf("Light single layer", "Open outer layer", "Layered for warmth", "Contrasting lengths"), p.costume.layering) { change(p.copy(costume = p.costume.copy(layering = it))) }
        BoundedTextListEditor("Accessory", p.accessories.items, 3) { change(p.copy(accessories = p.accessories.copy(items = it))) }
        TextField("Custom outfit details", p.costume.customNotes, multiline = true) { change(p.copy(costume = p.costume.copy(customNotes = it))) }
    }
}

@Composable
fun EnvironmentControls(p: CharacterProject, change: (CharacterProject) -> Unit) {
    val e = p.environment
    VisualOptionGrid("Choose a setting", VisualGuideRegistry.environment, e.category) { change(p.withEnvironment(it)) }
    e.category?.takeIf { it.locations.isNotEmpty() }?.let { category -> Choices("Place", category.locations, e.worldContextHint) { change(p.copy(environment = e.copy(worldContextHint = it))) } }
    AdvancedSection("Environment details") {
        if (e.category !in listOf(EnvironmentCategory.NONE, EnvironmentCategory.MINIMAL, EnvironmentCategory.ABSTRACT)) {
            Choices("Time of day", listOf("Dawn", "Morning", "Midday", "Afternoon", "Dusk", "Night"), e.time) { change(p.copy(environment = e.copy(time = it))) }
            if (e.category?.outdoors == true) {
                Choices("Weather", listOf("Clear", "Overcast", "Rainy", "Snowy", "Misty"), e.weather) { change(p.copy(environment = e.copy(weather = it))) }
                Choices("Season", listOf("Spring", "Summer", "Autumn", "Winter"), e.season) { change(p.copy(environment = e.copy(season = it))) }
            }
            Choices("Density", listOf("Empty", "Quiet", "Lively", "Crowded"), e.density) { change(p.copy(environment = e.copy(density = it))) }
            Choices("Depth", listOf("Shallow", "Layered", "Distant horizon"), e.depthBehavior) { change(p.copy(environment = e.copy(depthBehavior = it))) }
            Choices("Background detail", listOf("Suggested", "Soft focus", "Clearly defined"), e.backgroundDetail) { change(p.copy(environment = e.copy(backgroundDetail = it))) }
            Choices("Mood", listOf("Peaceful", "Mysterious", "Lively", "Lonely"), e.mood) { change(p.copy(environment = e.copy(mood = it))) }
            TextField("Foreground elements", e.foreground) { change(p.copy(environment = e.copy(foreground = it))) }
            if (e.category != EnvironmentCategory.NATURE) TextField("Architecture", e.architecture) { change(p.copy(environment = e.copy(architecture = it))) }
        }
        TextField("Custom environment description", e.additionalInstructions, multiline = true) { change(p.copy(environment = e.copy(additionalInstructions = it))) }
    }
}

@Composable
fun LightingControls(p: CharacterProject, change: (CharacterProject) -> Unit) {
    ChoiceField("Lighting preset", lightingPresets.firstOrNull { it.name == p.lighting.preset }, listOf(null) + lightingPresets, { it?.name ?: "Custom / soft daylight" }) { it?.let { preset -> change(preset.applyTo(p)) } }
    VisualOptionGrid("Where is the light?", VisualGuideRegistry.lighting, p.lighting.lightDirection) { change(p.copy(lighting = p.lighting.copy(lightDirection = it, direction = "", preset = ""))) }
    Choices("Light character", listOf("soft", "hard", "diffused", "dramatic", "flat", "high contrast", "low contrast"), p.lighting.quality, { it.replaceFirstChar { c -> c.uppercase() } }) { change(p.copy(lighting = p.lighting.copy(quality = it, contrast = "", shadowSoftness = null, sourceQuality = null, preset = ""))) }
    AdvancedSection("Lighting details") {
        Choices("Temperature", listOf("warm", "neutral", "cool"), p.lighting.temperature) { change(p.copy(lighting = p.lighting.copy(temperature = it, preset = ""))) }
        Choices("Contrast", listOf("low", "medium", "high"), p.lighting.contrast) { change(p.copy(lighting = p.lighting.copy(contrast = it, preset = ""))) }
        TextField("Custom lighting description", p.lighting.additionalInstructions, multiline = true) { change(p.copy(lighting = p.lighting.copy(additionalInstructions = it))) }
    }
}

private fun hexColor(hex: String) = Color(0xFF000000L or hex.removePrefix("#").toLong(16))
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorControls(p: CharacterProject, change: (CharacterProject) -> Unit) {
    val d = p.colorDirection
    var picker by remember { mutableStateOf<ColorRole?>(null) }
    var all by remember { mutableStateOf(false) }
    TextField("Color direction", d.description) { change(p.copy(colorDirection = d.copy(description = it))) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        palettePresets.take(if (all) palettePresets.size else 6).forEach { preset ->
            OutlinedCard(onClick = { change(p.copy(colorDirection = preset.direction(), colorAccents = ColorAccentConfiguration())) }) {
                Column(Modifier.padding(12.dp)) {
                    Row { preset.hexes.forEach { Box(Modifier.size(24.dp).background(hexColor(it))) } }
                    Text(preset.name, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
    TextButton(onClick = { all = !all }) { Text(if (all) "Fewer palettes" else "More palettes") }
    Text("Your palette · select a swatch to edit", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorRole.entries.forEach { role ->
            val color = d.colors.firstOrNull { it.role == role }
            OutlinedCard(onClick = { picker = role }, modifier = Modifier.width(136.dp).semantics { contentDescription = "Edit ${role.label} color" }) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth().height(42.dp).background(color?.let { hexColor(it.hex) } ?: MaterialTheme.colorScheme.surfaceContainerHighest))
                    Text(role.label, style = MaterialTheme.typography.labelMedium)
                    Text(color?.hex ?: "+ Add color", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    picker?.let { role -> ColorPicker(role, d.colors.firstOrNull { it.role == role }?.hex, { picker = null }) { hex -> change(p.copy(colorDirection = d.withColor(role, hex))); picker = null } }
    AdvancedSection("Color balance") {
        RangeControl("Saturation", "Low", "High", d.saturation ?: 50) { change(p.copy(colorDirection = d.copy(saturation = it))) }
        RangeControl("Contrast", "Low", "High", d.contrast ?: 50) { change(p.copy(colorDirection = d.copy(contrast = it))) }
        RangeControl("Temperature", "Cool", "Warm", d.temperature ?: 50) { change(p.copy(colorDirection = d.copy(temperature = it))) }
        RangeControl("Brightness", "Dark", "Light", d.brightness ?: 50) { change(p.copy(colorDirection = d.copy(brightness = it))) }
    }
}
@Composable
private fun RangeControl(label: String, low: String, high: String, value: Int, change: (Int) -> Unit) {
    Text("$label · $value", style = MaterialTheme.typography.labelLarge)
    Slider(value.toFloat(), { change(it.toInt()) }, valueRange = 0f..100f, modifier = Modifier.semantics { contentDescription = label })
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(low, style = MaterialTheme.typography.bodySmall); Text(high, style = MaterialTheme.typography.bodySmall) }
}
@Composable
private fun ColorPicker(role: ColorRole, initial: String?, dismiss: () -> Unit, save: (String?) -> Unit) {
    var hex by remember(role) { mutableStateOf(initial ?: "#7D688C") }
    val valid = normalizeHex(hex)
    val color = hexColor(valid ?: "#7D688C")
    AlertDialog(onDismissRequest = dismiss, title = { Text(role.label) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth().height(72.dp).background(color, MaterialTheme.shapes.medium))
            OutlinedTextField(hex, { hex = it }, singleLine = true, label = { Text("HEX") }, isError = valid == null, supportingText = { Text(if (valid == null) "Enter six hexadecimal digits, such as #7D688C" else "Adjust the channels or type a HEX color") })
            listOf("Red", "Green", "Blue").forEachIndexed { index, name ->
                val channels = listOf(color.red, color.green, color.blue).map { (it * 255).toInt() }
                Text("$name · ${channels[index]}", style = MaterialTheme.typography.labelMedium)
                Slider(channels[index].toFloat(), { amount -> hex = "#" + channels.mapIndexed { i, c -> (if (i == index) amount.toInt() else c).toString(16).padStart(2, '0') }.joinToString("").uppercase() }, valueRange = 0f..255f, modifier = Modifier.semantics { contentDescription = "$name color channel" })
            }
            TextButton(onClick = { save(null) }) { Text("Remove color") }
        }
    }, confirmButton = { Button(onClick = { save(valid) }, enabled = valid != null) { Text("Apply color") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EffectsControls(p: CharacterProject, change: (CharacterProject) -> Unit) {
    var family by remember { mutableStateOf<EffectFamily?>(p.effects.items.firstOrNull()?.family) }
    Choices("Effect family", listOf<EffectFamily?>(null) + EffectFamily.entries, family, { it?.label ?: "None" }) {
        family = it
        if (it == null) change(p.copy(effects = EffectsConfiguration(), powerSignature = PowerSignatureConfiguration()))
    }
    family?.let { selected ->
        Text("Choose effects", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            selected.choices.forEach { name ->
                val item = EffectItem(selected, name)
                val items = p.effects.items
                FilterChip(item in items, {
                    if (item in items || items.size < 6) change(p.copy(effects = p.effects.copy(items = if (item in items) items - item else items + item)))
                }, enabled = item in items || items.size < 6, label = { Text(name) })
            }
        }
    }

    if (p.effects.items.isNotEmpty()) {
        Text("Selected · tap to remove", style = MaterialTheme.typography.labelLarge)
        Choices("", p.effects.items, null as EffectItem?, { "Remove ${it.name}" }) { change(p.copy(effects = p.effects.copy(items = p.effects.items - it))) }
        Choices("Intensity", EffectIntensity.entries, p.effects.intensity, { it.label }) { change(p.copy(effects = p.effects.copy(intensity = it))) }
        Text("Up to six effects. A little atmosphere goes a long way.", style = MaterialTheme.typography.bodySmall)
    } else Text("No effects. Keep the focus on your character.", style = MaterialTheme.typography.bodyMedium)
    AdvancedSection("Custom effects") {
        if (p.effects.items.any { it.family == EffectFamily.WIND || it.family == EffectFamily.MOTION }) Choices("Direction", listOf("Left to right", "Right to left", "Toward viewer", "Away from viewer"), p.effects.direction) { change(p.copy(effects = p.effects.copy(direction = it))) }
        TextField("Custom effect description", p.effects.customDescription, multiline = true) { change(p.copy(effects = p.effects.copy(customDescription = it))) }
    }
}
