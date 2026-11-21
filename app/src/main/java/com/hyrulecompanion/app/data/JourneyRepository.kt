package com.hyrulecompanion.app.data

import com.hyrulecompanion.app.BuildConfig
import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

const val K_PROGRESS = "hc-progress"
const val K_GUIDE_PROGRESS = "hc-guide-progress"
const val K_GUIDE_COMPLETION = "hc-guide-completion"
const val K_GUIDE_FAVORITES = "hc-guide-favorites"
const val K_GUIDE_NOTES = "hc-guide-notes"
const val K_DUNGEON_PROGRESS = "hc-dungeon-progress"
const val K_DUNGEON_ITEMS = "hc-dungeon-item-progress"
const val K_ATLAS_VISITED = "hc-atlas-visited"
const val K_PREFS = "hc-user-preferences"
const val K_SESSION = "hc-auth-session"
const val K_LAST_CLOUD_SYNC = "hc-last-cloud-sync"

val SYNC_KEYS = listOf(K_PROGRESS, K_GUIDE_PROGRESS, K_GUIDE_COMPLETION, K_GUIDE_FAVORITES, K_GUIDE_NOTES, K_DUNGEON_PROGRESS, K_DUNGEON_ITEMS, K_ATLAS_VISITED, K_PREFS)
val GAME_STATUSES = listOf("Não iniciado", "Em andamento", "Concluído", "100%")

data class GameProgress(val status: String = "Não iniciado", val startDate: String = "", val finishDate: String = "", val notes: String = "")
data class Preferences(val theme: String = "system", val avatar: String = "", val introSkip: Boolean = false, val introMusic: Boolean = true,
    val hideSpoilers: Boolean = true, val reduceMotion: Boolean = false,
    val startScreen: String = "home", val keepAwake: Boolean = false)

class JourneyRepository(private val context: Context, val catalog: Catalog) {
    // Only app-private storage; legacy localStorage uses the same literal keys, but is a separate origin.
    private val prefs = context.getSharedPreferences("hyrule-local-data-v13", Context.MODE_PRIVATE)
    fun raw(key: String): String? = prefs.getString(key, null)
    fun storeRaw(key: String, value: String) { prefs.edit().putString(key,value).apply() }
    fun erase(key: String) { prefs.edit().remove(key).apply() }
    fun obj(key: String): JSONObject = runCatching { JSONObject(raw(key) ?: "{}") }.getOrElse { JSONObject() }
    fun arr(key: String): JSONArray = runCatching { JSONArray(raw(key) ?: "[]") }.getOrElse { JSONArray() }
    fun store(key: String, value: JSONObject) = storeRaw(key, value.toString())
    fun store(key: String, value: JSONArray) = storeRaw(key, value.toString())

    fun gameProgress(id: String): GameProgress {
        val saved = obj(K_PROGRESS).opt(id)
        return when (saved) {
            is String -> GameProgress(status = saved.takeIf { it in GAME_STATUSES } ?: "Não iniciado")
            is JSONObject -> GameProgress(saved.optString("status", "Não iniciado").takeIf { it in GAME_STATUSES } ?: "Não iniciado", saved.optString("startDate"), saved.optString("finishDate"), saved.optString("notes"))
            else -> GameProgress()
        }
    }
    fun saveGame(id: String, record: GameProgress) {
        require(catalog.game(id) != null)
        val data = obj(K_PROGRESS)
        val start = if (record.status != "Não iniciado" && record.startDate.isBlank()) LocalDate.now().toString() else record.startDate
        val finish = if (record.status in listOf("Concluído", "100%") && record.finishDate.isBlank()) LocalDate.now().toString() else record.finishDate
        val prior = data.optJSONObject(id)
        val completedAt = if (record.status in listOf("Concluído", "100%")) {
            prior?.optLong("completedAt")?.takeIf { it > 0 } ?: System.currentTimeMillis()
        } else 0L
        data.put(id, JSONObject().put("completedAt", completedAt).put("status", record.status.takeIf { it in GAME_STATUSES } ?: "Não iniciado").put("startDate", start).put("finishDate", finish).put("notes", record.notes.take(5000)))
        store(K_PROGRESS, data)
    }
    fun doneCount() = catalog.games.count { gameProgress(it.id).status in listOf("Concluído", "100%") }
    fun playingCount() = catalog.games.count { gameProgress(it.id).status == "Em andamento" }
    fun completedGames() = catalog.games.filter { gameProgress(it.id).status in listOf("Concluído", "100%") }

    fun latestCompletedGame(): Game? {
        val records = obj(K_PROGRESS)
        return completedGames().mapNotNull { game ->
            val date = runCatching { LocalDate.parse(gameProgress(game.id).finishDate) }.getOrNull()
            date?.let { Triple(game, it, records.optJSONObject(game.id)?.optLong("completedAt") ?: 0L) }
        }.maxWithOrNull(compareBy<Triple<Game, LocalDate, Long>> { it.second }.thenBy { it.third })?.first
    }

    fun checked(key: String, id: String): Set<Int> {
        val data = obj(key).optJSONArray(id) ?: JSONArray()
        return (0 until data.length()).mapNotNull { data.optInt(it,-1).takeIf { v -> v >= 0 } }.toSet()
    }
    fun toggleChecked(key: String, id: String, index: Int) {
        val map = obj(key)
        val now = checked(key,id).toMutableSet()
        if (!now.add(index)) now.remove(index)
        val values = JSONArray()
        now.sorted().forEach { values.put(it) }
        map.put(id,values)
        store(key,map)
    }
    fun clearChecked(key: String, id: String) { val map = obj(key); map.remove(id); store(key,map) }
    fun isFavorite(id: String) = arr(K_GUIDE_FAVORITES).let { a -> (0 until a.length()).any { a.optString(it) == id } }
    fun toggleFavorite(id: String) {
        val old = arr(K_GUIDE_FAVORITES).let { a -> (0 until a.length()).map { a.optString(it) }.toMutableSet() }
        if (!old.add(id)) old.remove(id)
        store(K_GUIDE_FAVORITES, JSONArray(old.sorted()))
    }
    fun guideNote(id: String) = obj(K_GUIDE_NOTES).optString(id, "")
    fun saveGuideNote(id: String, note: String) { val data=obj(K_GUIDE_NOTES);data.put(id,note.take(10000));store(K_GUIDE_NOTES,data) }
    fun preferences(): Preferences {
        val o=obj(K_PREFS)
        return Preferences(o.optString("theme","system"),o.optString("avatar",""),o.optBoolean("introSkip",false),o.optBoolean("introMusic",true),
            o.optBoolean("hideSpoilers",true), o.optBoolean("reduceMotion",false),
            o.optString("startScreen","home"), o.optBoolean("keepAwake",false))
    }
    fun savePreferences(value: Preferences) { val o = obj(K_PREFS); o.put("theme",value.theme);o.put("avatar",value.avatar);o.put("introSkip",value.introSkip);o.put("introMusic",value.introMusic);o.put("hideSpoilers",value.hideSpoilers)
        o.put("reduceMotion",value.reduceMotion)
        o.put("startScreen",value.startScreen)
        o.put("keepAwake",value.keepAwake)
        store(K_PREFS,o)
    }
    fun avatarImage(uri: Uri): String {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        require(mime in listOf("image/png", "image/jpeg", "image/webp", "image/gif")) { "Use uma imagem PNG, JPG, WebP ou GIF." }
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val stream = java.io.ByteArrayOutputStream()
            val buf=ByteArray(8192)
            while (true) {
                val read = input.read(buf)
                if (read == -1) break
                stream.write(buf,0,read)
                if (stream.size()>1_800_000) error("A imagem deve ter no máximo 1,8 MB.")
            }
            stream.toByteArray()
        } ?: error("Não foi possível abrir a imagem.")
        return "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
    fun snapshot(): JSONObject {
        val data=JSONObject()
        SYNC_KEYS.forEach { k -> raw(k)?.let { data.put(k,it) } }
        return JSONObject().put("project","Hyrule Companion").put("schemaVersion",13).put("appVersion",BuildConfig.VERSION_NAME).put("savedAt",OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).put("data",data)
    }
    fun applySnapshot(snapshot: JSONObject) {
        val data=snapshot.optJSONObject("data") ?: error("Os dados na nuvem não possuem o formato esperado.")
        val editor=prefs.edit()
        SYNC_KEYS.forEach { key -> data.optString(key,"").takeIf { it.isNotEmpty() }?.let { value ->
            runCatching { if (key == K_GUIDE_FAVORITES) JSONArray(value) else JSONObject(value) }.getOrElse { error("Dados inválidos em $key") }
            editor.putString(key,value)
        } }
        editor.apply()
    }
    fun exportBackup(): String {
        val j=JSONObject()
        j.put("project","Hyrule Companion").put("version","1.15").put("appVersion",BuildConfig.VERSION_NAME)
            .put("schemaVersion",13).put("exportedAt",OffsetDateTime.now().toString())
        val mapping=mapOf("progress" to K_PROGRESS,"guideProgress" to K_GUIDE_PROGRESS,"guideCompletion" to K_GUIDE_COMPLETION,"guideFavorites" to K_GUIDE_FAVORITES,"guideNotes" to K_GUIDE_NOTES,"dungeonProgress" to K_DUNGEON_PROGRESS,"dungeonItemProgress" to K_DUNGEON_ITEMS,"atlasVisited" to K_ATLAS_VISITED,"preferences" to K_PREFS)
        mapping.forEach { (out,storage) -> j.put(out, if (storage == K_GUIDE_FAVORITES) arr(storage) else obj(storage)) }
        return j.toString(2)
    }
    fun importBackup(text: String) {
        val j=JSONObject(text)
        if (j.optJSONObject("data") != null && j.optInt("schemaVersion") == 13) {
            applySnapshot(j)
            return
        }
        if (j.optJSONObject("progress") == null && j.optJSONObject("guideProgress") == null && j.optJSONArray("guideFavorites") == null) error("Arquivo não reconhecido como backup do Hyrule Companion.")
        val progress=JSONObject()
        j.optJSONObject("progress")?.let { old -> old.keys().forEach { id ->
            if (catalog.game(id) == null) return@forEach
            val entry=old.opt(id)
            val prev=if(entry is JSONObject) entry else JSONObject().put("status",entry?.toString() ?: "Não iniciado")
            val status=prev.optString("status", "Não iniciado").takeIf { it in GAME_STATUSES } ?: "Não iniciado"
            progress.put(id, JSONObject().put("completedAt",prev.optLong("completedAt")).put("status",status).put("startDate",prev.optString("startDate")).put("finishDate",prev.optString("finishDate")).put("notes",prev.optString("notes").take(5000)))
        } }
        val guideIds = catalog.guides.associateBy { it.id }
        fun cleanSteps(source: JSONObject?, sizes: Map<String,Int>): JSONObject {
            val clean=JSONObject()
            source?.keys()?.forEach { id ->
                val limit=sizes[id] ?: return@forEach
                val arr=source.optJSONArray(id) ?: return@forEach
                val filtered=(0 until arr.length()).map { arr.optInt(it,-1) }.filter { it in 0 until limit }.distinct().sorted()
                clean.put(id,JSONArray(filtered))
            }
            return clean
        }
        val editor=prefs.edit()
        editor.putString(K_PROGRESS,progress.toString())
        editor.putString(K_GUIDE_PROGRESS,cleanSteps(j.optJSONObject("guideProgress"),guideIds.mapValues { it.value.steps.size }).toString())
        editor.putString(K_GUIDE_COMPLETION,cleanSteps(j.optJSONObject("guideCompletion"),guideIds.mapValues { it.value.completion.size }).toString())
        val favorites=JSONArray()
        j.optJSONArray("guideFavorites")?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it in guideIds }.distinct().forEach { favorites.put(it) } }
        editor.putString(K_GUIDE_FAVORITES,favorites.toString())
        val notes=JSONObject()
        j.optJSONObject("guideNotes")?.let { old -> old.keys().forEach { id -> if (id in guideIds) notes.put(id,old.optString(id).take(10000)) } }
        editor.putString(K_GUIDE_NOTES,notes.toString())
        val dungeons=catalog.dungeons.associateBy { it.id }
        editor.putString(K_DUNGEON_PROGRESS,cleanSteps(j.optJSONObject("dungeonProgress"),dungeons.mapValues { it.value.rooms.size+it.value.checklist.size }).toString())
        editor.putString(K_DUNGEON_ITEMS,cleanSteps(j.optJSONObject("dungeonItemProgress"),dungeons.mapValues { it.value.items.size }).toString())
        val atlasSizes=mutableMapOf<String,Int>()
        catalog.atlasMaps.forEach { m -> if(m.id == "totk") catalog.totkLocations.forEach { (layer, list) -> atlasSizes["totk-$layer"]=list.size } else atlasSizes[m.id]=m.locations.size }
        editor.putString(K_ATLAS_VISITED,cleanSteps(j.optJSONObject("atlasVisited"),atlasSizes).toString())
        if (j.optJSONObject("preferences") != null) editor.putString(K_PREFS,j.getJSONObject("preferences").toString())
        editor.apply()
    }
    fun lastCloudSync() = raw(K_LAST_CLOUD_SYNC) ?: ""
    fun saveLastCloudSync(at: String) = storeRaw(K_LAST_CLOUD_SYNC,at)
}
