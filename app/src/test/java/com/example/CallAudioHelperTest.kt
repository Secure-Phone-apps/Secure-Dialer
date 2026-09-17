package com.example

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import com.example.util.CallAudioHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallAudioHelperTest {

    private lateinit var context: Application
    private lateinit var audioManager: AudioManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        CallAudioHelper.reset()
    }

    @Test
    fun testPrepareSpeakerForRecordingSetsSweetSpotVolume() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
        val initialVolume = maxVolume / 2
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, initialVolume, 0)

        // Run preparation
        val result = CallAudioHelper.prepareSpeakerForRecording(
            context = context,
            inCallService = null,
            currentAudioState = null
        )

        assertTrue("CallAudioHelper preparation should succeed", result)
        assertTrue(CallAudioHelper.isAudioAdjusted)

        // Expected sweet spot: (maxVolume * 0.55).toInt()
        val expectedVolume = (maxVolume * 0.55).toInt().coerceIn(1, maxVolume)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        assertEquals("Volume should be tuned to 55% sweet spot", expectedVolume, currentVolume)

        // Restore audio state
        CallAudioHelper.restoreAudioState(context, null)
        assertFalse(CallAudioHelper.isAudioAdjusted)

        val restoredVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        assertEquals("Volume should be restored to initial level", initialVolume, restoredVolume)
    }

    @Test
    fun testRestoreWithoutAdjustIsSafeNoOp() {
        assertFalse(CallAudioHelper.isAudioAdjusted)
        // Calling restore without prior prepare should not crash or alter volume
        CallAudioHelper.restoreAudioState(context, null)
        assertFalse(CallAudioHelper.isAudioAdjusted)
    }
}
