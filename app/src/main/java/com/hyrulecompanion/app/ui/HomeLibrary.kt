package com.hyrulecompanion.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import coil.size.Precision
import com.hyrulecompanion.app.BuildConfig
import com.hyrulecompanion.app.HyruleViewModel
import com.hyrulecompanion.app.data.Game

@Composable
fun CoverArt(game: Game, modifier: Modifier = Modifier, large: Boolean = false) {
    val context = LocalContext.current
    val request = remember(context, game.cover, large) {
        game.cover?.let {
            ImageRequest.Builder(context).data(it).size(if (large) 640 else 320)
                .precision(Precision.INEXACT).build()
        }
    }
    var visible by remember(request) { mutableStateOf(false) }
    Box(modifier.clip(RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        if (!visible) Text(game.icon, style = MaterialTheme.typography.headlineMedium)
        if (request != null) AsyncImage(request, "Capa de ${game.name}", Modifier.matchParentSize(),
            contentScale = ContentScale.Crop, onSuccess = { visible = true }, onError = { visible = false })
    }
}

@Composable fun HomeScreen(vm:HyruleViewModel,userName:String?,onNavigate:(String)->Unit,onGame:(String)->Unit) {
    val revision=vm.revision
    val games=vm.catalog.games
    val done=vm.repo.doneCount()
    val playing=vm.repo.playingCount()
    val latest=vm.repo.latestCompletedGame()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        ScreenTitle(userName?.let { "Bem-vindo de volta, $it!" } ?: "Bem-vindo de volta!", "", "▲")
        SmallHeading("Descubra Hyrule")
        SectionCarousel(onNavigate)
        ParchmentCard(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Abrir Store") {
            onNavigate("store")
        }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage("file:///android_asset/home/store.webp", null,
                    Modifier.size(68.dp, 48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                Column(Modifier.weight(1f)) {
                    Text("Store", style = MaterialTheme.typography.titleLarge)
                    Text("Cosméticos e pesca no Mar de Hyrule", style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.ChevronRight, null)
            }
        }
        ParchmentCard {
            SmallHeading("✦ Seu progresso")
            LinearProgressIndicator(progress={ done.toFloat()/games.size },modifier=Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(16.dp)))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                StatsTile("$done","Concluídos",Modifier.weight(1f))
                StatsTile("$playing","Jogando",Modifier.weight(1f))
                StatsTile("${games.size}","Na biblioteca",Modifier.weight(1f))
            }
        }
        if(latest!=null) ParchmentCard(Modifier.fillMaxWidth().clickable{onGame(latest.id)}) {
            SmallHeading("Último Zelda concluído")
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                CoverArt(latest,Modifier.size(102.dp,112.dp))
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(latest.name,fontWeight=FontWeight.Bold)
                    StatusTag(vm.repo.gameProgress(latest.id).status)
                    Text("Ver jogo →",color=MaterialTheme.colorScheme.primary)
                }
            }
        } else ParchmentCard {
            SmallHeading("Último Zelda concluído")
            Text(if (done == 0) "Quando você concluir uma aventura, ela aparecerá aqui." else "Adicione a data de conclusão para destacar sua última aventura.")
            TextButton(onClick={onNavigate("library")}) { Text("Abrir biblioteca") }
        }

    }
}

private data class SectionCard(val label: String, val detail: String, val target: String, val image: String)

// Replace one of the three feature slots by request. The changelog stays in slot four.
private val homeSections = listOf(
    SectionCard("Biblioteca", "Jogos, guias e dungeons", "library", "library"),
    SectionCard("Atlas", "Mapas e pontos de interesse", "atlas", "atlas"),
    SectionCard("Cronologia", "Explore as linhas do tempo", "timeline", "timeline")
).take(3) + SectionCard("Novidades da ${BuildConfig.VERSION_NAME}", "", "changelog", "changelog")

@Composable
private fun SectionCarousel(onNavigate: (String) -> Unit) {
    val pager = rememberPagerState { homeSections.size }
    val scope = rememberCoroutineScope()
    HorizontalPager(pager, contentPadding = PaddingValues(end = 28.dp), pageSpacing = 12.dp, beyondViewportPageCount = 1) { page ->
        val section = homeSections[page]
        Box(Modifier.fillMaxWidth().height(158.dp).clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(role = Role.Button, onClickLabel = "Abrir ${section.label}") { onNavigate(section.target) }) {
            AsyncImage("file:///android_asset/home/${section.image}.webp", null,
                Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            if (section.target == "changelog") {
                Text(section.label, Modifier.align(Alignment.Center).fillMaxWidth(.65f),
                    textAlign = TextAlign.Center, color = Color(0xFF3A2919),
                    style = MaterialTheme.typography.titleLarge)
            } else {
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = .15f), Color(0xED101815))
                )))
                Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(section.label, color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Text(section.detail, color = Color(0xFFF0EEE5), style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color.White)
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        homeSections.forEachIndexed { page, section ->
            Box(Modifier.size(40.dp).clip(CircleShape)
                .semantics { contentDescription = section.label }
                .clickable { scope.launch { pager.animateScrollToPage(page) } }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(if (pager.currentPage == page) 10.dp else 7.dp).clip(CircleShape)
                    .background(if (pager.currentPage == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
            }
        }
    }
}

@Composable fun LibraryScreen(vm:HyruleViewModel,onGame:(String)->Unit,onDungeons:()->Unit) {
    val revision=vm.revision
    var query by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Todos") }
    val types=listOf("Todos","Principal","Spin-off","Remake","Remaster","Edição","Coleção","DLC")
    var onlyCompleted by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val filtered=vm.catalog.games.filter {
        (!onlyCompleted || vm.repo.gameProgress(it.id).status in listOf("Concluído", "100%")) && (type=="Todos" || it.type==type) && "${it.name} ${it.platform} ${it.year} ${it.type}".contains(query,ignoreCase=true) }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {ScreenTitle("Biblioteca","Catálogo de aventuras, remakes e spin-offs","▤")}
        item { OutlinedButton(onClick=onDungeons) { Text("Enciclopédia de Dungeons") } }
        item { OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),label={Text("Buscar jogo, plataforma ou ano")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true) }
        item {Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            types.forEach {t->FilterChip(selected=type==t,onClick={type=t},label={Text(t)})}
        }}
        item { FilterChip(selected=onlyCompleted,onClick={onlyCompleted=!onlyCompleted},label={Text("Só concluídos")}) }
        item{ Text("${filtered.size} aventuras",style=MaterialTheme.typography.labelLarge) }
        if (filtered.isEmpty()) item { Text("Nenhum jogo corresponde aos filtros.") }
        val favorites = vm.repo.gameFavorites()
        items(filtered, key = { it.id }) { game ->
            GameCard(vm, game, game.id in favorites, onGame)
        }
        item { Spacer(Modifier.height(14.dp)) }
    }
}

@Composable
internal fun FavoriteGamesScreen(vm: HyruleViewModel, onBack: () -> Unit, onGame: (String) -> Unit) {
    val revision = vm.revision
    val favorites = remember(revision) { vm.repo.gameFavorites() }
    val games = vm.catalog.games.filter { it.id in favorites }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TextButton(onClick = onBack) { Text("← Voltar ao perfil") } }
        item { ScreenTitle("Jogos favoritos", "", "♥") }
        if (games.isEmpty()) item { Text("Toque no coração de um jogo na Biblioteca para salvá-lo aqui.") }
        items(games, key = { it.id }) { game -> GameCard(vm, game, true, onGame) }
    }
}

@Composable
private fun GameCard(vm: HyruleViewModel, game: Game, favorite: Boolean, onGame: (String) -> Unit) {
    ParchmentCard(Modifier.fillMaxWidth().clickable { onGame(game.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoverArt(game, Modifier.size(82.dp, 104.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(game.name, fontWeight = FontWeight.Bold, maxLines = 3)
                Text("${game.year} • ${game.platform}", style = MaterialTheme.typography.bodySmall)
                StatusTag(vm.repo.gameProgress(game.id).status)
                Text(game.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = { vm.toggleGameFavorite(game.id) }) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (favorite) "Remover ${game.name} dos favoritos" else "Salvar ${game.name} nos favoritos",
                    tint = if (favorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun GameDetailScreen(vm:HyruleViewModel,gameId:String,onBack:()->Unit,onAtlas:(String)->Unit,onReference:(String)->Unit) {
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
        CoverArt(game, Modifier.fillMaxWidth().height(188.dp), large = true)
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
        val guides = vm.catalog.guides.filter { it.gameId == gameId }
        val dungeons = vm.catalog.dungeons.filter { it.gameId == gameId }
        SmallHeading("Guias e dungeons")
        if (guides.isEmpty() && dungeons.isEmpty()) Text("Ainda não há guias para este jogo.")
        guides.sortedByDescending { vm.repo.isFavorite(it.id) }.forEach { guide ->
            OutlinedButton(onClick={onReference("guide:${guide.id}")}, modifier=Modifier.fillMaxWidth()) {
                Text(if (vm.repo.isFavorite(guide.id)) "★ ${guide.title}" else guide.title)
            }
        }
        dungeons.forEach { dungeon ->
            OutlinedButton(onClick={onReference("dungeon:${dungeon.id}")}, modifier=Modifier.fillMaxWidth()) {
                Text(dungeon.name)
            }
        }
    }
}
