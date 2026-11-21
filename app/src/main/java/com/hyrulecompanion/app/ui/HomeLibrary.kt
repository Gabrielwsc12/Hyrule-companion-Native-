package com.hyrulecompanion.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.hyrulecompanion.app.HyruleViewModel
import com.hyrulecompanion.app.data.Game

@Composable fun CoverArt(game:Game,modifier:Modifier=Modifier) {
    if(game.cover.isNullOrBlank()) Box(modifier,contentAlignment=Alignment.Center){Text(game.icon,style=MaterialTheme.typography.headlineMedium)}
    else SubcomposeAsyncImage(model=game.cover,contentDescription="Capa de ${game.name}",modifier=modifier.clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Crop,
        loading={Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(game.icon)}},
        error={Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(game.icon)}})
}

@Composable fun HomeScreen(vm:HyruleViewModel,onNavigate:(String)->Unit,onGame:(String)->Unit) {
    val revision=vm.revision
    val games=vm.catalog.games
    val done=vm.repo.doneCount()
    val playing=vm.repo.playingCount()
    val totk=vm.catalog.game("totk")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        ScreenTitle("Bem-vindo de volta, Herói!","Que a Triforce guie sua jornada...","▲")
        ParchmentCard {
            SmallHeading("✦ Seu progresso")
            LinearProgressIndicator(progress={ done.toFloat()/games.size },modifier=Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(16.dp)))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                StatsTile("$done","Concluídos",Modifier.weight(1f))
                StatsTile("$playing","Jogando",Modifier.weight(1f))
                StatsTile("${games.size}","Na biblioteca",Modifier.weight(1f))
            }
        }
        if(totk!=null) ParchmentCard(Modifier.fillMaxWidth().clickable{onGame("totk")}) {
            SmallHeading("Sua jornada em destaque")
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                CoverArt(totk,Modifier.size(102.dp,112.dp))
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(totk.name,fontWeight=FontWeight.Bold)
                    StatusTag(vm.repo.gameProgress(totk.id).status)
                    Text("Ver jogo →",color=MaterialTheme.colorScheme.primary)
                }
            }
        }
        SmallHeading("Descubra Hyrule")
        listOf(
            Triple("⌛ Cronologia","Explore as linhas do tempo","timeline"),
            Triple("🗺 Atlas","Visite os mapas e seus marcos","atlas"),
            Triple("▤ Biblioteca","Todos os 62 jogos catalogados","library"),
            Triple("✧ Guias","Passo a passo e dungeons","guides"),
            Triple("♜ Meu mural","Acompanhe suas conquistas","profile")
        ).forEach { (title,sub,target) ->
            ParchmentCard(Modifier.fillMaxWidth().clickable{onNavigate(target)}) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title,style=MaterialTheme.typography.titleLarge)
                        Text(sub,style=MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight,contentDescription=null)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable fun LibraryScreen(vm:HyruleViewModel,onGame:(String)->Unit) {
    val revision=vm.revision
    var query by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Todos") }
    val types=listOf("Todos","Principal","Spin-off","Remake","Remaster","Edição","Coleção","DLC")
    val filtered=vm.catalog.games.filter { (type=="Todos" || it.type==type) && "${it.name} ${it.platform} ${it.year} ${it.type}".contains(query,ignoreCase=true) }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {ScreenTitle("Biblioteca","Catálogo de aventuras, remakes e spin-offs","▤")}
        item { OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),label={Text("Buscar jogo, plataforma ou ano")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true) }
        item {Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            types.forEach {t->FilterChip(selected=type==t,onClick={type=t},label={Text(t)})}
        }}
        item{ Text("${filtered.size} aventuras",style=MaterialTheme.typography.labelLarge) }
        items(filtered,key={it.id}) { g ->
            ParchmentCard(Modifier.fillMaxWidth().clickable{onGame(g.id)}) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    CoverArt(g,Modifier.size(94.dp,104.dp))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text(g.name,fontWeight=FontWeight.Bold,maxLines=3)
                        Text("${g.year} • ${g.platform}",style=MaterialTheme.typography.bodySmall)
                        StatusTag(vm.repo.gameProgress(g.id).status)
                        Text(g.type,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
                    }
                    Icon(Icons.Default.ChevronRight,contentDescription=null)
                }
            }
        }
        item { Spacer(Modifier.height(14.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun GameDetailScreen(vm:HyruleViewModel,gameId:String,onBack:()->Unit,onAtlas:(String)->Unit,onGuides:(String)->Unit) {
    val revision=vm.revision
    val game=vm.catalog.game(gameId) ?: return
    val initial=vm.repo.gameProgress(gameId)
    var status by remember(gameId,revision) { mutableStateOf(initial.status) }
    var start by remember(gameId,revision) { mutableStateOf(initial.startDate) }
    var finish by remember(gameId,revision) { mutableStateOf(initial.finishDate) }
    var notes by remember(gameId,revision) { mutableStateOf(initial.notes) }
    var statusExpanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        TextButton(onClick=onBack){Text("← Voltar")}
        CoverArt(game,Modifier.fillMaxWidth().height(188.dp))
        ScreenTitle(game.name,"${game.year} • ${game.platform} • ${game.type}",game.icon)
        ParchmentCard {
            Text(game.description)
            Text("Status atual: ${initial.status}",style=MaterialTheme.typography.labelLarge)
            ExposedDropdownMenuBox(expanded=statusExpanded,onExpandedChange={statusExpanded=!statusExpanded}) {
                OutlinedTextField(value=status,onValueChange={},readOnly=true,label={Text("Status da aventura")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded)},modifier=Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded=statusExpanded,onDismissRequest={statusExpanded=false}){
                    com.hyrulecompanion.app.data.GAME_STATUSES.forEach { option -> DropdownMenuItem(text={Text(option)},onClick={status=option;statusExpanded=false}) }
                }
            }
            OutlinedTextField(value=start,onValueChange={start=it.take(10)},modifier=Modifier.fillMaxWidth(),label={Text("Início (AAAA-MM-DD)")},singleLine=true)
            OutlinedTextField(value=finish,onValueChange={finish=it.take(10)},modifier=Modifier.fillMaxWidth(),label={Text("Conclusão (AAAA-MM-DD)")},singleLine=true)
            OutlinedTextField(value=notes,onValueChange={notes=it.take(5000)},modifier=Modifier.fillMaxWidth(),label={Text("Anotações da jornada")},minLines=3,maxLines=6)
            Button(onClick={vm.saveGame(gameId,com.hyrulecompanion.app.data.GameProgress(status,start,finish,notes))},modifier=Modifier.fillMaxWidth()){Text("Salvar jornada")}
        }
        if(vm.catalog.mapForGame(gameId)!=null) OutlinedButton(onClick={onAtlas(gameId)},modifier=Modifier.fillMaxWidth()){Text("🗺 Abrir mapa no Atlas")}
        Button(onClick={onGuides(gameId)},modifier=Modifier.fillMaxWidth()){Text("✧ Ver guias deste jogo")}
        if(!game.cover.isNullOrBlank())Text("Arte de capa carregada da fonte externa original; requer internet.",style=MaterialTheme.typography.labelSmall)
    }
}
