/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.app.Application
import android.content.Context
import android.media.MediaRecorder
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.DialerDao
import com.example.model.AppSetting
import com.example.model.CallRecording
import com.example.util.CallAudioRecorder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowMediaRecorder
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Batch3TelephonyAudioAndRecordingTest {

    private lateinit var context: Application
    private lateinit var memoryDb: AppDatabase
    private lateinit var dao: DialerDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        CallAudioRecorder.stopRecording()
        CallManager.releaseToneGenerator()
        TrackingShadowMediaRecorder.resetStats()
        memoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = memoryDb.dialerDao()
    }

    @After
    fun tearDown() {
        CallAudioRecorder.stopRecording()
        CallManager.releaseToneGenerator()
        TrackingShadowMediaRecorder.resetStats()
        memoryDb.close()
    }

    @Implements(MediaRecorder::class)
    class TrackingShadowMediaRecorder : ShadowMediaRecorder() {
        private var currentSource: Int = -1

        @Implementation
        override fun setAudioSource(audioSource: Int) {
            currentSource = audioSource
            triedSources.add(audioSource)
            super.setAudioSource(audioSource)
        }

        @Implementation
        override fun prepare() {
            prepareAttempts++
            if (prepareAttempts <= failFirstNPrepareCalls) {
                throw RuntimeException("Simulated OEM audio source failure #$prepareAttempts (src=$currentSource)")
            }
            super.prepare()
        }

        @Implementation
        override fun reset() {
            resetCount++
            super.reset()
        }

        @Implementation
        override fun release() {
            releaseCount++
            super.release()
        }

        companion object {
            var failFirstNPrepareCalls: Int = 0
            var prepareAttempts: Int = 0
            var resetCount: Int = 0
            var releaseCount: Int = 0
            val triedSources = mutableListOf<Int>()

            fun resetStats() {
                failFirstNPrepareCalls = 0
                prepareAttempts = 0
                resetCount = 0
                releaseCount = 0
                triedSources.clear()
            }
        }
    }

    @Test
    @Config(shadows = [TrackingShadowMediaRecorder::class])
    fun `startRecording releases failed MediaRecorder handles immediately on each audio source fallback`() {
        // Fail first 2 sources (VOICE_COMMUNICATION and MIC), succeed on 3rd (VOICE_RECOGNITION)
        TrackingShadowMediaRecorder.failFirstNPrepareCalls = 2

        val started = CallAudioRecorder.startRecording(context, "+15550199")
        assertTrue("Recording should succeed on 3rd fallback AudioSource", started)
        assertEquals(
            listOf(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.VOICE_RECOGNITION
            ),
            TrackingShadowMediaRecorder.triedSources
        )
        // Crucial leak check: both failed recorders must already be reset & released BEFORE stopRecording()!
        assertEquals(2, TrackingShadowMediaRecorder.resetCount)
        assertEquals(2, TrackingShadowMediaRecorder.releaseCount)

        CallAudioRecorder.stopRecording()
        assertEquals(3, TrackingShadowMediaRecorder.resetCount)
        assertEquals(3, TrackingShadowMediaRecorder.releaseCount)
    }

    @Test
    @Config(shadows = [TrackingShadowMediaRecorder::class])
    fun `startRecording releases all 4 MediaRecorder instances cleanly when every audio source fails`() {
        TrackingShadowMediaRecorder.failFirstNPrepareCalls = 10

        val started = CallAudioRecorder.startRecording(context, "+15550123")
        assertFalse("Recording must return false when all 4 sources fail", started)
        assertEquals(
            listOf(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                MediaRecorder.AudioSource.DEFAULT
            ),
            TrackingShadowMediaRecorder.triedSources
        )
        assertEquals(4, TrackingShadowMediaRecorder.resetCount)
        assertEquals(4, TrackingShadowMediaRecorder.releaseCount)

        // Ensure helper handles null safely
        CallAudioRecorder.releaseMediaRecorderSafely(null)
    }

    @Test
    fun `persistNotificationRecording deduplicates by file path and obeys auto-export preference`() = runBlocking {
        val recordDir = File(context.filesDir, "CallRecordings").apply { mkdirs() }
        val file = File(recordDir, "REC_15559876543_20261120_120000.m4a").apply {
            writeBytes(ByteArray(512) { 1 })
        }
        try {
            dao.insertSetting(AppSetting("is_auto_export_recordings_enabled", "false"))
            val recording = CallRecording(
                number = "15559876543",
                name = "Alice",
                timestamp = "Nov 20, 12:00",
                duration = 42L,
                filePath = file.absolutePath
            )

            // Persist twice for the same file path (notification + call drop race)
            MyInCallService.persistNotificationRecording(
                context = context,
                dao = dao,
                recording = recording,
                file = file,
                autoExportFallback = false
            )
            MyInCallService.persistNotificationRecording(
                context = context,
                dao = dao,
                recording = recording,
                file = file,
                autoExportFallback = false
            )

            val stored = dao.getAllCallRecordingsList()
            assertEquals("Duplicate path must be stored only once", 1, stored.size)
            assertEquals(file.absolutePath, stored.first().filePath)
            assertEquals("Alice", stored.first().name)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `upsertCnapSettingCapped caps cnap rows at 200 without deleting non-cnap or underscore-like keys`() = runBlocking {
        dao.insertSetting(AppSetting("theme_preference", "amoled"))
        dao.insertSetting(AppSetting("is_auto_export_recordings_enabled", "true"))
        dao.insertSetting(AppSetting("cnapX_unrelated", "must_survive_like_escape"))

        // Insert 225 CNAP rows (1..225)
        for (i in 1..225) {
            dao.upsertCnapSettingCapped(
                key = "cnap_${1000 + i}",
                value = "Caller $i"
            )
        }

        val allSettings = dao.getAllSettingsList()
        val cnapRows = allSettings.filter { it.key.startsWith("cnap_") }

        assertEquals(DialerDao.MAX_CACHED_CNAP_ENTRIES, cnapRows.size)
        assertNull("Oldest CNAP #1 should be pruned", dao.getSetting("cnap_1001"))
        assertNull("Oldest CNAP #25 should be pruned", dao.getSetting("cnap_1025"))
        assertEquals("Caller 26", dao.getSetting("cnap_1026"))
        assertEquals("Caller 225", dao.getSetting("cnap_1225"))

        // Non-CNAP settings including `cnapX_unrelated` must never be pruned
        assertEquals("amoled", dao.getSetting("theme_preference"))
        assertEquals("true", dao.getSetting("is_auto_export_recordings_enabled"))
        assertEquals("must_survive_like_escape", dao.getSetting("cnapX_unrelated"))

        // Refresh cnap_1026 so its rowid becomes newest, then insert one more entry
        dao.upsertCnapSettingCapped("cnap_1026", "Caller 26 Refreshed")
        dao.upsertCnapSettingCapped("cnap_9999", "Newest Caller")

        assertEquals("Caller 26 Refreshed", dao.getSetting("cnap_1026"))
        assertNull("Now-oldest CNAP #27 should be evicted instead of refreshed #26", dao.getSetting("cnap_1027"))
        assertEquals("Newest Caller", dao.getSetting("cnap_9999"))
    }

    @Test
    fun `CallManager releases local ToneGenerator cleanly on updateCall null and onInCallServiceUnbound`() {
        val service = Robolectric.buildService(MyInCallService::class.java).create().get()
        CallManager.inCallService = service

        // Ensure releaseToneGenerator & updateCall(null) cleanly tear down ToneGenerator without throwing
        CallManager.updateCall(null)
        assertFalse(CallManager.hasActiveToneGeneratorForTest())

        CallManager.onInCallServiceUnbound(service)
        assertFalse(CallManager.hasActiveToneGeneratorForTest())
    }
}
