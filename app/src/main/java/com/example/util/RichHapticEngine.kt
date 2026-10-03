/*
 * Copyright (C) 2026 MovStore
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Rich, low-latency tactile haptic feedback engine for physical keypad response,
 * button presses, and call alerts. Uses Android 12+ VibratorManager with fallback
 * for ERM motors on budget hardware.
 */
object RichHapticEngine {

    enum class HapticStyle {
        KEY_TICK,         // Lightweight key tap (dialpad)
        CLICK,            // Standard button click
        HEAVY_CLICK,      // Long press / key action
        SUCCESS,          // Positive action confirmation
        WARNING,          // Rejection / Call end
        DOUBLE_TICK,      // Notification or state change
        RECORDING_START,  // Crisp, tactile confirmation pulse when recording begins
        RECORDING_STOP    // Soft release tap when recording stops
    }

    @Volatile
    private var cachedVibrator: Vibrator? = null

    fun performHaptic(context: Context, style: HapticStyle) {
        try {
            val vibrator = getCachedVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val predefinedId = when (style) {
                    HapticStyle.KEY_TICK -> VibrationEffect.EFFECT_TICK
                    HapticStyle.CLICK -> VibrationEffect.EFFECT_CLICK
                    HapticStyle.HEAVY_CLICK -> VibrationEffect.EFFECT_HEAVY_CLICK
                    HapticStyle.SUCCESS -> VibrationEffect.EFFECT_CLICK
                    HapticStyle.WARNING -> VibrationEffect.EFFECT_DOUBLE_CLICK
                    HapticStyle.DOUBLE_TICK -> VibrationEffect.EFFECT_DOUBLE_CLICK
                    HapticStyle.RECORDING_START -> VibrationEffect.EFFECT_HEAVY_CLICK
                    HapticStyle.RECORDING_STOP -> VibrationEffect.EFFECT_TICK
                }

                try {
                    val effect = VibrationEffect.createPredefined(predefinedId)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val attrs = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
                        vibrator.vibrate(effect, attrs)
                    } else {
                        vibrator.vibrate(effect)
                    }
                } catch (_: Exception) {
                    // FIXED: Fallback to OneShot for budget devices with ERM motors that reject predefined effects
                    val durationMs = getFallbackDuration(style)
                    val amplitude = if (style == HapticStyle.KEY_TICK || style == HapticStyle.RECORDING_STOP) 60 else 180
                    val fallbackEffect = VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255))
                    vibrator.vibrate(fallbackEffect)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val durationMs = getFallbackDuration(style)
                val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(getFallbackDuration(style))
            }
        } catch (_: Exception) {}
    }

    private fun getFallbackDuration(style: HapticStyle): Long {
        return when (style) {
            HapticStyle.KEY_TICK -> 10L
            HapticStyle.CLICK -> 18L
            HapticStyle.HEAVY_CLICK -> 35L
            HapticStyle.SUCCESS -> 25L
            HapticStyle.WARNING -> 50L
            HapticStyle.DOUBLE_TICK -> 20L
            HapticStyle.RECORDING_START -> 30L
            HapticStyle.RECORDING_STOP -> 12L
        }
    }

    private fun getCachedVibrator(context: Context): Vibrator? {
        return cachedVibrator ?: synchronized(this) {
            cachedVibrator ?: run {
                val appContext = context.applicationContext
                val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                cachedVibrator = v
                v
            }
        }
    }
}