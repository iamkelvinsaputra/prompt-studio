package com.kelvinsaputra.promptstudio.feature.studio

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kelvinsaputra.promptstudio.guide.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.*
import promptstudio.shared.generated.resources.Res

private val LocalGuideImageCache = staticCompositionLocalOf<MutableMap<String, Painter?>> { mutableMapOf() }
private val LocalGuideAssets = staticCompositionLocalOf<Map<String, VisualGuideAsset>> { emptyMap() }

@Composable
fun VisualGuideAssets(content: @Composable () -> Unit) {
    val assets by produceState<Map<String, VisualGuideAsset>>(emptyMap()) {
        value = try { Json.decodeFromString<Map<String, VisualGuideAsset>>(Res.readBytes("files/visual-guides/registry.json").decodeToString()) }
        catch (e: CancellationException) { throw e } catch (_: Exception) { emptyMap() }
    }
    val cache = remember { mutableMapOf<String, Painter?>() }
    CompositionLocalProvider(LocalGuideAssets provides assets, LocalGuideImageCache provides cache, content = content)
}

enum class OptionDensity { Comfortable, Compact }

/** One control for every spatial library. Surface provides keyboard activation and focus semantics. */
@Composable
fun <T> VisualOptionCard(option: VisualGuideOption<T>, selected: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, density: OptionDensity = OptionDensity.Comfortable) {
    val asset = LocalGuideAssets.current[option.key]
    val label = asset?.label ?: option.label
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    Surface(onClick = onClick, interactionSource = interaction,
        modifier = modifier.hoverable(interaction).semantics { this.selected = selected; role = Role.RadioButton; contentDescription = label },
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected || focused) 2.dp else 1.dp,
            if (selected || focused) MaterialTheme.colorScheme.primary else if (hovered) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AssetImage(asset, label, Modifier.fillMaxWidth().height(if (density == OptionDensity.Compact) 86.dp else 132.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (selected) {
                    val ink = MaterialTheme.colorScheme.primary
                    Canvas(Modifier.size(16.dp)) {
                        drawLine(ink, Offset(size.width * .15f, size.height * .5f), Offset(size.width * .4f, size.height * .75f), 2.dp.toPx(), StrokeCap.Round)
                        drawLine(ink, Offset(size.width * .4f, size.height * .75f), Offset(size.width * .85f, size.height * .2f), 2.dp.toPx(), StrokeCap.Round)
                    }
                }
            }
            Text(asset?.description ?: option.promptValue, modifier = Modifier.height(34.dp).alpha(if (hovered || focused) 1f else 0f), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun AssetImage(asset: VisualGuideAsset?, label: String, modifier: Modifier = Modifier) {
    val path = asset?.thumbnail ?: asset?.image
    val density = LocalDensity.current
    val cache = LocalGuideImageCache.current
    val painter by produceState<Painter?>(null, path, density) {
        value = null
        if (path != null) {
            val cacheKey = "$path@${density.density}"
            if (cache.containsKey(cacheKey)) { value = cache[cacheKey]; return@produceState }
            value = try {
                val bytes = Res.readBytes("files/visual-guides/$path")
                if (path.endsWith(".svg", true)) decodeGuideSvg(bytes, density) else BitmapPainter(bytes.decodeToImageBitmap())
            } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
            if (cache.size >= 64) cache.remove(cache.keys.first())
            cache[cacheKey] = value
        }
    }
    if (painter != null) Image(painter!!, "$label guide", modifier)
    else Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
            Text("+", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.outline)
            Text("Text guide", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> VisualOptionGrid(title: String, options: List<VisualGuideOption<T>>, selected: T?, onSelect: (T) -> Unit) {
    val assets = LocalGuideAssets.current
    var search by remember(title) { mutableStateOf("") }
    var expanded by remember(title) { mutableStateOf(false) }
    var compact by remember(title) { mutableStateOf(false) }
    val sorted = options.sortedBy { assets[it.key]?.order ?: 100 }
    val filtered = sorted.filter { it.matches(search, assets[it.key]) }
    val shown = if (expanded || search.isNotBlank()) filtered else (sorted.take(6) + sorted.filter { it.value == selected }).distinct()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            if (options.size > 6) TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Show less" else "Browse all ${options.size}") }
        }
        if (expanded) {
            OutlinedTextField(search, { search = it }, label = { Text("Search $title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(compact, { compact = !compact }, label = { Text("Compact cards") })
                Spacer(Modifier.width(12.dp)); Text("${filtered.size} options", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (shown.isEmpty()) Text("No matching guides. Try another word, or describe your own below.", style = MaterialTheme.typography.bodyMedium)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = (maxWidth / if (compact) 124.dp else 156.dp).toInt().coerceIn(2, if (compact) 5 else 3).coerceAtMost(shown.size.coerceAtLeast(2))
            val width = (maxWidth - 12.dp * (columns - 1)) / columns
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                shown.forEach { option -> key(option.key) {
                    VisualOptionCard(option, option.value == selected, { onSelect(option.value) }, Modifier.width(width), if (compact) OptionDensity.Compact else OptionDensity.Comfortable)
                } }
            }
        }
        options.firstOrNull { it.value == selected }?.let { option ->
            Text(assets[option.key]?.description ?: option.promptValue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AdvancedSection(title: String = "Refine", content: @Composable ColumnScope.() -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        TextButton(onClick = { open = !open }, modifier = Modifier.fillMaxWidth()) {
            Text(title, Modifier.weight(1f)); Text(if (open) "-" else "+")
        }
        if (open) content()
    }
}
