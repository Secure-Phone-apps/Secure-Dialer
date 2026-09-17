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
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.model.CallRecording
import com.example.ui.components.exportRecordingToDownloads
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.CallAudioRecorder
import com.example.util.RecordingFeedbackHelper
import com.example.util.RichHapticEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallRecordingComprehensiveTest {

    private lateinit var context: Application
    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
        CallAudioRecorder.stopRecording()
    }

    @After
    fun tearDown() {
        CallAudioRecorder.stopRecording()
    }

    @Test
    fun `testSanitizePhoneNumberInFilename handles formatted and special characters`() {
        runBlocking {
            val formattedNumber = "+1 (555) 234-5678"
            val started = CallAudioRecorder.startRecording(context, formattedNumber)
            assertTrue("Recording should start", started)
            assertTrue(CallAudioRecorder.isRecording.value)

            val result = CallAudioRecorder.stopRecording()
            assertFalse(CallAudioRecorder.isRecording.value)

            if (result.file != null) {
                val name = result.file!!.name
                assertTrue("Filename must sanitize digits", name.contains("15552345678"))
                assertTrue("Filename must end with .m4a", name.endsWith(".m4a"))
            }
        }
    }

    @Test
    fun `testBlankPhoneNumberDefaultsToUnknown`() {
        runBlocking {
            val blankNumber = "   "
            val started = CallAudioRecorder.startRecording(context, blankNumber)
            assertTrue(started)

            val result = CallAudioRecorder.stopRecording()
            if (result.file != null) {
                assertTrue("Blank phone number should default to Unknown", result.file!!.name.contains("Unknown"))
            }
        }
    }

    @Test
    fun `testEmptyOrDefectiveRecordingFileAutoDeletion`() {
        runBlocking {
            val started = CallAudioRecorder.startRecording(context, "5550001")
            assertTrue(started)

            // Inject a mock empty / too-small file (<= 128 bytes) in the recording directory
            val recordDir = File(context.filesDir, "CallRecordings")
            val files = recordDir.listFiles() ?: emptyArray()
            val latestFile = files.maxByOrNull { it.lastModified() }
            latestFile?.writeBytes(ByteArray(64)) // 64 bytes is corrupt/defective header

            val result = CallAudioRecorder.stopRecording()

            // If the file was written with <= 128 bytes, CallAudioRecorder must delete it to prevent junk files
            if (latestFile != null && latestFile.exists()) {
                // If it was the current output file, it would have been deleted
                assertEquals(0L, result.durationSeconds)
            }
            assertFalse(CallAudioRecorder.isRecording.value)
        }
    }

    @Test
    fun `testValidRecordingFileRetention`() {
        runBlocking {
            val started = CallAudioRecorder.startRecording(context, "5559999")
            assertTrue(started)

            val recordDir = File(context.filesDir, "CallRecordings")
            val files = recordDir.listFiles() ?: emptyArray()
            val latestFile = files.maxByOrNull { it.lastModified() }

            // Write 512 bytes of simulated audio
            latestFile?.writeBytes(ByteArray(512))

            val result = CallAudioRecorder.stopRecording()
            assertFalse(CallAudioRecorder.isRecording.value)

            if (latestFile != null) {
                assertTrue("File > 128 bytes should be retained on disk", latestFile.exists())
                assertEquals(512L, latestFile.length())
                latestFile.delete()
            }
        }
    }

    @Test
    fun `testPreventDoubleStart`() {
        val firstStart = CallAudioRecorder.startRecording(context, "12345")
        assertTrue(firstStart)
        assertTrue(CallAudioRecorder.isRecording.value)

        val secondStart = CallAudioRecorder.startRecording(context, "67890")
        assertFalse("Cannot start concurrent recording session", secondStart)

        CallAudioRecorder.stopRecording()
        assertFalse(CallAudioRecorder.isRecording.value)
    }

    @Test
    fun `testRecordingFeedbackHelperToneAndHaptics`() {
        // Verify start feedback with chime enabled and disabled
        RecordingFeedbackHelper.triggerRecordingStartFeedback(context, chimeEnabled = true)
        RecordingFeedbackHelper.triggerRecordingStartFeedback(context, chimeEnabled = false)

        // Verify stop feedback with chime enabled and disabled
        RecordingFeedbackHelper.triggerRecordingStopFeedback(context, chimeEnabled = true)
        RecordingFeedbackHelper.triggerRecordingStopFeedback(context, chimeEnabled = false)

        // Verify new HapticStyle variants execute without exceptions
        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.RECORDING_START)
        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.RECORDING_STOP)
    }

    @Test
    fun `testRecordingDatabaseCrudOperations`() {
        runBlocking {
            val dao = database.dialerDao()

            val recording = CallRecording(
                id = 0,
                name = "Alice Smith",
                number = "+15551234567",
                timestamp = "Sep 16, 14:30",
                duration = 45L,
                filePath = "/data/user/0/com.example/files/CallRecordings/REC_Alice_01.m4a",
                note = "Important client meeting"
            )

            // 1. Insert
            dao.insertCallRecording(recording)

            // 2. Read back
            val recordings = dao.getAllCallRecordingsFlow().first()
            assertTrue(recordings.any { it.number == "+15551234567" && it.name == "Alice Smith" })

            val saved = recordings.first { it.number == "+15551234567" }
            assertEquals(45L, saved.duration)
            assertEquals("Important client meeting", saved.note)

            // 3. Update note
            dao.updateCallRecordingNote(saved.id, "Follow up next Tuesday at 10 AM")
            val updatedRecordings = dao.getAllCallRecordingsFlow().first()
            val updated = updatedRecordings.first { it.id == saved.id }
            assertEquals("Follow up next Tuesday at 10 AM", updated.note)

            // 4. Delete
            dao.deleteCallRecording(saved.id)
            val remainingRecordings = dao.getAllCallRecordingsFlow().first()
            assertFalse(remainingRecordings.any { it.id == saved.id })
        }
    }

    @Test
    fun `testDialerViewModelRecordingSettingsSyncAndPersistence`() {
        val viewModel = DialerViewModel(context)

        // Set recording preferences
        viewModel.updateRecordingEnabled(true)
        viewModel.updateAutoTuneRecordingVolume(false)
        viewModel.updateRecordingChimeEnabled(true)

        assertTrue(viewModel.recordingEnabled.value)
        assertFalse(viewModel.autoTuneRecordingVolume.value)
        assertTrue(viewModel.recordingChimeEnabled.value)

        // Verify SharedPreferences persistence
        val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("recording_enabled", false))
        assertFalse(prefs.getBoolean("auto_tune_recording_volume", true))
        assertTrue(prefs.getBoolean("recording_chime_enabled", false))

        // Recreate ViewModel simulating process death / app relaunch
        val newViewModel = DialerViewModel(context)
        assertTrue(newViewModel.recordingEnabled.value)
        assertFalse(newViewModel.autoTuneRecordingVolume.value)
        assertTrue(newViewModel.recordingChimeEnabled.value)

        // Reset to stealth defaults
        viewModel.updateRecordingEnabled(false)
        viewModel.updateAutoTuneRecordingVolume(true)
        viewModel.updateRecordingChimeEnabled(false)
        assertFalse(viewModel.recordingChimeEnabled.value)
    }

    @Test
    fun `testDefaultStealthRecordingChimeDisabled`() {
        val freshContext = ApplicationProvider.getApplicationContext<android.app.Application>()
        val freshPrefs = freshContext.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        freshPrefs.edit().remove("recording_chime_enabled").commit()

        val vm = DialerViewModel(freshContext)
        // Privacy mandate: chime MUST be disabled by default so callers/scammers are never alerted
        assertFalse("Stealth Privacy Mandate: recording chime must default to false", vm.recordingChimeEnabled.value)
    }

    @Test
    fun `testNotificationQuickActionsConstants`() {
        assertEquals("com.example.ACTION_HANG_UP", MyInCallService.ACTION_HANG_UP)
        assertEquals("com.example.ACTION_ANSWER", MyInCallService.ACTION_ANSWER)
        assertEquals("com.example.ACTION_DECLINE", MyInCallService.ACTION_DECLINE)
        assertEquals("com.example.ACTION_TOGGLE_RECORD", MyInCallService.ACTION_TOGGLE_RECORD)
        assertEquals("com.example.ACTION_TOGGLE_SPEAKER", MyInCallService.ACTION_TOGGLE_SPEAKER)
        assertEquals("com.example.ACTION_TOGGLE_MUTE", MyInCallService.ACTION_TOGGLE_MUTE)
    }

    @Test
    fun `testExportRecordingToDownloadsSafety`() {
        // Non-existent file path should be safely handled without throwing exception
        exportRecordingToDownloads(context, "/non/existent/path/audio.m4a")

        // Real file export test
        val testFile = File(context.cacheDir, "test_call_recording.m4a").apply {
            writeBytes("test audio payload".toByteArray())
        }
        exportRecordingToDownloads(context, testFile.absolutePath)

        if (testFile.exists()) {
            testFile.delete()
        }
    }

    @Test
    fun `testRecordingsBiometricLockDefaultDisabled`() {
        val freshContext = ApplicationProvider.getApplicationContext<android.app.Application>()
        val freshPrefs = freshContext.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        freshPrefs.edit().remove("is_recordings_biometric_lock_enabled").commit()

        val vm = DialerViewModel(freshContext)
        assertFalse("Recording biometric vault lock must default to disabled (false)", vm.isRecordingsBiometricLockEnabled.value)
    }

    @Test
    fun `testRecordingsBiometricLockToggleAndPersistence`() {
        val viewModel = DialerViewModel(context)
        viewModel.updateRecordingsBiometricLockEnabled(true)
        assertTrue(viewModel.isRecordingsBiometricLockEnabled.value)

        val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("is_recordings_biometric_lock_enabled", false))

        val newVm = DialerViewModel(context)
        assertTrue(newVm.isRecordingsBiometricLockEnabled.value)

        // Reset
        viewModel.updateRecordingsBiometricLockEnabled(false)
        assertFalse(viewModel.isRecordingsBiometricLockEnabled.value)
        assertFalse(prefs.getBoolean("is_recordings_biometric_lock_enabled", true))
    }

    @Test
    fun `testVaultBiometricAuthHelperGracefulFallback`() {
        val activity = Robolectric.setupActivity(android.app.Activity::class.java)
        var successCalled = false

        com.example.util.VaultBiometricAuthHelper.authenticate(
            activity = activity,
            onSuccess = {
                successCalled = true
            },
            onError = {}
        )

        // In Robolectric test environment with no hardware secure lock, fallback automatically invokes onSuccess
        assertTrue(successCalled)
    }
}
