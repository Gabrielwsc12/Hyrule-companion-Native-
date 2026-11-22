package com.hyrulecompanion.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Game(val id: String, val name: String, val year: Int, val type: String, val icon: String, val platform: String, val description: String, val cover: String?)
data class TimelineRoute(val id: String, val title: String, val subtitle: String, val description: String, val entries: List<TimelineEntry>)
data class TimelineEntry(val name: String, val gameId: String, val era: String)
data class AtlasLocation(val name: String, val region: String, val description: String, val x: Float, val y: Float)
data class AtlasLayer(val key: String, val label: String, val locations: List<AtlasLocation>)
data class AtlasMap(val id: String, val gameId: String, val name: String, val year: Int, val branch: String, val branchLabel: String, val kind: String, val symbol: String, val subtitle: String, val description: String, val land: String, val locations: List<AtlasLocation>, val layers: List<AtlasLayer>)
data class GuideSection(val title: String, val items: List<String>)
data class Guide(
    val id: String, val gameId: String, val title: String, val category: String,
    val area: String, val difficulty: String, val summary: String, val steps: List<String>,
    val completion: List<String>, val sections: List<GuideSection>, val tip: String,
    val time: String, val reward: String, val needs: String, val scope: String
)
data class DungeonRoom(val title: String, val introduction: String, val tip: String, val reward: String)
data class DungeonDetails(val floor: String, val objective: String, val puzzle: String, val loot: String, val hazard: String)
data class DungeonItem(val name: String, val description: String)
data class Dungeon(
    val id: String, val gameId: String, val name: String, val region: String,
    val era: String, val difficulty: String, val summary: String, val item: String,
    val boss: String, val bossGuide: String, val map: List<String>,
    val rooms: List<DungeonRoom>, val checklist: List<String>,
    val items: List<DungeonItem>, val roomDetails: List<DungeonDetails>, val enemies: List<String>
)

private fun JSONArray.strings(): List<String> = (0 until length()).map { optString(it) }
private fun JSONObject.string(k: String) = optString(k, "")

class Catalog(context: Context) {
    private val raw = JSONObject(context.assets.open("hyrule_content.json").bufferedReader().use { it.readText() })
    private val covers = raw.getJSONObject("gameCovers")
    private val localCovers = context.assets.list("covers").orEmpty().toSet()

    val games: List<Game> = raw.getJSONArray("games").let { array ->
        (0 until array.length()).map { i -> array.getJSONObject(i).let { j ->
            Game(j.string("id"), j.string("name"), j.optInt("year"), j.string("type"), j.string("icon"), j.string("platform"), j.string("desc"), "${j.string("id")}.webp".let { file ->
                if (file in localCovers) "file:///android_asset/covers/$file"
                else covers.optString(j.string("id"), "").ifBlank { null }
            })
        } }
    }
    val routes: List<TimelineRoute> = raw.getJSONObject("timelineRoutes").let { j ->
        j.keys().asSequence().map { key ->
            val route = j.getJSONObject(key)
            TimelineRoute(key, route.string("title"), route.string("subtitle"), route.string("description"), route.getJSONArray("entries").let { arr ->
                (0 until arr.length()).map { i -> arr.getJSONArray(i).let { e -> TimelineEntry(e.optString(0), e.optString(1), e.optString(2)) } }
            })
        }.toList()
    }
    private fun readLocations(arr: JSONArray): List<AtlasLocation> = (0 until arr.length()).map { i ->
        arr.getJSONArray(i).let { l -> AtlasLocation(l.optString(0),l.optString(1),l.optString(2),l.optDouble(3,50.0).toFloat(),l.optDouble(4,50.0).toFloat()) }
    }
    val atlasMaps: List<AtlasMap> = raw.getJSONArray("atlasMaps").let { a -> (0 until a.length()).map { i -> a.getJSONObject(i).let { j ->
        AtlasMap(j.string("id"), j.string("gameId"),j.string("name"),j.optInt("year"),j.string("branch"),j.string("branchLabel"),j.string("kind"),j.string("symbol"),j.string("subtitle"),j.string("description"),j.string("land"),readLocations(j.getJSONArray("locations")), j.optJSONArray("layers")?.let { layers ->
            (0 until layers.length()).map { index ->
                val layer = layers.getJSONObject(index)
                AtlasLayer(layer.getString("key"), layer.getString("label"), readLocations(layer.getJSONArray("locations")))
            }
        }.orEmpty())
    } } }
    val totkLocations: Map<String, List<AtlasLocation>> = raw.getJSONObject("totkLayerLocations").let { j -> j.keys().asSequence().associateWith { readLocations(j.getJSONArray(it)) } }
    private fun asText(value: Any?): String = when (value) {
        null, JSONObject.NULL -> ""
        is JSONArray -> value.strings().joinToString(" · ")
        else -> value.toString()
    }
    val guides: List<Guide> = raw.getJSONArray("guideData").let { a -> (0 until a.length()).map { i -> a.getJSONObject(i).let { j ->
        val sections = j.optJSONArray("sections")?.let { s -> (0 until s.length()).map { ix -> s.getJSONObject(ix).let { x -> GuideSection(x.string("title"),x.optJSONArray("items")?.strings().orEmpty()) } } }.orEmpty()
        Guide(j.string("id"),j.string("game"),j.string("title"),j.string("category"),j.string("area"),j.string("difficulty"),j.string("summary"),j.optJSONArray("steps")?.strings().orEmpty(),j.optJSONArray("completion")?.strings().orEmpty(), sections,j.string("tip"),j.string("time"),j.string("reward"),asText(j.opt("needs")),asText(j.opt("scope")))
    } } }
    val dungeons: List<Dungeon> = raw.getJSONArray("dungeonData").let { a ->
        val references = raw.getJSONObject("dungeonReference")
        val enemies = raw.getJSONObject("dungeonEnemies")
        (0 until a.length()).map { i -> a.getJSONObject(i).let { j ->
            val ref = references.optJSONObject(j.string("id")) ?: JSONObject()
            Dungeon(j.string("id"),j.string("game"),j.string("name"),j.string("region"),j.string("era"),j.string("difficulty"),j.string("summary"),j.string("item"),j.string("boss"),j.string("bossGuide"),j.optJSONArray("map")?.strings().orEmpty(),j.getJSONArray("rooms").let { rs -> (0 until rs.length()).map { n -> rs.getJSONArray(n).let { r -> DungeonRoom(r.optString(0),r.optString(1),r.optString(2),r.optString(3)) } } },j.getJSONArray("checklist").strings(),ref.optJSONArray("items")?.let { items -> (0 until items.length()).map { ix -> items.getJSONArray(ix).let { item -> DungeonItem(item.optString(0),item.optString(1)) } } }.orEmpty(),ref.optJSONArray("rooms")?.let { rooms -> (0 until rooms.length()).map { ix -> rooms.getJSONObject(ix).let { r -> DungeonDetails(r.string("floor"),r.string("objective"),r.string("puzzle"),r.string("loot"),r.string("hazard")) } } }.orEmpty(),enemies.optJSONArray(j.string("id"))?.strings().orEmpty())
        } }
    }
    fun game(id: String) = games.find { it.id == id }
    fun mapForGame(id: String) = atlasMaps.find { it.gameId == id }
    fun atlasLayers(map: AtlasMap): List<AtlasLayer> = when {
        map.id == "totk" -> listOf("surface" to "Superfície", "sky" to "Céus", "depths" to "Profundezas").map { (key, label) ->
            AtlasLayer("totk-$key", label, totkLocations.getValue(key))
        }
        map.layers.any { it.key == map.id } -> map.layers
        else -> listOf(AtlasLayer(map.id, if (map.layers.isEmpty()) map.name else "Mapa geral", map.locations)) + map.layers
    }
}
