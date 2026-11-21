package com.hyrulecompanion.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hyrulecompanion.app.HyruleViewModel
import com.hyrulecompanion.app.data.AtlasLocation
import com.hyrulecompanion.app.data.AtlasMap
import com.hyrulecompanion.app.data.K_ATLAS_VISITED

@Composable fun TimelineScreen(vm:HyruleViewModel,onGame:(String)->Unit) {
    val routes=vm.catalog.routes
    var selected by remember { mutableStateOf("before") }
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
        itemsIndexed(route.entries) { index,entry ->
            ParchmentCard(Modifier.fillMaxWidth().clickable {onGame(entry.gameId)}) {
                Text("${index+1}. ${entry.name}",style=MaterialTheme.typography.titleLarge)
                Text(entry.era,style=MaterialTheme.typography.bodySmall)
                Text("Ver jogo →",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable private fun WorldMap(map:AtlasMap, locations:List<AtlasLocation>,selectedIndex:Int,visited:Set<Int>,onSelect:(Int)->Unit) {
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    val land=remember(map.land) { runCatching { PathParser().parsePathString(map.land).toPath() }.getOrNull() }
    Canvas(Modifier.fillMaxWidth().height(245.dp).onSizeChanged{bounds=it}.pointerInput(map.id,locations,bounds) {
        detectTapGestures { tap ->
            val closest=locations.indices.minByOrNull { index ->
                val location=locations[index]
                val dx=tap.x-bounds.width*location.x/100f
                val dy=tap.y-bounds.height*location.y/100f
                dx*dx+dy*dy
            }
            if(closest!=null)onSelect(closest)
        }
    }) {
        drawRect(brush=Brush.linearGradient(listOf(Color(0xFF7CB4B0),Color(0xFF376A79)),start=Offset.Zero,end=Offset(size.width,size.height)))
        if(land!=null)withTransform({scale(size.width/1000f,size.height/650f, Offset.Zero)}) {
            drawPath(land,brush=Brush.linearGradient(listOf(Color(0xFF8DB374),Color(0xFF345C37)),start=Offset(60f,10f),end=Offset(920f,650f)))
            drawPath(land,color=Color(0xFFE8CD80),style=Stroke(width=7f))
        }
        locations.forEachIndexed { index,l ->
            val center=Offset(size.width*l.x/100f,size.height*l.y/100f)
            val radius=if(index==selectedIndex)12.dp.toPx() else 8.dp.toPx()
            drawCircle(color=if(visited.contains(index))Gold else Color(0xFFFCF2DA),radius=radius,center=center)
            drawCircle(color=Color(0xFF183828),radius=radius,center=center,style=Stroke(width=2.dp.toPx()))
            if(index==selectedIndex)drawCircle(color=Gold,radius=radius+5.dp.toPx(),center=center,style=Stroke(width=2.dp.toPx()))
        }
    }
}

@Composable fun AtlasScreen(vm:HyruleViewModel,onGame:(String)->Unit){
    val revision=vm.revision
    var query by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("all") }
    var selectedMapId by remember { mutableStateOf("oot") }
    var layer by remember { mutableStateOf("surface") }
    var selectedLocation by remember { mutableIntStateOf(0) }
    var locationQuery by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("Todas") }
    val maps=vm.catalog.atlasMaps
    val shown=maps.filter { (branch=="all" || branch==it.branch) && "${it.name} ${it.subtitle} ${it.branchLabel}".contains(query,true) }
    val map=maps.find { it.id==selectedMapId } ?: maps.first()
    val locations=vm.catalog.locations(map,layer)
    val key=if(map.id=="totk")"totk-$layer" else map.id
    val visited=vm.repo.checked(K_ATLAS_VISITED,key)
    val current=locations.getOrNull(selectedLocation)
    val regions=listOf("Todas")+locations.map { it.region }.distinct()
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {ScreenTitle("Atlas de Hyrule","Mapas esquemáticos originais e marcos selecionáveis","🗺")}
        item {OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),label={Text("Buscar mapa")},singleLine=true)}
        item{Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            listOf("all" to "Todos","shared" to "Antes da divisão","downfall" to "Herói derrotado","child" to "Criança","adult" to "Adulto","future" to "Era distante").forEach{(id,text)->FilterChip(selected=branch==id,onClick={branch=id},label={Text(text)})}
        }}
        item {Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            shown.chunked(2).forEach { row -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                row.forEach{a->FilterChip(modifier=Modifier.weight(1f),selected=selectedMapId==a.id,onClick={selectedMapId=a.id;selectedLocation=0;layer="surface";locationQuery="";region="Todas"},label={Text("${a.symbol} ${a.name}",maxLines=2)})}
                if(row.size==1) Spacer(Modifier.weight(1f))
            }}
        }}
        item {ParchmentCard {
            SmallHeading("${map.symbol} ${map.name}")
            Text(map.subtitle,style=MaterialTheme.typography.labelLarge)
            Text(map.description,style=MaterialTheme.typography.bodyMedium)
            StatusTag(map.branchLabel)
        }}
        if(map.id=="totk") item {Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("sky" to "☁ Céus","surface" to "🌿 Superfície","depths" to "◆ Profundezas").forEach{(code,name)->FilterChip(selected=layer==code,onClick={layer=code;selectedLocation=0;region="Todas"},label={Text(name)})}
        }}
        item {ParchmentCard {
            Text("Toque nos marcadores do mapa para explorar.",style=MaterialTheme.typography.labelSmall)
            WorldMap(map,locations,selectedLocation,visited,{selectedLocation=it})
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
}
