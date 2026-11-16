/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.app.Application
import android.telecom.Call
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.InCallBottomBar
import com.example.ui.components.InCallHeader
import com.example.ui.components.MinimizedCallBanner
import com.example.ui.theme.MyApplicationTheme
import com.example.util.OutgoingCallDisconnectCoordinator
import com.example.util.TelecomCallHandle
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeTelecomCallHandle(
    override var state: Int = Call.STATE_CONNECTING
) : TelecomCallHandle {
    override val identityKey: Any = this
    var disconnectCalls: Int = 0
        private set
    var rejectCalls: Int = 0
        private set

    override fun disconnectCall() {
        disconnectCalls++
    }

    override fun rejectCall(rejectWithMessage: Boolean, textMessage: String?) {
        rejectCalls++
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OutgoingCallDisconnectTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        CallManager.disconnectCoordinator.reset()
        CallManager.updateCall(null)
    }

    @Test
    fun preAttachHangup_latchesAndImmediatelyTerminatesOutgoingCallOnAttach() {
        var nowMs = 1_000L
        val coordinator = OutgoingCallDisconnectCoordinator(clockMs = { nowMs }, latchTtlMs = 12_000L)

        coordinator.onOutgoingCallPlaced()
        assertTrue(coordinator.isInPreAttachPlacementWindow())

        // User taps Hang Up before Telecom triggers InCallService.onCallAdded()
        coordinator.armPreAttachCancel()
        assertTrue(coordinator.hasPendingOutgoingCancel())

        // Unrelated incoming call should not consume outgoing cancel latch
        val incomingLeg = FakeTelecomCallHandle(Call.STATE_RINGING)
        assertFalse(coordinator.shouldAutoDisconnectOnAdd(incomingLeg, isOutgoing = false))
        assertTrue(coordinator.hasPendingOutgoingCancel())

        // 250ms later, Telecom attaches the delayed outgoing call in STATE_CONNECTING
        nowMs += 250L
        val outgoingLeg = FakeTelecomCallHandle(Call.STATE_CONNECTING)
        assertTrue(coordinator.shouldAutoDisconnectOnAdd(outgoingLeg, isOutgoing = true))
        assertTrue(coordinator.isPreCancelled(outgoingLeg.identityKey))
        assertTrue(coordinator.isDisconnectRequested(outgoingLeg.identityKey))
        assertFalse(coordinator.hasPendingOutgoingCancel())

        assertTrue(coordinator.terminateCallHandle(outgoingLeg))
        assertEquals(1, outgoingLeg.disconnectCalls)
        assertEquals(0, outgoingLeg.rejectCalls)
    }

    @Test
    fun preAttachLatch_expiresAfterTtlSoFutureCallsAreNotKilled() {
        var nowMs = 5_000L
        val coordinator = OutgoingCallDisconnectCoordinator(clockMs = { nowMs }, latchTtlMs = 10_000L)

        coordinator.onOutgoingCallPlaced()
        coordinator.armPreAttachCancel()

        nowMs += 15_000L
        assertFalse(coordinator.hasPendingOutgoingCancel())

        val futureOutgoing = FakeTelecomCallHandle(Call.STATE_DIALING)
        assertFalse(coordinator.shouldAutoDisconnectOnAdd(futureOutgoing, isOutgoing = true))
        assertFalse(coordinator.isDisconnectRequested(futureOutgoing.identityKey))
    }

    @Test
    fun volteTransition_fromConnectingToDialingAfterHangup_triggersDisconnectRetry() {
        val coordinator = OutgoingCallDisconnectCoordinator()
        val outgoingLeg = FakeTelecomCallHandle(Call.STATE_CONNECTING)

        coordinator.markDisconnectRequested(listOf(outgoingLeg))
        assertTrue(coordinator.terminateCallHandle(outgoingLeg))
        assertEquals(1, outgoingLeg.disconnectCalls)

        // Modem/IMS ignores early cancel and advances CONNECTING -> DIALING (ringing recipient)
        outgoingLeg.state = Call.STATE_DIALING
        val retryOnDialing = coordinator.onCallStateUpdated(outgoingLeg, Call.STATE_DIALING)
        assertTrue("Must retry disconnect if call enters DIALING after user hung up", retryOnDialing)
        coordinator.terminateCallHandle(outgoingLeg)
        assertEquals(2, outgoingLeg.disconnectCalls)

        // Modem enters DISCONNECTING -> no retry flood while tearing down cleanly
        outgoingLeg.state = Call.STATE_DISCONNECTING
        assertFalse(coordinator.onCallStateUpdated(outgoingLeg, Call.STATE_DISCONNECTING))
        assertTrue(coordinator.isDisconnectRequested(outgoingLeg.identityKey))

        // Modem finishes teardown -> tracking clears cleanly
        outgoingLeg.state = Call.STATE_DISCONNECTED
        assertFalse(coordinator.onCallStateUpdated(outgoingLeg, Call.STATE_DISCONNECTED))
        assertFalse(coordinator.isDisconnectRequested(outgoingLeg.identityKey))
        assertFalse(coordinator.hasAnyPendingDisconnects())
    }

    @Test
    fun collectDisconnectTargets_sweepsDetachedServiceCallsAndPreservesWaitingCall() {
        val coordinator = OutgoingCallDisconnectCoordinator()
        val primaryDialing = FakeTelecomCallHandle(Call.STATE_DIALING)
        val detachedServiceLeg = FakeTelecomCallHandle(Call.STATE_NEW)
        val waitingRinging = FakeTelecomCallHandle(Call.STATE_RINGING)
        val staleDisconnected = FakeTelecomCallHandle(Call.STATE_DISCONNECTED)

        val targets = coordinator.collectDisconnectTargets(
            trackedCalls = listOf(primaryDialing, waitingRinging, staleDisconnected),
            serviceCalls = listOf(primaryDialing, detachedServiceLeg, staleDisconnected),
            currentCall = primaryDialing,
            waitingCall = waitingRinging
        )

        assertEquals(2, targets.size)
        assertTrue(targets.any { it.identityKey === primaryDialing.identityKey })
        assertTrue(targets.any { it.identityKey === detachedServiceLeg.identityKey })
        assertFalse(targets.any { it.identityKey === waitingRinging.identityKey })
        assertFalse(targets.any { it.identityKey === staleDisconnected.identityKey })
    }

    @Test
    fun callManagerDisconnectWithNoAttachedCalls_armsPendingCancelLatchSafely() {
        CallManager.disconnectCoordinator.onOutgoingCallPlaced()
        assertFalse(CallManager.hasPendingOutgoingCancel())

        CallManager.disconnect()
        assertTrue(CallManager.hasPendingOutgoingCancel())
        assertEquals(Call.STATE_DISCONNECTED, CallManager.callState.value)
    }

    @Test
    fun inCallHeader_and_MinimizedBanner_and_BottomBar_renderDisconnectingAndSetupStatesProperly() {
        var retryClicks = 0
        composeRule.setContent {
            MyApplicationTheme {
                Column {
                    InCallHeader(
                        isOnHold = false,
                        callState = Call.STATE_DISCONNECTING,
                        participants = emptyList(),
                        preferredSim = "SIM 1",
                        contactName = "Alex Mercer",
                        contactNumber = "5550199",
                        heldCall = null,
                        contacts = emptyList()
                    )
                    MinimizedCallBanner(
                        contactName = "Taylor",
                        contactNumber = "5550144",
                        callState = Call.STATE_SELECT_PHONE_ACCOUNT,
                        onExpand = {},
                        onHangUp = {}
                    )
                    InCallBottomBar(
                        isIncoming = false,
                        onAnswer = {},
                        onHangUp = { retryClicks++ },
                        onToggleQuickDeclineMenu = {},
                        isDisconnecting = true
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(context.getString(R.string.call_status_ended)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.call_status_connecting)).assertIsDisplayed()
        composeRule.onNodeWithTag("hangup_button").assertIsDisplayed().performClick()
        assertEquals(1, retryClicks)
    }
}
