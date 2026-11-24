/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.app.Application
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Looper
import android.os.PowerManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.ui.components.SoundAndGesturesSettings
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.FakeCallReceiver
import com.example.util.IncomingCallDisplayMode
import com.example.util.LockScreenCallPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IncomingCallDisplayPolicyTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application
    private val viewModelStore = ViewModelStore()

    private fun createManagedViewModel(key: String = "vm_${System.nanoTime()}"): DialerViewModel {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DialerViewModel(app) as T
            }
        }
        return ViewModelProvider(viewModelStore, factory)[key, DialerViewModel::class.java]
    }

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences(LockScreenCallPolicy.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        runBlocking(Dispatchers.IO) {
            AppDatabase.getDatabase(app).clearAllTables()
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `defaults use banner on active screen and full screen when screen off`() {
        assertEquals(
            IncomingCallDisplayMode.NOTIFICATION,
            LockScreenCallPolicy.getIncomingDisplayModeScreenOn(app)
        )
        assertEquals(
            IncomingCallDisplayMode.FULL_SCREEN,
            LockScreenCallPolicy.getIncomingDisplayModeScreenOff(app)
        )
    }

    @Test
    fun `presentation decision matrix eliminates duplicate banner and honors user choices`() {
        // 1. Active + Unlocked + Default (Notification): no full-screen takeover, no screen wake
        val activeDefault = LockScreenCallPolicy.resolveIncomingCallPresentation(
            isScreenInteractive = true,
            isKeyguardLocked = false,
            screenOnMode = IncomingCallDisplayMode.NOTIFICATION,
            screenOffMode = IncomingCallDisplayMode.FULL_SCREEN
        )
        assertFalse(activeDefault.launchFullScreenUi)
        assertFalse(activeDefault.wakeScreen)
        assertFalse(activeDefault.attachFullScreenIntent)
        assertFalse(activeDefault.useSilentShadeNotification)

        // 2. Active + Unlocked + Full Screen: launches UI without competing heads-up banner
        val activeFullScreen = LockScreenCallPolicy.resolveIncomingCallPresentation(
            isScreenInteractive = true,
            isKeyguardLocked = false,
            screenOnMode = IncomingCallDisplayMode.FULL_SCREEN,
            screenOffMode = IncomingCallDisplayMode.FULL_SCREEN
        )
        assertTrue(activeFullScreen.launchFullScreenUi)
        assertFalse(activeFullScreen.wakeScreen)
        assertFalse(activeFullScreen.attachFullScreenIntent)
        assertTrue(activeFullScreen.useSilentShadeNotification)

        // 3. Screen Off + Default (Full Screen): wakes screen and attaches FSI for Android 14-17 / OEM BAL reliability
        val screenOffFullScreen = LockScreenCallPolicy.resolveIncomingCallPresentation(
            isScreenInteractive = false,
            isKeyguardLocked = true,
            screenOnMode = IncomingCallDisplayMode.NOTIFICATION,
            screenOffMode = IncomingCallDisplayMode.FULL_SCREEN
        )
        assertTrue(screenOffFullScreen.launchFullScreenUi)
        assertTrue(screenOffFullScreen.wakeScreen)
        assertTrue(screenOffFullScreen.attachFullScreenIntent)
        assertFalse(screenOffFullScreen.useSilentShadeNotification)

        // 4. Screen Interactive but Keyguard Locked + Full Screen: attaches FSI over lockscreen without extra wake lock
        val lockscreenPeekFullScreen = LockScreenCallPolicy.resolveIncomingCallPresentation(
            isScreenInteractive = true,
            isKeyguardLocked = true,
            screenOnMode = IncomingCallDisplayMode.NOTIFICATION,
            screenOffMode = IncomingCallDisplayMode.FULL_SCREEN
        )
        assertTrue(lockscreenPeekFullScreen.launchFullScreenUi)
        assertFalse(lockscreenPeekFullScreen.wakeScreen)
        assertTrue(lockscreenPeekFullScreen.attachFullScreenIntent)
        assertFalse(lockscreenPeekFullScreen.useSilentShadeNotification)

        // 5. Screen Off + Notification Only: NEVER wakes screen or launches full-screen UI
        val screenOffNotifyOnly = LockScreenCallPolicy.resolveIncomingCallPresentation(
            isScreenInteractive = false,
            isKeyguardLocked = true,
            screenOnMode = IncomingCallDisplayMode.NOTIFICATION,
            screenOffMode = IncomingCallDisplayMode.NOTIFICATION
        )
        assertFalse(screenOffNotifyOnly.launchFullScreenUi)
        assertFalse(screenOffNotifyOnly.wakeScreen)
        assertFalse(screenOffNotifyOnly.attachFullScreenIntent)
        assertFalse(screenOffNotifyOnly.useSilentShadeNotification)
    }

    @Test
    fun `inCallService registers both high-priority and full-screen shade channels`() {
        val controller = Robolectric.buildService(MyInCallService::class.java).create()
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val bannerChannel = nm.getNotificationChannel(MyInCallService.CHANNEL_INCOMING)
        val shadeChannel = nm.getNotificationChannel(MyInCallService.CHANNEL_INCOMING_SHADE)

        assertEquals(NotificationManager.IMPORTANCE_HIGH, bannerChannel.importance)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, shadeChannel.importance)
        assertEquals(Notification.VISIBILITY_PUBLIC, shadeChannel.lockscreenVisibility)

        controller.destroy()
    }

    @Test
    fun `viewModel persists incoming display modes to SharedPreferences and Room`() = runBlocking {
        val vm = createManagedViewModel("vm_initial")
        vm.updateIncomingCallScreenOnMode(IncomingCallDisplayMode.FULL_SCREEN)
        vm.updateIncomingCallScreenOffMode(IncomingCallDisplayMode.NOTIFICATION)

        // Allow async Room insert coroutine to settle
        delay(250)

        assertEquals(IncomingCallDisplayMode.FULL_SCREEN.key, vm.incomingCallScreenOnMode.value)
        assertEquals(IncomingCallDisplayMode.NOTIFICATION.key, vm.incomingCallScreenOffMode.value)
        assertEquals(
            IncomingCallDisplayMode.FULL_SCREEN,
            LockScreenCallPolicy.getIncomingDisplayModeScreenOn(app)
        )
        assertEquals(
            IncomingCallDisplayMode.NOTIFICATION,
            LockScreenCallPolicy.getIncomingDisplayModeScreenOff(app)
        )

        val dao = AppDatabase.getDatabase(app).dialerDao()
        assertEquals(
            IncomingCallDisplayMode.FULL_SCREEN.key,
            dao.getSetting(LockScreenCallPolicy.PREF_INCOMING_DISPLAY_SCREEN_ON)
        )
        assertEquals(
            IncomingCallDisplayMode.NOTIFICATION.key,
            dao.getSetting(LockScreenCallPolicy.PREF_INCOMING_DISPLAY_SCREEN_OFF)
        )

        // Fresh ViewModel reload from Room/prefs preserves values
        val reloadedVm = createManagedViewModel("vm_reloaded")
        delay(150)
        assertEquals(IncomingCallDisplayMode.FULL_SCREEN.key, reloadedVm.incomingCallScreenOnMode.value)
        assertEquals(IncomingCallDisplayMode.NOTIFICATION.key, reloadedVm.incomingCallScreenOffMode.value)
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-port-xhdpi", sdk = [34])
    fun `soundAndGesturesSettings toggles incoming display modes cleanly on 320dp at 1_5x fontScale`() {
        val vm = createManagedViewModel("vm_ui")
        composeRule.mainClock.autoAdvance = true

        composeRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                CompositionLocalProvider(
                    LocalDensity provides Density(density = 2.0f, fontScale = 1.5f)
                ) {
                    Box(modifier = Modifier.requiredSize(width = 320.dp, height = 640.dp)) {
                        SoundAndGesturesSettings(
                            viewModel = vm,
                            cardBgColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()

        // Defaults selected initially
        composeRule.onNodeWithTag("sound_gestures_list").performScrollToNode(hasTestTag("incoming_screen_on_notification"))
        composeRule.onNodeWithTag("incoming_screen_on_notification").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithTag("incoming_screen_on_fullscreen").assertIsDisplayed()

        composeRule.onNodeWithTag("sound_gestures_list").performScrollToNode(hasTestTag("incoming_screen_off_fullscreen"))
        composeRule.onNodeWithTag("incoming_screen_off_fullscreen").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithTag("incoming_screen_off_notification").assertIsDisplayed()

        // Tap Full Screen for active screen, Notification for screen off
        composeRule.onNodeWithTag("sound_gestures_list").performScrollToNode(hasTestTag("incoming_screen_on_fullscreen"))
        composeRule.onNodeWithTag("incoming_screen_on_fullscreen").performClick()

        composeRule.onNodeWithTag("sound_gestures_list").performScrollToNode(hasTestTag("incoming_screen_off_notification"))
        composeRule.onNodeWithTag("incoming_screen_off_notification").performClick()
        composeRule.waitForIdle()

        assertEquals(IncomingCallDisplayMode.FULL_SCREEN.key, vm.incomingCallScreenOnMode.value)
        assertEquals(IncomingCallDisplayMode.NOTIFICATION.key, vm.incomingCallScreenOffMode.value)

        composeRule.onNodeWithTag("sound_gestures_list").performScrollToNode(hasTestTag("incoming_screen_on_fullscreen"))
        composeRule.onNodeWithTag("incoming_screen_on_fullscreen").assertIsSelected()
        composeRule.onNodeWithTag("sound_gestures_list").performScrollToNode(hasTestTag("incoming_screen_off_notification"))
        composeRule.onNodeWithTag("incoming_screen_off_notification").assertIsSelected()
    }

    @Test
    fun `resolveIncomingCallPresentation using real PowerManager and KeyguardManager reflects OS state`() {
        val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
        val km = app.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        @Suppress("DEPRECATION")
        Shadows.shadowOf(pm).setIsInteractive(true)
        Shadows.shadowOf(km).setKeyguardLocked(false)
        LockScreenCallPolicy.setIncomingDisplayModeScreenOn(app, IncomingCallDisplayMode.NOTIFICATION)

        val unlockedDecision = LockScreenCallPolicy.resolveIncomingCallPresentation(
            context = app,
            isScreenInteractive = pm.isInteractive,
            isKeyguardLocked = km.isKeyguardLocked
        )
        assertFalse(unlockedDecision.launchFullScreenUi)
        assertFalse(unlockedDecision.wakeScreen)

        @Suppress("DEPRECATION")
        Shadows.shadowOf(pm).setIsInteractive(false)
        Shadows.shadowOf(km).setKeyguardLocked(true)
        LockScreenCallPolicy.setIncomingDisplayModeScreenOff(app, IncomingCallDisplayMode.FULL_SCREEN)

        val lockedDecision = LockScreenCallPolicy.resolveIncomingCallPresentation(
            context = app,
            isScreenInteractive = pm.isInteractive,
            isKeyguardLocked = km.isKeyguardLocked
        )
        assertTrue(lockedDecision.launchFullScreenUi)
        assertTrue(lockedDecision.wakeScreen)
        assertTrue(lockedDecision.attachFullScreenIntent)
        assertNull(Shadows.shadowOf(app).nextStartedActivity)
    }

    @Test
    fun `fakeCallReceiver omits fullScreenIntent when screen-off notification only is selected`() {
        Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
        val km = app.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        @Suppress("DEPRECATION")
        Shadows.shadowOf(pm).setIsInteractive(false)
        Shadows.shadowOf(km).setKeyguardLocked(true)

        val receiver = FakeCallReceiver()
        val intent = android.content.Intent("com.example.ACTION_TRIGGER_FAKE").apply {
            putExtra("caller_name", "Test Caller")
            putExtra("caller_number", "+15550101")
        }

        // Case 1: Screen-off Notification Only -> fullScreenIntent must be null
        LockScreenCallPolicy.setIncomingDisplayModeScreenOff(app, IncomingCallDisplayMode.NOTIFICATION)
        receiver.onReceive(app, intent)
        val notifyOnlyNotif = Shadows.shadowOf(nm).getNotification(FakeCallReceiver.NOTIFICATION_ID)
        assertNotNull(notifyOnlyNotif)
        assertNull(notifyOnlyNotif.fullScreenIntent)

        // Case 2: Screen-off Full Screen -> fullScreenIntent must be attached
        LockScreenCallPolicy.setIncomingDisplayModeScreenOff(app, IncomingCallDisplayMode.FULL_SCREEN)
        receiver.onReceive(app, intent)
        val fullScreenNotif = Shadows.shadowOf(nm).getNotification(FakeCallReceiver.NOTIFICATION_ID)
        assertNotNull(fullScreenNotif)
        assertNotNull(fullScreenNotif.fullScreenIntent)
    }

    @Test
    fun `fakeCallReceiver falls back to activity launch when POST_NOTIFICATIONS is denied in full-screen mode`() {
        Shadows.shadowOf(app).denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
        val km = app.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        @Suppress("DEPRECATION")
        Shadows.shadowOf(pm).setIsInteractive(false)
        Shadows.shadowOf(km).setKeyguardLocked(true)
        LockScreenCallPolicy.setIncomingDisplayModeScreenOff(app, IncomingCallDisplayMode.FULL_SCREEN)

        val receiver = FakeCallReceiver()
        val intent = android.content.Intent("com.example.ACTION_TRIGGER_FAKE").apply {
            putExtra("caller_name", "Fallback Caller")
            putExtra("caller_number", "+15550102")
        }
        receiver.onReceive(app, intent)

        val started = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull(started)
        assertEquals(MainActivity::class.java.name, started.component?.className)
    }
}
