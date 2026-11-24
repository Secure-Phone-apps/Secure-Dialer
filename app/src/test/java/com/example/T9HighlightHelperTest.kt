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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.util.T9HighlightHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class T9HighlightHelperTest {

    private val primaryColor = Color(0xFF6750A4)
    private val defaultOnSurface = Color(0xFF1C1B1F)
    private val defaultOnSurfaceVariant = Color(0xFF49454F)

    @Test
    fun testNameToT9Conversion() {
        assertEquals("5646", T9HighlightHelper.nameToT9("John"))
        assertEquals("5646 363", T9HighlightHelper.nameToT9("John Doe"))
        assertEquals("22233344455566677778889999", T9HighlightHelper.nameToT9("abcdefghijklmnopqrstuvwxyz"))
        assertEquals("12345!@#", T9HighlightHelper.nameToT9("12345!@#"))
    }

    @Test
    fun testHighlightName_T9PrefixMatch() {
        val displayName = "Alice Smith"
        // "25423" corresponds to "ALICE" (2=A, 5=L, 4=I, 2=C, 3=E)
        val query = "25423"
        val result = T9HighlightHelper.highlightName(
            displayName = displayName,
            query = query,
            highlightColor = primaryColor,
            defaultColor = defaultOnSurface
        )

        assertEquals("Alice Smith", result.text)
        val styles = result.spanStyles
        assertTrue("Should have span styles", styles.isNotEmpty())

        val highlightedSpan = styles.find { it.item.color == primaryColor }
        assertNotNull("Should have primary highlighted span", highlightedSpan)
        assertEquals(0, highlightedSpan?.start)
        assertEquals(5, highlightedSpan?.end)
        assertEquals(FontWeight.SemiBold, highlightedSpan?.item?.fontWeight)
    }

    @Test
    fun testHighlightName_T9MiddleMatch() {
        val displayName = "John Doe"
        // "363" corresponds to "DOE" (3=D, 6=O, 3=E)
        val query = "363"
        val result = T9HighlightHelper.highlightName(
            displayName = displayName,
            query = query,
            highlightColor = primaryColor,
            defaultColor = defaultOnSurface
        )

        assertEquals("John Doe", result.text)
        val highlightedSpan = result.spanStyles.find { it.item.color == primaryColor }
        assertNotNull("Should have primary highlighted span", highlightedSpan)
        assertEquals(5, highlightedSpan?.start)
        assertEquals(8, highlightedSpan?.end)
    }

    @Test
    fun testHighlightName_DirectSubstringMatch() {
        val displayName = "Alexander Great"
        val query = "Alex"
        val result = T9HighlightHelper.highlightName(
            displayName = displayName,
            query = query,
            highlightColor = primaryColor,
            defaultColor = defaultOnSurface
        )

        assertEquals("Alexander Great", result.text)
        val highlightedSpan = result.spanStyles.find { it.item.color == primaryColor }
        assertNotNull(highlightedSpan)
        assertEquals(0, highlightedSpan?.start)
        assertEquals(4, highlightedSpan?.end)
    }

    @Test
    fun testHighlightNumber_DirectMatch() {
        val number = "+1 (555) 123-4567"
        val query = "123"
        val result = T9HighlightHelper.highlightNumber(
            number = number,
            query = query,
            highlightColor = primaryColor,
            defaultColor = defaultOnSurfaceVariant
        )

        assertEquals(number, result.text)
        val highlightedSpan = result.spanStyles.find { it.item.color == primaryColor }
        assertNotNull(highlightedSpan)
        val startIndex = number.indexOf("123")
        assertEquals(startIndex, highlightedSpan?.start)
        assertEquals(startIndex + 3, highlightedSpan?.end)
    }

    @Test
    fun testHighlightNumber_FormattedMatch() {
        val number = "+1 (555) 123-4567"
        // Query digits spanning across parentheses and spaces
        val query = "555123"
        val result = T9HighlightHelper.highlightNumber(
            number = number,
            query = query,
            highlightColor = primaryColor,
            defaultColor = defaultOnSurfaceVariant
        )

        assertEquals(number, result.text)
        val highlightedSpan = result.spanStyles.find { it.item.color == primaryColor }
        assertNotNull(highlightedSpan)
        // In "+1 (555) 123-4567", "555) 123" spans from index 4 to 12
        val startExpected = number.indexOf("555")
        val endExpected = number.indexOf("123") + 3
        assertEquals(startExpected, highlightedSpan?.start)
        assertEquals(endExpected, highlightedSpan?.end)
    }

    @Test
    fun testHighlight_EmptyOrBlankQuery() {
        val displayName = "Robert Paulson"
        val resultEmpty = T9HighlightHelper.highlightName(
            displayName = displayName,
            query = "",
            highlightColor = primaryColor,
            defaultColor = defaultOnSurface
        )
        assertEquals("Robert Paulson", resultEmpty.text)
        val highlightedSpanEmpty = resultEmpty.spanStyles.find { it.item.color == primaryColor }
        assertNull("Empty query should have no highlight span", highlightedSpanEmpty)

        val resultBlank = T9HighlightHelper.highlightName(
            displayName = displayName,
            query = "   ",
            highlightColor = primaryColor,
            defaultColor = defaultOnSurface
        )
        assertEquals("Robert Paulson", resultBlank.text)
        val highlightedSpanBlank = resultBlank.spanStyles.find { it.item.color == primaryColor }
        assertNull("Blank query should have no highlight span", highlightedSpanBlank)
    }

    @Test
    fun testHighlight_NoMatchSafety() {
        val displayName = "Alice"
        val query = "999999" // Does not match Alice in T9 or direct
        val result = T9HighlightHelper.highlightName(
            displayName = displayName,
            query = query,
            highlightColor = primaryColor,
            defaultColor = defaultOnSurface
        )
        assertEquals("Alice", result.text)
        val highlightedSpan = result.spanStyles.find { it.item.color == primaryColor }
        assertNull("No match should have no primary highlight span", highlightedSpan)
    }

    @Test
    fun testMultiScriptT9MappingAndLengthPreservation() {
        // Cyrillic: Иван (И=4, в=2, а=2, н=5) -> "4225"
        val cyrillicName = "Иван"
        val cyrillicT9 = T9HighlightHelper.nameToT9(cyrillicName)
        assertEquals("4225", cyrillicT9)
        assertEquals(cyrillicName.length, cyrillicT9.length)

        // Arabic: أحمد (أ=2, ح=3, م=8, د=4) -> "2384"
        val arabicName = "أحمد"
        val arabicT9 = T9HighlightHelper.nameToT9(arabicName)
        assertEquals("2384", arabicT9)
        assertEquals(arabicName.length, arabicT9.length)

        // Greek: Νίκος (Ν=6, ί=4, κ=5, ο=6, ς=7) -> "64567"
        val greekName = "Νίκος"
        val greekT9 = T9HighlightHelper.nameToT9(greekName)
        assertEquals("64567", greekT9)
        assertEquals(greekName.length, greekT9.length)

        // Devanagari: अमन (अ=2, म=6, न=5) -> "265"
        val hindiName = "अमन"
        val hindiT9 = T9HighlightHelper.nameToT9(hindiName)
        assertEquals("265", hindiT9)
        assertEquals(hindiName.length, hindiT9.length)

        // Turkish: Barış (B=2, a=2, r=7, ı=4, ş=7) -> "22747"
        val turkishName = "Barış"
        val turkishT9 = T9HighlightHelper.nameToT9(turkishName)
        assertEquals("22747", turkishT9)
        assertEquals(turkishName.length, turkishT9.length)

        // Japanese Kana: さくら (さ=3, く=2, ら=8) -> "328"
        val kanaName = "さくら"
        val kanaT9 = T9HighlightHelper.nameToT9(kanaName)
        assertEquals("328", kanaT9)
        assertEquals(kanaName.length, kanaT9.length)

        // Full-width Latin: Ｊｏｈｎ -> "5646"
        val fullWidth = "Ｊｏｈｎ"
        val fullWidthT9 = T9HighlightHelper.nameToT9(fullWidth)
        assertEquals("5646", fullWidthT9)
        assertEquals(fullWidth.length, fullWidthT9.length)
    }

    @Test
    fun testMultiScriptHighlightName() {
        val displayName = "Barış Özcan"
        val query = "22747" // Barış
        val result = T9HighlightHelper.highlightName(
            displayName = displayName,
            query = query,
            highlightColor = primaryColor,
            defaultColor = defaultOnSurface
        )
        val highlightedSpan = result.spanStyles.find { it.item.color == primaryColor }
        assertNotNull("Should highlight Barış via multi-script T9", highlightedSpan)
        assertEquals(0, highlightedSpan?.start)
        assertEquals(5, highlightedSpan?.end)
    }
}
