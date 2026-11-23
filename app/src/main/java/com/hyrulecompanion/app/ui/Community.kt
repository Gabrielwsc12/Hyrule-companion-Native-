package com.hyrulecompanion.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hyrulecompanion.app.HyruleViewModel
import com.hyrulecompanion.app.data.Cosmetic
import com.hyrulecompanion.app.network.CommunityProfile
import com.hyrulecompanion.app.network.CommunityPerson
import com.hyrulecompanion.app.network.CommunityMedia
import com.hyrulecompanion.app.network.CommunityReaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

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
    var following by remember { mutableStateOf<List<CommunityPerson>>(emptyList()) }
    var results by remember { mutableStateOf<List<CommunityPerson>?>(null) }
    var selected by remember { mutableStateOf<CommunityProfile?>(null) }
    var profileOnline by remember { mutableStateOf(false) }
    val selectedId = selected?.userId
    var equipped by remember(selectedId) { mutableStateOf<Set<Cosmetic>>(emptySet()) }
    LaunchedEffect(userId, selectedId) {
        if (selectedId != null) {
            equipped = try {
                vm.communityRequest(userId) { publicCosmetics(it, selectedId) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptySet()
            }
        }
    }
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

    var searchError by remember { mutableStateOf(false) }
    var submittedQuery by remember { mutableStateOf<String?>(null) }
    var submission by remember { mutableIntStateOf(0) }
    val focus = LocalFocusManager.current
    val avatarLoads = remember { Semaphore(2) }
    val handle = search.trim().removePrefix("@").lowercase()
    val validSearch = handle.matches(Regex("[a-z0-9_]{1,24}"))
    fun submitSearch() {
        if (!validSearch) return
        focus.clearFocus()
        submittedQuery = handle
        submission++
    }
    LaunchedEffect(search, submission, enabled, selectedId) {
        if (!enabled || selectedId != null || !validSearch) return@LaunchedEffect
        if (submittedQuery != handle) delay(300)
        searchError = false
        try {
            results = vm.communityRequest(userId) { searchCommunity(it, handle) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            results = null
            searchError = true
        }
    }

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

    fun open(profileId: String) = runAction {
        vm.communityRequest(userId) { account ->
            val (latest, images) = communityProfileWithMedia(account, profileId)
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
            results = results?.filterNot { it.userId == profileId }
            following = following.filterNot { it.userId == profileId }
            error = "Esse perfil não está disponível."
        }
    }

    LaunchedEffect(Unit) { refresh() }
    BackHandler(selected != null) { selected = null }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    ProfileHeader("@${profile.handle}", media.avatar.orEmpty(), media.banner, vm.repo.preferences().reduceMotion, online = profileOnline, equipped = equipped)
                    Text("Zelda favorito: ${vm.catalog.game(profile.favoriteGame.orEmpty())?.name ?: "A escolher"}")
                    if (profile.userId != userId) {
                        val canFollow = own != null
                        Button(enabled = !busy && (isFollowing || canFollow), onClick = {
                            runAction {
                                val target = !isFollowing
                                vm.communityRequest(userId) { account -> setFollowing(account, profile.userId, target) }
                                isFollowing = target
                                following = if (target) (following + CommunityPerson(profile.userId, profile.handle)).distinctBy { it.userId }.sortedBy { it.handle }
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
                    SmallHeading("Encontrar aventureiros")
                    OutlinedTextField(search, {
                        search = it.take(25)
                        submittedQuery = null
                        results = null
                        searchError = false
                    }, label = { Text("Nome de usuário") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                        trailingIcon = {
                            IconButton(enabled = validSearch, onClick = ::submitSearch) {
                                Icon(Icons.Default.Search, "Buscar usuário")
                            }
                        })
                    if (searchError) Text("Não foi possível buscar. Toque na lupa para tentar de novo.",
                        color = MaterialTheme.colorScheme.error)
                    else if (validSearch && results?.isEmpty() == true) Text("Nenhum perfil encontrado.")
                    else if (validSearch && results == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            items(results.orEmpty(), key = { "result-${it.userId}" }) { user ->
                ParchmentCard(Modifier.fillMaxWidth().clickable(enabled = !busy) { open(user.userId) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("@${user.handle}", Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
            item { SmallHeading("Seguindo") }
            if (following.isEmpty()) item { Text("Você ainda não segue nenhum perfil visível.") }
            items(following, key = { "following-${it.userId}" }) { user ->
                FollowedProfileCard(user, vm.repo.preferences().reduceMotion, !busy, onOpen = { open(user.userId) }) {
                    avatarLoads.withPermit {
                        vm.communityRequest(userId) { communityProfileWithMedia(it, user.userId).second.avatar }
                    }
                }
            }
        } else {
            item { OutlinedButton(enabled = !busy, onClick = ::refresh) { Text("Carregar comunidade") } }
        }
    }

}

@Composable
private fun FollowedProfileCard(person: CommunityPerson, reduceMotion: Boolean, enabled: Boolean,
                                onOpen: () -> Unit, loadAvatar: suspend () -> String?) {
    var avatar by remember(person.userId) { mutableStateOf("") }
    LaunchedEffect(person.userId) {
        avatar = try { loadAvatar().orEmpty() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { "" }
    }
    ParchmentCard(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(avatar, reduceMotion, Modifier.size(52.dp))
            Text("@${person.handle}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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


internal fun sharedGameStatuses(vm: HyruleViewModel): Map<String, String> =
    vm.repo.journeyGames().associate { it.id to vm.repo.gameProgress(it.id).status }
