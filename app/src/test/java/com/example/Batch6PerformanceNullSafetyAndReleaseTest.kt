package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.ui.components.CallGroup
import com.example.ui.components.DialpadTabContent
import com.example.ui.components.RecentCallRow
import com.example.ui.components.isBackupPayloadEncrypted
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.LockScreenCallPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Batch6PerformanceNullSafetyAndReleaseTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun backupEncryptedDetectionHandlesPrettyCompactPlaintextAndBase64() {
        assertTrue(isBackupPayloadEncrypted("""{"version":3,"encrypted":true}"""))
        assertTrue(isBackupPayloadEncrypted("{\n  \"version\": 3,\n  \"encrypted\": true,\n  \"payload\": \"abc\"\n}"))
        assertFalse(isBackupPayloadEncrypted("""{"version":3,"encrypted":false,"blocked_numbers":[]}"""))
        assertFalse(isBackupPayloadEncrypted("{\n  \"version\": 3,\n  \"app_settings\": []\n}"))
        assertTrue(isBackupPayloadEncrypted("U2FsdGVkX19encryptedBase64Payload=="))
        assertTrue(isBackupPayloadEncrypted("{corrupt-json"))
    }

    @Test
    fun inCallActivityRefreshesPreferencesOnResumeAndNewIntent() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences(LockScreenCallPolicy.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear()
            .putBoolean("is_dark_theme", true)
            .putBoolean("is_amoled_mode", false)
            .putString("theme_color", "expressive_lime")
            .putBoolean("recording_enabled", false)
            .putBoolean("is_call_notes_enabled", true)
            .putBoolean("is_pocket_protection_enabled", false)
            .apply()

        val controller = Robolectric.buildActivity(InCallActivity::class.java)
            .create()
            .start()
            .resume()

        val activity = controller.get()
        val initial = activity.currentPreferencesForTest()
        assertTrue(initial.isDarkTheme)
        assertFalse(initial.isAmoledMode)
        assertEquals("expressive_lime", initial.themeColor)
        assertFalse(initial.recordingEnabled)
        assertFalse(initial.isPocketProtectionEnabled)

        // Simulate user changing theme, recording, notes, and pocket protection while InCallActivity is backgrounded
        controller.pause().stop()
        prefs.edit()
            .putBoolean("is_dark_theme", false)
            .putBoolean("is_amoled_mode", true)
            .putString("theme_color", "ocean_blue")
            .putBoolean("recording_enabled", true)
            .putBoolean("is_call_notes_enabled", false)
            .putBoolean("is_pocket_protection_enabled", true)
            .apply()

        // Re-enter via onNewIntent + onStart + onResume (singleInstance behavior)
        controller.newIntent(Intent(app, InCallActivity::class.java)).start().resume()

        val refreshed = activity.currentPreferencesForTest()
        assertFalse(refreshed.isDarkTheme)
        assertTrue(refreshed.isAmoledMode)
        assertEquals("ocean_blue", refreshed.themeColor)
        assertTrue(refreshed.recordingEnabled)
        assertFalse(refreshed.callNotesEnabled)
        assertTrue(refreshed.isPocketProtectionEnabled)

        controller.pause().stop().destroy()
    }

    @Test
    fun recentCallRowUsesHoistedBlockedAndSimStateAcrossLargeFontScale() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = DialerViewModel(app)
        val record = CallRecord(
            id = 42,
            name = "Ada Lovelace",
            number = "+15550101",
            label = "Mobile",
            timestamp = "May 15, 17:05",
            type = CallType.INCOMING,
            avatarText = "AL",
            avatarBgValue = 0xFF1E88E5,
            avatarTextColorValue = 0xFFFFFFFF,
            duration = 65L,
            hasVoicemail = false,
            timestampMs = 1_778_889_900_000L,
            simSlot = 1
        )
        val group = CallGroup(primary = record, calls = listOf(record))

        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 1.5f)) {
                MaterialTheme {
                    RecentCallRow(
                        group = group,
                        onCallClick = {},
                        onDeleteRecord = {},
                        getHistory = { emptyList() },
                        viewModel = viewModel,
                        onHistoryClick = {},
                        blockedNumbers = setOf("+15550101"),
                        physicalSimCount = 2,
                        allRecordings = emptyList(),
                        allContacts = emptyList()
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ada Lovelace").assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.sim_1)).assertIsDisplayed()

        // Expanding row should reflect hoisted blockedNumbers state -> "Unblock" action
        composeRule.onNodeWithText("Ada Lovelace").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(app.getString(R.string.unblock)).assertIsDisplayed()
    }

    @Test
    fun dialpadTabDisplaysNumberFieldAndAllKeypadButtonsSimultaneously() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        composeRule.setContent {
            MaterialTheme {
                DialpadTabContent(
                    inputValue = "",
                    onValueChange = {},
                    onCallClick = {},
                    onSpeedDialCall = {},
                    voicemailNumber = "",
                    speedDialMap = emptyMap(),
                    dialpadMatches = emptyList(),
                    onCollapseClick = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText(app.getString(R.string.dialpad_enter_number)).assertIsDisplayed()
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#").forEach { key ->
            composeRule.onNodeWithTag("dialpad_key_$key").assertIsDisplayed()
        }
        composeRule.onNodeWithTag("dialpad_call_button").assertIsDisplayed()
    }

    @Test
    fun proguardRulesNarrowAppKeepsWithoutBlanketComExampleKeep() {
        val proguardFile = File("proguard-rules.pro")
        assertTrue("app/proguard-rules.pro must exist", proguardFile.exists())
        val content = proguardFile.readText()
        assertFalse("Blanket -keep class com.example.** must be removed", content.contains("-keep class com.example.** { *; }"))
        assertFalse("Blanket -keepclassmembers class com.example.** must be removed", content.contains("-keepclassmembers class com.example.** { *; }"))
        assertTrue("Targeted com.example.model.** keep must be present", content.contains("-keep class com.example.model.** { *; }"))
        assertTrue("Room @Dao keep must be present", content.contains("-keep @androidx.room.Dao interface * { *; }"))
        assertTrue("Room @Entity keep must be present", content.contains("-keep @androidx.room.Entity class * { *; }"))
    }
}
