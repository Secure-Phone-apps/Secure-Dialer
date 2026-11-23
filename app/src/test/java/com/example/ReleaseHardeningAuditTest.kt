/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.app.Application
import android.content.Context
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.ui.components.PrivacyAndBackupSettings
import com.example.ui.components.UssdResponseDialog
import com.example.ui.theme.getCustomSeedColorScheme
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.BlockedNumberContractManager
import com.example.util.CallAudioRecorder
import com.example.util.UssdDialogData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReleaseHardeningAuditTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        runBlocking(Dispatchers.IO) {
            AppDatabase.getDatabase(app).clearAllTables()
        }
    }

    @Test
    fun `private and restricted callers are blocked only when setting is enabled while emergency stays exempt`() = runBlocking {
        val dao = AppDatabase.getDatabase(app).dialerDao()
        val prefs = app.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)

        // Disabled by default -> do not block unknown/restricted presentations
        prefs.edit().putBoolean(BlockedNumberContractManager.PREF_BLOCK_UNKNOWN_PRIVATE, false).commit()
        assertFalse(
            BlockedNumberContractManager.shouldBlockIncomingCall(
                context = app,
                number = "",
                dao = dao,
                presentation = TelecomManager.PRESENTATION_RESTRICTED
            )
        )

        // Enabled -> blocks empty, restricted, unknown, and payphone presentations
        prefs.edit().putBoolean(BlockedNumberContractManager.PREF_BLOCK_UNKNOWN_PRIVATE, true).commit()
        assertTrue(
            BlockedNumberContractManager.shouldBlockIncomingCall(
                context = app,
                number = "",
                dao = dao,
                presentation = TelecomManager.PRESENTATION_RESTRICTED
            )
        )
        assertTrue(
            BlockedNumberContractManager.shouldBlockIncomingCall(
                context = app,
                number = "Private number",
                dao = dao,
                presentation = TelecomManager.PRESENTATION_UNKNOWN
            )
        )
        assertTrue(
            BlockedNumberContractManager.shouldBlockIncomingCall(
                context = app,
                number = "",
                dao = dao,
                presentation = TelecomManager.PRESENTATION_PAYPHONE
            )
        )

        // Normal caller allowed; emergency never blocked
        assertFalse(
            BlockedNumberContractManager.shouldBlockIncomingCall(
                context = app,
                number = "+15551234567",
                dao = dao,
                presentation = TelecomManager.PRESENTATION_ALLOWED
            )
        )
        assertFalse(
            BlockedNumberContractManager.shouldBlockIncomingCall(
                context = app,
                number = "911",
                dao = dao,
                presentation = TelecomManager.PRESENTATION_ALLOWED
            )
        )
    }

    @Test
    fun `clearSystemCallLogs preserves negative local manual call records`() = runBlocking {
        val dao = AppDatabase.getDatabase(app).dialerDao()
        dao.insertCallLogs(
            listOf(
                CallRecord(
                    id = 101,
                    name = "System Caller",
                    number = "+15550001",
                    label = "Mobile",
                    timestamp = "10:00",
                    type = CallType.INCOMING,
                    avatarText = "SC",
                    avatarBgValue = 0xFF1E88E5,
                    avatarTextColorValue = 0xFFFFFFFF,
                    duration = 30L,
                    hasVoicemail = false,
                    timestampMs = 1000L
                ),
                CallRecord(
                    id = -1,
                    name = "Manual Caller",
                    number = "+15550002",
                    label = "Mobile",
                    timestamp = "10:05",
                    type = CallType.OUTGOING,
                    avatarText = "MC",
                    avatarBgValue = 0xFF1E88E5,
                    avatarTextColorValue = 0xFFFFFFFF,
                    duration = 45L,
                    hasVoicemail = false,
                    timestampMs = 2000L
                )
            )
        )

        dao.clearSystemCallLogs()
        val remaining = dao.getAllCallLogsList()
        assertEquals(1, remaining.size)
        assertEquals(-1, remaining.first().id)
        assertEquals("Manual Caller", remaining.first().name)
        assertEquals(-1, dao.getMinCallLogId())
    }

    @Test
    fun `custom seed theme computes high-contrast onPrimary for both light and dark seeds`() {
        val brightScheme = getCustomSeedColorScheme("#FFF59D", darkTheme = true)
        assertTrue("Light seed color should select dark onPrimary text", brightScheme.onPrimary.luminance() < 0.2f)

        val deepScheme = getCustomSeedColorScheme("#1A237E", darkTheme = false)
        assertTrue("Dark seed color should select white onPrimary text", deepScheme.onPrimary.luminance() > 0.8f)
    }

    @Test
    fun `call audio time formatter honors system 24h vs 12h clock setting`() {
        val fixedCal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.MAY, 15, 21, 5, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val fixedMs = fixedCal.timeInMillis

        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "24")
        val time24 = CallAudioRecorder.formatTimeOnly(app, fixedMs)
        assertTrue("Expected 24-hour clock output (21:05) but got: $time24", time24.contains("21"))

        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "12")
        val time12 = CallAudioRecorder.formatTimeOnly(app, fixedMs)
        assertFalse("12-hour format should not emit 21:05: $time12", time12.contains("21"))
    }

    @Test
    fun `ussd dialog uses structured isLoading flag independent of locale text`() {
        composeRule.setContent {
            MaterialTheme {
                UssdResponseDialog(
                    data = UssdDialogData(
                        title = "Netzabfrage",
                        message = "USSD-Code wird ausgeführt...",
                        code = "*100#",
                        isLoading = true
                    ),
                    onDismiss = {}
                )
            }
        }
        composeRule.waitForIdle()
        // When loading in non-English text, Copy button stays hidden until result returns
        composeRule.onNodeWithTag("ussd_dismiss_button").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodes(androidx.compose.ui.test.hasTestTag("ussd_copy_button")).fetchSemanticsNodes().size)
    }

    @Test
    fun `privacy and backup tabs render safely on narrow 320dp viewport at 1_5x font scale`() {
        val viewModel = DialerViewModel(app)
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 1.5f)) {
                MaterialTheme {
                    Box(modifier = Modifier.requiredSize(320.dp, 640.dp)) {
                        PrivacyAndBackupSettings(
                            viewModel = viewModel,
                            cardBgColor = Color.DarkGray
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `manifest and device-transfer rules enforce security hardening`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(
            "MissedCallNotificationReceiver must require MODIFY_PHONE_STATE",
            manifest.contains("""android:permission="android.permission.MODIFY_PHONE_STATE"""")
        )

        val rules = File("src/main/res/xml/data_extraction_rules.xml").readText()
        assertTrue(rules.contains("<device-transfer>"))
        assertTrue(rules.contains("dialer_database-wal"))
        assertTrue(rules.contains("dialer_prefs.xml"))
    }
}
