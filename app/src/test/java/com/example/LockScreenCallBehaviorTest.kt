/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.app.Application
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.telecom.Call
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.ActiveCallScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.LockScreenCallPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LockScreenCallBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        LockScreenCallPolicy.setRequireUnlockToAnswerEnabled(context, false)
        CallManager.setMainActivityInForeground(false)
        CallManager.setInCallUiVisible(false)
    }

    @Test
    fun manifestIsolation_mainActivityLockedOut_inCallActivityShowWhenLocked() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())

        val doc = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(manifestFile)

        val activities = doc.getElementsByTagName("activity")
        var mainElement: Element? = null
        var inCallElement: Element? = null
        val androidNs = "http://schemas.android.com/apk/res/android"

        for (i in 0 until activities.length) {
            val el = activities.item(i) as Element
            when (el.getAttributeNS(androidNs, "name")) {
                ".MainActivity" -> mainElement = el
                ".InCallActivity" -> inCallElement = el
            }
        }

        assertNotNull("MainActivity declaration missing", mainElement)
        assertNotNull("InCallActivity declaration missing", inCallElement)

        // MainActivity must NOT declare lockscreen exposure attributes
        assertEquals("", mainElement!!.getAttributeNS(androidNs, "showWhenLocked"))
        assertEquals("", mainElement.getAttributeNS(androidNs, "turnScreenOn"))
        assertEquals("", mainElement.getAttributeNS(androidNs, "inheritShowWhenLocked"))

        // InCallActivity must declare non-exported, isolated lockscreen presentation attributes
        assertEquals("false", inCallElement!!.getAttributeNS(androidNs, "exported"))
        assertEquals("true", inCallElement.getAttributeNS(androidNs, "showWhenLocked"))
        assertEquals("true", inCallElement.getAttributeNS(androidNs, "turnScreenOn"))
        assertEquals("true", inCallElement.getAttributeNS(androidNs, "excludeFromRecents"))
        assertEquals("singleInstance", inCallElement.getAttributeNS(androidNs, "launchMode"))

        val pm = context.packageManager
        val inCallInfo = pm.getActivityInfo(
            ComponentName(context, InCallActivity::class.java),
            PackageManager.GET_META_DATA
        )
        assertFalse(inCallInfo.exported)
        assertEquals(ActivityInfo.LAUNCH_SINGLE_INSTANCE, inCallInfo.launchMode)
        assertTrue((inCallInfo.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS) != 0)
    }

    @Test
    fun lockScreenPolicy_defaultsToInstantAnswerWithoutUnlock_andRespectsOptionalToggle() {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        Shadows.shadowOf(km).setKeyguardLocked(true)

        // Default behavior matches Samsung/Google Phone: zero forced unlock challenge on incoming call
        assertFalse(LockScreenCallPolicy.isRequireUnlockToAnswerEnabled(context))
        assertTrue(LockScreenCallPolicy.isDeviceLocked(context))
        assertFalse(LockScreenCallPolicy.shouldPromptUnlockBeforeAnswering(context))

        // Enabling optional setting gates answering behind unlock only while keyguard is locked
        LockScreenCallPolicy.setRequireUnlockToAnswerEnabled(context, true)
        assertTrue(LockScreenCallPolicy.isRequireUnlockToAnswerEnabled(context))
        assertTrue(LockScreenCallPolicy.shouldPromptUnlockBeforeAnswering(context))

        // Unlocked device never prompts unlock even when setting is ON
        Shadows.shadowOf(km).setKeyguardLocked(false)
        assertFalse(LockScreenCallPolicy.shouldPromptUnlockBeforeAnswering(context))
    }

    @Test
    fun lockScreenPolicy_appliesAndClearsWindowFlagsWithoutForceDismissingKeyguard() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).create()
        val activity = controller.get()

        LockScreenCallPolicy.applyLockScreenWindowFlags(activity, showWhenLocked = true, keepScreenOn = true)
        @Suppress("DEPRECATION")
        val inCallFlags = activity.window.attributes.flags
        @Suppress("DEPRECATION")
        assertTrue((inCallFlags and WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED) != 0)
        @Suppress("DEPRECATION")
        assertTrue((inCallFlags and WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON) != 0)
        @Suppress("DEPRECATION")
        assertTrue((inCallFlags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)

        LockScreenCallPolicy.applyLockScreenWindowFlags(activity, showWhenLocked = false, keepScreenOn = false)
        @Suppress("DEPRECATION")
        val clearedFlags = activity.window.attributes.flags
        @Suppress("DEPRECATION")
        assertEquals(0, clearedFlags and WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        @Suppress("DEPRECATION")
        assertEquals(0, clearedFlags and WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        assertEquals(0, clearedFlags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        controller.destroy()
    }

    @Test
    fun callManager_tracksInCallUiAndMainActivityForegroundIndependently() {
        assertFalse(CallManager.isAppInForeground.value)
        assertFalse(CallManager.isInCallUiVisible.value)

        CallManager.setInCallUiVisible(true)
        assertTrue(CallManager.isInCallUiVisible.value)
        assertTrue(CallManager.isAppInForeground.value)

        CallManager.setMainActivityInForeground(true)
        CallManager.setInCallUiVisible(false)
        assertFalse(CallManager.isInCallUiVisible.value)
        assertTrue(CallManager.isMainActivityInForeground.value)
        assertTrue(CallManager.isAppInForeground.value)

        CallManager.setMainActivityInForeground(false)
        assertFalse(CallManager.isAppInForeground.value)
    }

    @Test
    fun activeCallScreen_hidesMinimizeAndGatesPrivateActionsWhileLocked() {
        var unlockPromptCount = 0

        composeRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                ActiveCallScreen(
                    contactName = "Alice",
                    contactNumber = "+15550199",
                    preferredSim = "Ask",
                    quickResponses = listOf("I'll call you back"),
                    onHangUp = {},
                    onQuickDecline = {},
                    isIncoming = false,
                    callState = Call.STATE_ACTIVE,
                    onMinimize = {},
                    isDeviceLocked = true,
                    onRequestUnlock = { action ->
                        unlockPromptCount++
                        action()
                    }
                )
            }
        }

        // Minimize must NOT be rendered when keyguard is locked (protects Recents/Contacts)
        assertTrue(
            composeRule.onAllNodesWithContentDescription("Minimize Call")
                .fetchSemanticsNodes()
                .isEmpty()
        )

        // Tapping Add Call while locked must request device unlock instead of opening private contacts list
        val addCallLabel = context.getString(R.string.add_call)
        composeRule.onNodeWithText(addCallLabel).assertIsDisplayed().performClick()
        assertEquals(1, unlockPromptCount)
    }

    @Test
    fun activeCallScreen_showsMinimizeWhenDeviceUnlocked() {
        composeRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                ActiveCallScreen(
                    contactName = "Alice",
                    contactNumber = "+15550199",
                    preferredSim = "Ask",
                    quickResponses = emptyList(),
                    onHangUp = {},
                    onQuickDecline = {},
                    isIncoming = false,
                    callState = Call.STATE_ACTIVE,
                    onMinimize = {},
                    isDeviceLocked = false
                )
            }
        }

        composeRule.onNodeWithContentDescription("Minimize Call").assertIsDisplayed()
    }
}
