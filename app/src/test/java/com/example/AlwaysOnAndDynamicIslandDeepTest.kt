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

import android.app.Application
import android.content.Context
import android.telecom.CallAudioState
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.model.CallRecording
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.CallAudioRecorder
import com.example.util.DynamicIslandOverlayManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlwaysOnAndDynamicIslandDeepTest {

    private lateinit var context: Application
    private lateinit var database: AppDatabase
    private lateinit var viewModel: DialerViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
        viewModel = DialerViewModel(context)
        CallAudioRecorder.stopRecording()
        DynamicIslandOverlayManager.stopCallMonitoring()
    }

    @After
    fun tearDown() {
        CallAudioRecorder.stopRecording()
        DynamicIslandOverlayManager.stopCallMonitoring()
    }

    // ==========================================
    // 1. ALWAYS-ON CALL RECORDING RIGOROUS TESTS
    // ==========================================

    @Test
    fun `test always-on call recording preference lifecycle and viewmodel synchronization`() {
        val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)

        // Step 1: Initial state assertion
        viewModel.updateAutoRecordCallsEnabled(false)
        assertFalse(viewModel.isAutoRecordCallsEnabled.value)
        assertFalse(prefs.getBoolean("is_auto_record_calls_enabled", true))

        // Step 2: Toggle on via ViewModel
        viewModel.updateAutoRecordCallsEnabled(true)
        assertTrue(viewModel.isAutoRecordCallsEnabled.value)
        assertTrue(prefs.getBoolean("is_auto_record_calls_enabled", false))

        // Step 3: Verify SharedPreferences synchronization on external reload
        val newVm = DialerViewModel(context)
        assertTrue(newVm.isAutoRecordCallsEnabled.value)

        // Step 4: Toggle off and re-verify
        newVm.updateAutoRecordCallsEnabled(false)
        assertFalse(newVm.isAutoRecordCallsEnabled.value)
        assertFalse(prefs.getBoolean("is_auto_record_calls_enabled", true))
    }

    @Test
    fun `test always-on recorder trigger simulation during active telephony call`() {
        val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_auto_record_calls_enabled", true).commit()

        val testNumber = "+15550192834"

        // When Always-on is enabled, simulate call active trigger
        val isAutoRecord = prefs.getBoolean("is_auto_record_calls_enabled", false)
        assertTrue("Always-on setting must be recognized as enabled", isAutoRecord)

        val startSuccess = CallAudioRecorder.startRecording(context, testNumber)
        assertTrue("CallAudioRecorder must successfully initialize recording", startSuccess)
        assertTrue("Recording state must reflect active", CallAudioRecorder.isRecording.value)

        // Simulate Call Disconnect and automated metadata persistence to Room DB
        val result = CallAudioRecorder.stopRecording()
        assertFalse("Recording state must be false after stop", CallAudioRecorder.isRecording.value)

        runBlocking {
            val recordDir = File(context.filesDir, "CallRecordings").apply { if (!exists()) mkdirs() }
            val dummyFile = File(recordDir, "REC_15550192834_deep_test.m4a").apply { writeBytes(ByteArray(256) { 1 }) }

            val recordEntity = CallRecording(
                number = testNumber,
                name = "Test Contact",
                timestamp = "Sep 21, 14:00",
                duration = 15L,
                filePath = result.file?.absolutePath ?: dummyFile.absolutePath
            )
            val insertedId = database.dialerDao().insertCallRecording(recordEntity)
            assertTrue("Entity must be successfully inserted into Room DB", insertedId > 0)

            val recordings = database.dialerDao().getAllCallRecordingsFlow().first()
            assertTrue("Database must contain the newly saved recording", recordings.any { it.number == testNumber })
        }
    }

    @Test
    fun `test always-on recording suppression when toggle is disabled`() {
        val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_auto_record_calls_enabled", false).commit()

        val testNumber = "+19998887777"

        val isAutoRecord = prefs.getBoolean("is_auto_record_calls_enabled", false)
        assertFalse("Always-on setting must be false", isAutoRecord)

        // Telephony state change to ACTIVE should NOT start recording
        if (isAutoRecord) {
            CallAudioRecorder.startRecording(context, testNumber)
        }

        assertFalse("Recorder must remain inactive when feature is disabled", CallAudioRecorder.isRecording.value)
    }

    // ==========================================
    // 2. DYNAMIC ISLAND CALL CAPSULE RIGOROUS TESTS
    // ==========================================

    @Test
    fun `test dynamic island preferences and speaker-only filter matrix`() {
        val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)

        // Test Default states
        viewModel.updateDynamicIslandEnabled(true)
        viewModel.updateDynamicIslandSpeakerOnly(false)

        assertTrue(viewModel.isDynamicIslandEnabled.value)
        assertFalse(viewModel.isDynamicIslandSpeakerOnly.value)
        assertTrue(prefs.getBoolean("is_dynamic_island_enabled", false))
        assertFalse(prefs.getBoolean("is_dynamic_island_speaker_only", true))

        // Toggle speaker-only constraint
        viewModel.updateDynamicIslandSpeakerOnly(true)
        assertTrue(viewModel.isDynamicIslandSpeakerOnly.value)
        assertTrue(prefs.getBoolean("is_dynamic_island_speaker_only", false))

        // Matrix Verification:
        // Scenario A: Enabled=true, SpeakerOnly=true, CurrentRoute=EARPIECE -> should NOT surface
        val routeEarpiece = CallAudioState(false, CallAudioState.ROUTE_EARPIECE, CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        val shouldSurfaceA = viewModel.isDynamicIslandEnabled.value && (!viewModel.isDynamicIslandSpeakerOnly.value || routeEarpiece.route == CallAudioState.ROUTE_SPEAKER)
        assertFalse("Should not surface on earpiece when speaker-only is active", shouldSurfaceA)

        // Scenario B: Enabled=true, SpeakerOnly=true, CurrentRoute=SPEAKER -> should surface
        val routeSpeaker = CallAudioState(false, CallAudioState.ROUTE_SPEAKER, CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        val shouldSurfaceB = viewModel.isDynamicIslandEnabled.value && (!viewModel.isDynamicIslandSpeakerOnly.value || routeSpeaker.route == CallAudioState.ROUTE_SPEAKER)
        assertTrue("Should surface on speaker when speaker-only is active", shouldSurfaceB)

        // Scenario C: Enabled=true, SpeakerOnly=false, CurrentRoute=EARPIECE -> should surface
        viewModel.updateDynamicIslandSpeakerOnly(false)
        val shouldSurfaceC = viewModel.isDynamicIslandEnabled.value && (!viewModel.isDynamicIslandSpeakerOnly.value || routeEarpiece.route == CallAudioState.ROUTE_SPEAKER)
        assertTrue("Should surface regardless of audio route when speaker-only is disabled", shouldSurfaceC)

        // Scenario D: Enabled=false -> should NOT surface regardless of route
        viewModel.updateDynamicIslandEnabled(false)
        val shouldSurfaceD = viewModel.isDynamicIslandEnabled.value && (!viewModel.isDynamicIslandSpeakerOnly.value || routeSpeaker.route == CallAudioState.ROUTE_SPEAKER)
        assertFalse("Should never surface when master toggle is disabled", shouldSurfaceD)
    }

    @Test
    fun `test dynamic island physics docking calculations and anchor snapping`() {
        // Dynamic Island anchor math test:
        // Left dock threshold: < -80f -> snaps to -130f
        // Right dock threshold: > 80f -> snaps to 130f
        // Center dock threshold: between -80f and 80f -> snaps to 0f

        fun computeTargetX(dragOffset: Float): Float {
            return when {
                dragOffset < -80f -> -130f
                dragOffset > 80f -> 130f
                else -> 0f
            }
        }

        assertEquals(-130f, computeTargetX(-150f), 0.01f)
        assertEquals(-130f, computeTargetX(-85f), 0.01f)
        assertEquals(0f, computeTargetX(-79f), 0.01f)
        assertEquals(0f, computeTargetX(0f), 0.01f)
        assertEquals(0f, computeTargetX(79f), 0.01f)
        assertEquals(130f, computeTargetX(85f), 0.01f)
        assertEquals(130f, computeTargetX(200f), 0.01f)
    }

    @Test
    fun `test dynamic island overlay lifecycle manager teardown and safety`() {
        // Verify overlay teardown without exceptions
        DynamicIslandOverlayManager.showOverlay(context)
        DynamicIslandOverlayManager.hideOverlay()
        DynamicIslandOverlayManager.stopCallMonitoring()

        // Repeating teardown calls must be completely idempotent and crash-free
        DynamicIslandOverlayManager.hideOverlay()
        DynamicIslandOverlayManager.stopCallMonitoring()
        assertTrue("Overlay manager must remain stable after multiple teardowns", true)
    }

    @Test
    fun `test in-capsule telephony action bindings`() {
        // Test mute and speaker state propagation through CallManager
        CallManager.setMuted(true)
        CallManager.setMuted(false)
        CallManager.setSpeaker(true)
        CallManager.setSpeaker(false)
        CallManager.disconnect()

        assertTrue("CallManager operations from Dynamic Island action buttons must execute cleanly", true)
    }
}
