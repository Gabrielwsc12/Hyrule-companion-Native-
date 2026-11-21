package com.hyrulecompanion.app.ui

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
@Composable fun HyruleTheme(dark:Boolean, content:@Composable ()->Unit) {
    MaterialTheme(colorScheme=if(dark)night else day,typography=Typography(
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
        Text("$icon  $title",style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
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
