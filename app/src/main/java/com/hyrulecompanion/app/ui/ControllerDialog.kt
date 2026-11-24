package com.hyrulecompanion.app.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hyrulecompanion.app.data.BONUS_RUPEES
import com.hyrulecompanion.app.data.StoreRepository
import com.hyrulecompanion.app.data.bonusSequence
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun AppBarTitle(onOpenController: () -> Unit) {
    val onOpen by rememberUpdatedState(onOpenController)
    Text("▲ Hyrule Companion", Modifier.pointerInput(Unit) {
        var count = 0
        var lastTap = 0L
        detectTapGestures {
            val now = SystemClock.elapsedRealtime()
            if (now - lastTap > 800) count = 0
            lastTap = now
            if (++count == 6) {
                count = 0
                onOpen()
            }
        }
    }, style = MaterialTheme.typography.titleLarge)
}

@Composable
internal fun ControllerDialog(store: StoreRepository, userId: String?, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var matched by remember { mutableIntStateOf(0) }
    var claiming by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    fun input(key: String) {
        if (claiming) return
        val candidate = bonusSequence.take(matched) + key
        matched = (candidate.size downTo 0).first { candidate.takeLast(it) == bonusSequence.take(it) }
        if (matched != bonusSequence.size) return
        matched = 0
        claiming = true
        status = null
        scope.launch {
            try {
                status = if (store.redeem(userId, candidate)) {
                    "Cheater desbloqueado · +${rupees(BONUS_RUPEES)} rúpias verdes"
                } else {
                    "Recompensa já resgatada."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                status = storeError(failure)
            } finally {
                claiming = false
            }
        }
    }

    Dialog(onDismissRequest = { if (!claiming) onDismiss() }) {
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.widthIn(max = 420.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("O que é isso?", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss, enabled = !claiming) {
                        Icon(Icons.Default.Close, contentDescription = translate("Fechar"))
                    }
                }
                RetroController(enabled = !claiming, onKey = ::input)
                if (claiming) LinearProgressIndicator(Modifier.fillMaxWidth())
                status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun RetroController(enabled: Boolean, onKey: (String) -> Unit) {
    val shell = RoundedCornerShape(45)
    val charcoal = Color(0xFF363441)
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1.83f)) {
        val width = maxWidth
        val height = maxHeight
        val keySize = width * .11f
        Box(Modifier.offset(y = height * .14f).fillMaxWidth().height(height * .86f).background(
            Brush.verticalGradient(listOf(Color(0xFFF1F0F4), Color(0xFFADABB9))), shell))
        Box(Modifier.offset(width * .65f, height * .23f).size(width * .30f, height * .62f)
            .background(Color(0xFF9995A8), CircleShape))
        val padX = width * .16f
        val padY = height * .50f
        val pad = RoundedCornerShape(4.dp)
        Box(Modifier.offset(padX - keySize / 2, padY - keySize * 1.5f)
            .size(keySize, keySize * 3).background(charcoal, pad))
        Box(Modifier.offset(padX - keySize * 1.5f, padY - keySize / 2)
            .size(keySize * 3, keySize).background(charcoal, pad))
        fun direction(x: Int, y: Int) = Modifier
            .offset(padX + keySize * (x - .5f), padY + keySize * (y - .5f)).size(keySize)
        ControllerKey("UP", "▲", direction(0, -1), charcoal, "Cima", enabled, onKey)
        ControllerKey("DOWN", "▼", direction(0, 1), charcoal, "Baixo", enabled, onKey)
        ControllerKey("LEFT", "◀", direction(-1, 0), charcoal, "Esquerda", enabled, onKey)
        ControllerKey("RIGHT", "▶", direction(1, 0), charcoal, "Direita", enabled, onKey)

        val upperButton = Color(0xFFAD9BDC)
        val lowerButton = Color(0xFF554098)
        ControllerKey("X", "X", Modifier.offset(width * .75f, height * .25f).size(keySize), upperButton, "X", enabled, onKey)
        ControllerKey("Y", "Y", Modifier.offset(width * .65f, height * .42f).size(keySize), upperButton, "Y", enabled, onKey)
        ControllerKey("A", "A", Modifier.offset(width * .85f, height * .42f).size(keySize), lowerButton, "A", enabled, onKey)
        ControllerKey("B", "B", Modifier.offset(width * .75f, height * .59f).size(keySize), lowerButton, "B", enabled, onKey)

        val backButton = Color(0xFF635F71)
        ControllerKey("L", "L", Modifier.offset(width * .11f).size(width * .18f, height * .15f),
            backButton, "L", enabled, onKey)
        ControllerKey("R", "R", Modifier.offset(width * .71f).size(width * .18f, height * .15f),
            backButton, "R", enabled, onKey)
        ControllerKey("SELECT", "SELECT", Modifier.offset(width * .33f, height * .57f).size(width * .15f, height * .14f),
            backButton, "Select", enabled, onKey)
        ControllerKey("START", "START", Modifier.offset(width * .49f, height * .57f).size(width * .15f, height * .14f),
            backButton, "Start", enabled, onKey)
    }
}

@Composable
private fun ControllerKey(key: String, glyph: String, modifier: Modifier, color: Color, label: String,
                          enabled: Boolean, onKey: (String) -> Unit) {
    Box(modifier.clip(CircleShape).background(color)
        .semantics { contentDescription = label }
        .clickable(enabled = enabled, role = Role.Button) { onKey(key) }, contentAlignment = Alignment.Center) {
        Text(glyph, color = if (color == Color(0xFFAD9BDC)) Color(0xFF211333) else Color.White,
            fontSize = if (glyph.length > 4) 8.sp else 12.sp, maxLines = 1)
    }
}
