package com.hyrulecompanion.app.data

import com.hyrulecompanion.app.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

internal enum class Cosmetic(val id: String, val title: String, val price: Int, val description: String) {
    SWORD("sword", "Master Sword", 700, "Uma espada luminosa cravada na foto do perfil."),
    GOLD("gold", "Emblema da rúpia dourada", 10000, "Uma rúpia dourada que gira ao lado da foto."),
    HAT("hat", "Gorro do herói", 200, "O gorro verde dá à sua foto o visual do herói."),
    THEME("theme", "Tears of the Kingdom", 70, "Desbloqueia a paleta de jade e ouro nas Configurações."),
    BETA("beta", "Emblema Beta tester", 20, "Fica ao lado do nome. À venda até a chegada da versão 2.00."),
    CHEATER("chester", "Cheater", 0, "Emblema para o seu perfil."),
    CREATOR("creator", "Creator", 0, ""),
    FLAME("flame", "Efeito de chama", 500, "Um brilho de chama contorna seu nome."),
    SHADOW("shadow", "Efeito de escuridão", 350, "Uma névoa sombria envolve seu nome."),
    FISH_BADGE("fish_badge", "Peixe dourado", 0, "Emblema do Aquário de Hyrule."),
    RA_TROPHY("ra_trophy", "Troféu #1", 0, "Emblema de conquistas de Zelda.");

    val forSale: Boolean get() = price > 0

    val available: Boolean get() = this != BETA || BuildConfig.VERSION_CODE < 20000
}

internal val nameEffects = setOf(Cosmetic.FLAME, Cosmetic.SHADOW)

internal fun Set<Cosmetic>.withOneNameEffect(): Set<Cosmetic> =
    if (containsAll(nameEffects)) this - Cosmetic.SHADOW else this

internal const val TOTK_PALETTE = "totk"
internal const val BONUS_RUPEES = 10_000L
internal val bonusSequence = listOf("UP", "UP", "DOWN", "DOWN", "LEFT", "RIGHT", "LEFT", "RIGHT", "B", "A")

internal data class Wallet(
    val balance: Long = 0,
    val owned: Set<Cosmetic> = emptySet(),
    val equipped: Set<Cosmetic> = emptySet(),
    val betaAvailable: Boolean = true,
    val creator: Boolean = false,
    val fish: Set<String> = emptySet()
) {
    val profileCosmetics: Set<Cosmetic> get() = if (creator) equipped + Cosmetic.CREATOR else equipped

    fun json(): JSONObject = JSONObject().put("balance", balance)
        .put("owned", JSONArray(owned.map { it.id }))
        .put("equipped", JSONArray(equipped.map { it.id }))
        .put("beta_available", betaAvailable)
        .put("creator", creator).put("fish", JSONArray(fish.sorted()))

    companion object {
        fun from(json: JSONObject) = Wallet(json.optLong("balance"), cosmetics(json.optJSONArray("owned")),
            cosmetics(json.optJSONArray("equipped")).withOneNameEffect(), json.optBoolean("beta_available", true), json.optBoolean("creator"),
            json.optJSONArray("fish")?.let { a -> (0 until a.length()).map { a.optString(it) }.toSet() }.orEmpty())
    }
}

internal fun cosmetics(array: JSONArray?): Set<Cosmetic> = Cosmetic.entries.filterTo(mutableSetOf()) { item ->
    array != null && (0 until array.length()).any { array.optString(it) == item.id }
}

internal enum class FishRarity(val label: String, val weight: Int, val reward: IntRange, val species: List<String>) {
    COMMON("Comum", 695, 10..25, listOf("Hyrule Bass", "Staminoka Bass", "Chillfin Trout", "Mighty Porgy", "Armored Porgy")),
    UNCOMMON("Incomum", 280, 45..70, listOf("Sizzlefin Trout", "Voltfin Trout", "Mighty Carp", "Armored Carp", "Glowing Cave Fish")),
    RARE("Raro", 20, 60..90, listOf("Hearty Bass", "Hearty Salmon", "Stealthfin Trout", "Sanke Carp", "Ancient Arowana")),
    LEGENDARY("Lendário", 5, 250..500, listOf("Hylian Loach", "Ordon Catfish", "Reekfish"));

    companion object {
        fun roll(random: Random = Random.Default): FishRarity {
            var roll = random.nextInt(1000)
            return entries.first { rarity ->
                val hit = roll < rarity.weight
                roll -= rarity.weight
                hit
            }
        }
    }
}

internal data class CatchAttempt(val id: String, val rarity: FishRarity, val species: String, val waitMs: Long)
