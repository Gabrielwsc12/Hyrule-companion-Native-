package com.hyrulecompanion.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

internal class FishingHaptics(context: Context, enabled: Boolean) {
    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? = if (!enabled) null else {
        val primary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator else null
        primary?.takeIf { it.hasVibrator() }
            ?: context.getSystemService(Vibrator::class.java)?.takeIf { it.hasVibrator() }
    }
    private var nextPull = 0f
    private val gameAudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun hook() {
        nextPull = .28f
        vibrate(longArrayOf(0, 75, 32, 48), intArrayOf(0, 255, 0, 171))
    }

    fun pull(elapsed: Float, tension: Float) {
        if (vibrator == null || elapsed < nextPull || tension < .22f) return
        nextPull = elapsed + (.48f - .23f * tension)
        val strength = (88 + 129 * tension).toInt()
        if (tension > .62f) {
            vibrate(longArrayOf(0, 58, 24, 48), intArrayOf(0, strength, 0, strength * 2 / 3))
        } else {
            vibrate(longArrayOf(0, 50), intArrayOf(0, strength))
        }
    }

    fun escape() {
        vibrate(longArrayOf(0, 78, 41, 60, 46, 42), intArrayOf(0, 255, 0, 171, 0, 104))
    }

    fun stop() { vibrator?.cancel() }

    @Suppress("DEPRECATION")
    private fun vibrate(timings: LongArray, amplitudes: IntArray) {
        val engine = vibrator ?: return
        val effect = if (engine.hasAmplitudeControl()) VibrationEffect.createWaveform(timings, amplitudes, -1)
            else VibrationEffect.createWaveform(timings, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Fishing events are game effects, even when there is no finger on the screen.
            engine.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA))
        } else {
            engine.vibrate(effect, gameAudioAttributes)
        }
    }
}
