/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.telecom.CallAudioState
import android.telecom.PhoneAccountHandle
import androidx.test.core.app.ApplicationProvider
import com.example.util.CallAudioRouteCoordinator
import com.example.util.CallScreenPermissionHelper
import com.example.util.MultiSimManager
import com.example.util.OemFamily
import com.example.util.PhoneAccountMeta
import com.example.util.SimAccountInfo
import com.example.util.SimSubscriptionDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class P1OemTelephonyTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        CallAudioRouteCoordinator.clearUserIntent()
    }

    private fun fakeHandle(id: String): PhoneAccountHandle {
        return PhoneAccountHandle(
            ComponentName("com.android.phone", "com.android.services.telephony.TelephonyConnectionService"),
            id
        )
    }

    @Test
    fun testCarrierNamesContainingTwoNeverHijackSim2() {
        val h1 = fakeHandle("sub_10")
        val h2 = fakeHandle("sub_20")
        val sims = listOf(
            SimAccountInfo(
                slotIndex = 0,
                subscriptionId = 10,
                displayName = "O2",
                carrierName = "O2 - UK",
                number = "",
                accountHandle = h1
            ),
            SimAccountInfo(
                slotIndex = 1,
                subscriptionId = 20,
                displayName = "Tele2",
                carrierName = "Tele2",
                number = "",
                accountHandle = h2
            )
        )

        val (o2Account, o2Slot) = MultiSimManager.resolvePreferredSimTarget("O2", sims)
        assertEquals(h1, o2Account?.accountHandle)
        assertEquals("Carrier 'O2' on slot 0 must stay on SIM 1, not hijack SIM 2", 1, o2Slot)

        val (tele2Account, tele2Slot) = MultiSimManager.resolvePreferredSimTarget("Tele2", sims)
        assertEquals(h2, tele2Account?.accountHandle)
        assertEquals(2, tele2Slot)

        val (explicitSim1, s1) = MultiSimManager.resolvePreferredSimTarget("SIM 1", sims)
        assertEquals(h1, explicitSim1?.accountHandle)
        assertEquals(1, s1)

        val (explicitSim2, s2) = MultiSimManager.resolvePreferredSimTarget("SIM 2", sims)
        assertEquals(h2, explicitSim2?.accountHandle)
        assertEquals(2, s2)
    }

    @Test
    fun testExclusivePhoneAccountHandleSlotMatchingAcrossOems() {
        // Case A: MediaTek 0-based handle IDs vs 1-based labels (SIM 1 & SIM 2 must never share handle)
        val mtk0 = fakeHandle("slot_0")
        val mtk1 = fakeHandle("slot_1")
        val subsMtk = listOf(
            SimSubscriptionDescriptor(slotIndex = 0, subscriptionId = 3, displayName = "Carrier A", carrierName = "Carrier A"),
            SimSubscriptionDescriptor(slotIndex = 1, subscriptionId = 4, displayName = "Carrier B", carrierName = "Carrier B")
        )
        val matchedMtk = MultiSimManager.matchPhoneAccountsToSlots(
            subsMtk,
            listOf(
                PhoneAccountMeta(mtk0, isSimSubscription = true),
                PhoneAccountMeta(mtk1, isSimSubscription = true)
            )
        )
        assertEquals(mtk0, matchedMtk[0])
        assertEquals(mtk1, matchedMtk[1])
        assertNotEquals(matchedMtk[0], matchedMtk[1])

        // Case B: Only one PhoneAccountHandle exposed — SIM 2 must stay null instead of cloning SIM 1's handle
        val matchedPartial = MultiSimManager.matchPhoneAccountsToSlots(
            subsMtk,
            listOf(PhoneAccountMeta(mtk0, isSimSubscription = true))
        )
        assertEquals(mtk0, matchedPartial[0])
        assertNull("Unmatched SIM 2 must never borrow SIM 1's PhoneAccountHandle", matchedPartial[1])
    }

    @Test
    fun testCallLogSimSlotAttributionNoSubstringCollision() {
        val subIdMap = mapOf(
            "1" to 1,
            "12" to 2,
            "sub_1" to 1,
            "sub_12" to 2,
            "8901260000000000001" to 1,
            "8901260000000000002" to 2
        )

        // SubId "12" must resolve to slot 2, NEVER falsely matching "1" via substring contains
        assertEquals(2, MultiSimManager.resolveCallLogSimSlot(null, "12", subIdMap))
        assertEquals(2, MultiSimManager.resolveCallLogSimSlot(null, "tel:12", subIdMap))
        assertEquals(2, MultiSimManager.resolveCallLogSimSlot(null, "sub_12", subIdMap))
        assertEquals(1, MultiSimManager.resolveCallLogSimSlot(null, "1", subIdMap))
        assertEquals(2, MultiSimManager.resolveCallLogSimSlot(null, "iccid:8901260000000000002", subIdMap))
        assertEquals(2, MultiSimManager.resolveCallLogSimSlot(null, "slot_1", subIdMap))
        assertEquals(1, MultiSimManager.resolveCallLogSimSlot(null, "slot_0", subIdMap))
        // Explicit tracked slot takes precedence
        assertEquals(2, MultiSimManager.resolveCallLogSimSlot(2, "1", subIdMap))
    }

    @Test
    fun testBluetoothDisconnectPrefersWiredHeadsetOverEarpiece() {
        val wiredAndEarpieceMask = CallAudioState.ROUTE_EARPIECE or
            CallAudioState.ROUTE_WIRED_HEADSET or
            CallAudioState.ROUTE_SPEAKER
        val recovery = CallAudioRouteCoordinator.selectRecoveryRoute(
            previousRoute = CallAudioState.ROUTE_BLUETOOTH,
            currentRoute = CallAudioState.ROUTE_EARPIECE,
            supportedRouteMask = wiredAndEarpieceMask,
            userRequestedRoute = null,
            userRequestAgeMs = 10_000L
        )
        assertEquals(CallAudioState.ROUTE_WIRED_HEADSET, recovery)
    }

    @Test
    fun testBluetoothDisconnectFallsBackToEarpieceThenSpeakerOnTablet() {
        // Tablet without earpiece bit falls back safely to ROUTE_SPEAKER instead of trapping audio
        val tabletMask = CallAudioState.ROUTE_SPEAKER or CallAudioState.ROUTE_BLUETOOTH
        val tabletRecovery = CallAudioRouteCoordinator.selectRecoveryRoute(
            previousRoute = CallAudioState.ROUTE_BLUETOOTH,
            currentRoute = CallAudioState.ROUTE_EARPIECE,
            supportedRouteMask = tabletMask,
            userRequestedRoute = null,
            userRequestAgeMs = 10_000L
        )
        assertEquals(CallAudioState.ROUTE_SPEAKER, tabletRecovery)

        // Wired headset unplugged recovers to Earpiece on handset
        val handsetMask = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER
        val unplugRecovery = CallAudioRouteCoordinator.selectRecoveryRoute(
            previousRoute = CallAudioState.ROUTE_WIRED_HEADSET,
            currentRoute = CallAudioState.ROUTE_WIRED_HEADSET or CallAudioState.ROUTE_EARPIECE,
            supportedRouteMask = handsetMask,
            userRequestedRoute = null,
            userRequestAgeMs = 10_000L
        )
        assertEquals(CallAudioState.ROUTE_EARPIECE, unplugRecovery)
    }

    @Test
    fun testExplicitSpeakerSwitchIsRespectedAndNotOverridden() {
        val fullMask = CallAudioState.ROUTE_EARPIECE or
            CallAudioState.ROUTE_BLUETOOTH or
            CallAudioState.ROUTE_SPEAKER
        val recovery = CallAudioRouteCoordinator.selectRecoveryRoute(
            previousRoute = CallAudioState.ROUTE_BLUETOOTH,
            currentRoute = CallAudioState.ROUTE_SPEAKER,
            supportedRouteMask = fullMask,
            userRequestedRoute = CallAudioState.ROUTE_SPEAKER,
            userRequestAgeMs = 300L
        )
        assertNull("User-selected Speaker route must not be overridden back to Earpiece", recovery)
    }

    @Test
    fun testCallEndpointTypeMappingAndRouteMaskSynthesis() {
        assertEquals(
            CallAudioState.ROUTE_BLUETOOTH,
            CallAudioRouteCoordinator.mapEndpointTypeToRoute(CallAudioRouteCoordinator.ENDPOINT_TYPE_BLUETOOTH)
        )
        assertEquals(
            CallAudioState.ROUTE_WIRED_HEADSET,
            CallAudioRouteCoordinator.mapEndpointTypeToRoute(CallAudioRouteCoordinator.ENDPOINT_TYPE_WIRED_HEADSET)
        )
        assertEquals(
            CallAudioRouteCoordinator.ENDPOINT_TYPE_SPEAKER,
            CallAudioRouteCoordinator.mapRouteToEndpointType(CallAudioState.ROUTE_SPEAKER)
        )

        val mask = CallAudioRouteCoordinator.computeSupportedRouteMask(
            listOf(
                CallAudioRouteCoordinator.ENDPOINT_TYPE_EARPIECE,
                CallAudioRouteCoordinator.ENDPOINT_TYPE_BLUETOOTH,
                CallAudioRouteCoordinator.ENDPOINT_TYPE_SPEAKER
            )
        )
        assertTrue((mask and CallAudioState.ROUTE_EARPIECE) != 0)
        assertTrue((mask and CallAudioState.ROUTE_BLUETOOTH) != 0)
        assertTrue((mask and CallAudioState.ROUTE_SPEAKER) != 0)
        assertFalse((mask and CallAudioState.ROUTE_WIRED_HEADSET) != 0)
    }

    @Test
    @Config(sdk = [28])
    fun testClearStaleBluetoothHalSafeOnAndroid9() {
        CallAudioRouteCoordinator.clearStaleBluetoothHal(context)
    }

    @Test
    fun testOemFamilyDetectionAcrossGlobalBrands() {
        assertEquals(OemFamily.XIAOMI_HYPEROS, CallScreenPermissionHelper.detectOemFamily("Xiaomi", "Redmi"))
        assertEquals(OemFamily.XIAOMI_HYPEROS, CallScreenPermissionHelper.detectOemFamily("Xiaomi", "POCO"))
        assertEquals(OemFamily.SAMSUNG_ONEUI, CallScreenPermissionHelper.detectOemFamily("samsung", "samsung"))
        assertEquals(OemFamily.OPPO_REALME_ONEPLUS, CallScreenPermissionHelper.detectOemFamily("OnePlus", "OnePlus"))
        assertEquals(OemFamily.OPPO_REALME_ONEPLUS, CallScreenPermissionHelper.detectOemFamily("realme", "realme"))
        assertEquals(OemFamily.VIVO_IQOO, CallScreenPermissionHelper.detectOemFamily("vivo", "iQOO"))
        assertEquals(OemFamily.HUAWEI_HONOR, CallScreenPermissionHelper.detectOemFamily("HUAWEI", "HONOR"))
        assertEquals(OemFamily.TRANSSION, CallScreenPermissionHelper.detectOemFamily("TECNO MOBILE LIMITED", "TECNO"))
        assertEquals(OemFamily.ASUS, CallScreenPermissionHelper.detectOemFamily("asus", "asus"))
        assertEquals(OemFamily.LENOVO, CallScreenPermissionHelper.detectOemFamily("LENOVO", "Lenovo"))
        assertEquals(OemFamily.MOTOROLA, CallScreenPermissionHelper.detectOemFamily("motorola", "motorola"))
        assertEquals(OemFamily.NOTHING, CallScreenPermissionHelper.detectOemFamily("Nothing", "Nothing"))
        assertEquals(OemFamily.GENERIC_AOSP, CallScreenPermissionHelper.detectOemFamily("Google", "google"))

        assertTrue(CallScreenPermissionHelper.requiresAggressiveOemWhitelist(OemFamily.XIAOMI_HYPEROS))
        assertTrue(CallScreenPermissionHelper.requiresAggressiveOemWhitelist(OemFamily.SAMSUNG_ONEUI))
        assertFalse(CallScreenPermissionHelper.requiresAggressiveOemWhitelist(OemFamily.GENERIC_AOSP))
    }

    @Test
    fun testOemBackgroundIntentCandidatesIncludeOemAndAospFallbacks() {
        val xiaomiIntents = CallScreenPermissionHelper.getOemBackgroundIntentCandidates(context, OemFamily.XIAOMI_HYPEROS)
        assertTrue(xiaomiIntents.any { it.component?.packageName == "com.miui.securitycenter" })
        assertTrue(xiaomiIntents.any { it.component?.packageName == "com.miui.powerkeeper" })
        assertTrue(xiaomiIntents.any { it.action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS })

        val samsungIntents = CallScreenPermissionHelper.getOemBackgroundIntentCandidates(context, OemFamily.SAMSUNG_ONEUI)
        assertTrue(samsungIntents.any { it.component?.packageName == "com.samsung.android.lool" })
        assertTrue(samsungIntents.any { it.action == Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS })

        val oppoIntents = CallScreenPermissionHelper.getOemBackgroundIntentCandidates(context, OemFamily.OPPO_REALME_ONEPLUS)
        assertTrue(oppoIntents.any { it.component?.packageName == "com.coloros.safecenter" })
        assertTrue(oppoIntents.any { it.component?.packageName == "com.oneplus.security" })

        val huaweiIntents = CallScreenPermissionHelper.getOemBackgroundIntentCandidates(context, OemFamily.HUAWEI_HONOR)
        assertTrue(huaweiIntents.any { it.component?.packageName == "com.huawei.systemmanager" })
        assertTrue(huaweiIntents.any { it.component?.packageName == "com.hihonor.systemmanager" })

        // Does not throw even when OEM activities are absent in test sandbox
        CallScreenPermissionHelper.openOemBackgroundSettings(context, OemFamily.SAMSUNG_ONEUI)
    }
}
