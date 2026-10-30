/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.PostCallData
import com.example.util.PhoneFormatHelper
import com.example.util.UssdManager
import com.example.util.WhatsAppHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TelephonyEnhancementsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testUssdAndMmiCodeDetection() {
        assertTrue(UssdManager.isMmiOrUssdCode("*#06#"))
        assertTrue(UssdManager.isMmiOrUssdCode("*123#"))
        assertTrue(UssdManager.isMmiOrUssdCode("*#21#"))
        assertTrue(UssdManager.isMmiOrUssdCode("#31#"))

        assertFalse(UssdManager.isMmiOrUssdCode("911"))
        assertFalse(UssdManager.isMmiOrUssdCode("+1234567890"))
        assertFalse(UssdManager.isMmiOrUssdCode("5551234"))
        assertFalse(UssdManager.isMmiOrUssdCode(""))
    }

    @Test
    fun testUssdImeiInterception() {
        val intercepted = UssdManager.handleUssdOrMmi(context, "*#06#")
        assertTrue("IMEI code *#06# should be intercepted internally", intercepted)

        val dialogData = UssdManager.ussdDialogData.value
        assertNotNull("USSD Dialog data must not be null after *#06# interception", dialogData)
        assertEquals("*#06#", dialogData?.code)
        assertEquals("Device Information", dialogData?.title)

        UssdManager.dismissDialog()
        assertNull(UssdManager.ussdDialogData.value)
    }

    @Test
    fun testWhatsAppE164Normalization() {
        val normalizedWithPlus = WhatsAppHelper.formatToE164Digits(context, "+1 (555) 234-5678")
        assertEquals("15552345678", normalizedWithPlus)

        val rawDigitsOnly = WhatsAppHelper.formatToE164Digits(context, "9876543210")
        assertFalse("Formatted digits should not be empty", rawDigitsOnly.isBlank())

        val emptyDigits = WhatsAppHelper.formatToE164Digits(context, "   ")
        assertEquals("", emptyDigits)
    }

    @Test
    fun testPhoneFormatHelperDisplayFormatting() {
        val formattedUs = PhoneFormatHelper.formatDisplayNumber("2125551234", "US")
        assertTrue("US number should be formatted with dashes or parentheses", formattedUs.contains("-") || formattedUs.contains(" "))

        val mmiCode = PhoneFormatHelper.formatDisplayNumber("*#06#", "US")
        assertEquals("*#06#", mmiCode)

        val emptyFormatted = PhoneFormatHelper.formatDisplayNumber("", "US")
        assertEquals("", emptyFormatted)
    }

    @Test
    fun testPostCallDataModelIntegrity() {
        val postCall = PostCallData(
            number = "+15550001111",
            name = "Test Caller",
            photoUri = "",
            durationSeconds = 85L,
            isUnsaved = true
        )

        assertEquals("+15550001111", postCall.number)
        assertEquals("Test Caller", postCall.name)
        assertEquals(85L, postCall.durationSeconds)
        assertTrue(postCall.isUnsaved)
    }

    @Test
    fun testCallManagerWaitingCallReset() {
        CallManager.updateWaitingCall(null)
        assertNull("Waiting call must be null when updated with null", CallManager.waitingCall.value)
    }

    @Test
    fun testIsSameCallerWithFormattingAndPrefixes() {
        // Same number formatted vs raw
        assertTrue(PhoneFormatHelper.isSameCaller("+1 (555) 234-5678", "5552345678", context))
        assertTrue(PhoneFormatHelper.isSameCaller("5552345678", "+15552345678", context))
        assertTrue(PhoneFormatHelper.isSameCaller("+91 98765 43210", "09876543210", context))
        assertTrue(PhoneFormatHelper.isSameCaller("tel:+15552345678", "5552345678", context))
        assertTrue(PhoneFormatHelper.isSameCaller("1234567890", "1234567890", context))
    }

    @Test
    fun testIsSameCallerRejectionForDistinctNumbers() {
        // Distinct callers should NOT match
        assertFalse(PhoneFormatHelper.isSameCaller("5551234", "5559876", context))
        assertFalse(PhoneFormatHelper.isSameCaller("+15551234567", "+15559876543", context))
        assertFalse(PhoneFormatHelper.isSameCaller("", "5551234", context))
        assertFalse(PhoneFormatHelper.isSameCaller(null, "5551234", context))
        assertFalse(PhoneFormatHelper.isSameCaller("", "", context))
    }
}
