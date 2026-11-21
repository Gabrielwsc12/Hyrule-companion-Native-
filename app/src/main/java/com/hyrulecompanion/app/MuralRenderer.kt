package com.hyrulecompanion.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import com.hyrulecompanion.app.data.Catalog
import com.hyrulecompanion.app.data.JourneyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.min

/** Native Android Canvas JPEG renderer. Does not depend on the old HTML or a WebView. */
object MuralRenderer {
    suspend fun render(context: Context, repo: JourneyRepository, catalog: Catalog, options: MuralOptions, output: Uri) = withContext(Dispatchers.IO) {
        val portrait=options.layout=="retrato"
        val w=if(portrait)1080 else 1600
        val h=if(portrait)1600 else 1080
        val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        val dark=options.theme=="noite"
        val parchment=options.theme=="pergaminho"
        val bg=when{
            options.theme=="oceano" -> Color.parseColor("#10364F")
            options.theme=="vulcao" -> Color.parseColor("#3E201D")
            options.theme=="crepusculo" -> Color.parseColor("#2D203E")
            dark->Color.rgb(19,30,28);parchment->Color.rgb(234,216,169);else->Color.rgb(20,43,30)}
        val surface=when{
            options.theme=="oceano" -> Color.parseColor("#19506B")
            options.theme=="vulcao" -> Color.parseColor("#65362D")
            options.theme=="crepusculo" -> Color.parseColor("#4C355B")
            dark->Color.rgb(41,58,55);parchment->Color.rgb(249,239,204);else->Color.rgb(33,65,47)}
        val ink=if(parchment) Color.rgb(48,43,26) else Color.rgb(252,231,177)
        val muted=if(parchment) Color.rgb(90,76,51) else Color.rgb(204,204,172)
        val gold=Color.rgb(215,169,86)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        fun rect(x: Float,y: Float,width: Float,height: Float,color: Int,radius: Float=24f) {
            paint.color=color;paint.style=Paint.Style.FILL;canvas.drawRoundRect(RectF(x,y,x+width,y+height),radius,radius,paint)
        }
        fun txt(value: String,x: Float,y: Float,size: Float,color: Int,bold: Boolean=false) {
            paint.color=color;paint.style=Paint.Style.FILL;paint.textSize=size;paint.typeface=if(bold) android.graphics.Typeface.create("serif",android.graphics.Typeface.BOLD) else android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL)
            canvas.drawText(value,x,y,paint)
        }
        fun multiline(value:String,x:Float,y:Float,size:Float,color:Int,maxWidth:Float,maxLines:Int=3):Int {
            paint.textSize=size;paint.typeface=android.graphics.Typeface.DEFAULT
            val words=value.split(' ')
            var line="";var row=0
            for(word in words){
                val attempt=if(line.isBlank())word else "$line $word"
                if(paint.measureText(attempt)>maxWidth && line.isNotBlank()) { txt(line,x,y+(row*size*1.3f),size,color);row++;line=word;if(row>=maxLines)break }
                else line=attempt
            }
            if(row<maxLines && line.isNotBlank()){txt(line,x,y+(row*size*1.3f),size,color);row++}
            return row
        }
        canvas.drawColor(bg)
        paint.color=gold;paint.style=Paint.Style.STROKE;paint.strokeWidth=7f
        canvas.drawRoundRect(RectF(26f,26f,w-26f,h-26f),28f,28f,paint)
        paint.style=Paint.Style.FILL
        txt("▲  HYRULE COMPANION  ▲",66f,110f,38f,gold,true)
        multiline(options.name.ifBlank { "Herói de Hyrule" },66f,195f,65f,ink,w-132f,2)
        txt("MINHA JORNADA POR HYRULE",66f,278f,28f,muted)
        val started=catalog.games.count { repo.gameProgress(it.id).status!="Não iniciado" }
        val completed=repo.doneCount()
        val playing=repo.playingCount()
        val progress=(completed*100f/catalog.games.size).toInt()
        rect(66f,315f,w-132f,190f,surface)
        txt("${catalog.games.size} JOGOS",94f,385f,35f,gold,true)
        txt("$completed concluídos  •  $playing em andamento",94f,438f,31f,ink)
        rect(94f,464f,w-190f,15f,muted,10f)
        rect(94f,464f,(w-190f)*completed/catalog.games.size,15f,gold,10f)
        txt("$progress% da biblioteca",w-385f,391f,29f,ink,true)
        val completedGames = repo.completedGames()
        val previousAchievements = catalog.games.filter { it !in completedGames && repo.earnedTriforce(it.id) }
        val chosen = (completedGames + previousAchievements)
            .ifEmpty { catalog.games.filter { repo.gameProgress(it.id).status == "Em andamento" } }
            .take(if (portrait) 8 else 6)
        txt("✦ MEMÓRIAS DO HERÓI",66f,565f,40f,gold,true)
        val cols=if(portrait)2 else 3
        val rows=(chosen.size+cols-1)/cols
        val itemWidth=(w-132f-(cols-1)*18f)/cols
        val startY=606f
        val boxHeight=if(portrait)187f else 180f
        chosen.forEachIndexed { index,g ->
            val col=index%cols;val row=index/cols
            val x=66f+col*(itemWidth+18f);val y=startY+row*(boxHeight+16f)
            rect(x,y,itemWidth,boxHeight,surface,16f)
            if(options.showCovers && !g.cover.isNullOrBlank()) {
                runCatching {
                    val connection=URL(g.cover).openConnection().apply { connectTimeout=3000;readTimeout=3000 }
                    val cover=connection.getInputStream().use { BitmapFactory.decodeStream(it) }
                    if(cover!=null){
                        val iconH=125f;val iconW=108f
                        val destination=RectF(x+14f,y+16f,x+14f+iconW,y+16f+iconH)
                        canvas.drawBitmap(cover,null,destination,Paint(Paint.ANTI_ALIAS_FLAG));cover.recycle()
                        multiline(g.name,x+135f,y+48f,22f,ink,itemWidth-150f,3)
                    } else multiline(g.name,x+22f,y+64f,26f,ink,itemWidth-44f,2)
                }.onFailure { multiline(g.name,x+22f,y+64f,26f,ink,itemWidth-44f,2) }
            } else multiline(g.name,x+22f,y+64f,26f,ink,itemWidth-44f,2)
            val status = repo.gameProgress(g.id).status
            txt(status, x + 20f, y + boxHeight - 21f, 23f, gold, true)
            if (repo.earnedTriforce(g.id)) {
                val left = x + itemWidth - 48f
                val top = y + boxHeight - 43f
                paint.color = gold
                paint.style = Paint.Style.FILL
                listOf(14f to 0f, 7f to 12f, 21f to 12f).forEach { (dx, dy) ->
                    canvas.drawPath(android.graphics.Path().apply {
                        moveTo(left + dx, top + dy)
                        lineTo(left + dx - 7f, top + dy + 12f)
                        lineTo(left + dx + 7f, top + dy + 12f)
                        close()
                    }, paint)
                }
            }
        }
        if(chosen.isEmpty()) multiline("Sua aventura está apenas começando. Marque jogos como concluídos para registrar suas conquistas aqui.",80f,670f,32f,ink,w-160f,5)
        txt("EXPLORE • COMPLETE • CONQUISTE",66f,h-95f,28f,gold,true)
        context.contentResolver.openOutputStream(output)?.use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG,92,stream)) { "Não foi possível salvar a imagem" }
        } ?: error("Não foi possível criar o arquivo JPEG")
        bitmap.recycle()
    }
}
