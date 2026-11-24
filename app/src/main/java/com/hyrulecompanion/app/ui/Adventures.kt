package com.hyrulecompanion.app.ui

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.saveable.rememberSaveable
import com.hyrulecompanion.app.network.CommunityAuthor
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hyrulecompanion.app.HyruleViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

private data class AdventurePost(val id: Long, val handle: String, val body: String, val hasImage: Boolean,
                                 val mine: Boolean, val applause: Int, val applauded: Boolean, val comments: Int)
private data class AdventureComment(val id: Long, val handle: String, val body: String, val mine: Boolean)

private fun <T> String.records(read: (JSONObject) -> T): List<T> = JSONArray(this).let { rows ->
    List(rows.length()) { read(rows.getJSONObject(it)) }
}

private suspend fun adventureImage(resolver: ContentResolver, uri: Uri): String = withContext(Dispatchers.IO) {
    val info = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, info) }
    require(info.outWidth > 0 && info.outHeight > 0)
    var sample = 1
    while (maxOf(info.outWidth, info.outHeight) / sample > 1440) sample *= 2
    val bitmap = requireNotNull(resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    })
    try {
        val bytes = ByteArrayOutputStream()
        for (quality in listOf(88, 75, 60, 42, 25)) {
            bytes.reset()
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bytes))
            if (bytes.size() <= 300000) return@withContext "data:image/jpeg;base64," +
                Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
        }
        error("image_size")
    } finally { bitmap.recycle() }
}

private fun adventureError(failure: Exception): String = when {
    failure.message.orEmpty().contains("profile_required") -> "Ative seu perfil público na Comunidade para publicar ou comentar."
    failure.message.orEmpty().contains("_rate") -> "Aguarde alguns segundos antes de publicar de novo."
    else -> "Não foi possível concluir a ação. Tente novamente."
}

@Composable
internal fun AdventuresScreen(vm: HyruleViewModel, userId: String, onBack: () -> Unit, onAuthor: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val resolver = LocalContext.current.contentResolver
    var following by rememberSaveable { mutableStateOf(false) }
    var composePost by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Long?>(null) }
    var delete by remember { mutableStateOf<Long?>(null) }
    var body by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<String?>(null) }
    var readingImage by remember { mutableStateOf(false) }
    var writing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableIntStateOf(0) }
    val images = remember(userId) { android.util.LruCache<Long, String>(10) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                readingImage = true
                error = null
                try { picked = adventureImage(resolver, uri) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = "Não foi possível abrir a imagem escolhida." }
                finally { readingImage = false }
            }
        }
    }

    fun change(block: suspend () -> Unit) {
        if (writing) return
        writing = true
        error = null
        scope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = adventureError(failure) }
            finally { writing = false }
        }
    }

    key(following, version) {
        AdventureFeed(vm, userId, images, following, onBack,
            controls = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!following, { following = false }, label = { Text("Todas") })
                    FilterChip(following, { following = true }, label = { Text("Quem eu sigo") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { error = null; composePost = true }) { Text("Publicar aventura") }
                    TextButton(onClick = { error = null; version++ }) { Text("Atualizar") }
                }
                if (!composePost) error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }, onComments = { selected = it }, onDelete = { delete = it }, onAuthor = onAuthor)
    }

    selected?.let { post ->
        AdventureComments(vm, userId, post, onDismiss = { selected = null }, onChanged = { version++ })
    }
    delete?.let { id ->
        AlertDialog(onDismissRequest = { delete = null }, title = { Text("Excluir publicação?") },
            confirmButton = { TextButton(onClick = {
                delete = null
                change {
                    vm.communityRequest(userId) { featureRpc(it, "adventure_delete", JSONObject().put("p_post", id)) }
                    images.remove(id)
                    version++
                }
            }, enabled = !writing) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { delete = null }) { Text("Cancelar") } })
    }
    if (composePost) AlertDialog(onDismissRequest = { if (!writing && !readingImage) composePost = false },
        title = { Text("Nova aventura") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(body, { body = it.take(1600) }, Modifier.fillMaxWidth(),
                    label = { Text("Conte sua aventura ou compartilhe uma dica") }, minLines = 3, maxLines = 6)
                picked?.let { PhotoPreview(it, Modifier.fillMaxWidth().height(110.dp)) }
                Row {
                    TextButton(onClick = { picker.launch("image/*") }, enabled = !readingImage && !writing) { Text("Escolher imagem") }
                    if (picked != null) TextButton(onClick = { picked = null }, enabled = !writing) { Text("Remover") }
                }
                if (readingImage || writing) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { Button(enabled = (body.isNotBlank() || picked != null) && !writing && !readingImage, onClick = {
            change {
                val args = JSONObject().put("p_body", body.trim()).put("p_image", picked ?: JSONObject.NULL)
                vm.communityRequest(userId) { featureRpc(it, "adventure_publish", args) }
                body = ""
                picked = null
                composePost = false
                if (following) following = false else version++
            }
        }) { Text("Publicar") } },
        dismissButton = { TextButton(enabled = !writing && !readingImage, onClick = { composePost = false }) { Text("Cancelar") } })
}

@Composable
private fun AdventureFeed(vm: HyruleViewModel, userId: String, images: android.util.LruCache<Long, String>,
                          following: Boolean, onBack: () -> Unit, controls: @Composable ColumnScope.() -> Unit,
                          onComments: (Long) -> Unit, onDelete: (Long) -> Unit, onAuthor: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val authors = remember { mutableStateMapOf<String, CommunityAuthor?>() }
    val pendingAuthors = remember { mutableSetOf<String>() }
    val avatarRequests = remember { kotlinx.coroutines.sync.Semaphore(2) }
    var posts by remember { mutableStateOf<List<AdventurePost>>(emptyList()) }
    var more by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun fetchNext() {
        if (loading || !more) return
        loading = true
        error = null
        val before = posts.lastOrNull()?.id
        scope.launch {
            try {
                val page = vm.communityRequest(userId) {
                    featureRpc(it, "adventure_feed", JSONObject().put("p_following", following)
                        .put("p_before", before ?: JSONObject.NULL))
                }.records { row ->
                    AdventurePost(row.getLong("id"), row.getString("handle"), row.getString("body"),
                        row.getBoolean("has_image"), row.getBoolean("mine"), row.getInt("applause"),
                        row.getBoolean("applauded"), row.getInt("comments"))
                }
                posts = posts + page
                more = page.size == 12
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = adventureError(failure) }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { fetchNext() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column {
                TextButton(onClick = onBack) { Text("← Voltar à Comunidade") }
                ScreenTitle("Aventuras", "Compartilhe o que encontrou em Hyrule", "✦")
                controls()
            }
        }
        if (posts.isEmpty() && !loading && error == null) item {
            Text(if (following) "Nenhuma publicação de quem você segue." else "Ainda não há aventuras publicadas.")
        }
        items(posts, key = { it.id }) { post ->
            var clapping by remember { mutableStateOf(false) }
            var photo by remember { mutableStateOf(images.get(post.id)) }
            var photoFailed by remember { mutableStateOf(false) }
            var photoRetry by remember { mutableIntStateOf(0) }
            LaunchedEffect(post.id, photoRetry) {
                if (!post.hasImage || photo != null) return@LaunchedEffect
                photoFailed = false
                try {
                    val raw = vm.communityRequest(userId) { featureRpc(it, "adventure_image", JSONObject().put("p_post", post.id)) }
                    photo = (org.json.JSONTokener(raw).nextValue() as? String)?.also { images.put(post.id, it) }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { photoFailed = true }
            }
            LaunchedEffect(post.handle) {
                if (!authors.containsKey(post.handle) && pendingAuthors.add(post.handle)) {
                    scope.launch {
                        try {
                            authors[post.handle] = avatarRequests.withPermit {
                                vm.communityRequest(userId) { adventureAuthor(it, post.handle) }
                            }
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { /* The avatar falls back to the default. */ }
                        finally { pendingAuthors.remove(post.handle) }
                    }
                }
            }
            ParchmentCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .clickable(onClickLabel = translate("Abrir perfil")) { onAuthor(post.handle) }
                    .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Avatar(authors[post.handle]?.avatar.orEmpty(), vm.repo.preferences().reduceMotion, Modifier.size(38.dp))
                    androidx.compose.material3.Text("@${post.handle}", fontWeight = FontWeight.Bold)
                }
                if (post.body.isNotBlank()) androidx.compose.material3.Text(post.body)
                photo?.let { PhotoPreview(it, Modifier.fillMaxWidth().heightIn(max = 350.dp)) }
                if (photoFailed) TextButton(onClick = { photoRetry++ }) { Text("Recarregar imagem") }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(post.applauded, {
                        clapping = true
                        scope.launch {
                            try {
                                val response = vm.communityRequest(userId) {
                                    featureRpc(it, "adventure_clap", JSONObject().put("p_post", post.id).put("p_applaud", !post.applauded))
                                }
                                posts = posts.map { row ->
                                    if (row.id == post.id) row.copy(applauded = !post.applauded, applause = JSONObject(response).getInt("count")) else row
                                }
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (failure: Exception) { error = adventureError(failure) }
                            finally { clapping = false }
                        }
                    }, enabled = !clapping, label = { Text("👏 ${post.applause}") })
                    if (post.mine) TextButton(onClick = { onDelete(post.id) }) { Text("Excluir") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { onComments(post.id) }) {
                        Icon(Icons.Outlined.ChatBubbleOutline, translate("Comentários"), Modifier.size(19.dp))
                        Text(" ${post.comments}")
                    }
                }
            }
        }
        if (error != null) item { Text(error.orEmpty(), color = MaterialTheme.colorScheme.error) }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        else if (more) item { TextButton(onClick = { fetchNext() }) { Text("Carregar mais") } }
    }
}

@Composable
private fun PhotoPreview(photo: String, modifier: Modifier) {
    AsyncImage(rememberProfileImage(photo, reduceMotion = true), translate("Imagem da aventura"),
        modifier, contentScale = ContentScale.Fit)
}

@Composable
private fun AdventureComments(vm: HyruleViewModel, userId: String, post: Long, onDismiss: () -> Unit, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    var comments by remember(post) { mutableStateOf<List<AdventureComment>>(emptyList()) }
    var message by remember(post) { mutableStateOf("") }
    var busy by remember(post) { mutableStateOf(false) }
    var more by remember(post) { mutableStateOf(false) }
    var error by remember(post) { mutableStateOf<String?>(null) }

    suspend fun load(before: Long?) {
        val result = vm.communityRequest(userId) {
            featureRpc(it, "adventure_comments", JSONObject().put("p_post", post).put("p_before", before ?: JSONObject.NULL))
        }.records { row ->
            AdventureComment(row.getLong("id"), row.getString("handle"), row.getString("body"), row.getBoolean("mine"))
        }
        comments = if (before == null) result else comments + result
        more = result.size == 30
    }
    fun act(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = adventureError(failure) }
            finally { busy = false }
        }
    }
    LaunchedEffect(post) { act { load(null) } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Comentários") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 230.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(comments, key = { it.id }) { comment ->
                    Text("@${comment.handle}", fontWeight = FontWeight.Bold)
                    androidx.compose.material3.Text(comment.body)
                    if (comment.mine) TextButton(enabled = !busy, onClick = {
                        act {
                            vm.communityRequest(userId) {
                                featureRpc(it, "adventure_delete_comment", JSONObject().put("p_comment", comment.id))
                            }
                            onChanged()
                            load(null)
                        }
                    }) { Text("Excluir comentário") }
                    HorizontalDivider()
                }
                if (more) item { TextButton(enabled = !busy, onClick = { act { load(comments.lastOrNull()?.id) } }) { Text("Carregar mais") } }
            }
            if (comments.isEmpty() && !busy && error == null) Text("Nenhum comentário ainda.")
            OutlinedTextField(message, { message = it.take(600) }, Modifier.fillMaxWidth(), maxLines = 3,
                label = { Text("Escreva um comentário") })
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        Button(enabled = !busy && message.isNotBlank(), onClick = {
            act {
                vm.communityRequest(userId) {
                    featureRpc(it, "adventure_comment", JSONObject().put("p_post", post).put("p_body", message.trim()))
                }
                message = ""
                onChanged()
                load(null)
            }
        }) { Text("Comentar") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } })
}
