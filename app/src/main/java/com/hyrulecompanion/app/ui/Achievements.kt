package com.hyrulecompanion.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsEsports
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hyrulecompanion.app.HyruleViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val RETRO_SETTINGS = "https://retroachievements.org/controlpanel.php"

internal data class RetroGame(val title: String, val earned: Int, val possible: Int, val lastEarned: Instant?)

internal data class RetroSummary(val handle: String, val games: List<RetroGame>) {
    val total get() = games.sumOf { it.earned }
    val latestGame get() = games.filter { it.lastEarned != null }.maxByOrNull { it.lastEarned!! }

    companion object {
        fun read(raw: String): RetroSummary? {
            if (raw.trim() in setOf("", "null")) return null
            val value = JSONObject(raw)
            val rows = value.getJSONArray("games")
            return RetroSummary(value.getString("handle"), List(rows.length()) { index ->
                val row = rows.getJSONObject(index)
                RetroGame(row.getString("title"), row.getInt("earned"), row.getInt("possible"),
                    row.optString("last_earned").takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() })
            }.sortedByDescending { it.earned })
        }
    }
}

internal suspend fun readRetro(vm: HyruleViewModel, userId: String, profileId: String): RetroSummary? =
    vm.communityRequest(userId) {
        RetroSummary.read(featureRpc(it, "retro_summary", JSONObject().put("p_user", profileId)))
    }

@Composable
internal fun RetroSummaryCard(summary: RetroSummary, compact: Boolean = false) {
    var expanded by remember { mutableStateOf(!compact) }
    ParchmentCard(Modifier.fillMaxWidth()) {
        SmallHeading("Conquistas de Zelda")
        Text("RetroAchievements · @${summary.handle}")
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Total de conquistas", style = MaterialTheme.typography.labelMedium)
                        Text(summary.total.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.SportsEsports, null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Jogo da última conquista", style = MaterialTheme.typography.labelMedium)
                        val latest = summary.latestGame
                        Text(latest?.title ?: if (summary.total == 0) "Nenhuma conquista registrada" else "Disponível após nova sincronização",
                            style = MaterialTheme.typography.titleSmall)
                        latest?.lastEarned?.let { date ->
                            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                                .withLocale(Locale.forLanguageTag(activeLanguage())).withZone(ZoneId.systemDefault())
                            Text(formatter.format(date), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        if (summary.games.isNotEmpty()) {
            if (expanded) summary.games.forEach { game ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(game.title, Modifier.weight(1f))
                    Text("${game.earned}/${game.possible}", Modifier.padding(start = 8.dp))
                }
            }
            if (compact) TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Recolher jogos" else "Ver jogos")
            }
        }
    }
}

@Composable
internal fun AchievementsScreen(vm: HyruleViewModel, onBack: () -> Unit, onAccount: () -> Unit) {
    val userId = vm.session?.userId
    val uri = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var summary by remember(userId) { mutableStateOf<RetroSummary?>(null) }
    var handle by remember(userId) { mutableStateOf("") }
    var key by remember(userId) { mutableStateOf("") }
    var challenge by remember(userId) { mutableStateOf<String?>(null) }
    var verified by remember(userId) { mutableStateOf(false) }
    var busy by remember(userId) { mutableStateOf(false) }
    var error by remember(userId) { mutableStateOf<String?>(null) }
    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect
        try {
            summary = readRetro(vm, userId, userId)
            if (handle.isBlank()) handle = summary?.handle.orEmpty()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = "Não foi possível carregar suas conquistas." }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Voltar ao Início") }
        ScreenTitle("Conquistas", "RetroAchievements", "◆")
        if (userId == null) {
            Text("Entre na sua conta para sincronizar conquistas.")
            Button(onClick = onAccount) { Text("Ir para Conta e Sincronização") }
            return@Column
        }
        summary?.let { RetroSummaryCard(it) }
        ParchmentCard(Modifier.fillMaxWidth()) {
            SmallHeading("Sincronizar conquistas")
            Text("A sincronização importa seu progresso nos jogos principais de Zelda com conjuntos compatíveis. O resumo aparece no seu perfil público.")
            Text("Use a chave da API da sua conta RetroAchievements. A chave é usada nesta sincronização e não fica salva.")
            TextButton(onClick = { uri.openUri(RETRO_SETTINGS) }) { Text("Abrir configurações do RetroAchievements") }
            OutlinedTextField(handle, { handle = it.take(32) }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Usuário do RetroAchievements") }, enabled = !busy)
            val requiresLink = summary?.handle?.equals(handle.trim(), ignoreCase = true) != true
            if (requiresLink) {
                val code = challenge
                if (code == null) OutlinedButton(onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            val response = vm.communityRequest(userId) { featureRpc(it, "retro_challenge") }
                            challenge = org.json.JSONTokener(response).nextValue() as? String
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = "Não foi possível gerar o código de vínculo." }
                        finally { busy = false }
                    }
                }, enabled = !busy && handle.isNotBlank()) { Text("Gerar código de vínculo") }
                else {
                    Text("No site do RetroAchievements, entre na sua conta e cole este código no campo User motto. Depois volte e toque em Sincronizar conquistas.")
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(code)) }) { Text("Copiar código") }
                    androidx.compose.foundation.text.selection.SelectionContainer { Text(code) }
                }
            } else if (verified) Text("Conta vinculada. Você já pode retirar o código do seu User motto.")
            OutlinedTextField(key, { key = it.take(128) }, Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation(), label = { Text("Chave da API") }, enabled = !busy)
            Button(onClick = {
                val credential = key
                val requestedHandle = handle
                key = ""
                busy = true
                error = null
                scope.launch {
                    try {
                        val result = vm.communityRequest(userId) { retroSync(it, requestedHandle, credential) }
                        summary = RetroSummary.read(result.toString())
                        handle = summary?.handle ?: requestedHandle
                        verified = requiresLink
                        challenge = null
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        error = when {
                            failure.message.orEmpty().contains("cooldown") -> "Aguarde um minuto antes de sincronizar de novo."
                            failure.message.orEmpty().contains("credentials") -> "Confira seu nome de usuário e sua chave da API no RetroAchievements."
                            failure.message.orEmpty().contains("verification") -> {
                                challenge = null
                                "O vínculo não foi confirmado. Gere um código e confira o campo User motto no site."
                            }
                            failure.message.orEmpty().contains("linked") -> "Essa conta do RetroAchievements já está vinculada a outro perfil."
                            else -> "Não foi possível sincronizar. Tente novamente mais tarde."
                        }
                    } finally { busy = false }
                }
            }, enabled = !busy && handle.isNotBlank() && key.isNotBlank() && (!requiresLink || challenge != null)) {
                Text("Sincronizar conquistas")
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
