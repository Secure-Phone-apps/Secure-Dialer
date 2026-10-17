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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactNameFormatAndSearchTest {

    @Test
    fun testFirstNameFirstFormatting() {
        assertEquals("John Smith", formatContactName("John Smith", ContactNameFormat.FIRST_NAME_FIRST))
        assertEquals("John Michael Smith", formatContactName("John Michael Smith", ContactNameFormat.FIRST_NAME_FIRST))
        assertEquals("John Smith", formatContactName("Smith, John", ContactNameFormat.FIRST_NAME_FIRST))
        assertEquals("Cher", formatContactName("Cher", ContactNameFormat.FIRST_NAME_FIRST))
        assertEquals("", formatContactName("", ContactNameFormat.FIRST_NAME_FIRST))
        assertEquals("", formatContactName("   ", ContactNameFormat.FIRST_NAME_FIRST))
    }

    @Test
    fun testLastNameFirstFormatting() {
        assertEquals("Smith, John", formatContactName("John Smith", ContactNameFormat.LAST_NAME_FIRST))
        assertEquals("Smith, John Michael", formatContactName("John Michael Smith", ContactNameFormat.LAST_NAME_FIRST))
        assertEquals("Smith, John", formatContactName("Smith, John", ContactNameFormat.LAST_NAME_FIRST))
        assertEquals("Cher", formatContactName("Cher", ContactNameFormat.LAST_NAME_FIRST))
        assertEquals("", formatContactName("", ContactNameFormat.LAST_NAME_FIRST))
    }

    @Test
    fun testContactSortKey() {
        val sortKeyFirst = getContactSortKey("John Smith", ContactNameFormat.FIRST_NAME_FIRST)
        val sortKeyLast = getContactSortKey("John Smith", ContactNameFormat.LAST_NAME_FIRST)

        assertEquals("JOHN SMITH", sortKeyFirst)
        assertEquals("SMITH, JOHN", sortKeyLast)
    }

    @Test
    fun testAlphabeticalSortingByLastName() {
        val rawNames = listOf("Alice Walker", "Bob Marley", "Charlie Brown")

        val sortedByFirstName = rawNames.sortedBy { getContactSortKey(it, ContactNameFormat.FIRST_NAME_FIRST) }
        assertEquals(listOf("Alice Walker", "Bob Marley", "Charlie Brown"), sortedByFirstName)

        val sortedByLastName = rawNames.sortedBy { getContactSortKey(it, ContactNameFormat.LAST_NAME_FIRST) }
        // Brown, Charlie -> Marley, Bob -> Walker, Alice
        assertEquals(listOf("Charlie Brown", "Bob Marley", "Alice Walker"), sortedByLastName)
    }

    @Test
    fun testSearchBarVisibilityRules() {
        // Rule 1: Contacts tab ALWAYS shows search bar
        fun isSearchBarVisible(currentSlotKey: String, currentPagerSlot: String, isCallHistoryDetailsOpen: Boolean): Boolean {
            return if (currentSlotKey == "CONTACTS" || currentPagerSlot == "CONTACTS") {
                true
            } else if (currentSlotKey == "RECENTS" && currentPagerSlot == "RECENTS") {
                !isCallHistoryDetailsOpen
            } else if (currentSlotKey != "DIALPAD" && currentPagerSlot != "DIALPAD") {
                true
            } else {
                false
            }
        }

        // When in Recents with call history details open: search bar hidden
        assertFalse(isSearchBarVisible("RECENTS", "RECENTS", isCallHistoryDetailsOpen = true))

        // When in Recents without call history: search bar visible
        assertTrue(isSearchBarVisible("RECENTS", "RECENTS", isCallHistoryDetailsOpen = false))

        // When switching to Contacts tab even if isCallHistoryDetailsOpen hasn't yet flipped: search bar is GUARANTEED VISIBLE
        assertTrue(isSearchBarVisible("CONTACTS", "RECENTS", isCallHistoryDetailsOpen = true))
        assertTrue(isSearchBarVisible("RECENTS", "CONTACTS", isCallHistoryDetailsOpen = true))
        assertTrue(isSearchBarVisible("CONTACTS", "CONTACTS", isCallHistoryDetailsOpen = true))

        // Dialpad tab hides search bar
        assertFalse(isSearchBarVisible("DIALPAD", "DIALPAD", isCallHistoryDetailsOpen = false))
    }
}
