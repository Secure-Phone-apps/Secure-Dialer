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
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Rich, low-latency tactile haptic feedback engine for physical keypad response,
 * button presses, and call alerts. Uses Android 12+ VibratorManager with backward compatibility.
 */
object RichHapticEngine {

    enum class HapticStyle {
        KEY_TICK,         // Lightweight key tap (dialpad)
        CLICK,            // Standard button click
        HEAVY_CLICK,      // Long press / key action
        SUCCESS,          // Positive action confirmation
        WARNING,          // Rejection / Call end
        DOUBLE_TICK        // Notification or state change
    }

    fun performHaptic(context: Context, style: HapticStyle) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = when (style) {
                    HapticStyle.KEY_TICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    HapticStyle.CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    HapticStyle.HEAVY_CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    HapticStyle.SUCCESS -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    HapticStyle.WARNING -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                    HapticStyle.DOUBLE_TICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val durationMs = when (style) {
                    HapticStyle.KEY_TICK -> 10L
                    HapticStyle.CLICK -> 20L
                    HapticStyle.HEAVY_CLICK -> 40L
                    HapticStyle.SUCCESS -> 30L
                    HapticStyle.WARNING -> 60L
                    HapticStyle.DOUBLE_TICK -> 25L
                }
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
