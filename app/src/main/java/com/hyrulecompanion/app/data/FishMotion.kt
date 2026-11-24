package com.hyrulecompanion.app.data

import kotlin.random.Random

internal class FishMotion(rarity: FishRarity, private val random: Random = Random.Default) {
    private val behavior = when (rarity) {
        FishRarity.COMMON -> Behavior(0.15f..0.19f, 2.6f..4.3f, 0f)
        FishRarity.UNCOMMON -> Behavior(0.27f..0.40f, 0.85f..1.55f, .65f)
        FishRarity.RARE -> Behavior(0.43f..0.63f, 0.42f..0.88f, .79f)
        FishRarity.LEGENDARY -> Behavior(0.69f..0.83f, 0.23f..0.43f, .90f)
    }

    var position = START_POSITION
        private set
    private var direction = if (random.nextBoolean()) 1f else -1f
    private var speed = sample(behavior.speed)
    private var turnIn = sample(behavior.interval)
    private var velocity = 0f

    fun advance(seconds: Float): Float {
        turnIn -= seconds
        if (turnIn <= 0f) {
            if (random.nextFloat() < behavior.reverseChance) direction = -direction
            speed = sample(behavior.speed)
            turnIn += sample(behavior.interval)
        }
        val blend = (seconds * 18f).coerceAtMost(1f)
        velocity += (direction * speed - velocity) * blend
        position += velocity * seconds
        if (position !in LEFT_EDGE..RIGHT_EDGE) {
            position = position.coerceIn(LEFT_EDGE, RIGHT_EDGE)
            direction = if (position == LEFT_EDGE) 1f else -1f
            velocity = 0f
        }
        return position
    }

    private fun sample(range: ClosedFloatingPointRange<Float>) =
        range.start + random.nextFloat() * (range.endInclusive - range.start)

    private data class Behavior(
        val speed: ClosedFloatingPointRange<Float>,
        val interval: ClosedFloatingPointRange<Float>,
        val reverseChance: Float
    )

    companion object {
        const val START_POSITION = .42f
        private const val LEFT_EDGE = .05f
        private const val RIGHT_EDGE = .95f
    }
}
