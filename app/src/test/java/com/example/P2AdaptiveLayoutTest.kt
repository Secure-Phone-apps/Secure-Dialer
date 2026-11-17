/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.Manifest
import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainScreen
import com.example.ui.components.ActiveCallScreen
import com.example.ui.components.DialpadOverlay
import com.example.ui.components.DialpadTabContent
import com.example.ui.components.SimSelectDialog
import com.example.ui.theme.ContainerHeightClass
import com.example.ui.theme.ContainerWidthClass
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.computeAdaptiveLayoutMetrics
import com.example.ui.viewmodel.DialerViewModel
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
class P2AdaptiveLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setupPermissionsAndCallState() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )
        CallManager.setMainActivityInForeground(false)
        CallManager.setInCallUiVisible(false)
    }

    @Test
    fun adaptiveMetrics_breakpointsCoverAllFormFactorsAndFontScales() {
        // Ultra-compact portrait phone (320dp wide)
        val tinyPhone = computeAdaptiveLayoutMetrics(maxWidth = 320.dp, maxHeight = 480.dp, fontScale = 1.0f)
        assertEquals(ContainerWidthClass.COMPACT, tinyPhone.widthClass)
        assertEquals(ContainerHeightClass.MEDIUM, tinyPhone.heightClass)
        assertTrue(tinyPhone.isVeryNarrowWidth)
        assertTrue(tinyPhone.isTightVerticalSpace)
        assertFalse(tinyPhone.useNavigationRail)
        assertFalse(tinyPhone.useTwoPaneInCall)
        assertFalse(tinyPhone.useTwoPaneDialpad)

        // Standard flagship portrait phone (411dp x 915dp)
        val standardPhone = computeAdaptiveLayoutMetrics(maxWidth = 411.dp, maxHeight = 915.dp, fontScale = 1.0f)
        assertEquals(ContainerWidthClass.COMPACT, standardPhone.widthClass)
        assertEquals(ContainerHeightClass.EXPANDED, standardPhone.heightClass)
        assertFalse(standardPhone.isVeryNarrowWidth)
        assertFalse(standardPhone.isTightVerticalSpace)
        assertFalse(standardPhone.useNavigationRail)
        assertEquals(120.dp, standardPhone.inCallAvatarSize)

        // Foldable inner display portrait (600dp x 900dp)
        val foldable = computeAdaptiveLayoutMetrics(maxWidth = 600.dp, maxHeight = 900.dp, fontScale = 1.0f)
        assertEquals(ContainerWidthClass.MEDIUM, foldable.widthClass)
        assertEquals(ContainerHeightClass.EXPANDED, foldable.heightClass)
        assertTrue(foldable.useNavigationRail)
        assertEquals(680.dp, foldable.contentMaxWidth)

        // Landscape phone / compact multi-window (640dp x 360dp)
        val landscapePhone = computeAdaptiveLayoutMetrics(maxWidth = 640.dp, maxHeight = 360.dp, fontScale = 1.0f)
        assertEquals(ContainerHeightClass.COMPACT, landscapePhone.heightClass)
        assertTrue(landscapePhone.useNavigationRail)
        assertTrue(landscapePhone.useTwoPaneInCall)
        assertTrue(landscapePhone.useTwoPaneDialpad)
        assertEquals(72.dp, landscapePhone.inCallAvatarSize)
        assertTrue(landscapePhone.dialButtonHeight >= 44.dp)

        // Expanded tablet (900dp x 800dp)
        val tablet = computeAdaptiveLayoutMetrics(maxWidth = 900.dp, maxHeight = 800.dp, fontScale = 1.0f)
        assertEquals(ContainerWidthClass.EXPANDED, tablet.widthClass)
        assertTrue(tablet.useNavigationRail)
        assertTrue(tablet.useTwoPaneInCall)
        assertTrue(tablet.useTwoPaneDialpad)
        assertEquals(840.dp, tablet.contentMaxWidth)

        // Large accessibility font scale tightens vertical spacing safely
        val accessiblePhone = computeAdaptiveLayoutMetrics(maxWidth = 390.dp, maxHeight = 700.dp, fontScale = 1.6f)
        assertTrue(accessiblePhone.isTightVerticalSpace)
        assertEquals(88.dp, accessiblePhone.inCallAvatarSize)
    }

    @Test
    @Config(qualifiers = "w320dp-h533dp-port-hdpi", sdk = [34])
    fun mainScreen_compactPortrait_usesBottomNavBar() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = DialerViewModel(context)
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                MainScreen(
                    viewModel = viewModel,
                    onShowRestrictedSettings = {},
                    isDefaultDialer = true
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bottom_nav_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("app_navigation_rail").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-hdpi", sdk = [34])
    fun mainScreen_tabletExpanded_usesNavigationRail() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = DialerViewModel(context)
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                MainScreen(
                    viewModel = viewModel,
                    onShowRestrictedSettings = {},
                    isDefaultDialer = true
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_navigation_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_rail_item_RECENTS").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_nav_bar").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-hdpi", sdk = [34])
    fun mainScreen_landscapePhoneMultiWindowContainer_usesNavigationRail() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = DialerViewModel(context)
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Box(modifier = Modifier.requiredSize(width = 640.dp, height = 360.dp)) {
                    MainScreen(
                        viewModel = viewModel,
                        onShowRestrictedSettings = {},
                        isDefaultDialer = true
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_navigation_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_nav_bar").assertDoesNotExist()
    }

    @Test
    fun dialpadTab_compactPortraitWithLargeFont_callButtonRemainsVisible() {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = currentDensity.density, fontScale = 1.6f)
            ) {
                MyApplicationTheme(darkTheme = false) {
                    Box(modifier = Modifier.requiredSize(width = 320.dp, height = 533.dp)) {
                        DialpadTabContent(
                            inputValue = "5550199",
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
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dialpad_key_5").assertIsDisplayed()
        composeRule.onNodeWithTag("dialpad_call_button").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-hdpi", sdk = [34])
    fun dialpadTab_landscapePhoneTwoPane_rendersKeysAndCallButton() {
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Box(modifier = Modifier.requiredSize(width = 640.dp, height = 360.dp)) {
                    DialpadTabContent(
                        inputValue = "5550199",
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
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dialpad_key_5").assertIsDisplayed()
        composeRule.onNodeWithTag("dialpad_call_button").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h533dp-port-hdpi", sdk = [34])
    fun dialpadOverlay_tinyPhone_unsavedChipsAndCallButtonDisplayed() {
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Box(modifier = Modifier.requiredSize(width = 320.dp, height = 480.dp)) {
                    DialpadOverlay(
                        inputValue = "5550123",
                        onValueChange = {},
                        onClose = {},
                        onCallClick = {},
                        onSpeedDialCall = {},
                        speedDialMap = emptyMap(),
                        voicemailNumber = ""
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("overlay_create_contact_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("dialpad_key_5").assertIsDisplayed()
        composeRule.onNodeWithTag("dialpad_call_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-hdpi", sdk = [34])
    fun dialpadOverlay_landscapePhone_scrollsAndDisplaysCallButton() {
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Box(modifier = Modifier.requiredSize(width = 640.dp, height = 360.dp)) {
                    DialpadOverlay(
                        inputValue = "5550123",
                        onValueChange = {},
                        onClose = {},
                        onCallClick = {},
                        onSpeedDialCall = {},
                        speedDialMap = emptyMap(),
                        voicemailNumber = ""
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dialpad_key_5").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("dialpad_call_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun activeCall_tinyPhoneWithKeypadAndLargeFont_hangupRemainsPinnedAndVisible() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val keypadLabel = context.getString(R.string.keypad)

        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = currentDensity.density, fontScale = 1.6f)
            ) {
                MyApplicationTheme(darkTheme = false) {
                    Box(modifier = Modifier.requiredSize(width = 320.dp, height = 480.dp)) {
                        ActiveCallScreen(
                            contactName = "Alex Mercer",
                            contactNumber = "555-0188",
                            preferredSim = "SIM 1",
                            quickResponses = listOf("Busy right now."),
                            onHangUp = {},
                            onQuickDecline = {},
                            isIncoming = false,
                            isFake = true,
                            fakeState = "ACTIVE"
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hangup_button").assertIsDisplayed()
        composeRule.onNodeWithText(keypadLabel).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hangup_button").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-hdpi", sdk = [34])
    fun activeCall_landscapePhoneTwoPane_incomingAnswerAndHangupDisplayed() {
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Box(modifier = Modifier.requiredSize(width = 640.dp, height = 360.dp)) {
                    ActiveCallScreen(
                        contactName = "John Smith",
                        contactNumber = "555-0144",
                        preferredSim = "SIM 2",
                        quickResponses = listOf("Sorry, busy."),
                        onHangUp = {},
                        onQuickDecline = {},
                        isIncoming = true,
                        isFake = true,
                        fakeState = "RINGING"
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("answer_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hangup_button").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-hdpi", sdk = [34])
    fun activeCall_tabletTwoPane_hangupDisplayed() {
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Box(modifier = Modifier.requiredSize(width = 900.dp, height = 800.dp)) {
                    ActiveCallScreen(
                        contactName = "Jane Doe",
                        contactNumber = "555-0199",
                        preferredSim = "SIM 1",
                        quickResponses = listOf("In a meeting."),
                        onHangUp = {},
                        onQuickDecline = {},
                        isIncoming = false,
                        isFake = true,
                        fakeState = "ACTIVE"
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hangup_button").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h533dp-port-hdpi", sdk = [34])
    fun simSelectDialog_tinyPhoneLargeFont_displaysBothSimsAndCancel() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val cancelLabel = context.getString(R.string.btn_cancel)

        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = currentDensity.density, fontScale = 1.6f)
            ) {
                MyApplicationTheme(darkTheme = false) {
                    SimSelectDialog(
                        context = context,
                        pendingCallNumber = "+1 555-0100",
                        onDismiss = {},
                        onSimSelected = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("SIM 1").assertIsDisplayed()
        composeRule.onNodeWithText("SIM 2").assertIsDisplayed()
        composeRule.onNodeWithText(cancelLabel).assertIsDisplayed()
    }

    @Test
    fun mainScreen_rtlLayoutDirection_rendersCleanlyWithoutClipping() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = DialerViewModel(context)
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MyApplicationTheme(darkTheme = false) {
                    MainScreen(
                        viewModel = viewModel,
                        onShowRestrictedSettings = {},
                        isDefaultDialer = true
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bottom_nav_bar").assertIsDisplayed()
    }
}
