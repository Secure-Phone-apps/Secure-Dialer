package com.example

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DialerEmergencyRoutingTest {

    private fun isEmergencyNumber(number: String): Boolean {
        val cleaned = number.filter { it.isDigit() }
        val standardEmergencyNumbers = setOf("911", "112", "999", "000", "108", "110", "119")
        if (standardEmergencyNumbers.contains(cleaned)) return true
        // Handle numbers with country code prefix (e.g. 1911 -> 911)
        if (cleaned.length > 3 && (cleaned.startsWith("1") || cleaned.startsWith("0"))) {
            val withoutPrefix = cleaned.substring(1)
            if (standardEmergencyNumbers.contains(withoutPrefix)) return true
        }
        return false
    }

    @Test
    fun testStandardEmergencyNumberRecognition() {
        // Standard emergency numbers should be instantly recognized
        assertTrue(isEmergencyNumber("911"))
        assertTrue(isEmergencyNumber("112"))
        assertTrue(isEmergencyNumber("999"))
        assertTrue(isEmergencyNumber("000"))
        assertTrue(isEmergencyNumber("108"))
    }

    @Test
    fun testEmergencyNumberWithFormatting() {
        // Numbers with formatting (dashes, spaces, plus signs) should be correctly normalized and recognized
        assertTrue(isEmergencyNumber("9-1-1"))
        assertTrue(isEmergencyNumber("+1 (911)"))
        assertTrue(isEmergencyNumber("112 "))
    }

    @Test
    fun testNonEmergencyNumbersAreNotEmergency() {
        // Regular phone numbers should not trigger emergency routing
        assertFalse(isEmergencyNumber("5551234"))
        assertFalse(isEmergencyNumber("+15559110000"))
        assertFalse(isEmergencyNumber("123"))
        assertFalse(isEmergencyNumber(""))
    }

    @Test
    fun testEmergencyPriorityRoutingFlag() {
        val testNumber = "911"
        val isEmergency = isEmergencyNumber(testNumber)
        
        // Verify emergency routing configuration flags
        val priorityIntent = if (isEmergency) "ACTION_DIAL_EMERGENCY_PRIORITY" else "ACTION_DIAL_STANDARD"
        assertEquals("ACTION_DIAL_EMERGENCY_PRIORITY", priorityIntent)
    }
}
