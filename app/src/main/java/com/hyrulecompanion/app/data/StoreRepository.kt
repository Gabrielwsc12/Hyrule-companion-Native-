package com.hyrulecompanion.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hyrulecompanion.app.HyruleViewModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import kotlin.random.Random

internal class StoreRepository(private val vm: HyruleViewModel) {
    private val guestKey = "hc-guest-wallet"
    private val writes = Mutex()
    private var guest by mutableStateOf(Wallet.from(vm.repo.obj(guestKey)).copy(creator = false))
    private var cloud by mutableStateOf<Pair<String, Wallet>?>(null)
    private var lastGuestClaim: Pair<String, Int>? = null

    private fun cacheKey(userId: String) = "hc-store-cache-$userId"

    fun wallet(userId: String?): Wallet? {
        if (userId == null) return guest
        return cloud?.takeIf { it.first == userId }?.second
            ?: vm.repo.raw(cacheKey(userId))?.let { Wallet.from(vm.repo.obj(cacheKey(userId))) }
    }

    private fun saveGuest(value: Wallet) {
        vm.repo.store(guestKey, value.json())
        guest = value
    }

    suspend fun refresh(userId: String?) = writes.withLock {
        if (userId != null) refreshCloud(userId)
    }

    private suspend fun refreshCloud(userId: String) {
        saveCloud(userId, vm.communityRequest(userId) { storeState(it) })
    }

    private fun saveCloud(userId: String, state: Wallet) {
        if (vm.session?.userId == userId) {
            vm.repo.store(cacheKey(userId), state.json())
            cloud = userId to state
        }
    }

    suspend fun buy(userId: String?, item: Cosmetic) = writes.withLock {
        check(item.forSale) { "Item indisponível." }
        if (userId == null) {
            if (item in guest.owned) return@withLock
            check(item.available) { "Este item saiu de venda." }
            check(guest.balance >= item.price) { "Rúpias insuficientes." }
            saveGuest(guest.copy(balance = guest.balance - item.price, owned = guest.owned + item))
        } else {
            vm.communityRequest(userId) { storeBuy(it, item) }
            refreshCloud(userId)
        }
    }

    suspend fun equip(userId: String?, item: Cosmetic, equipped: Boolean) = writes.withLock {
        if (userId == null) {
            check(item != Cosmetic.THEME && item in guest.owned) { "Compre o cosmético antes de equipá-lo." }
            saveGuest(guest.copy(equipped = if (equipped) guest.equipped + item else guest.equipped - item))
        } else {
            vm.communityRequest(userId) { storeEquip(it, item, equipped) }
            refreshCloud(userId)
        }
    }

    suspend fun redeem(userId: String?, keys: List<String>): Boolean = writes.withLock {
        check(keys == bonusSequence)
        if (userId != null) {
            val (unlocked, updated) = vm.communityRequest(userId) { storeRedeem(it, keys) }
            saveCloud(userId, updated)
            unlocked
        } else {
            if (Cosmetic.CHESTER in guest.owned) return@withLock false
            saveGuest(guest.copy(balance = guest.balance + BONUS_RUPEES, owned = guest.owned + Cosmetic.CHESTER))
            true
        }
    }

    suspend fun cast(userId: String?, id: String): CatchAttempt {
        if (userId != null) return vm.communityRequest(userId) { storeCast(it, id) }
        val rarity = FishRarity.roll()
        return CatchAttempt(id, rarity, rarity.species.random(), Random.nextLong(2400, 4201))
    }

    suspend fun claim(userId: String?, attempt: CatchAttempt): Int = writes.withLock {
        if (userId != null) {
            val reward = vm.communityRequest(userId) { storeClaim(it, attempt.id) }
            if (vm.session?.userId == userId) {
                try {
                    refreshCloud(userId)
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // The receipt remains valid if the separate wallet request fails.
                }
            }
            reward
        } else {
            lastGuestClaim?.takeIf { it.first == attempt.id }?.second ?: run {
                val reward = attempt.rarity.reward.random()
                saveGuest(guest.copy(balance = guest.balance + reward))
                lastGuestClaim = attempt.id to reward
                reward
            }
        }
    }
}
