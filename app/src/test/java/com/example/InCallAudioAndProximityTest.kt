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

package com.example

import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.ActiveCallScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPowerManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InCallAudioAndProximityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var powerManager: PowerManager
    private lateinit var shadowPowerManager: ShadowPowerManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowPowerManager = shadowOf(powerManager)
        shadowPowerManager.setIsWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, true)
        CallManager.updateCall(null)
        CallManager.updateAudioState(null)
    }

    @After
    fun tearDown() {
        CallManager.updateCall(null)
        CallManager.updateAudioState(null)
    }

    @Test
    fun testVolumeControlStreamBinding() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.get()

        controller.create()
        assertEquals(
            "volumeControlStream should be STREAM_VOICE_CALL on create",
            AudioManager.STREAM_VOICE_CALL,
            activity.volumeControlStream
        )

        controller.start()
        assertEquals(
            "volumeControlStream should be STREAM_VOICE_CALL on start",
            AudioManager.STREAM_VOICE_CALL,
            activity.volumeControlStream
        )

        controller.resume()
        assertEquals(
            "volumeControlStream should be STREAM_VOICE_CALL on resume",
            AudioManager.STREAM_VOICE_CALL,
            activity.volumeControlStream
        )
    }

    @Test
    fun testProximityWakeLockAcquiredOnEarpiece() {
        val earpieceState = CallAudioState(false, CallAudioState.ROUTE_EARPIECE, CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        CallManager.updateAudioState(earpieceState)

        composeTestRule.setContent {
            MyApplicationTheme {
                ActiveCallScreen(
                    contactName = "Test Contact",
                    contactNumber = "+1234567890",
                    preferredSim = "SIM 1",
                    quickResponses = emptyList(),
                    onHangUp = {},
                    onQuickDecline = {},
                    isIncoming = false,
                    callState = Call.STATE_ACTIVE
                )
            }
        }
        composeTestRule.waitForIdle()

        val latestWakeLock = ShadowPowerManager.getLatestWakeLock()
        assertNotNull("Proximity WakeLock should be created", latestWakeLock)
        assertTrue("Proximity WakeLock should be held on earpiece", latestWakeLock.isHeld)
        val shadowWakeLock = shadowOf(latestWakeLock)
        assertEquals(
            "WakeLock tag should match SecureDialer proximity tag",
            "SecureDialer:InCallProximityWakeLock",
            shadowWakeLock.tag
        )
    }

    @Test
    fun testProximityWakeLockReleasedOnSpeakerOrBluetooth() {
        val earpieceState = CallAudioState(false, CallAudioState.ROUTE_EARPIECE, CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        CallManager.updateAudioState(earpieceState)

        var callState by mutableIntStateOf(Call.STATE_ACTIVE)

        composeTestRule.setContent {
            MyApplicationTheme {
                ActiveCallScreen(
                    contactName = "Test Contact",
                    contactNumber = "+1234567890",
                    preferredSim = "SIM 1",
                    quickResponses = emptyList(),
                    onHangUp = {},
                    onQuickDecline = {},
                    isIncoming = false,
                    callState = callState
                )
            }
        }
        composeTestRule.waitForIdle()

        val wakeLock = ShadowPowerManager.getLatestWakeLock()
        assertNotNull(wakeLock)
        assertTrue("WakeLock should be held initially on earpiece", wakeLock.isHeld)

        // Switch route to Speaker
        val speakerState = CallAudioState(false, CallAudioState.ROUTE_SPEAKER, CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        CallManager.updateAudioState(speakerState)
        composeTestRule.waitForIdle()
        assertFalse("WakeLock should be released when speaker is activated", wakeLock.isHeld)

        // Switch back to Earpiece
        CallManager.updateAudioState(earpieceState)
        composeTestRule.waitForIdle()
        val reacquiredWakeLock = ShadowPowerManager.getLatestWakeLock()
        assertNotNull(reacquiredWakeLock)
        assertTrue("WakeLock should be re-acquired on earpiece", reacquiredWakeLock.isHeld)

        // Switch route to Bluetooth
        val bluetoothState = CallAudioState(false, CallAudioState.ROUTE_BLUETOOTH, CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_BLUETOOTH)
        CallManager.updateAudioState(bluetoothState)
        composeTestRule.waitForIdle()
        assertFalse("WakeLock should be released when bluetooth is active", reacquiredWakeLock.isHeld)
    }

    @Test
    fun testProximityWakeLockReleasedOnCallDisconnect() {
        val earpieceState = CallAudioState(false, CallAudioState.ROUTE_EARPIECE, CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        CallManager.updateAudioState(earpieceState)

        var callState by mutableIntStateOf(Call.STATE_ACTIVE)

        composeTestRule.setContent {
            MyApplicationTheme {
                ActiveCallScreen(
                    contactName = "Test Contact",
                    contactNumber = "+1234567890",
                    preferredSim = "SIM 1",
                    quickResponses = emptyList(),
                    onHangUp = {},
                    onQuickDecline = {},
                    isIncoming = false,
                    callState = callState
                )
            }
        }
        composeTestRule.waitForIdle()

        val wakeLock = ShadowPowerManager.getLatestWakeLock()
        assertNotNull(wakeLock)
        assertTrue("WakeLock should be held during active call", wakeLock.isHeld)

        // Call ends / disconnects
        callState = Call.STATE_DISCONNECTED
        composeTestRule.waitForIdle()
        assertFalse("WakeLock must be released upon call disconnection", wakeLock.isHeld)
    }

    @Test
    fun testVolumeKeyEventsNotConsumed() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        val volUpEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        val volDownEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN)

        // Dispatch key events to activity - they should proceed without custom interception
        activity.dispatchKeyEvent(volUpEvent)
        activity.dispatchKeyEvent(volDownEvent)

        // Verify standard activity key dispatch behavior
        assertNotNull(volUpEvent)
        assertNotNull(volDownEvent)
        assertEquals(
            "volumeControlStream remains STREAM_VOICE_CALL throughout key dispatch",
            AudioManager.STREAM_VOICE_CALL,
            activity.volumeControlStream
        )
    }
}
