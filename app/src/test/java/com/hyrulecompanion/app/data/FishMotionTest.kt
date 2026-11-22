package com.hyrulecompanion.app.data

import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FishMotionTest {
    private val frame = 1f / 60

    @Test
    fun raritiesMoveFasterAndTurnMoreOften() {
        val metrics = FishRarity.entries.map { rarity ->
            var distance = 0f
            var turns = 0
            repeat(20) { seed ->
                val fish = FishMotion(rarity, Random(seed))
                assertEquals(FishMotion.START_POSITION, fish.position, 0f)
                var lastDirection = 0
                repeat(30 * 60) {
                    val previous = fish.position
                    val position = fish.advance(frame)
                    val delta = position - previous
                    assertTrue(position in 0.05f..0.95f)
                    distance += abs(delta)
                    if (abs(delta) > .0001f) {
                        val direction = if (delta > 0) 1 else -1
                        if (lastDirection != 0 && lastDirection != direction) turns++
                        lastDirection = direction
                    }
                }
            }
            distance to turns
        }
        for ((easier, harder) in metrics.zipWithNext()) {
            assertTrue(harder.first > easier.first * 1.25f)
            assertTrue(harder.second > easier.second * 1.35f)
        }
    }

    @Test
    fun fishRemainCatchableWithTheExistingBar() {
        for (rarity in FishRarity.entries) {
            repeat(15) { seed ->
                val fish = FishMotion(rarity, Random(seed))
                var bar = FishMotion.START_POSITION
                var progress = 0f
                var held = false
                var frames = 0
                while (progress < 1f && frames < 30 * 60) {
                    if (frames % 7 == 0) held = fish.position > bar
                    bar = (bar + (if (held) .57f else -.47f) * frame).coerceIn(.12f, .88f)
                    val overlap = abs(fish.advance(frame) - bar) < .12f
                    progress = (progress + (if (overlap) .17f else -.085f) * frame).coerceIn(0f, 1f)
                    frames++
                }
                assertEquals("Capture $rarity using seed $seed", 1f, progress, 0f)
                assertTrue("Keep the server's earliest claim time", frames * frame >= 5f)
            }
        }
    }
}
