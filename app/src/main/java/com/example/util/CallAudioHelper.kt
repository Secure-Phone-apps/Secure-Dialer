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
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log

/**
 * Manages acoustic balancing for call recording.
 *
 * Balances speakerphone output at ~55% of STREAM_VOICE_CALL max volume to avoid
 * chassis clipping and acoustic distortion while capturing both parties clearly.
 * Silently saves and restores the original volume index and audio route.
 */
object CallAudioHelper {
    private const val TAG = "CallAudioHelper"
    private const val SWEET_SPOT_RATIO = 0.55

    @Volatile
    private var originalVolume: Int? = null

    @Volatile
    private var originalRoute: Int? = null

    @Volatile
    private var didAdjustAudio: Boolean = false

    /**
     * Prepares speakerphone and volume level at the 55% sweet spot for recording.
     *
     * @param context Application or Service Context
     * @param inCallService Telecom InCallService instance (can be null in simulations)
     * @param currentAudioState Current CallAudioState
     * @return true if adjustments were applied
     */
    fun prepareSpeakerForRecording(
        context: Context,
        inCallService: InCallService?,
        currentAudioState: CallAudioState? = null
    ): Boolean {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return false

            // 1. Determine current route (from CallAudioState or fallback to AudioManager)
            val currentRoute = currentAudioState?.route ?: CallAudioState.ROUTE_EARPIECE
            originalRoute = currentRoute

            // 2. Save original voice call volume before modifying
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
            originalVolume = currentVol

            // 3. Switch route to Speakerphone if not already on speaker
            if (currentRoute != CallAudioState.ROUTE_SPEAKER) {
                inCallService?.setAudioRoute(CallAudioState.ROUTE_SPEAKER)
            }

            // 4. Calculate balanced 55% sweet spot
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            val balancedVolume = (maxVolume * SWEET_SPOT_RATIO).toInt().coerceIn(1, maxVolume)

            // 5. Set volume silently (flag 0 = no visible UI volume slider overlay)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, balancedVolume, 0)

            didAdjustAudio = true
            Log.d(TAG, "Prepared recording audio: vol=$balancedVolume/$maxVolume (was $currentVol), route=SPEAKER")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing speaker for recording", e)
            return false
        }
    }

    /**
     * Restores the user's previous volume and audio route when recording ends or call disconnects.
     *
     * @param context Application or Service Context
     * @param inCallService Telecom InCallService instance
     */
    fun restoreAudioState(context: Context, inCallService: InCallService?) {
        if (!didAdjustAudio) return
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

            // 1. Restore previous volume level silently
            originalVolume?.let { prevVol ->
                audioManager?.setStreamVolume(AudioManager.STREAM_VOICE_CALL, prevVol, 0)
                Log.d(TAG, "Restored volume: $prevVol")
            }

            // 2. Restore previous route if it was earpiece
            originalRoute?.let { prevRoute ->
                if (prevRoute == CallAudioState.ROUTE_EARPIECE) {
                    inCallService?.setAudioRoute(CallAudioState.ROUTE_EARPIECE)
                    Log.d(TAG, "Restored route: EARPIECE")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring audio state", e)
        } finally {
            originalVolume = null
            originalRoute = null
            didAdjustAudio = false
        }
    }

    /**
     * Reset state tracking without hardware side-effects.
     */
    fun reset() {
        originalVolume = null
        originalRoute = null
        didAdjustAudio = false
    }

    val isAudioAdjusted: Boolean
        get() = didAdjustAudio
}
