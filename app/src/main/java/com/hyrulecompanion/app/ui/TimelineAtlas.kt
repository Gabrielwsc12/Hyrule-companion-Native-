package com.hyrulecompanion.app.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyrulecompanion.app.HyruleViewModel
import com.hyrulecompanion.app.data.K_ATLAS_VISITED

@Composable fun TimelineScreen(vm:HyruleViewModel,onGame:(String)->Unit) {
    val routes=vm.catalog.routes
    var selected by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("before") }
    val route=routes.find { it.id==selected } ?: routes.first()
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {ScreenTitle("Atlas da Cronologia","Explore as ramificações da história de Hyrule","⌛")}
        item {ParchmentCard {
            Text("Após Ocarina of Time, a cronologia se divide em três caminhos principais. A era de Breath of the Wild permanece distante.")
            Text("▲  A linha do tempo",style=MaterialTheme.typography.titleLarge)
        }}
        item {Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            routes.forEach { r -> FilterChip(selected=selected==r.id,onClick={selected=r.id},label={Text(when(r.id){"before"->"Antes da divisão";"downfall"->"Herói derrotado";"child"->"Criança";"adult"->"Adulto";else->"Era distante"})}) }
        }}
        item {ParchmentCard {SmallHeading(route.title);Text(route.subtitle,style=MaterialTheme.typography.labelLarge);Text(route.description)}}
        itemsIndexed(route.entries) { _,entry ->
            ParchmentCard(Modifier.fillMaxWidth().clickable {onGame(entry.gameId)}) {
                val game = vm.catalog.game(entry.gameId)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    game?.let { CoverArt(it, Modifier.size(64.dp, 84.dp)) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(entry.era, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(entry.name, style = MaterialTheme.typography.titleLarge)
                        if (game != null) Text("${game.year} · ${game.platform}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("›", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private val atlasPages = mapOf(
    "ss" to "skyward-sword",
    "oot" to "ocarina-of-time",
    "alttp" to "a-link-to-the-past",
    "zelda1" to "the-legend-of-zelda",
    "mm" to "majoras-mask",
    "tp" to "twilight-princess",
    "ww" to "the-wind-waker",
    "botw" to "breath-of-the-wild",
    "totk" to "tears-of-the-kingdom",
    "echoes" to "echoes-of-wisdom",
    "minish-cap" to "the-minish-cap",
    "spirit-tracks" to "spirit-tracks",
    "links-awakening-remake" to "links-awakening-switch",
    "alb-w" to "a-link-between-worlds",
    "oracle-ages" to "oracle-of-ages",
    "oracle-seasons" to "oracle-of-seasons",
    "phantom-hourglass" to "phantom-hourglass",
)

private fun mapUrl(gameId: String): String {
    val slug = atlasPages.getValue(gameId)
    val suffix = if (gameId == "zelda1") "-first-and-second-quests" else ""
    return "https://zeldacollection.de/en/walkthroughs/$slug/$slug-interactive-map$suffix/"
}

@Composable private fun MapPreview(mapKey: String, name: String) {
    val assets = androidx.compose.ui.platform.LocalContext.current.assets
    val catalog = remember(assets) {
        assets.open("atlas/maps.json").bufferedReader().use { org.json.JSONObject(it.readText()) }
    }
    val map = catalog.getJSONObject(mapKey)
    val points = map.getJSONArray("points")
    var selected by remember(mapKey) { mutableIntStateOf(-1) }
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(map.getDouble("aspect").toFloat()).clip(RoundedCornerShape(8.dp))) {
        coil.compose.AsyncImage(model = "file:///android_asset/atlas/$mapKey.webp",
            contentDescription = "Mapa de $name", modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds)
        for (index in 0 until points.length()) {
            val point = points.getJSONArray(index)
            val x = maxWidth * point.getDouble(0).toFloat()
            val y = maxHeight * point.getDouble(1).toFloat()
            Box(Modifier.offset(x - 16.dp, y - 16.dp).size(32.dp)
                .semantics { contentDescription = point.getString(2) }
                .clickable { selected = index }, contentAlignment = Alignment.Center) {
                Surface(shape = CircleShape, color = if (selected == index) Color(0xFFFFD86E) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black)) {
                    Text((index + 1).toString(), Modifier.padding(horizontal = 4.dp), color = Color.Black,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    Text(if (selected >= 0) points.getJSONArray(selected).getString(2) else "Toque em um ponto para identificar o local.",
        style = MaterialTheme.typography.bodySmall)
}

@Composable fun AtlasScreen(vm:HyruleViewModel,initialGame:String,onGame:(String)->Unit){
    val revision=vm.revision
    var query by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("all") }
    var selectedMapId by androidx.compose.runtime.saveable.rememberSaveable(initialGame) { mutableStateOf(initialGame) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var layerKey by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableIntStateOf(0) }
    var locationQuery by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("Todas") }
    val maps=vm.catalog.atlasMaps
    val shown=maps.filter { (branch=="all" || branch==it.branch) && "${it.name} ${it.subtitle} ${it.branchLabel}".contains(query,true) }
    val map=maps.find { it.id==selectedMapId } ?: maps.first()
    val layers = vm.catalog.atlasLayers(map)
    val layer = layers.find { it.key == layerKey } ?: layers.first()
    val locations = layer.locations
    val key = layer.key
    val visited=vm.repo.checked(K_ATLAS_VISITED,key)
    var fullscreen by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val current=locations.getOrNull(selectedLocation)
    val regions=listOf("Todas")+locations.map { it.region }.distinct()
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {ScreenTitle("Atlas de Hyrule","Escolha um jogo para explorar o mapa e registrar suas visitas","🗺")}
        item {OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),label={Text("Buscar mapa")},singleLine=true)}
        item{Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            listOf("all" to "Todos","shared" to "Antes da divisão","downfall" to "Herói derrotado","child" to "Criança","adult" to "Adulto","future" to "Era distante").forEach{(id,text)->FilterChip(selected=branch==id,onClick={branch=id},label={Text(text)})}
        }}
        item {Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            shown.chunked(2).forEach { row -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                row.forEach{a->FilterChip(modifier=Modifier.weight(1f),selected=selectedMapId==a.id,onClick={selectedMapId=a.id;selectedLocation=0;layerKey="";locationQuery="";region="Todas"},label={Text("${a.symbol} ${a.name}",maxLines=2)})}
                if(row.size==1) Spacer(Modifier.weight(1f))
            }}
        }}
        item {ParchmentCard {
            SmallHeading("${map.symbol} ${map.name}")
            Text(map.subtitle,style=MaterialTheme.typography.labelLarge)
            Text(map.description,style=MaterialTheme.typography.bodyMedium)
            StatusTag(map.branchLabel)
        }}
        if (layers.size > 1) item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                layers.forEach { entry ->
                    FilterChip(selected = layer.key == entry.key, onClick = {
                        layerKey = entry.key
                        selectedLocation = 0
                        locationQuery = ""
                        region = "Todas"
                    }, label = { Text(entry.label) })
                }
            }
        }
        item {ParchmentCard {
            MapPreview(key, map.name)
            Button(onClick={fullscreen=true},modifier=Modifier.fillMaxWidth()) { Text("Abrir mapa em tela cheia") }
            Button(onClick={
                try {
                    androidx.browser.customtabs.CustomTabsIntent.Builder().setShowTitle(true).build()
                        .launchUrl(context, android.net.Uri.parse(mapUrl(map.id)))
                } catch (_: android.content.ActivityNotFoundException) {
                    android.widget.Toast.makeText(context, "Instale um navegador para abrir o mapa.", android.widget.Toast.LENGTH_LONG).show()
                }
            },modifier=Modifier.fillMaxWidth()) { Text("Abrir mapa de referência online") }
            Text("Fonte dos mapas e pontos: ZeldaCollection. O mapa completo requer internet.",style=MaterialTheme.typography.labelSmall)
            Text("${visited.size} / ${locations.size} marcos visitados",style=MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress={if(locations.isEmpty())0f else visited.size.toFloat()/locations.size},modifier=Modifier.fillMaxWidth())
        }}
        if(current!=null) item{ParchmentCard {
            SmallHeading(current.name)
            Text(current.region,style=MaterialTheme.typography.labelSmall)
            Text(current.description)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedButton(onClick={selectedLocation=(selectedLocation-1+locations.size)%locations.size},enabled=locations.size>1){Text("←")}
                Button(onClick={vm.toggleChecked(K_ATLAS_VISITED,key,selectedLocation)},modifier=Modifier.weight(1f)){
                    Text(if(selectedLocation in visited)"✓ Visitado" else "Marcar visitado")
                }
                OutlinedButton(onClick={selectedLocation=(selectedLocation+1)%locations.size},enabled=locations.size>1){Text("→")}
            }
        }}
        item {SmallHeading("Índice de locais")}
        item {OutlinedTextField(locationQuery,{locationQuery=it},modifier=Modifier.fillMaxWidth(),label={Text("Buscar ponto no mapa")},singleLine=true)}
        item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            regions.forEach{r->FilterChip(selected=r==region,onClick={region=r},label={Text(r)})}
        }}
        itemsIndexed(locations.filter { (region=="Todas" || it.region==region) && "${it.name} ${it.region}".contains(locationQuery,true) }) {_,loc ->
            val index=locations.indexOf(loc)
            ParchmentCard(Modifier.fillMaxWidth().clickable {selectedLocation=index}){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text(if(index in visited)"✓" else "○",color=MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {Text(loc.name,style=MaterialTheme.typography.titleMedium);Text(loc.region,style=MaterialTheme.typography.labelSmall)}
                    Text("${index+1}/${locations.size}",style=MaterialTheme.typography.labelSmall)
                }
            }
        }
        item { OutlinedButton(onClick={onGame(map.gameId)},modifier=Modifier.fillMaxWidth()){Text("Ver detalhes de ${map.name}")} }
    }
    if (fullscreen) InteractiveAtlas(key, map.name, locations,
        layers = layers.map { it.key to it.label }, onLayerChange = {
            layerKey = it
            selectedLocation = 0
            locationQuery = ""
            region = "Todas"
        }, onDismiss={fullscreen=false})
}
