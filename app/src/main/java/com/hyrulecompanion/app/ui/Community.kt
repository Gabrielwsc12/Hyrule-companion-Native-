package com.hyrulecompanion.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hyrulecompanion.app.HyruleViewModel
import com.hyrulecompanion.app.network.CommunityProfile
import com.hyrulecompanion.app.network.CommunityMedia
import com.hyrulecompanion.app.network.CommunityReaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun CommunityScreen(vm: HyruleViewModel, onBack: () -> Unit, onAccount: () -> Unit) {
    val session = vm.session
    if (session == null) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack) { Text("Voltar ao perfil") }
            ScreenTitle("Comunidade", "Encontre outros aventureiros", "♜")
            Text("Entre na sua conta para participar da comunidade.")
            Button(onClick = onAccount) { Text("Ir para Conta e Sincronização") }
        }
        return
    }
    key(session.userId) { SignedInCommunity(vm, session.userId, onBack) }
}

@Composable
private fun SignedInCommunity(vm: HyruleViewModel, userId: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var own by remember { mutableStateOf<CommunityProfile?>(null) }
    var following by remember { mutableStateOf<List<CommunityProfile>>(emptyList()) }
    var results by remember { mutableStateOf<List<CommunityProfile>?>(null) }
    var selected by remember { mutableStateOf<CommunityProfile?>(null) }
    var profileOnline by remember { mutableStateOf(false) }
    LaunchedEffect(selected?.userId) {
        profileOnline = false
        val target = selected?.userId ?: return@LaunchedEffect
        while (true) {
            profileOnline = try {
                vm.communityRequest(userId) { communityOnline(it, target) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
            kotlinx.coroutines.delay(10_000)
        }
    }
    var reactions by remember { mutableStateOf<Map<String, CommunityReaction>>(emptyMap()) }
    var isFollowing by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var media by remember { mutableStateOf(CommunityMedia()) }
    var search by remember { mutableStateOf("") }

    fun runAction(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = when {
                    failure.message.orEmpty().contains("hc_community_profiles_handle_key") -> "Esse nome já está em uso."
                    failure.message.orEmpty().contains("schema cache") -> "A comunidade ainda não está habilitada neste servidor."
                    else -> "Não foi possível concluir a ação. Verifique a conexão e tente novamente."
                }
            } finally {
                busy = false
            }
        }
    }

    fun refresh() = runAction {
        val data = vm.communityRequest(userId) { account -> communityProfile(account) to followedProfiles(account) }
        own = data.first
        following = data.second
        enabled = true
    }

    fun open(profile: CommunityProfile) = runAction {
        vm.communityRequest(userId) { account ->
            val (latest, images) = communityProfileWithMedia(account, profile.userId)
            if (latest != null) {
                val followingProfile = follows(account, latest.userId)
                val applause = if (latest.shareJourney) communityReactions(account, latest.userId) else emptyMap()
                isFollowing = followingProfile
                reactions = applause
                media = images
                selected = latest
            }
        }
        if (selected == null) {
            results = results?.filterNot { it.userId == profile.userId }
            following = following.filterNot { it.userId == profile.userId }
            error = "Esse perfil não está disponível."
        }
    }

    LaunchedEffect(Unit) { refresh() }
    BackHandler(selected != null) { selected = null }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = { if (selected != null) selected = null else onBack() }) {
                Text(if (selected != null) "Voltar à comunidade" else "Voltar ao perfil")
            }
            if (selected == null) ScreenTitle("Comunidade", "", "♜")
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        val profile = selected
        if (profile != null) {
            item {
                ParchmentCard {
                    ProfileHeader("@${profile.handle}", media.avatar.orEmpty(), media.banner, vm.repo.preferences().reduceMotion, online = profileOnline)
                    Text("Zelda favorito: ${vm.catalog.game(profile.favoriteGame.orEmpty())?.name ?: "A escolher"}")
                    if (profile.userId != userId) {
                        val canFollow = own != null
                        Button(enabled = !busy && (isFollowing || canFollow), onClick = {
                            runAction {
                                val target = !isFollowing
                                vm.communityRequest(userId) { account -> setFollowing(account, profile.userId, target) }
                                isFollowing = target
                                following = if (target) (following + profile).distinctBy { it.userId }
                                    else following.filterNot { it.userId == profile.userId }
                            }
                        }) { Text(if (isFollowing) "Deixar de seguir" else "Seguir") }
                        if (!canFollow && !isFollowing) Text("Configure seu nome de usuário para seguir alguém.")
                    }
                }
            }
            if (profile.shareJourney) {
                val games = vm.catalog.games.filter { profile.journey[it.id] in listOf("Em andamento", "Concluído", "100%") }
                item { SmallHeading("Jornada compartilhada") }
                if (games.isEmpty()) item { Text("Nenhuma aventura registrada.") }
                items(games, key = { it.id }) { game ->
                    val status = profile.journey.getValue(game.id)
                    val completed = status == "Concluído" || status == "100%"
                    ParchmentCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CoverArt(game, Modifier.size(68.dp, 84.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(game.name, style = MaterialTheme.typography.titleMedium)
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusTag(status)
                                    if (completed) TriforceEmblem()
                                }
                            }
                        }
                        if (completed) {
                            val reaction = reactions[game.id] ?: CommunityReaction(0, false)
                            Row(Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (profile.userId == userId) {
                                    Text("👏", Modifier.clearAndSetSemantics {})
                                } else {
                                    val label = if (reaction.applauded) "Retirar aplauso" else "Aplaudir conquista"
                                    FilledTonalIconToggleButton(
                                        checked = reaction.applauded,
                                        enabled = !busy,
                                        modifier = Modifier.semantics { contentDescription = label },
                                        onCheckedChange = { applaud ->
                                            runAction {
                                                reactions = vm.communityRequest(userId) { account ->
                                                    setCommunityApplause(account, profile.userId, game.id, applaud)
                                                    communityReactions(account, profile.userId)
                                                }
                                            }
                                        }
                                    ) { Text("👏", Modifier.clearAndSetSemantics {}) }
                                }
                                val countLabel = if (reaction.count == 1) "1 aplauso" else "${reaction.count} aplausos"
                                Text(reaction.count.toString(), Modifier.clearAndSetSemantics {
                                    contentDescription = countLabel
                                }, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            } else item { Text("A jornada deste aventureiro é privada.") }
        } else if (enabled) {
            item {
                ParchmentCard {
                    SmallHeading("Meu perfil na comunidade")
                    Text(own?.let { "@${it.handle} · ${if (it.discoverable) "Visível" else "Privado"}" }
                        ?: "Escolha um nome de usuário e o que deseja compartilhar.")
                    TextButton(onClick = onBack) { Text("Editar em Meu Perfil") }
                    own?.takeIf { it.discoverable }?.let { published ->
                        TextButton(enabled = !busy, onClick = { open(published) }) { Text("Ver meu perfil público") }
                        if (published.shareJourney) {
                            TextButton(enabled = !busy, onClick = {
                                runAction {
                                    val updated = published.copy(journey = sharedGameStatuses(vm))
                                    vm.communityRequest(userId) { account -> saveCommunityProfile(account, updated) }
                                    own = updated
                                    vm.notifyMessage("Jornada compartilhada atualizada.")
                                }
                            }) { Text("Atualizar jornada compartilhada") }
                        }
                    }
                }
            }
            item {
                ParchmentCard {
                    SmallHeading("Encontrar aventureiros")
                    OutlinedTextField(search, { search = it.take(24); results = null }, label = { Text("Nome de usuário") },
                        singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                    val prefix = search.trim().lowercase()
                    Button(enabled = !busy && prefix.matches(Regex("[a-z0-9_]{1,24}")), onClick = {
                        runAction { results = vm.communityRequest(userId) { account -> searchCommunity(account, prefix) } }
                    }) { Text("Buscar") }
                    if (results?.isEmpty() == true) Text("Nenhum perfil encontrado.")
                }
            }
            items(results.orEmpty(), key = { "result-${it.userId}" }) { user ->
                OutlinedButton(enabled = !busy, onClick = { open(user) }) { Text("@${user.handle}") }
            }
            item { SmallHeading("Seguindo") }
            if (following.isEmpty()) item { Text("Você ainda não segue nenhum perfil visível.") }
            items(following, key = { "following-${it.userId}" }) { user ->
                OutlinedButton(enabled = !busy, onClick = { open(user) }) { Text("@${user.handle}") }
            }
        } else {
            item { OutlinedButton(enabled = !busy, onClick = ::refresh) { Text("Carregar comunidade") } }
        }
    }

}

@Composable
internal fun CommunityProfileEditor(
    vm: HyruleViewModel, userId: String, current: CommunityProfile?, busy: Boolean,
    error: String?, mediaEditor: @Composable (String, Boolean) -> Unit, onClose: () -> Unit, onSave: (CommunityProfile) -> Unit
) {
    var handle by remember { mutableStateOf(current?.handle.orEmpty()) }
    var favorite by remember { mutableStateOf(current?.favoriteGame) }
    var discoverable by remember { mutableStateOf(current?.discoverable ?: false) }
    var shareJourney by remember { mutableStateOf(current?.shareJourney ?: false) }
    var picking by remember { mutableStateOf(false) }
    val normalized = handle.trim().lowercase()

    AlertDialog(onDismissRequest = { if (!busy) onClose() }, title = { Text("Editar perfil") }, text = {
        Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(handle, { handle = it.take(24) }, singleLine = true, enabled = !busy,
                label = { Text("Nome de usuário") }, supportingText = { Text("3 a 24 letras, números ou _") })
            Box {
                OutlinedButton(enabled = !busy, onClick = { picking = true }) {
                    Text(vm.catalog.game(favorite.orEmpty())?.name ?: "Escolher Zelda favorito")
                }
                DropdownMenu(expanded = picking, onDismissRequest = { picking = false }, modifier = Modifier.heightIn(max = 250.dp)) {
                    DropdownMenuItem(text = { Text("Sem favorito") }, onClick = { favorite = null; picking = false })
                    vm.catalog.games.forEach { game ->
                        DropdownMenuItem(text = { Text(game.name) }, onClick = { favorite = game.id; picking = false })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(discoverable, { discoverable = it }, enabled = !busy)
                Text("Aparecer na comunidade", Modifier.padding(start = 8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(shareJourney && discoverable, { shareJourney = it }, enabled = !busy && discoverable)
                Text("Compartilhar status dos jogos", Modifier.padding(start = 8.dp))
            }
            mediaEditor(normalized, discoverable)
            Text("Salve o perfil de novo para atualizar a jornada compartilhada.", style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton(enabled = !busy && normalized.matches(Regex("[a-z0-9_]{3,24}")), onClick = {
            val share = discoverable && shareJourney
            val journey = if (share) sharedGameStatuses(vm) else emptyMap()
            onSave(CommunityProfile(userId, normalized, favorite, discoverable, share, journey))
        }) { Text("Salvar") }
    }, dismissButton = { TextButton(enabled = !busy, onClick = onClose) { Text("Cancelar") } })
}


private fun sharedGameStatuses(vm: HyruleViewModel): Map<String, String> =
    vm.catalog.games.mapNotNull { game ->
        val status = vm.repo.gameProgress(game.id).status
        if (status in listOf("Em andamento", "Concluído", "100%")) game.id to status else null
    }.toMap()
