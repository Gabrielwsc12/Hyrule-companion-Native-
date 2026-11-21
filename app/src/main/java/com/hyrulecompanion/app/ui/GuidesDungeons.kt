package com.hyrulecompanion.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyrulecompanion.app.HyruleViewModel
import com.hyrulecompanion.app.data.*

internal val LocalSpoilerProtection = compositionLocalOf { true }

@Composable private fun Spoiler(label: String = "Revelar dica", content: @Composable () -> Unit) {
    if (!LocalSpoilerProtection.current) {
        Column { content() }
        return
    }
    var visible by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    Column {
        TextButton(onClick={visible = !visible}) { Text(if (visible) "Ocultar detalhe" else label) }
        if (visible) content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun GuidesScreen(vm:HyruleViewModel,initialGameFilter:String,onGame:(String)->Unit) {
    val revision=vm.revision
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Todos") }
    var game by remember(initialGameFilter) { mutableStateOf(initialGameFilter) }
    var status by remember { mutableStateOf("Todos") }
    var difficulty by remember { mutableStateOf("Todas") }
    var openedGuide by remember { mutableStateOf<String?>(null) }
    var openedDungeon by remember { mutableStateOf<String?>(null) }
    var dungeonList by remember { mutableStateOf(false) }

    BackHandler(openedGuide!=null || dungeonList || openedDungeon!=null) {
        when {openedGuide!=null->openedGuide=null;openedDungeon!=null->openedDungeon=null;dungeonList->dungeonList=false}
    }
    if(openedGuide!=null) {
        val guide=vm.catalog.guides.find{it.id==openedGuide}
        if(guide!=null) key(guide.id) { GuideReader(vm,guide,onBack={openedGuide=null},onGame=onGame) }
        return
    }
    if(openedDungeon!=null) {
        val dungeon=vm.catalog.dungeons.find{it.id==openedDungeon}
        if(dungeon!=null) key(dungeon.id) { DungeonReader(vm,dungeon,onBack={openedDungeon=null}) }
        return
    }
    if(dungeonList) {
        DungeonIndex(vm,onBack={dungeonList=false},onOpen={openedDungeon=it})
        return
    }
    val guides=vm.catalog.guides
    val started=guides.count { vm.repo.checked(K_GUIDE_PROGRESS,it.id).isNotEmpty() }
    val completed=guides.count { it.steps.isNotEmpty() && vm.repo.checked(K_GUIDE_PROGRESS,it.id).size==it.steps.size }
    val filtered=guides.filter { g ->
        (category=="Todos" || (category=="Favoritos" && vm.repo.isFavorite(g.id)) || g.category==category) &&
        (game=="" || g.gameId==game) &&
        (difficulty=="Todas" || g.difficulty==difficulty) &&
        when(status){"Concluídos" -> g.steps.isNotEmpty() && vm.repo.checked(K_GUIDE_PROGRESS,g.id).size==g.steps.size;"Iniciados" -> vm.repo.checked(K_GUIDE_PROGRESS,g.id).isNotEmpty();"Não iniciados" -> vm.repo.checked(K_GUIDE_PROGRESS,g.id).isEmpty();else -> true} &&
        "${g.title} ${g.category} ${g.area} ${g.summary}".contains(search,true)
    }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {ScreenTitle("Biblioteca de Guias","Guias de exploração, histórias, puzzles e chefes","✧")}
        item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            StatsTile("${guides.size}","Total",Modifier.weight(1f))
            StatsTile("$started","Iniciados",Modifier.weight(1f))
            StatsTile("$completed","Concluídos",Modifier.weight(1f))
        }}
        item {Button(onClick={dungeonList=true},modifier=Modifier.fillMaxWidth()){Text("▣ Enciclopédia de Dungeons (${vm.catalog.dungeons.size})")}}
        item {OutlinedTextField(search,{search=it},label={Text("Buscar guia, região ou tema")},modifier=Modifier.fillMaxWidth(),singleLine=true)}
        item {Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            (listOf("Todos","História","Dungeon","Puzzle","Chefe","Exploração","Favoritos")+guides.map{it.category}.distinct()).distinct().forEach { c ->FilterChip(selected=category==c,onClick={category=c},label={Text(if(c=="Favoritos")"★ Favoritos" else c)})}
        }}
        item { var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded=expanded,onExpandedChange={expanded=!expanded}) {
                OutlinedTextField(value=if(game.isBlank())"Todos os jogos" else vm.catalog.game(game)?.name ?: "Todos os jogos",onValueChange={},readOnly=true,label={Text("Filtrar por jogo")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded)},modifier=Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                    DropdownMenuItem(text={Text("Todos os jogos")},onClick={game="";expanded=false})
                    vm.catalog.games.filter{g->guides.any{it.gameId==g.id}}.forEach { g ->DropdownMenuItem(text={Text(g.name)},onClick={game=g.id;expanded=false})}
                }
            }
        }
        item {Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){
            listOf("Todos","Iniciados","Concluídos","Não iniciados").forEach{v->FilterChip(selected=status==v,onClick={status=v},label={Text(v)})}
        }}
        item {Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){
            (listOf("Todas")+guides.map{it.difficulty}.distinct()).forEach{d->FilterChip(selected=difficulty==d,onClick={difficulty=d},label={Text(d)})}
        }}
        item {Text("${filtered.size} guias encontrados",style=MaterialTheme.typography.labelLarge)}
        items(filtered,key={it.id}){guide->
            val checked=vm.repo.checked(K_GUIDE_PROGRESS,guide.id)
            ParchmentCard(Modifier.fillMaxWidth().clickable{openedGuide=guide.id}) {
                Row(verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text(guide.title,fontWeight=FontWeight.Bold)
                        Text("${vm.catalog.game(guide.gameId)?.name ?: guide.gameId} • ${guide.category} • ${guide.difficulty}",style=MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick={vm.favorite(guide.id)}) {Icon(if(vm.repo.isFavorite(guide.id))Icons.Default.Favorite else Icons.Default.FavoriteBorder,contentDescription="Favorito",tint=MaterialTheme.colorScheme.secondary)}
                }
                if (!LocalSpoilerProtection.current) Text(guide.summary,style=MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(progress={if(guide.steps.isEmpty())0f else checked.size.toFloat()/guide.steps.size},modifier=Modifier.fillMaxWidth())
                Text("${checked.size} / ${guide.steps.size} etapas concluídas",style=MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable private fun GuideReader(vm:HyruleViewModel,guide:Guide,onBack:()->Unit,onGame:(String)->Unit){
    val revision=vm.revision
    val checked=vm.repo.checked(K_GUIDE_PROGRESS,guide.id)
    val completed=vm.repo.checked(K_GUIDE_COMPLETION,guide.id)
    var notes by remember(guide.id) { mutableStateOf(vm.repo.guideNote(guide.id)) }
    var showReset by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { TextButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null);Text("Voltar aos guias")} }
        item { ScreenTitle(guide.title,"${guide.category} • ${guide.difficulty} • ${guide.area}","✧") }
        item {ParchmentCard {
            Spoiler("Revelar resumo") { Text(guide.summary) }
            Text("Jogo: ${vm.catalog.game(guide.gameId)?.name ?: guide.gameId}",style=MaterialTheme.typography.labelLarge)
            if(guide.time.isNotEmpty())Text("Tempo estimado: ${guide.time}")
            if(guide.needs.isNotEmpty())Spoiler("Revelar preparação") { Text(guide.needs) }
            if(guide.reward.isNotEmpty())Spoiler("Revelar recompensa") { Text(guide.reward) }
            if(guide.scope.isNotEmpty())Spoiler("Revelar escopo") { Text(guide.scope) }
            LinearProgressIndicator(progress={if(guide.steps.isEmpty())0f else checked.size.toFloat()/guide.steps.size},modifier=Modifier.fillMaxWidth())
            Text("${checked.size} / ${guide.steps.size} etapas")
            OutlinedButton(onClick={vm.favorite(guide.id)}){Text(if(vm.repo.isFavorite(guide.id))"★ Remover favorito" else "☆ Adicionar favorito")}
        }}
        if(guide.steps.isNotEmpty())item{ SmallHeading("Etapas da aventura") }
        itemsIndexed(guide.steps){index,step ->
            ParchmentCard(Modifier.fillMaxWidth().clickable{vm.toggleChecked(K_GUIDE_PROGRESS,guide.id,index)}) {
                Row(verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Checkbox(checked=index in checked,onCheckedChange={vm.toggleChecked(K_GUIDE_PROGRESS,guide.id,index)})
                    Column(Modifier.weight(1f)){Text("Etapa ${index+1}",style=MaterialTheme.typography.labelLarge);Spoiler { Text(step) }}
                }
            }
        }
        if(guide.completion.isNotEmpty())item{ SmallHeading("Checklist de 100%") }
        itemsIndexed(guide.completion){index,task->
            ParchmentCard(Modifier.clickable { vm.toggleChecked(K_GUIDE_COMPLETION,guide.id,index) }) {
                Row(verticalAlignment=Alignment.CenterVertically){
                    Checkbox(index in completed,onCheckedChange={vm.toggleChecked(K_GUIDE_COMPLETION,guide.id,index)})
                    Spoiler("Revelar objetivo") { Text(task) }
                }
            }
        }
        if(guide.sections.isNotEmpty())item { SmallHeading("Informações adicionais") }
        items(guide.sections){s->ParchmentCard {Spoiler("Revelar detalhes") { SmallHeading(s.title);s.items.forEach {Text("• $it")} } }}
        if(guide.tip.isNotEmpty())item{ParchmentCard {SmallHeading("Dica do guia");Spoiler { Text(guide.tip) }}}
        item {ParchmentCard {
            SmallHeading("Minhas anotações")
            OutlinedTextField(notes,{notes=it.take(10000)},modifier=Modifier.fillMaxWidth(),minLines=4,maxLines=8,label={Text("Escreva lembretes desta aventura")})
            Button(onClick={vm.note(guide.id,notes)},modifier=Modifier.fillMaxWidth()){Text("Salvar anotações")}
            OutlinedButton(onClick={showReset=true}){Text("Reiniciar etapas do guia")}
        }}
        item { OutlinedButton(onClick={onGame(guide.gameId)},modifier=Modifier.fillMaxWidth()){Text("Ver jogo na biblioteca")} }
    }
    if(showReset)AlertDialog(onDismissRequest={showReset=false},title={Text("Reiniciar guia?")},text={Text("Isso desmarca todas as etapas, mas mantém suas anotações e favoritos.")},confirmButton={TextButton(onClick={vm.clearChecked(K_GUIDE_PROGRESS,guide.id);showReset=false}){Text("Reiniciar")}},dismissButton={TextButton(onClick={showReset=false}){Text("Cancelar")}})
}

@Composable private fun DungeonIndex(vm:HyruleViewModel,onBack:()->Unit,onOpen:(String)->Unit){
    val revision=vm.revision
    var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    val filtered = vm.catalog.dungeons.filter { dungeon ->
        "${dungeon.name} ${dungeon.region} ${vm.catalog.game(dungeon.gameId)?.name.orEmpty()}".contains(query.trim(), ignoreCase = true)
    }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item{TextButton(onClick=onBack){Text("← Voltar aos guias")}}
        item{ScreenTitle("Enciclopédia de Dungeons","Salas, puzzles, recompensas, itens e chefes","▣")}
        item {
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar dungeon, jogo ou região") }, singleLine = true)
        }
        if (filtered.isEmpty()) item { Text("Nenhuma dungeon encontrada.") }
        items(filtered,key={it.id}){d->
            val checked=vm.repo.checked(K_DUNGEON_PROGRESS,d.id)
            ParchmentCard(Modifier.clickable{onOpen(d.id)}){
                SmallHeading(d.name)
                Text("${vm.catalog.game(d.gameId)?.name ?: d.gameId} • ${d.region}")
                Spoiler("Revelar resumo") { Text(d.summary) }
                Spoiler("Revelar chefe e item") { Text("${d.boss} · ${d.item}") }
                val total=d.rooms.size+d.checklist.size
                LinearProgressIndicator(progress={if(total==0)0f else checked.size.toFloat()/total},modifier=Modifier.fillMaxWidth())
                Text("${checked.size} / $total concluídos",style=MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable private fun DungeonReader(vm:HyruleViewModel,d:Dungeon,onBack:()->Unit) {
    val revision=vm.revision
    val progress=vm.repo.checked(K_DUNGEON_PROGRESS,d.id)
    val items=vm.repo.checked(K_DUNGEON_ITEMS,d.id)
    var showReset by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item{TextButton(onClick=onBack){Text("← Enciclopédia")}}
        item{ScreenTitle(d.name,"${d.region} • ${d.difficulty}","▣")}
        item {ParchmentCard {
            Spoiler("Revelar resumo") { Text(d.summary) }
            Text("Era: ${d.era}"); Spoiler("Revelar item principal") { Text(d.item) }
            Spoiler("Revelar chefe") { Text(d.boss) }
        }}
        item{SmallHeading("Rota pela dungeon")}
        itemsIndexed(d.rooms){index,room->
            val detail=d.roomDetails.getOrNull(index)
            ParchmentCard(Modifier.clickable { vm.toggleChecked(K_DUNGEON_PROGRESS,d.id,index) }) {
                Row(verticalAlignment=Alignment.CenterVertically){
                    Checkbox(index in progress,onCheckedChange={vm.toggleChecked(K_DUNGEON_PROGRESS,d.id,index)})
                    Text(if (LocalSpoilerProtection.current) "Sala ${index+1}" else "${index+1}. ${room.title}",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
                }
                Spoiler("Revelar orientação da sala") {
                    if (LocalSpoilerProtection.current) SmallHeading(room.title)
                    Text(room.introduction)
                    if (room.tip.isNotBlank()) Text(room.tip)
                    if (room.reward.isNotBlank()) Text("Objetivo: ${room.reward}")
                    if (detail != null) {
                        Text("Andar: ${detail.floor}")
                        if (detail.objective != room.introduction) Text("Objetivo: ${detail.objective}")
                        Text("Atenção: ${detail.hazard}")
                    }
                    d.enemies.getOrNull(index)?.let { Text("Inimigos: $it") }
                }
                if (detail != null) {
                    Spoiler("Revelar solução do puzzle") { Text(detail.puzzle) }
                    Spoiler("Revelar recompensas") { Text(detail.loot) }
                }
            }
        }
        item{SmallHeading("Itens e tesouros")}
        itemsIndexed(d.items){index,item->ParchmentCard(Modifier.clickable{vm.toggleChecked(K_DUNGEON_ITEMS,d.id,index)}){
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(index in items,onCheckedChange={vm.toggleChecked(K_DUNGEON_ITEMS,d.id,index)});Spoiler("Revelar tesouro") { Text(item.name,style=MaterialTheme.typography.titleMedium); Text(item.description) }}
        }}
        item{SmallHeading("Checklist final")}
        itemsIndexed(d.checklist){index,task->
            ParchmentCard(Modifier.clickable{vm.toggleChecked(K_DUNGEON_PROGRESS,d.id,d.rooms.size+index)}) {
                Row(verticalAlignment=Alignment.CenterVertically){Checkbox(d.rooms.size+index in progress,onCheckedChange={vm.toggleChecked(K_DUNGEON_PROGRESS,d.id,d.rooms.size+index)});Spoiler("Revelar objetivo") { Text(task) }}
            }
        }
        item{ParchmentCard {Spoiler("Revelar estratégia do chefe") { SmallHeading(d.boss);Text(d.bossGuide) }}}
        item{OutlinedButton(onClick={showReset=true},modifier=Modifier.fillMaxWidth()){Text("Reiniciar progresso da dungeon")}}
    }
    if(showReset)AlertDialog(onDismissRequest={showReset=false},title={Text("Reiniciar dungeon?")},text={Text("Todas as salas e todos os itens serão desmarcados.")},confirmButton={TextButton(onClick={vm.clearChecked(K_DUNGEON_PROGRESS,d.id);vm.clearChecked(K_DUNGEON_ITEMS,d.id);showReset=false}){Text("Reiniciar")}},dismissButton={TextButton(onClick={showReset=false}){Text("Cancelar")}})
}
