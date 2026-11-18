/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.ui.components.BatterySurvivabilitySettings
import com.example.ui.components.CallSystemPermissionsCard
import com.example.ui.components.SettingsPanel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.CallScreenPermissionHelper
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatteryWarningAndGuideTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        runBlocking(Dispatchers.IO) {
            AppDatabase.getDatabase(app).clearAllTables()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = app.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null) {
                Shadows.shadowOf(roleManager).addHeldRole(RoleManager.ROLE_DIALER)
            }
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    @Config(sdk = [28, 33, 34])
    fun `battery warning snooze 1 day 1 week disable permanently and re-enable across Android 9 to 14`() {
        val viewModel = DialerViewModel(app)
        val t0 = 1_700_000_000_000L

        assertTrue(viewModel.shouldShowBatteryWarning(t0))

        // 1-Day Snooze
        viewModel.snoozeBatteryWarning(DialerViewModel.ONE_DAY_MS, nowMs = t0)
        assertFalse(viewModel.shouldShowBatteryWarning(t0 + 12 * 3600 * 1000L))
        assertTrue(viewModel.shouldShowBatteryWarning(t0 + DialerViewModel.ONE_DAY_MS + 1L))

        // 1-Week Snooze
        viewModel.snoozeBatteryWarning(DialerViewModel.ONE_WEEK_MS, nowMs = t0)
        assertFalse(viewModel.shouldShowBatteryWarning(t0 + 3 * DialerViewModel.ONE_DAY_MS))
        assertTrue(viewModel.shouldShowBatteryWarning(t0 + DialerViewModel.ONE_WEEK_MS + 1L))

        // Disable Permanently + verify persistence across ViewModel recreation
        viewModel.setBatteryWarningEnabled(false)
        assertFalse(viewModel.shouldShowBatteryWarning(t0 + 365L * DialerViewModel.ONE_DAY_MS))

        val recreated = DialerViewModel(app)
        assertEquals(DialerViewModel.BATTERY_WARNING_DISABLED, recreated.batteryWarningMode.value)
        assertFalse(recreated.shouldShowBatteryWarning(t0 + 365L * DialerViewModel.ONE_DAY_MS))

        // Re-enable cleanly
        recreated.setBatteryWarningEnabled(true)
        assertTrue(recreated.shouldShowBatteryWarning(t0))
    }

    @Test
    fun `dismissing battery banner hides card persists preference and unmasks overlay permission`() {
        assertFalse(CallScreenPermissionHelper.isIgnoringBatteryOptimizations(app))

        val viewModel = DialerViewModel(app)
        viewModel.isDefaultDialer.value = true
        viewModel.isDynamicIslandEnabled.value = true

        composeRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.requiredSize(360.dp, 640.dp)) {
                    CallSystemPermissionsCard(viewModel = viewModel)
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("permissions_warning_card").assertIsDisplayed()
        composeRule.onNodeWithTag("battery_warning_learn_more_button").assertIsDisplayed()
        composeRule.onNodeWithTag("battery_warning_dismiss_button").assertIsDisplayed()

        // Tap Dismiss
        composeRule.onNodeWithTag("battery_warning_dismiss_button").performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()

        assertEquals(DialerViewModel.BATTERY_WARNING_DISABLED, viewModel.batteryWarningMode.value)

        // Dismissing battery warning unmasks the next pending system warning cleanly (FullScreen or Overlay)
        val expectedNextWarning = when {
            !CallScreenPermissionHelper.canUseFullScreenIntent(app) ->
                app.getString(R.string.settings_full_screen_intent_warning)
            !CallScreenPermissionHelper.canDrawOverlays(app) ->
                app.getString(R.string.settings_overlay_permission_warning)
            else -> null
        }
        if (expectedNextWarning != null) {
            composeRule.waitUntil(timeoutMillis = 3_000) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                composeRule.onAllNodesWithText(expectedNextWarning).fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText(expectedNextWarning).assertIsDisplayed()
        }

        // Turn off Dynamic Island so no overlay warning remains if FSI was already allowed
        viewModel.isDynamicIslandEnabled.value = false
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        if (CallScreenPermissionHelper.canUseFullScreenIntent(app)) {
            assertTrue(composeRule.onAllNodesWithTag("permissions_warning_card").fetchSemanticsNodes().isEmpty())
        }
    }

    @Test
    fun `learn more opens dedicated BatterySurvivabilitySettings screen with controls and explanation`() {
        assertFalse(CallScreenPermissionHelper.isIgnoringBatteryOptimizations(app))

        val viewModel = DialerViewModel(app)
        viewModel.isDefaultDialer.value = true

        composeRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.requiredSize(400.dp, 780.dp)) {
                    CallSystemPermissionsCard(viewModel = viewModel)
                    if (viewModel.isSettingsVisible.value) {
                        SettingsPanel(
                            viewModel = viewModel,
                            onClose = { viewModel.isSettingsVisible.value = false }
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("battery_warning_learn_more_button").performClick()
        composeRule.waitForIdle()

        assertTrue(viewModel.isSettingsVisible.value)
        composeRule.onNodeWithTag("battery_survivability_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("battery_status_card").assertIsDisplayed()
        composeRule.onNodeWithTag("battery_warning_controls_card").assertIsDisplayed()

        // Tap 1-Week Snooze chip
        composeRule.onNodeWithTag("battery_snooze_7d_chip").performClick()
        composeRule.waitForIdle()
        assertEquals(DialerViewModel.BATTERY_WARNING_SNOOZED, viewModel.batteryWarningMode.value)
        assertFalse(viewModel.shouldShowBatteryWarning())

        // Scroll LazyColumn items to explanation & shortcuts
        composeRule.onNodeWithTag("battery_survivability_screen")
            .performScrollToNode(hasTestTag("battery_guide_explanation_card"))
        composeRule.onNodeWithTag("battery_guide_explanation_card").assertIsDisplayed()
        composeRule.onNodeWithTag("battery_survivability_screen")
            .performScrollToNode(hasTestTag("open_oem_battery_settings_btn"))
        composeRule.onNodeWithTag("open_oem_battery_settings_btn").assertIsDisplayed()
    }

    @Test
    fun `banner and battery guide render cleanly on 320dp narrow cover screen at 2x font scale`() {
        assertFalse(CallScreenPermissionHelper.isIgnoringBatteryOptimizations(app))

        val viewModel = DialerViewModel(app)
        viewModel.isDefaultDialer.value = true

        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2.0f, fontScale = 2.0f)) {
                MyApplicationTheme {
                    Box(modifier = Modifier.requiredSize(320.dp, 533.dp)) {
                        CallSystemPermissionsCard(viewModel = viewModel)
                    }
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("permissions_warning_card").assertIsDisplayed()
        composeRule.onNodeWithTag("battery_warning_dismiss_button").assertIsDisplayed()
        composeRule.onNodeWithTag("battery_warning_learn_more_button").assertIsDisplayed()
        composeRule.onNodeWithTag("permissions_warning_action_button").assertIsDisplayed()

        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2.0f, fontScale = 2.0f)) {
                MyApplicationTheme {
                    Box(modifier = Modifier.requiredSize(320.dp, 533.dp)) {
                        BatterySurvivabilitySettings(
                            viewModel = viewModel,
                            cardBgColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("battery_survivability_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("battery_survivability_screen")
            .performScrollToNode(hasTestTag("battery_warning_controls_card"))
        composeRule.onNodeWithTag("battery_snooze_1d_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("battery_disable_forever_chip").assertIsDisplayed()
    }
}
