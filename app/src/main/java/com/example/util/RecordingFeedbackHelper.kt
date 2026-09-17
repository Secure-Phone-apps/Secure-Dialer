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
import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Provides tactile haptic feedback and optional subtle acoustic audio chimes
 * for start and stop events during phone call audio recording.
 */
object RecordingFeedbackHelper {

    private val feedbackScope = CoroutineScope(Dispatchers.Default)

    /**
     * Dispatches tactile haptics and an optional subtle prompt tone when call recording starts.
     */
    fun triggerRecordingStartFeedback(context: Context, chimeEnabled: Boolean) {
        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.RECORDING_START)
        if (chimeEnabled) {
            playTone(ToneGenerator.TONE_PROP_PROMPT, durationMs = 120, volume = 35)
        }
    }

    /**
     * Dispatches tactile haptics and an optional subtle beep tone when call recording stops.
     */
    fun triggerRecordingStopFeedback(context: Context, chimeEnabled: Boolean) {
        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.RECORDING_STOP)
        if (chimeEnabled) {
            playTone(ToneGenerator.TONE_PROP_BEEP, durationMs = 90, volume = 30)
        }
    }

    private fun playTone(toneType: Int, durationMs: Int, volume: Int) {
        feedbackScope.launch {
            var toneGen: ToneGenerator? = null
            try {
                toneGen = ToneGenerator(AudioManager.STREAM_VOICE_CALL, volume)
                toneGen.startTone(toneType, durationMs)
                delay(durationMs.toLong() + 30L)
            } catch (_: Exception) {
                // Audio hardware or focus temporarily unavailable; fail silently
            } finally {
                try {
                    toneGen?.release()
                } catch (_: Exception) {}
            }
        }
    }
}
