package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Secure Dialer", appName)
  }

  @Test
  fun `verify CallManager initial state`() {
    assertNull(CallManager.currentCall.value)
    assertEquals(android.telecom.Call.STATE_DISCONNECTED, CallManager.callState.value)
    assertEquals("", CallManager.callerNumber.value)
    assertEquals("", CallManager.callerName.value)
  }

  @Test
  fun `verify CallManager updates correctly when cleared`() {
    CallManager.updateCall(null)
    assertNull(CallManager.currentCall.value)
    assertEquals(android.telecom.Call.STATE_DISCONNECTED, CallManager.callState.value)
    assertEquals("", CallManager.callerNumber.value)
  }

  @Test
  fun `verify CallAudioRecorder disk recovery finds files`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dir = java.io.File(context.filesDir, "CallRecordings").apply { mkdirs() }
    val testFile = java.io.File(dir, "REC_1234567890_20260920_120000.m4a")
    testFile.writeBytes(ByteArray(256) { 1 })

    val recovered = com.example.util.CallAudioRecorder.recoverRecordingsFromDisk(context)
    val found = recovered.any { it.filePath == testFile.absolutePath && it.number == "1234567890" }
    assertEquals(true, found)

    testFile.delete()
  }

  @Test
  fun `verify exportRecordingToPublicDownloads execution`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dir = java.io.File(context.filesDir, "CallRecordings").apply { mkdirs() }
    val testFile = java.io.File(dir, "REC_EXPORT_TEST.m4a")
    testFile.writeBytes(ByteArray(256) { 2 })

    val exported = com.example.util.CallAudioRecorder.exportRecordingToPublicDownloads(context, testFile)
    assertEquals(true, exported)

    testFile.delete()
  }

  @Test
  fun `verify audio compression profile serialization and cleanup`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.CallAudioRecorder.setCompressionProfile(context, com.example.util.RecordingCompressionProfile.COMPACT)
    val retrieved = com.example.util.CallAudioRecorder.getSelectedCompressionProfile(context)
    assertEquals(com.example.util.RecordingCompressionProfile.COMPACT, retrieved)

    val dir = java.io.File(context.filesDir, "CallRecordings").apply { mkdirs() }
    val emptyFile = java.io.File(dir, "REC_EMPTY_STUB.m4a")
    emptyFile.writeBytes(ByteArray(32) { 0 })

    val cleaned = com.example.util.CallAudioRecorder.cleanupCorruptOrEmptyFiles(context)
    assertEquals(1, cleaned)
    assertEquals(false, emptyFile.exists())
  }
}
