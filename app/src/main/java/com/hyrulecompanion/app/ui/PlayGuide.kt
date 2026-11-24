package com.hyrulecompanion.app.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.hyrulecompanion.app.data.Game
import org.json.JSONObject

internal class PlayGuides(context: Context) {
    private val entries = context.assets.open("play-guide.json").bufferedReader().use { JSONObject(it.readText()) }
    operator fun contains(id: String) = entries.has(id)
    operator fun get(id: String): JSONObject? = entries.optJSONObject(id)
}

@Composable
internal fun NesController() {
    val shell = MaterialTheme.colorScheme.onSurfaceVariant
    val inset = MaterialTheme.colorScheme.surface
    val red = Color(0xFFBA3F3F)
    Canvas(Modifier.size(26.dp, 17.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(shell, cornerRadius = CornerRadius(2.dp.toPx()))
        drawRoundRect(inset, Offset(w * .075f, h * .12f), Size(w * .85f, h * .76f), CornerRadius(1.dp.toPx()))
        drawRect(shell, Offset(w * .16f, h * .43f), Size(w * .28f, h * .18f))
        drawRect(shell, Offset(w * .255f, h * .3f), Size(w * .09f, h * .44f))
        drawCircle(red, h * .115f, Offset(w * .64f, h * .59f))
        drawCircle(red, h * .115f, Offset(w * .8f, h * .52f))
    }
}

@Composable
internal fun PlayGuideScreen(game: Game, entry: JSONObject, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack) { Text("← Voltar à ficha") }
        ScreenTitle("Forma definitiva de jogar", game.name, "")
        CoverArt(game, Modifier.fillMaxWidth().height(144.dp), large = true)
        GuideRecommendation("Plataforma recomendada", entry.getJSONObject("official"))
        ScreenTitle("Emulação", "", "")
        val emulator = entry.optJSONObject("emulation")
        if (emulator != null) GuideRecommendation("Emulador recomendado", emulator)
        else Text("Nenhum emulador foi verificado para esta edição.", style = MaterialTheme.typography.bodySmall)
        if (activeLanguage() != "en") {
            val patch = entry.optJSONObject("translation")
            if (patch != null) GuideRecommendation("Tradução em português do Brasil", patch)
            else Text("Nenhuma tradução para esta edição foi verificada.", style = MaterialTheme.typography.bodySmall)
        }
        val mods = entry.getJSONArray("mods")
        SmallHeading("Mods e melhorias")
        if (mods.length() == 0) {
            Text("A recomendação é jogar com as opções padrão. Nenhum mod específico foi verificado para esta edição.",
                style = MaterialTheme.typography.bodySmall)
        }
        repeat(mods.length()) { index -> GuideRecommendation(null, mods.getJSONObject(index)) }
    }
}

@Composable
private fun GuideRecommendation(heading: String?, item: JSONObject) {
    val uri = LocalUriHandler.current
    var linkError by remember(item) { mutableStateOf(false) }
    ParchmentCard(Modifier.fillMaxWidth()) {
        heading?.let { SmallHeading(it) }
        Text(item.getString("title"), style = MaterialTheme.typography.titleMedium)
        Text(item.getString("description"), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = {
            linkError = runCatching { uri.openUri(item.getString("url")) }.isFailure
        }) {
            Text("Abrir página oficial")
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.OpenInNew, null, Modifier.size(15.dp))
        }
        if (linkError) Text("Não foi possível abrir o link.", color = MaterialTheme.colorScheme.error)
    }
}
