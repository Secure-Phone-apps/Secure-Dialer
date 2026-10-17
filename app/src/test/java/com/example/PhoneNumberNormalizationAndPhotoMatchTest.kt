/*
 * Copyright (C) 2026 MovStore
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PhoneNumberNormalizationAndPhotoMatchTest {

    @Test
    fun testSuffixAndFormattingNormalization() {
        val rawContactNumberWithCountryCode = "+91 98765-43210"
        val incomingFormattedNumber = "09876543210"
        val incomingRawDigits = "9876543210"

        val cleanContact = rawContactNumberWithCountryCode.filter { it.isDigit() }
        val cleanIncoming = incomingFormattedNumber.filter { it.isDigit() }
        val cleanRaw = incomingRawDigits.filter { it.isDigit() }

        // Suffix 10 match test (Indian context: 9876543210 matches +91 98765-43210)
        assertEquals(cleanContact.takeLast(10), cleanIncoming.takeLast(10))
        assertEquals(cleanContact.takeLast(10), cleanRaw.takeLast(10))
    }
}
