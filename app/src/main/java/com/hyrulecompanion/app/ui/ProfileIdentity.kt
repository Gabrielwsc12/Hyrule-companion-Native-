package com.hyrulecompanion.app.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
internal fun ProfileBanner(value: String?) {
    if (value.isNullOrBlank()) return
    val bytes = remember(value) {
        runCatching { Base64.decode(value.substringAfter("base64,"), Base64.DEFAULT) }.getOrNull()
    } ?: return
    AsyncImage(bytes, "Banner do perfil", Modifier.fillMaxWidth().aspectRatio(3f)
        .clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
}

@Composable
internal fun ProfileCommunitySettings(vm: HyruleViewModel, userId: String, localAvatar: String,
                                      onIdentity: (String?, String?) -> Unit) {
    val scope = rememberCoroutineScope()
    val resolver = LocalContext.current.contentResolver
    var profile by remember { mutableStateOf<CommunityProfile?>(null) }
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
                    else -> failure.message?.takeIf { it.startsWith("Banner:") } ?: "Não foi possível atualizar o perfil. Tente novamente."
                }
            } finally { busy = false }
        }
    }

    fun load(open: Boolean) = action {
        val (own, images) = vm.communityRequest(userId) { account ->
            communityProfile(account) to communityMedia(account, userId)
        }
        profile = own
        onIdentity(own?.handle, images.banner)
        if (open) {
            banner = images.banner
            publishPhoto = images.avatar != null
            editing = true
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

    LaunchedEffect(userId) { load(false) }
    OutlinedButton(onClick = { load(true) }, enabled = !busy) { Text("Editar nome, banner e visibilidade") }
    if (!editing) error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

    if (editing) CommunityProfileEditor(vm, userId, profile, busy, error, mediaEditor = {
        ProfileBanner(banner)
        Text("Banner JPG ou PNG · proporção recomendada 3:1 · até 1 MB")
        OutlinedButton(enabled = !busy, onClick = { bannerPicker.launch(arrayOf("image/jpeg", "image/png")) }) {
            Text("Escolher banner")
        }
        if (banner != null) TextButton(enabled = !busy, onClick = { banner = null }) { Text("Remover banner") }
        Row {
            Checkbox(publishPhoto, onCheckedChange = { publishPhoto = it }, enabled = !busy)
            Text("Compartilhar minha foto atual", Modifier.padding(top = 12.dp))
        }
        if (publishPhoto) Text("Salve o perfil para atualizar ou remover a foto que os visitantes veem.", style = MaterialTheme.typography.bodySmall)
        if (!it) Text("As imagens aparecem para outros jogadores quando o perfil está visível.")
        if (publishPhoto && localAvatar.isBlank()) Text("Escolha uma foto no Perfil.")
    }, onClose = { editing = false; error = null }) { updated ->
        action {
            val shared = CommunityMedia(if (publishPhoto) localAvatar.ifBlank { null } else null, banner)
            vm.communityRequest(userId) { account -> saveCommunityProfile(account, updated, shared) }
            profile = updated
            onIdentity(updated.handle, shared.banner)
            editing = false
        }
    }
}
