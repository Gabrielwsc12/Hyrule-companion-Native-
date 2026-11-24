package com.hyrulecompanion.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Path
import com.hyrulecompanion.app.HyruleViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyrulecompanion.app.data.TOTK_PALETTE

val Gold=Color(0xFFD5A552)
val DeepForest=Color(0xFF142A1D)
val Forest=Color(0xFF24513A)
val Parchment=Color(0xFFF4E5C5)
val Ink=Color(0xFF332B1A)
val MutedGold=Color(0xFF897348)
private val day = lightColorScheme(
    primary=Forest, onPrimary=Color.White,
    primaryContainer=Color(0xFFE7CF99), onPrimaryContainer=Ink,
    secondary=Color(0xFF89672E),onSecondary=Color.White,
    background=Color(0xFFF7F0DC),onBackground=Ink,
    surface=Color(0xFFFEF7E7),onSurface=Ink,
    surfaceVariant=Parchment,onSurfaceVariant=Ink,
    outline=Color(0xFFAA956D)
)
private val night=darkColorScheme(
    primary=Gold, onPrimary=Color(0xFF241A10),
    primaryContainer=Forest, onPrimaryContainer=Parchment,
    secondary=Gold,onSecondary=DeepForest,
    background=Color(0xFF121E18),onBackground=Parchment,
    surface=Color(0xFF1D3026),onSurface=Parchment,
    surfaceVariant=Color(0xFF2D4036),onSurfaceVariant=Parchment,
    outline=Color(0xFF9B855B)
)
internal val gamePalettes = listOf("classic" to "Clássico", "ocarina" to "Ocarina of Time",
    "majora" to "Majora’s Mask", "wind" to "The Wind Waker", "twilight" to "Twilight Princess")

@Composable fun HyruleTheme(dark:Boolean, palette:String, content:@Composable ()->Unit) {
    val base = if (dark) night else day
    val colors = when (palette) {
        "ocarina" -> listOf(0xFF284D70, 0xFFF3C870, 0xFFD7E5F7, 0xFF283749)
        "majora" -> listOf(0xFF62366D, 0xFFE7AFEF, 0xFFF0DDF3, 0xFF41294C)
        "wind" -> listOf(0xFF155F66, 0xFF84DCD3, 0xFFCBEDE8, 0xFF18454B)
        TOTK_PALETTE -> listOf(0xFF216051, 0xFF76E7BD, 0xFFD3F3DF, 0xFF1C3D33)
        "twilight" -> listOf(0xFF694437, 0xFFFFB89A, 0xFFFFE0CA, 0xFF503127)
        else -> emptyList()
    }
    val paletteScheme = if (colors.isEmpty()) base else {
        val (primary, highlight, light, shadow) = colors.map { Color(it) }
        base.copy(
            primary = if (dark) highlight else primary,
            onPrimary = if (dark) Color(0xFF161614) else Color.White,
            primaryContainer = if (dark) shadow else light,
            onPrimaryContainer = if (dark) Color.White else shadow,
            secondary = if (dark) highlight else shadow,
            onSecondary = if (dark) Color(0xFF161614) else Color.White,
            surfaceVariant = if (dark) shadow else light,
            onSurfaceVariant = if (dark) Color.White else shadow
        )
    }
    val scheme = if (palette == TOTK_PALETTE) paletteScheme.copy(
        secondary = if (dark) Color(0xFFFFD294) else Color(0xFF785014),
        background = if (dark) Color(0xFF0E2621) else Color(0xFFF2F4DB),
        surface = if (dark) Color(0xFF15342D) else Color(0xFFFCFAE8)
    ) else paletteScheme
    MaterialTheme(colorScheme=scheme,typography=Typography(
        headlineMedium=Typography().headlineMedium.copy(fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold),
        headlineSmall=Typography().headlineSmall.copy(fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold),
        titleLarge=Typography().titleLarge.copy(fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold)
    ),content=content)
}
@Composable fun ParchmentCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){
    Surface(modifier=modifier,shape=RoundedCornerShape(18.dp),tonalElevation=2.dp,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.45f))) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
    }
}
@Composable fun ScreenTitle(title:String,subtitle:String="",icon:String="✦"){
    Column(Modifier.fillMaxWidth().padding(bottom=8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text("$icon  ${translate(title)}",style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
        if(subtitle.isNotBlank())Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(color=MaterialTheme.colorScheme.outline.copy(alpha=.4f))
    }
}
@Composable fun StatsTile(value:String,title:String,modifier:Modifier=Modifier){
    ParchmentCard(modifier) {
        Text(value,style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
        Text(title,style=MaterialTheme.typography.labelLarge)
    }
}
@Composable fun StatusTag(text:String){
    val color=when(text){"100%","Concluído"->Color(0xFF377E56);"Em andamento"->Color(0xFFB07C22);else->MaterialTheme.colorScheme.secondary}
    Surface(shape=RoundedCornerShape(14.dp),color=color.copy(alpha=.14f)){
        Text(text,modifier=Modifier.padding(horizontal=10.dp,vertical=5.dp),fontSize=12.sp,color=color,fontWeight=FontWeight.SemiBold)
    }
}
@Composable fun SmallHeading(value:String){
    Text(value,style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
}

@Composable
internal fun TriforceBadge(vm: HyruleViewModel, gameId: String, animateNew: Boolean = false) {
    if (!vm.repo.earnedTriforce(gameId)) return
    val brightness = remember(gameId) { Animatable(1f) }
    val newlyEarned = animateNew && vm.hasNewTriforce(gameId)
    LaunchedEffect(gameId, newlyEarned) {
        if (newlyEarned) {
            if (!vm.repo.preferences().reduceMotion) {
                brightness.snapTo(.4f)
                repeat(2) {
                    brightness.animateTo(1f, tween(240))
                    brightness.animateTo(.55f, tween(220))
                }
                brightness.animateTo(1f, tween(220))
            }
            vm.consumeTriforce(gameId)
        }
    }
    TriforceEmblem(Modifier.alpha(brightness.value))
}

@Composable
internal fun TriforceEmblem(modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp).semantics { contentDescription = translate("Triforce conquistada") }) {
        val unit = size.width / 2
        val height = unit * .866f
        val y = (size.height - height * 2) / 2
        listOf(unit to y, unit / 2 to y + height, unit * 1.5f to y + height).forEach { (x, top) ->
            drawPath(Path().apply {
                moveTo(x, top)
                lineTo(x + unit / 2, top + height)
                lineTo(x - unit / 2, top + height)
                close()
            }, Gold)
        }
    }
}
