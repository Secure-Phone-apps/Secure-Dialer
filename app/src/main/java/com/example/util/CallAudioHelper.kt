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
import android.os.Build
import android.telecom.CallAudioState
import android.telecom.InCallService

/**
 * Manages acoustic balancing for call recording.
 *
 * Balances speakerphone output at ~55% of STREAM_VOICE_CALL max volume to avoid
 * chassis clipping and acoustic distortion while capturing both parties clearly.
 * Silently saves and restores the original volume index and audio route across Bluetooth,
 * Wired Headsets, and Earpiece.
 */
object CallAudioHelper {
    private const val SWEET_SPOT_RATIO = 0.55

    @Volatile
    private var originalVolume: Int? = null

    @Volatile
    private var originalRoute: Int? = null

    @Volatile
    private var didAdjustAudio: Boolean = false

    /**
     * Prepares speakerphone and volume level at the 55% sweet spot for recording.
     */
    @Synchronized
    fun prepareSpeakerForRecording(
        context: Context,
        inCallService: InCallService?,
        currentAudioState: CallAudioState? = null
    ): Boolean {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return false

            val currentRoute = currentAudioState?.route ?: CallAudioState.ROUTE_EARPIECE
            originalRoute = currentRoute

            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
            originalVolume = currentVol

            // Switch to Speakerphone if currently on earpiece
            if (currentRoute == CallAudioState.ROUTE_EARPIECE) {
                inCallService?.setAudioRoute(CallAudioState.ROUTE_SPEAKER)
            }

            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            val minVolume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try { audioManager.getStreamMinVolume(AudioManager.STREAM_VOICE_CALL) } catch (_: Exception) { 0 }
            } else {
                0
            }

            val balancedVolume = (maxVolume * SWEET_SPOT_RATIO).toInt().coerceIn(minVolume, maxVolume)

            try {
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, balancedVolume, 0)
            } catch (_: SecurityException) {
                // Ignore if restricted by system Zen/DND policy on certain OEM ROMs
            }

            didAdjustAudio = true
            return true
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Restores the user's previous volume and audio route (Bluetooth, Headset, or Earpiece).
     */
    @Synchronized
    fun restoreAudioState(context: Context, inCallService: InCallService?) {
        if (!didAdjustAudio) return
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

            originalVolume?.let { prevVol ->
                try {
                    audioManager?.setStreamVolume(AudioManager.STREAM_VOICE_CALL, prevVol, 0)
                } catch (_: SecurityException) {}
            }

            // FIXED: Fully restores ANY previous route (Bluetooth, Wired Headset, or Earpiece)
            originalRoute?.let { prevRoute ->
                if (prevRoute != CallAudioState.ROUTE_SPEAKER) {
                    inCallService?.setAudioRoute(prevRoute)
                }
            }
        } catch (_: Exception) {
        } finally {
            originalVolume = null
            originalRoute = null
            didAdjustAudio = false
        }
    }

    @Synchronized
    fun reset() {
        originalVolume = null
        originalRoute = null
        didAdjustAudio = false
    }

    val isAudioAdjusted: Boolean
        get() = didAdjustAudio
}