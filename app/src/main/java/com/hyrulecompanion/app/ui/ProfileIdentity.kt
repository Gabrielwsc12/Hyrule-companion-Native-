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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hyrulecompanion.app.HyruleViewModel
import com.hyrulecompanion.app.network.CommunityMedia
import com.hyrulecompanion.app.network.CommunityProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ProfileHeader(
    name: String, avatar: String, banner: String?, reduceMotion: Boolean,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val bytes = remember(banner) {
        banner?.let { runCatching { Base64.decode(it.substringAfter("base64,"), Base64.DEFAULT) }.getOrNull() }
    }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.primaryContainer)) {
        if (bytes != null) {
            AsyncImage(bytes, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = .12f), Color.Black.copy(alpha = .81f))
            )))
        }
        Column(Modifier.fillMaxWidth().heightIn(min = 132.dp).padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, content = actions)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(avatar, reduceMotion, Modifier.size(72.dp))
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
                    color = if (bytes == null) MaterialTheme.colorScheme.onPrimaryContainer else Color.White)
            }
        }
    }
}

@Composable
internal fun ProfileCommunitySettings(vm: HyruleViewModel, userId: String?, localAvatar: String,
                                      reduceMotion: Boolean) {
    val scope = rememberCoroutineScope()
    val resolver = LocalContext.current.contentResolver
    var profile by remember { mutableStateOf<CommunityProfile?>(null) }
    var avatar by remember { mutableStateOf("") }
    var publishedAvatar by remember { mutableStateOf<String?>(null) }
    var avatarEdited by remember { mutableStateOf(false) }
    var publishedBanner by remember { mutableStateOf<String?>(null) }
    var banner by remember { mutableStateOf<String?>(null) }
    var publishPhoto by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
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

    fun load(open: Boolean) = action {
        if (userId != null) {
            val (own, images) = vm.communityRequest(userId) { account ->
                communityProfile(account) to communityMedia(account, userId)
            }
            profile = own
            publishedBanner = images.banner
            publishedAvatar = images.avatar
            if (open) publishPhoto = images.avatar != null
        }
        if (open) {
            avatar = localAvatar.ifBlank { publishedAvatar.orEmpty() }
            avatarEdited = false
            banner = publishedBanner
            editing = true
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
                require(mime == "image/png" || mime == "image/jpeg") { "Banner: selecione um JPG ou PNG." }
                val bytes = resolver.openInputStream(uri)?.use { input ->
                    val result = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        require(result.size() + count <= 1_000_000) { "Banner: o limite é 1 MB." }
                        result.write(buffer, 0, count)
                    }
                    result.toByteArray()
                } ?: error("Banner: arquivo indisponível.")
                val info = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, info)
                require(info.outMimeType == mime && info.outWidth in 1..4096 && info.outHeight in 1..4096) {
                    "Banner: use um JPG ou PNG válido com até 4096 px por lado."
                }
                "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        }
    }

    LaunchedEffect(userId) { if (userId != null) load(false) }
    ProfileHeader(profile?.let { "@${it.handle}" } ?: "Herói de Hyrule", localAvatar, publishedBanner, reduceMotion) {
        FilledTonalIconButton(onClick = { load(true) }, enabled = !busy,
            modifier = Modifier.semantics { contentDescription = "Editar perfil" }) {
            Text("🪶")
        }
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
                ProfileHeader("Herói de Hyrule", avatar, null, reduceMotion)
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

    if (editing && userId != null) CommunityProfileEditor(vm, userId, profile, busy, error, mediaEditor = { handle, visible ->
        Text(if (visible) "Prévia do perfil público" else "Prévia · este perfil está privado")
        val visibleAvatar = if (publishPhoto) (if (!avatarEdited) publishedAvatar ?: avatar else avatar) else ""
        ProfileHeader(if (handle.isBlank()) "Herói de Hyrule" else "@$handle",
            visibleAvatar, banner, reduceMotion)
        if (!publishPhoto && avatar.isNotBlank()) Avatar(avatar, reduceMotion, Modifier.size(48.dp))
        photoControls()
        OutlinedButton(enabled = !busy, onClick = { bannerPicker.launch(arrayOf("image/jpeg", "image/png")) }) {
            Text("Trocar banner")
        }
        if (banner != null) TextButton(enabled = !busy, onClick = { banner = null }) { Text("Remover banner") }
        Text("Banner JPG ou PNG · até 1 MB", style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(publishPhoto, onCheckedChange = { publishPhoto = it }, enabled = !busy)
            Text("Exibir foto no perfil público")
        }
    }, onClose = { editing = false; error = null }) { updated ->
        action {
            val photo = if (!avatarEdited) publishedAvatar ?: avatar else avatar
            val shared = CommunityMedia(if (publishPhoto) photo.ifBlank { null } else null, banner)
            vm.communityRequest(userId) { account -> saveCommunityProfile(account, updated, shared) }
            if (avatarEdited) vm.savePreferences(vm.repo.preferences().copy(avatar = avatar))
            profile = updated
            publishedBanner = shared.banner
            publishedAvatar = shared.avatar
            editing = false
        }
    }
}
