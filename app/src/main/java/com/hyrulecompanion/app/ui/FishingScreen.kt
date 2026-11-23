package com.hyrulecompanion.app.ui

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.hyrulecompanion.app.R
import com.hyrulecompanion.app.data.CatchAttempt
import com.hyrulecompanion.app.data.FishRarity
import com.hyrulecompanion.app.data.FishMotion
import com.hyrulecompanion.app.data.StoreRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.abs

private enum class FishingPhase { READY, CASTING, WAITING, BITE, REELING, CREDITING, CAUGHT }
private const val CAPTURE_WIDTH = .24f

@Composable
internal fun FishingScreen(store: StoreRepository, userId: String?, reduceMotion: Boolean, hapticsEnabled: Boolean, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = remember(context, hapticsEnabled) { FishingHaptics(context, hapticsEnabled) }
    var phase by remember { mutableStateOf(FishingPhase.READY) }
    var attempt by remember { mutableStateOf<CatchAttempt?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingCast by remember { mutableStateOf<String?>(null) }
    var reward by remember { mutableStateOf<Int?>(null) }
    var elapsed by remember { mutableFloatStateOf(0f) }
    var fishAt by remember { mutableFloatStateOf(FishMotion.START_POSITION) }
    var barAt by remember { mutableFloatStateOf(FishMotion.START_POSITION) }
    var held by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var resumed by remember { mutableStateOf(false) }
    var fishMotion by remember { mutableStateOf<FishMotion?>(null) }

    LifecycleResumeEffect(haptics) {
        resumed = true
        onPauseOrDispose {
            resumed = false
            held = false
            haptics.stop()
        }
    }

    fun cast() {
        if (phase != FishingPhase.READY) return
        val id = pendingCast ?: UUID.randomUUID().toString()
        pendingCast = id
        phase = FishingPhase.CASTING
        error = null
        scope.launch {
            try {
                attempt = store.cast(userId, id)
                pendingCast = null
                phase = FishingPhase.WAITING
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = storeError(failure)
                phase = FishingPhase.READY
            }
        }
    }

    fun claim(catch: CatchAttempt) {
        haptics.stop()
        phase = FishingPhase.CREDITING
        error = null
        scope.launch {
            try {
                reward = store.claim(userId, catch)
                phase = FishingPhase.CAUGHT
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = storeError(failure)
            }
        }
    }

    LaunchedEffect(phase, resumed) {
        if (!resumed) return@LaunchedEffect
        when (phase) {
            FishingPhase.WAITING -> {
                delay(attempt?.waitMs ?: 3000L)
                phase = FishingPhase.BITE
            }
            FishingPhase.BITE -> {
                delay(2600)
                haptics.escape()
                attempt = null
                error = "O peixe soltou a isca. Tente lançar de novo."
                phase = FishingPhase.READY
            }
            FishingPhase.REELING -> {
                val movement = fishMotion ?: return@LaunchedEffect
                var last = withFrameNanos { it }
                while (phase == FishingPhase.REELING) {
                    val now = withFrameNanos { it }
                    val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, .08f)
                    last = now
                    elapsed += dt
                    val previousPosition = fishAt
                    fishAt = movement.advance(dt)
                    val motion = if (held) .57f else -.47f
                    barAt = (barAt + motion * dt).coerceIn(CAPTURE_WIDTH / 2, 1f - CAPTURE_WIDTH / 2)
                    val distance = abs(fishAt - barAt)
                    val speed = abs(fishAt - previousPosition) / dt.coerceAtLeast(.001f)
                    val tension = (speed * .9f + distance * 2.4f + if (held) .12f else 0f).coerceIn(0f, 1f)
                    val matches = distance < CAPTURE_WIDTH / 2
                    progress = (progress + dt * if (matches) .17f else -.085f).coerceIn(0f, 1f)
                    if (progress >= 1f) {
                        attempt?.let(::claim)
                    } else if (elapsed >= 30f) {
                        haptics.escape()
                        attempt = null
                        error = "O peixe escapou. Tente novamente!"
                        phase = FishingPhase.READY
                    } else {
                        haptics.pull(elapsed, tension)
                    }
                }
            }
            else -> Unit
        }
    }

    fun hook() {
        if (phase != FishingPhase.BITE) return
        val cast = attempt ?: return
        fishMotion = FishMotion(cast.rarity)
        haptics.hook()
        held = false
        progress = 0f
        elapsed = 0f
        fishAt = FishMotion.START_POSITION
        barAt = FishMotion.START_POSITION
        phase = FishingPhase.REELING
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Store") }
            Spacer(Modifier.weight(1f))
            Text(store.wallet(userId)?.balanceLabel().orEmpty(),
                style = MaterialTheme.typography.labelLarge)
        }
        Text("Pesca no Mar de Hyrule", style = MaterialTheme.typography.headlineSmall)
        val playing = phase == FishingPhase.REELING
        val controls = when (phase) {
            FishingPhase.BITE -> Modifier.clickable(role = Role.Button, onClickLabel = "Fisgar", onClick = ::hook)
            FishingPhase.REELING -> Modifier.pointerInput(Unit) {
                detectTapGestures(onPress = {
                    held = true
                    try { tryAwaitRelease() } finally { held = false }
                })
            }
            else -> Modifier
        }
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .then(controls)) {
            FishingWater(phase, reduceMotion, Modifier.matchParentSize())
            if (playing) Column(Modifier.align(Alignment.TopEnd).padding(10.dp).width(112.dp)
                .background(Color(0xEE132928), RoundedCornerShape(10.dp)).padding(8.dp)) {
                Text("Captura", color = Color.White, style = MaterialTheme.typography.labelMedium)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF35F60), trackColor = Color(0xFF563334))
            }
            if (playing) {
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp)
                    .background(Color(0xEA132928), RoundedCornerShape(12.dp)).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(attempt?.species.orEmpty(), color = Color.White, style = MaterialTheme.typography.labelLarge)
                    BoxWithConstraints(Modifier.fillMaxWidth().height(31.dp).clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF203D3D))) {
                        Box(Modifier.offset(x = maxWidth * (barAt - CAPTURE_WIDTH / 2))
                            .width(maxWidth * CAPTURE_WIDTH).fillMaxHeight().background(Color(0xFF4ED68A)))
                        FishIcon(attempt?.species.orEmpty(), Modifier
                            .offset(x = (maxWidth * fishAt - 16.dp).coerceIn(0.dp, (maxWidth - 32.dp).coerceAtLeast(0.dp)))
                            .align(Alignment.CenterStart).size(32.dp, 25.dp))
                    }
                }
            }
            if (phase == FishingPhase.BITE) Surface(Modifier.align(Alignment.Center),
                color = Color(0xFFF9DC83), contentColor = Color(0xFF193734), shape = RoundedCornerShape(14.dp)) {
                Text("Mordeu! Toque para fisgar", Modifier.padding(12.dp))
            }
        }
        Text(when (phase) {
            FishingPhase.READY -> "Lance a isca e espere a boia balançar."
            FishingPhase.CASTING -> "Lançando a isca…"
            FishingPhase.WAITING -> "Observe a boia."
            FishingPhase.BITE -> "Toque na água para começar a pesca."
            FishingPhase.REELING -> "Segure para mover a barra verde para a direita. Solte para mover para a esquerda. Mantenha o peixe dentro dela."
            FishingPhase.CREDITING -> "Registrando sua captura…"
            FishingPhase.CAUGHT -> "Captura registrada."
        }, style = MaterialTheme.typography.bodySmall)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (phase == FishingPhase.READY) Button(onClick = ::cast, modifier = Modifier.fillMaxWidth()) {
            Text("Lançar a isca")
        }
        if (phase == FishingPhase.CREDITING && error != null) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { attempt?.let(::claim) }) { Text("Tentar crédito novamente") }
            TextButton(onClick = { attempt = null; error = null; phase = FishingPhase.READY }) { Text("Nova isca") }
        }
    }

    if (phase == FishingPhase.CAUGHT) attempt?.let { catch ->
        CatchDialog(catch, reward ?: 0) {
            reward = null
            attempt = null
            phase = FishingPhase.READY
        }
    }
}

@Composable
private fun CatchDialog(catch: CatchAttempt, reward: Int, dismiss: () -> Unit) {
    val context = LocalContext.current
    DisposableEffect(catch.id, context) {
        val sound = if (catch.rarity == FishRarity.RARE) R.raw.item_catch_fanfare else R.raw.small_item_catch
        val player = runCatching { MediaPlayer.create(context, sound) }.getOrNull()
        runCatching { player?.start() }
        onDispose { player?.release() }
    }
    Dialog(onDismissRequest = dismiss) {
        Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClick = dismiss), color = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Parabéns pela captura!", style = MaterialTheme.typography.titleLarge)
                FishIcon(catch.species, Modifier.size(144.dp, 96.dp))
                Text(catch.species, style = MaterialTheme.typography.titleMedium)
                Text(catch.rarity.label)
                Text("+$reward rúpias verdes")
                Text("Clique para continuar", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun FishIcon(species: String, modifier: Modifier) {
    if (species.isBlank()) return
    val filename = species.lowercase().replace(' ', '-')
    AsyncImage("file:///android_asset/fishing/fish/$filename.webp", contentDescription = species,
        modifier = modifier, contentScale = ContentScale.Fit)
}

@Composable
private fun FishingWater(phase: FishingPhase, reduceMotion: Boolean, modifier: Modifier) {
    var wobble by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(phase, reduceMotion) {
        wobble = 0f
        if (phase != FishingPhase.BITE || reduceMotion) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) wobble = withFrameNanos { sin((it - start) / 1e9 * 6 * PI).toFloat() }
    }
    Box(modifier.background(Brush.verticalGradient(listOf(Color(0xFF8EA68D), Color(0xFF133E50))))) {
        AsyncImage("file:///android_asset/fishing/hyrule-sea.webp", null,
            Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val rod = Path().apply {
                moveTo(w * .025f, h * .98f)
                quadraticTo(w * .22f, h * .44f, w * .48f, h * .38f)
            }
            drawPath(rod, Color(0xFF231710), style = androidx.compose.ui.graphics.drawscope.Stroke(7.dp.toPx()))
            drawPath(rod, Color(0xFFC28E53), style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
            if (phase in listOf(FishingPhase.WAITING, FishingPhase.BITE, FishingPhase.REELING)) {
                val bob = Offset(w * (.62f + wobble * .035f), h * (.62f + wobble * .024f))
                drawLine(Color(0xFFE4DAB7), Offset(w*.48f,h*.38f), bob, 1.dp.toPx())
                drawOval(Color(0xFF7DEBCD).copy(alpha = .8f), bob - Offset(w*.08f,h*.019f),
                    Size(w*.16f,h*.038f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                drawCircle(Color(0xFFF26451), 8.dp.toPx(), bob)
                drawCircle(Color(0xFFFFEBB5), 4.dp.toPx(), bob - Offset(0f,5.dp.toPx()))
            }
        }
    }
}
