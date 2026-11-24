package com.hyrulecompanion.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size as CoilSize
import com.hyrulecompanion.app.data.AtlasLocation
import org.json.JSONObject

internal data class AtlasPin(val x: Float, val y: Float, val name: String, val category: String, val description: String)

@Composable
internal fun InteractiveAtlas(
    mapKey: String, title: String, locations: List<AtlasLocation>,
    layers: List<Pair<String, String>>, onLayerChange: (String) -> Unit, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val assets = context.assets
    val mapImage = remember(context, mapKey) {
        ImageRequest.Builder(context).data("file:///android_asset/atlas/$mapKey.webp")
            .size(CoilSize.ORIGINAL).build()
    }
    val map = remember(assets, mapKey) {
        assets.open("atlas/maps.json").bufferedReader().use { JSONObject(it.readText()).getJSONObject(mapKey) }
    }
    val pins = remember(map, locations) {
        val points = map.getJSONArray("points")
        (0 until points.length()).map { index ->
            val point = points.getJSONArray(index)
            val name = point.getString(2)
            val place = locations.find { it.name.equals(name, ignoreCase = true) }
            val category = point.optString(3).ifBlank { atlasCategory(name) }
            AtlasPin(point.getDouble(0).toFloat(), point.getDouble(1).toFloat(), name, category,
                point.optString(4).ifBlank { place?.description ?: atlasDescription(name, category) })
        }
    }
    val categories = remember(pins) { listOf("Todos") + pins.map { it.category }.distinct() }
    var category by remember(mapKey) { mutableStateOf("Todos") }
    var showPoints by remember(mapKey) { mutableStateOf(false) }
    var selected by remember(mapKey) { mutableStateOf<AtlasPin?>(null) }
    var scale by remember(mapKey) { mutableFloatStateOf(1f) }
    var pan by remember(mapKey) { mutableStateOf(Offset.Zero) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onDismiss) { Text("Fechar") }
                }
                if (layers.size > 1) Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    layers.forEach { (key, label) ->
                        FilterChip(selected = key == mapKey, onClick = { onLayerChange(key) }, label = { Text(label) })
                    }
                }
                if (categories.size > 2) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { value ->
                        FilterChip(selected = value == category, onClick = {
                            category = value
                            if (value != "Todos" && selected?.category != value) selected = null
                        }, label = { Text(value) })
                    }
                }
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).clipToBounds().background(MaterialTheme.colorScheme.surfaceVariant)) {
                    val density = LocalDensity.current
                    val aspect = map.getDouble("aspect").toFloat()
                    val viewWidth = maxWidth
                    val viewHeight = maxHeight
                    val width = minOf(viewWidth, viewHeight * aspect)
                    val height = width / aspect
                    fun clampPan(candidate: Offset, zoom: Float): Offset = with(density) {
                        val xBound = ((width * zoom - viewWidth).toPx() / 2).coerceAtLeast(0f)
                        val yBound = ((height * zoom - viewHeight).toPx() / 2).coerceAtLeast(0f)
                        Offset(candidate.x.coerceIn(-xBound, xBound), candidate.y.coerceIn(-yBound, yBound))
                    }
                    fun setZoom(value: Float, movement: Offset = Offset.Zero) {
                        scale = value.coerceIn(1f, 6f)
                        pan = clampPan(pan + movement, scale)
                    }
                    val transform = rememberTransformableState { zoom, movement, _ -> setZoom(scale * zoom, movement) }
                    val position = clampPan(pan, scale)
                    val mapWidth = width * scale
                    val mapHeight = height * scale
                    val left = (viewWidth - mapWidth) / 2 + with(density) { position.x.toDp() }
                    val top = (viewHeight - mapHeight) / 2 + with(density) { position.y.toDp() }
                    fun markerOffset(pin: AtlasPin): Pair<Dp, Dp>? {
                        val x = left + mapWidth * pin.x
                        val y = top + mapHeight * pin.y
                        if (x !in 0.dp..viewWidth || y !in 0.dp..viewHeight) return null
                        return (x - 15.dp).coerceIn(0.dp, (viewWidth - 30.dp).coerceAtLeast(0.dp)) to
                            (y - 15.dp).coerceIn(0.dp, (viewHeight - 30.dp).coerceAtLeast(0.dp))
                    }
                    Box(Modifier.fillMaxSize().transformable(transform).clipToBounds()) {
                        AsyncImage(mapImage, "Mapa de $title",
                            Modifier.offset(left, top).wrapContentSize(Alignment.TopStart, unbounded = true)
                                .requiredSize(mapWidth, mapHeight),
                            contentScale = ContentScale.FillBounds)
                        pins.forEachIndexed { index, pin ->
                            if (category == "Todos" || pin.category == category) {
                                val marker = markerOffset(pin)
                                if (marker != null) {
                                    Surface(Modifier.offset(marker.first, marker.second)
                                        .size(30.dp).zIndex(if (pin == selected) 1f else 0f)
                                        .semantics { contentDescription = pin.name }.clickable { selected = if (selected == pin) null else pin },
                                        shape = CircleShape, color = if (pin == selected)
                                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface) {
                                        Box(contentAlignment = Alignment.Center) { Text("${index + 1}", style = MaterialTheme.typography.labelSmall) }
                                    }
                                }
                            }
                        }
                    }
                    Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                        FilledTonalButton(onClick = { showPoints = true }) { Text("Pontos do mapa") }
                        DropdownMenu(expanded = showPoints, onDismissRequest = { showPoints = false },
                            modifier = Modifier.heightIn(max = 280.dp)) {
                            pins.forEachIndexed { index, pin ->
                                if (category == "Todos" || category == pin.category) {
                                    DropdownMenuItem(text = { Text("${index + 1}. ${translate(pin.name)}") }, onClick = {
                                        showPoints = false
                                        selected = pin
                                        scale = maxOf(2f, scale)
                                        pan = with(density) {
                                            clampPan(Offset((width * scale).toPx() * (.5f - pin.x),
                                                (height * scale).toPx() * (.5f - pin.y)), scale)
                                        }
                                    })
                                }
                            }
                        }
                    }
                    Row(Modifier.align(Alignment.BottomEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalIconButton(onClick = { setZoom(scale / 1.5f) }, enabled = scale > 1f,
                            modifier = Modifier.semantics { contentDescription = translate("Diminuir zoom") }) { Text("−") }
                        FilledTonalIconButton(onClick = { setZoom(scale * 1.5f) }, enabled = scale < 6f,
                            modifier = Modifier.semantics { contentDescription = translate("Aumentar zoom") }) { Text("+") }
                        FilledTonalIconButton(onClick = { setZoom(1f) },
                            modifier = Modifier.semantics { contentDescription = translate("Restaurar zoom") }) { Text("↺") }
                    }
                    selected?.let { pin ->
                        markerOffset(pin)?.let { (x, y) ->
                            AtlasPinDetails(pin, x, y, onClose = { selected = null })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AtlasPinDetails(pin: AtlasPin, markerX: Dp, markerY: Dp, onClose: () -> Unit) {
    Layout(
        modifier = Modifier.fillMaxSize().zIndex(2f),
        content = {
            ElevatedCard(Modifier.widthIn(max = 232.dp)) {
                Column(Modifier.padding(start = 12.dp, end = 8.dp, bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(pin.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        IconButton(onClick = onClose, modifier = Modifier.semantics {
                            contentDescription = translate("Fechar detalhes do ponto")
                        }) { Text("×") }
                    }
                    Column(Modifier.verticalScroll(key(pin) { rememberScrollState() })) {
                        Text(pin.category, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Text(pin.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val markerLeft = markerX.roundToPx()
        val markerTop = markerY.roundToPx()
        val markerSize = 30.dp.roundToPx()
        val gap = 6.dp.roundToPx()
        val leftSpace = (markerLeft - gap).coerceAtLeast(0)
        val rightSpace = (width - markerLeft - markerSize - gap).coerceAtLeast(0)
        val sideSpace = maxOf(leftSpace, rightSpace)
        val useSide = sideSpace >= 144.dp.roundToPx()
        val card = measurables.single().measure(Constraints(
            maxWidth = if (useSide) sideSpace else width,
            maxHeight = minOf(186.dp.roundToPx(), if (useSide) height else maxOf(
                markerTop - gap, height - markerTop - markerSize - gap
            ).coerceAtLeast(0))
        ))

        layout(width, height) {
            val x = when {
                !useSide -> markerLeft + (markerSize - card.width) / 2
                card.width <= rightSpace -> markerLeft + markerSize + gap
                else -> markerLeft - card.width - gap
            }.coerceIn(0, width - card.width)
            val y = when {
                useSide -> markerTop + (markerSize - card.height) / 2
                markerTop + markerSize + gap + card.height <= height -> markerTop + markerSize + gap
                else -> markerTop - card.height - gap
            }.coerceIn(0, height - card.height)
            card.place(x, y)
        }
    }
}

private fun atlasCategory(name: String): String = when {
    listOf("santuário", "shrine", "tower", "lightroot", "teletransporte").any { name.contains(it, true) } -> "Exploração"
    listOf("heart", "coração", "stamp", "peach", "concha", "loja", "mercado").any { name.contains(it, true) } -> "Itens e colecionáveis"
    listOf("temple", "templo", "palácio", "dungeon", "caverna").any { name.contains(it, true) } -> "Dungeons e acessos"
    else -> "Locais"
}

private fun atlasDescription(name: String, category: String): String = when {
    name.contains("Skyview Tower", true) -> "Torre de observação. Ative o terminal para liberar viagem rápida e revelar a região no mapa."
    name.contains("Shrine", true) -> "Entrada de um santuário. Examine o ponto no mapa para localizar o acesso à prova."
    name.contains("Lightroot", true) -> "Raiz luminosa das Profundezas. Ative-a para iluminar uma parte do mapa subterrâneo."
    name.contains("Stamp", true) -> "Local de um carimbo colecionável. Interaja com a estação para preencher seu cartão."
    name.contains("Heart", true) -> "Local ou acesso indicado para obter uma melhoria de coração. Confira o requisito no mapa de referência do jogo."
    category == "Dungeons e acessos" -> "Marco da dungeon ou de seu acesso no mapa da região."
    else -> "Ponto de referência para explorar a região de $name."
}
