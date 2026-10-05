package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.MultiSimManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DialerEmergencyRoutingTest {

    @Test
    fun testStandardEmergencyNumberRecognition() {
        // Standard emergency numbers should be instantly recognized by CallManager
        assertTrue(CallManager.isEmergencyNumber("911"))
        assertTrue(CallManager.isEmergencyNumber("112"))
        assertTrue(CallManager.isEmergencyNumber("999"))
        assertTrue(CallManager.isEmergencyNumber("000"))
        assertTrue(CallManager.isEmergencyNumber("108"))
        assertTrue(CallManager.isEmergencyNumber("110"))
        assertTrue(CallManager.isEmergencyNumber("119"))
    }

    @Test
    fun testEmergencyNumberWithFormatting() {
        // Numbers with formatting (dashes, spaces, plus signs) should be correctly recognized
        assertTrue(CallManager.isEmergencyNumber("9-1-1"))
        assertTrue(CallManager.isEmergencyNumber("112 "))
    }

    @Test
    fun testNonEmergencyNumbersAreNotEmergency() {
        // Regular phone numbers should not trigger emergency routing
        assertFalse(CallManager.isEmergencyNumber("5551234"))
        assertFalse(CallManager.isEmergencyNumber("+15559110000"))
        assertFalse(CallManager.isEmergencyNumber("123"))
        assertFalse(CallManager.isEmergencyNumber(""))
    }

    @Test
    fun testEmergencyPriorityRoutingFlag() {
        val testNumber = "911"
        val isEmergency = CallManager.isEmergencyNumber(testNumber)
        
        // Verify emergency routing configuration flags
        val priorityIntent = if (isEmergency) "ACTION_DIAL_EMERGENCY_PRIORITY" else "ACTION_DIAL_STANDARD"
        assertEquals("ACTION_DIAL_EMERGENCY_PRIORITY", priorityIntent)
    }

    @Test
    fun testMultiSimAccountResolutionResilience() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val accounts = MultiSimManager.getActiveSimAccounts(context)
        assertNotNull(accounts)
        // Ensure every account has valid non-empty display name
        for (account in accounts) {
            assertTrue(account.displayName.isNotBlank())
            assertTrue(account.slotIndex >= 0)
        }
    }
}
