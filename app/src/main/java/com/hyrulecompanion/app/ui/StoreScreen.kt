package com.hyrulecompanion.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import com.hyrulecompanion.app.data.Cosmetic
import com.hyrulecompanion.app.data.StoreRepository
import com.hyrulecompanion.app.data.Wallet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

internal fun rupees(value: Long): String = NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(value)

internal fun Wallet.balanceLabel() = if (creator) "∞ Rúpias infinitas" else "${rupees(balance)} rúpias verdes"

@Composable
internal fun StoreScreen(store: StoreRepository, userId: String?, reduceMotion: Boolean,
                         onBack: () -> Unit, onFish: () -> Unit, onSettings: () -> Unit) {
    val scope = rememberCoroutineScope()
    val wallet = store.wallet(userId)
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<Cosmetic?>(null) }
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
                error = storeError(failure)
            } finally {
                busy = false
            }
        }
    }
    LaunchedEffect(userId) { action { store.refresh(userId) } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Voltar ao Início") }
        ScreenTitle("Store", "Cosméticos e temas", "◆")
        ParchmentCard(Modifier.fillMaxWidth()) {
            SmallHeading("Rúpias")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RupeeIcon(Modifier.size(22.dp, 28.dp))
                Text(wallet?.balanceLabel() ?: "Saldo indisponível")
            }
            Button(onClick = onFish, enabled = !busy && wallet != null) { Text("Ganhar rúpias") }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { action { store.refresh(userId) } }) { Text("Atualizar saldo") }
        }
        Cosmetic.entries.filter { item ->
            when (item) {
                Cosmetic.CREATOR -> false
                Cosmetic.CHESTER -> item in wallet?.owned.orEmpty()
                Cosmetic.BETA -> item in wallet?.owned.orEmpty() || (item.available && wallet?.betaAvailable != false)
                else -> true
            }
        }.forEach { item ->
            val owned = item in wallet?.owned.orEmpty()
            val equipped = item in wallet?.equipped.orEmpty()
            ParchmentCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.width(76.dp).height(82.dp), contentAlignment = Alignment.Center) {
                        CosmeticPreview(item, reduceMotion)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(item.title, fontWeight = FontWeight.Bold)
                        Text(item.description, style = MaterialTheme.typography.bodySmall)
                        Text(if (owned) "Adquirido" else "${rupees(item.price.toLong())} rúpias verdes",
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                when {
                    owned && item == Cosmetic.THEME -> OutlinedButton(onClick = onSettings) { Text("Abrir Configurações") }
                    owned -> OutlinedButton(onClick = { action { store.equip(userId, item, !equipped) } }, enabled = !busy) {
                        Text(if (equipped) "Desequipar" else "Equipar")
                    }
                    else -> Button(onClick = { confirm = item },
                        enabled = !busy && wallet?.let { it.creator || it.balance >= item.price } == true) { Text("Comprar") }
                }
            }
        }
    }
    confirm?.let { item ->
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text("Comprar ${item.title}?") },
            text = {
                Text(if (wallet?.creator == true) "Sua conta tem rúpias infinitas."
                    else "Serão descontadas ${rupees(item.price.toLong())} rúpias verdes do seu saldo.")
            },
            confirmButton = { Button(onClick = { confirm = null; action { store.buy(userId, item) } }) { Text("Confirmar compra") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancelar") } })
    }
}

internal fun storeError(failure: Exception): String {
    val message = failure.message.orEmpty()
    return when {
        message.contains("schema cache") -> "A Store ainda não foi ativada no servidor."
        message.startsWith("Rúpias") || message.startsWith("O período") || message.startsWith("Espere") ||
            message.startsWith("Esse lançamento") || message.startsWith("Lance a isca") -> message
        else -> "Falha ao acessar a Store. Verifique a conexão e tente novamente."
    }
}

@Composable
private fun CosmeticPreview(item: Cosmetic, reduceMotion: Boolean) {
    when (item) {
        Cosmetic.SWORD -> SwordCosmetic(reduceMotion, Modifier.size(44.dp, 76.dp))
        Cosmetic.GOLD -> GoldenCosmetic(reduceMotion, Modifier.size(44.dp, 62.dp))
        Cosmetic.HAT -> HatCosmetic(Modifier.size(72.dp, 48.dp))
        Cosmetic.BETA -> ProfileBadge(item, reduceMotion)
        Cosmetic.CHESTER -> ProfileBadge(item, reduceMotion)
        Cosmetic.CREATOR -> ProfileBadge(item, reduceMotion)
        Cosmetic.THEME -> Canvas(Modifier.size(68.dp, 58.dp)) {
            listOf(Color(0xFF16483F), Color(0xFF63E4BC), Color(0xFFFFD294)).forEachIndexed { i, color ->
                drawCircle(color, size.width * .20f, Offset(size.width * (.23f + i * .26f), size.height / 2))
            }
        }
    }
}

@Composable
internal fun ProfileBadge(item: Cosmetic, reduceMotion: Boolean) {
    if (item == Cosmetic.CREATOR) {
        val gleam = if (reduceMotion) .5f else cosmeticAnimation(0f, 1f, 1400, RepeatMode.Reverse)
        val shape = RoundedCornerShape(7.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(20.dp, 12.dp)) {
                polygon(Color(0xFF744716), .03f,.15f, .28f,.41f, .50f,.02f, .72f,.41f, .97f,.15f, .84f,.94f, .16f,.94f)
                polygon(Color(0xFFFFD77A), .09f,.21f, .29f,.54f, .50f,.14f, .71f,.54f, .91f,.21f, .79f,.82f, .21f,.82f)
                drawCircle(Color.White.copy(alpha = .28f + gleam * .65f), size.height * .11f, Offset(size.width * .5f, size.height * .35f))
            }
            Box(Modifier.background(Brush.horizontalGradient(
                listOf(Color(0xFFD49B43), Color.White.copy(alpha = .58f + .38f * gleam), Color(0xFFE4B358))
            ), shape).border(1.dp, Color(0xFFAD7425), shape).padding(horizontal = 7.dp, vertical = 3.dp)) {
                Text(item.title, color = Color(0xFF482509), style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    } else {
        val chest = item == Cosmetic.CHESTER
        Surface(shape = RoundedCornerShape(6.dp),
            color = if (chest) Color(0xFF412D66) else Color(0xFF745124),
            contentColor = if (chest) Color(0xFFEEE0FF) else Color(0xFFFFE8AE)) {
            Text(if (chest) "Chester" else "Beta tester", Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
internal fun HatCosmetic(modifier: Modifier) {
    Canvas(modifier.semantics { contentDescription = "Gorro do herói" }) {
        val w = size.width
        val h = size.height
        val cap = Path().apply {
            moveTo(w * .08f, h * .90f)
            cubicTo(w * .06f, h * .50f, w * .41f, h * .05f, w * .83f, h * .04f)
            quadraticTo(w * .96f, h * .20f, w * .98f, h * .60f)
            quadraticTo(w * .81f, h * .39f, w * .72f, h * .42f)
            quadraticTo(w * .76f, h * .64f, w * .82f, h * .89f)
            close()
        }
        drawPath(cap, Color(0xFF28673B))
        drawPath(cap, Color(0xFF113C2B), style = androidx.compose.ui.graphics.drawscope.Stroke(w * .035f))
        drawLine(Color(0xFF88C16D), Offset(w * .11f, h * .88f), Offset(w * .78f, h * .87f), h * .12f)
    }
}

@Composable
internal fun SwordCosmetic(reduceMotion: Boolean, modifier: Modifier, behindAvatar: Boolean = false) {
    val light = if (reduceMotion) .7f else cosmeticAnimation(.28f, .95f, 1050, RepeatMode.Reverse)
    Canvas(modifier.semantics { contentDescription = "Master Sword" }) {
        val w = size.width
        val h = size.height
        val haloY = if (behindAvatar) .31f else .75f
        drawCircle(Color(0xFF9CFAFF).copy(alpha = light * .36f), w * .44f, Offset(w / 2, h * haloY))
        polygon(Color(0xFFCEFAFF), .39f,.31f, .61f,.31f, .58f,.86f, .50f,.98f, .42f,.86f)
        polygon(Color(0xFF57AFCE), .50f,.33f, .61f,.31f, .58f,.86f, .50f,.98f)
        drawLine(Color.White.copy(alpha = light), Offset(w * .48f, h * .34f), Offset(w * .48f, h * .84f), w * .045f)
        drawLine(Color(0xFF254787), Offset(w / 2, h * .09f), Offset(w / 2, h * .33f), w * .20f)
        drawCircle(Color(0xFF62C7B4), w * .115f, Offset(w / 2, h * .10f))
        polygon(Color(0xFF6761CC), .08f,.25f, .30f,.29f, .5f,.27f, .70f,.29f, .92f,.25f, .81f,.40f, .50f,.36f, .19f,.40f)
        drawCircle(Color(0xFFF5D474), w * .078f, Offset(w / 2, h * .32f))
    }
}

@Composable
internal fun GoldenCosmetic(reduceMotion: Boolean, modifier: Modifier) {
    val angle = if (reduceMotion) 0f else cosmeticAnimation(0f, 360f, 2800, RepeatMode.Restart)
    Canvas(modifier.semantics { contentDescription = "Emblema da rúpia dourada" }) {
        val radians = Math.toRadians(angle.toDouble())
        val faceWidth = abs(cos(radians)).toFloat().coerceAtLeast(.035f)
        val depth = sin(radians).toFloat() * size.width * .22f
        // Stack projected silhouettes so the edge is visible at 90 and 270 degrees.
        for (slice in 0..10) {
            val offset = (slice / 10f - .5f) * depth
            withTransform({ translate(left = offset); scale(faceWidth, 1f, center) }) {
                if (slice == 10) rupeeFace(gold = true, reverse = angle in 90f..270f)
                else polygon(Color(0xFF9C632A), .5f,.02f, .87f,.24f, .87f,.76f, .5f,.98f, .13f,.76f, .13f,.24f)
            }
        }
    }
}

@Composable
internal fun RupeeIcon(modifier: Modifier) {
    Canvas(modifier) { rupeeFace(gold = false, reverse = false) }
}

private fun DrawScope.rupeeFace(gold: Boolean, reverse: Boolean) {
    val light = if (gold) Color(0xFFFFE187) else Color(0xFF95FFAE)
    val base = if (gold) Color(0xFFD99A31) else Color(0xFF26AD65)
    val dark = if (gold) Color(0xFF8C511D) else Color(0xFF14583E)
    polygon(base, .5f,.02f, .87f,.24f, .87f,.76f, .5f,.98f, .13f,.76f, .13f,.24f)
    polygon(light, .5f,.14f, .70f,.31f, .70f,.69f, .5f,.86f, .30f,.69f, .30f,.31f)
    polygon(if (reverse) dark else base, .5f,.26f, .65f,.5f, .5f,.74f, .35f,.5f)
}

private fun DrawScope.polygon(color: Color, vararg xy: Float) {
    val path = Path().apply {
        moveTo(xy[0] * size.width, xy[1] * size.height)
        for (i in 2 until xy.size step 2) lineTo(xy[i] * size.width, xy[i + 1] * size.height)
        close()
    }
    drawPath(path, color)
}

@Composable
private fun cosmeticAnimation(start: Float, end: Float, duration: Int, repeat: RepeatMode): Float {
    val transition = rememberInfiniteTransition(label = "Cosmético")
    return transition.animateFloat(start, end, infiniteRepeatable(tween(duration, easing = LinearEasing), repeat),
        label = "Movimento").value
}
