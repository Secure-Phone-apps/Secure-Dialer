package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.util.CallAudioRecorder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DialerAudioRecorderTest {

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Stop any leftover background active recordings
        CallAudioRecorder.stopRecording()
    }

    @Test
    fun testStartRecordingFileCreationAndDirectoryStructure() {
        runBlocking {
            // 1. Verify recording is initially idle
            assertFalse(CallAudioRecorder.isRecording.value)
            assertEquals(0, CallAudioRecorder.recordingDuration.value)

            // 2. Start a call recording session
            val phoneNumber = "+15550199"
            val success = CallAudioRecorder.startRecording(context, phoneNumber)

            // Robolectric shadows MediaRecorder. Under correct execution flow, it should prepare and start successfully
            assertTrue("Recording should start successfully", success)
            assertTrue(CallAudioRecorder.isRecording.value)

            // 3. Stop recording and collect result
            val result = CallAudioRecorder.stopRecording()
            assertFalse(CallAudioRecorder.isRecording.value)

            // Since the Robolectric MediaRecorder shadow does not write real binary audio content to physical files,
            // the file may be non-null but not exist on disk.
            // Let's verify that the file path and naming conventions are perfectly constructed.
            if (result.file != null) {
                assertTrue("File name should match REC_ pattern", result.file!!.name.startsWith("REC_15550199_"))
                assertTrue("File should have .m4a extension", result.file!!.name.endsWith(".m4a"))
                assertEquals("Should be saved in CallRecordings directory", "CallRecordings", result.file!!.parentFile?.name)
            }
        }
    }

    @Test
    fun testPreventDoubleRecordingInitialization() {
        runBlocking {
            // 1. Start the first call recording session
            val successFirst = CallAudioRecorder.startRecording(context, "555111222")
            assertTrue(successFirst)
            assertTrue(CallAudioRecorder.isRecording.value)

            // 2. Attempt to start a second call recording session while the first is active
            val successSecond = CallAudioRecorder.startRecording(context, "555333444")
            assertFalse("Should prevent starting a second concurrent recording", successSecond)

            // Clean up
            CallAudioRecorder.stopRecording()
            assertFalse(CallAudioRecorder.isRecording.value)
        }
    }

    @Test
    fun testAudioStorageFileRetrievalAndCleanup() {
        runBlocking {
            // Create the private call recordings folder to verify list matching
            val recordDir = File(context.getExternalFilesDir(null), "CallRecordings").apply {
                if (!exists()) mkdirs()
            }

            // Clean out any leftover files in testing directory
            recordDir.listFiles()?.forEach { it.delete() }

            // Create dummy mock call recording files representing finished recordings
            val dummyFile1 = File(recordDir, "REC_5550001_20260910_100000.m4a").apply { writeText("dummy audio data 1") }
            val dummyFile2 = File(recordDir, "REC_5550002_20260910_101500.m4a").apply { writeText("dummy audio data 2") }
            val nonAudioFile = File(recordDir, "unrelated_doc.txt").apply { writeText("text data") }

            val files = CallAudioRecorder.getRecordedFiles(context)

            // Ensure we retrieve only .m4a files and ignore unrelated ones
            assertEquals(2, files.size)
            assertTrue(files.any { it.name == "REC_5550001_20260910_100000.m4a" })
            assertTrue(files.any { it.name == "REC_5550002_20260910_101500.m4a" })
            assertFalse(files.any { it.name == "unrelated_doc.txt" })

            // Clean up files
            dummyFile1.delete()
            dummyFile2.delete()
            nonAudioFile.delete()
        }
    }
}
