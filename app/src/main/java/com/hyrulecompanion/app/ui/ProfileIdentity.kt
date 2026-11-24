package com.hyrulecompanion.app.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hyrulecompanion.app.HyruleViewModel
import com.hyrulecompanion.app.data.Cosmetic
import com.hyrulecompanion.app.network.CommunityMedia
import com.hyrulecompanion.app.network.CommunityProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileHeader(
    name: String, avatar: String, banner: String?, reduceMotion: Boolean, online: Boolean = false, equipped: Set<Cosmetic> = emptySet(),
    actions: @Composable RowScope.() -> Unit = {}
) {
    val image = rememberProfileImage(banner.orEmpty(), reduceMotion)
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.primaryContainer)) {
        if (image != null) {
            AsyncImage(image, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = .12f), Color.Black.copy(alpha = .81f))
            )))
        }
        Column(Modifier.fillMaxWidth().heightIn(min = 132.dp).padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, content = actions)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val hasSword = Cosmetic.SWORD in equipped
                val hasHat = Cosmetic.HAT in equipped
                val avatarTop = when {
                    hasSword -> 33.dp
                    hasHat -> 21.dp
                    else -> 0.dp
                }
                Box(Modifier.size(72.dp, 72.dp + avatarTop)) {
                    if (hasSword) SwordCosmetic(reduceMotion,
                        Modifier.align(Alignment.TopCenter).offset(x = if (hasHat) 17.dp else 8.dp)
                            .size(32.dp, 76.dp), behindAvatar = true)
                    Avatar(avatar, reduceMotion, Modifier.align(Alignment.BottomCenter).size(72.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape))
                    if (hasHat) HatCosmetic(Modifier.align(Alignment.TopCenter).offset(y = avatarTop - 21.dp)
                        .size(72.dp, 41.dp))
                    if (online) Box(Modifier.align(Alignment.BottomEnd).size(16.dp)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .background(Color(0xFF38BC6E), CircleShape)
                        .semantics { contentDescription = translate("Online") })
                }
                if (Cosmetic.GOLD in equipped) GoldenCosmetic(reduceMotion, Modifier.size(24.dp, 40.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        StyledProfileName(name, equipped,
                            color = if (image == null) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                            modifier = Modifier.weight(1f, fill = false))
                        if (Cosmetic.FISH_BADGE in equipped) UnlockBadge(Cosmetic.FISH_BADGE, reduceMotion)
                        if (Cosmetic.RA_TROPHY in equipped) UnlockBadge(Cosmetic.RA_TROPHY, reduceMotion)
                        if (Cosmetic.CREATOR in equipped) ProfileBadge(Cosmetic.CREATOR, reduceMotion)
                    }
                    if (Cosmetic.BETA in equipped || Cosmetic.CHEATER in equipped) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (Cosmetic.BETA in equipped) ProfileBadge(Cosmetic.BETA, reduceMotion)
                            if (Cosmetic.CHEATER in equipped) ProfileBadge(Cosmetic.CHEATER, reduceMotion)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProfileCommunitySettings(vm: HyruleViewModel, userId: String?, localAvatar: String,
                                      reduceMotion: Boolean) {
    val scope = rememberCoroutineScope()
    val resolver = LocalContext.current.contentResolver
    val known = if (vm.ownProfileOwner == userId) vm.ownProfile else null
    var profile by remember(userId) { mutableStateOf(known?.first) }
    var avatar by remember { mutableStateOf("") }
    var publishedAvatar by remember(userId) { mutableStateOf(known?.second?.avatar) }
    var avatarEdited by remember { mutableStateOf(false) }
    var publishedBanner by remember(userId) { mutableStateOf(known?.second?.banner) }
    var banner by remember { mutableStateOf<String?>(null) }
    var publishPhoto by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var loaded by remember(userId) { mutableStateOf(userId == null || known?.first != null) }
    var loading by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun action(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = when {
                    failure.message.orEmpty().contains("hc_community_profiles_handle_key") -> "Esse nome já está em uso."
                    failure.message.orEmpty().contains("schema cache") -> "A atualização do perfil ainda precisa ser ativada no servidor."
                    else -> failure.message?.takeIf { it.startsWith("Banner:") || it.startsWith("Foto:") } ?: "Não foi possível atualizar o perfil. Tente novamente."
                }
            } finally { busy = false }
        }
    }

    fun editProfile() {
        avatar = localAvatar.ifBlank { publishedAvatar.orEmpty() }
        avatarEdited = false
        banner = publishedBanner
        publishPhoto = publishedAvatar != null
        error = null
        editing = true
    }

    fun loadProfile() {
        if (userId == null || loading || loaded) return
        loading = true
        loadFailed = false
        scope.launch {
            try {
                vm.loadOwnProfile(userId, online = true)
                val (own, images) = vm.ownProfile ?: (null to CommunityMedia())
                profile = own
                publishedBanner = images.banner
                publishedAvatar = images.avatar
                if (editing) editProfile()
                loaded = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                loadFailed = true
            } finally {
                loading = false
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) action {
            avatar = withContext(Dispatchers.IO) {
                try { vm.repo.avatarImage(uri) }
                catch (failure: Exception) { throw IllegalArgumentException("Foto: ${failure.message.orEmpty()}") }
            }
            avatarEdited = true
        }
    }

    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) action {
            banner = withContext(Dispatchers.IO) {
                val mime = resolver.getType(uri)
                require(mime in listOf("image/png", "image/jpeg", "image/gif")) { "Banner: selecione um JPG, PNG ou GIF." }
                val bytes = resolver.openInputStream(uri)?.use { input ->
                    val result = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        require(result.size() + count <= 1_800_000) { "Banner: o limite é 1,8 MB." }
                        result.write(buffer, 0, count)
                    }
                    result.toByteArray()
                } ?: error("Banner: arquivo indisponível.")
                val info = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, info)
                require(info.outMimeType == mime && info.outWidth in 1..4096 && info.outHeight in 1..4096) {
                    "Banner: use um JPG, PNG ou GIF válido com até 4096 px por lado."
                }
                "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        }
    }

    val equipped = vm.store.wallet(userId)?.profileCosmetics.orEmpty()
    LaunchedEffect(known) {
        known?.let { (own, images) ->
            profile = own
            publishedAvatar = images.avatar
            publishedBanner = images.banner
            if (own != null) loaded = true
        }
    }
    val handle = known?.first?.handle ?: profile?.handle ?: userId?.let { vm.repo.raw("hc-last-handle-$it") }
    val name = when {
        !handle.isNullOrBlank() -> "@$handle"
        userId == null -> translate("Herói de Hyrule")
        else -> ""
    }
    ProfileHeader(name, localAvatar.ifBlank { known?.second?.avatar.orEmpty() },
        if (known != null) known.second.banner else publishedBanner, reduceMotion,
        online = userId != null && vm.presenceHidden == false, equipped = equipped) {
        FilledTonalIconButton(onClick = { editProfile(); loadProfile() }, enabled = !busy,
            modifier = Modifier.semantics { contentDescription = translate("Editar perfil") }) {
            Text("🪶")
        }
    }
    val published = profile
    if (userId != null && published != null && published.discoverable && published.shareJourney &&
        published.journey != sharedGameStatuses(vm)) {
        TextButton(enabled = !busy, onClick = {
            action {
                val updated = published.copy(journey = sharedGameStatuses(vm))
                vm.updateOwnProfile(userId, updated)
                profile = updated
                vm.notifyMessage("Jornada compartilhada atualizada.")
            }
        }) { Text("Atualizar jornada compartilhada") }
    }
    if (!editing) error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

    val photoControls: @Composable () -> Unit = {
        OutlinedButton(enabled = !busy, onClick = { photoPicker.launch(arrayOf("image/jpeg", "image/png", "image/webp", "image/gif")) }) {
            Text("Trocar foto")
        }
        if (avatar.isNotBlank()) TextButton(enabled = !busy, onClick = { avatar = ""; avatarEdited = true }) { Text("Remover foto") }
    }
    if (editing && userId == null) AlertDialog(onDismissRequest = { if (!busy) editing = false },
        title = { Text("Editar perfil") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ProfileHeader(translate("Herói de Hyrule"), avatar, null, reduceMotion, equipped = equipped)
                photoControls()
                Text("Entre na sua conta para configurar nome, banner e visibilidade.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(enabled = !busy, onClick = {
            if (avatarEdited) vm.savePreferences(vm.repo.preferences().copy(avatar = avatar))
            editing = false
        }) { Text("Salvar") } }, dismissButton = {
            TextButton(enabled = !busy, onClick = { editing = false }) { Text("Cancelar") }
        }
    )

    if (editing && userId != null && !loaded) AlertDialog(
        onDismissRequest = { editing = false },
        title = { Text("Editar perfil") },
        text = {
            if (loadFailed) Text("Não foi possível carregar o perfil. Verifique a conexão e tente novamente.")
            else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Carregando seu perfil…")
            }
        },
        confirmButton = {
            if (loadFailed) TextButton(onClick = ::loadProfile) { Text("Tentar novamente") }
        },
        dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancelar") } }
    )

    if (editing && userId != null && loaded) CommunityProfileEditor(vm, userId, profile, busy, error, mediaEditor = { handle, visible ->
        Text(if (visible) "Prévia do perfil público" else "Prévia · este perfil está privado")
        val visibleAvatar = if (publishPhoto) (if (!avatarEdited) publishedAvatar ?: avatar else avatar) else ""
        ProfileHeader(if (handle.isBlank()) "Herói de Hyrule" else "@$handle",
            visibleAvatar, banner, reduceMotion, equipped = equipped)
        if (!publishPhoto && avatar.isNotBlank()) Avatar(avatar, reduceMotion, Modifier.size(48.dp))
        photoControls()
        OutlinedButton(enabled = !busy, onClick = { bannerPicker.launch(arrayOf("image/jpeg", "image/png", "image/gif")) }) {
            Text("Trocar banner")
        }
        if (banner != null) TextButton(enabled = !busy, onClick = { banner = null }) { Text("Remover banner") }
        Text("Banner JPG, PNG ou GIF · até 1,8 MB", style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(publishPhoto, onCheckedChange = { publishPhoto = it }, enabled = !busy)
            Text("Exibir foto no perfil público")
        }
    }, onClose = { editing = false; error = null }) { updated ->
        action {
            val photo = if (!avatarEdited) publishedAvatar ?: avatar else avatar
            val shared = CommunityMedia(if (publishPhoto) photo.ifBlank { null } else null, banner)
            vm.updateOwnProfile(userId, updated, shared)
            if (avatarEdited) vm.savePreferences(vm.repo.preferences().copy(avatar = avatar))
            profile = updated
            publishedBanner = shared.banner
            publishedAvatar = shared.avatar
            editing = false
        }
    }
}
