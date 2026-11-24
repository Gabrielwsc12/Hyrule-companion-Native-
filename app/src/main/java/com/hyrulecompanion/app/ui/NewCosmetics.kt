package com.hyrulecompanion.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hyrulecompanion.app.data.Cosmetic
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
private fun shimmerPhase(reduceMotion: Boolean): State<Float> {
    if (reduceMotion) return remember { mutableFloatStateOf(.2f) }
    return rememberInfiniteTransition(label = "cosmetic").animateFloat(0f, 1f,
        infiniteRepeatable(tween(2250, easing = LinearEasing)), label = "phase")
}

@Composable
internal fun StyledProfileName(name: String, equipped: Set<Cosmetic>, color: Color,
                               modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.titleLarge) {
    val glow = when {
        Cosmetic.FLAME in equipped -> Color(0xFFFF6B24)
        Cosmetic.SHADOW in equipped -> Color(0xFF6F40A2)
        else -> Color.Unspecified
    }
    val effectStyle = if (glow == Color.Unspecified) style else style.copy(
        shadow = Shadow(glow.copy(alpha = .9f), Offset.Zero, with(androidx.compose.ui.platform.LocalDensity.current) { 6.dp.toPx() })
    )
    androidx.compose.material3.Text(name, modifier, style = effectStyle,
        overflow = TextOverflow.Ellipsis, maxLines = 2, color = color)
}

@Composable
internal fun UnlockBadge(item: Cosmetic, reduceMotion: Boolean) {
    val phase = shimmerPhase(reduceMotion)
    Box(Modifier.size(32.dp, 38.dp), contentAlignment = Alignment.Center) {
        if (item == Cosmetic.FISH_BADGE) {
            AsyncImage("file:///android_asset/fishing/fish/gold-badge.webp", translate(item.title),
                Modifier.size(28.dp).graphicsLayer {
                    val angle = phase.value * 2 * PI
                    translationX = (cos(angle) * 3.dp.toPx()).toFloat()
                    translationY = (sin(angle) * 4.dp.toPx()).toFloat()
                    rotationZ = (sin(angle) * 18).toFloat()
                    scaleX = if (cos(angle) >= 0) 1f else -1f
                })
        } else {
            val gleam = sin(phase.value * PI).toFloat()
            Icon(Icons.Default.EmojiEvents, translate(item.title), Modifier.size(32.dp),
                tint = lerp(Color(0xFFE6AB31), Color(0xFFFFEBAB), gleam))
            Text("#1", Modifier.offset(y = (-4).dp), color = Color(0xFF55310F), fontWeight = FontWeight.Black, fontSize = 8.sp)
            Canvas(Modifier.matchParentSize()) {
                val center = Offset(size.width * (.28f + phase.value * .43f), size.height * .31f)
                drawCircle(Color.White.copy(alpha = gleam * .92f), 1.6.dp.toPx(), center)
            }
        }
    }
}

@Composable
internal fun AquariumIcon() {
    val ink = LocalContentColor.current
    val label = translate("Aquário de Hyrule")
    Canvas(Modifier.size(23.dp).semantics { contentDescription = label }) {
        drawRoundRect(ink, Offset(size.width * .08f, size.height * .18f),
            androidx.compose.ui.geometry.Size(size.width * .84f, size.height * .68f), CornerRadius(3.dp.toPx()),
            style = Stroke(1.7.dp.toPx()))
        drawLine(ink, Offset(size.width * .1f, size.height * .39f), Offset(size.width * .9f, size.height * .39f), 1.2.dp.toPx())
        drawOval(ink, Offset(size.width * .39f, size.height * .5f),
            androidx.compose.ui.geometry.Size(size.width * .31f, size.height * .20f))
        drawPath(Path().apply {
            moveTo(size.width * .44f, size.height * .6f)
            lineTo(size.width * .29f, size.height * .49f)
            lineTo(size.width * .29f, size.height * .72f)
            close()
        }, ink)
    }
}
