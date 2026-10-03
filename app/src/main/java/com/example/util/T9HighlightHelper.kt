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

package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import java.text.Normalizer

object T9HighlightHelper {

    /**
     * Converts a single character to its T9 keypad digit while strictly preserving 1-to-1 character index length.
     * Supports accented Latin, Cyrillic, and European diacritics.
     */
    private fun charToT9Digit(c: Char): Char {
        val upper = c.uppercaseChar()
        if (upper in 'A'..'Z') {
            return when (upper) {
                in 'A'..'C' -> '2'
                in 'D'..'F' -> '3'
                in 'G'..'I' -> '4'
                in 'J'..'L' -> '5'
                in 'M'..'O' -> '6'
                in 'P'..'S' -> '7'
                in 'T'..'V' -> '8'
                else -> '9'
            }
        }

        // Decompose single character to extract base Latin letter without altering string length
        val decomposed = Normalizer.normalize(upper.toString(), Normalizer.Form.NFD)
        val baseChar = decomposed.firstOrNull { it in 'A'..'Z' } ?: return upper

        return when (baseChar) {
            in 'A'..'C' -> '2'
            in 'D'..'F' -> '3'
            in 'G'..'I' -> '4'
            in 'J'..'L' -> '5'
            in 'M'..'O' -> '6'
            in 'P'..'S' -> '7'
            in 'T'..'V' -> '8'
            else -> '9'
        }
    }

    /**
     * Converts a name string to its corresponding T9 keypad digit sequence (1-to-1 character index mapping).
     */
    fun nameToT9(name: String): String {
        return buildString(name.length) {
            for (char in name) {
                append(charToT9Digit(char))
            }
        }
    }

    /**
     * Highlights the matching substring in [displayName] using [query] (via direct name match or T9 mapping).
     */
    fun highlightName(
        displayName: String,
        query: String,
        highlightColor: Color,
        defaultColor: Color,
        highlightFontWeight: FontWeight = FontWeight.SemiBold,
        defaultFontWeight: FontWeight = FontWeight.Medium
    ): AnnotatedString {
        if (displayName.isEmpty()) return AnnotatedString("")
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return buildAnnotatedString {
                append(displayName)
                addStyle(SpanStyle(color = defaultColor, fontWeight = defaultFontWeight), 0, displayName.length)
            }
        }

        // 1. Direct substring match (case-insensitive)
        val directIndex = displayName.indexOf(cleanQuery, ignoreCase = true)
        val matchRange = if (directIndex >= 0) {
            directIndex until (directIndex + cleanQuery.length).coerceAtMost(displayName.length)
        } else {
            // 2. 1-to-1 index-safe T9 mapping match
            val t9 = nameToT9(displayName)
            val t9Index = t9.indexOf(cleanQuery, ignoreCase = true)
            if (t9Index >= 0) {
                t9Index until (t9Index + cleanQuery.length).coerceAtMost(displayName.length)
            } else {
                null
            }
        }

        return buildAnnotatedString {
            append(displayName)
            addStyle(SpanStyle(color = defaultColor, fontWeight = defaultFontWeight), 0, displayName.length)
            if (matchRange != null && matchRange.first < displayName.length) {
                val start = matchRange.first
                val end = (matchRange.last + 1).coerceAtMost(displayName.length)
                if (start < end) {
                    addStyle(
                        SpanStyle(color = highlightColor, fontWeight = highlightFontWeight),
                        start,
                        end
                    )
                }
            }
        }
    }

    /**
     * Highlights the matching digit sequence in [number] based on [query].
     */
    fun highlightNumber(
        number: String,
        query: String,
        highlightColor: Color,
        defaultColor: Color,
        highlightFontWeight: FontWeight = FontWeight.SemiBold,
        defaultFontWeight: FontWeight = FontWeight.Normal
    ): AnnotatedString {
        if (number.isEmpty()) return AnnotatedString("")
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return buildAnnotatedString {
                append(number)
                addStyle(SpanStyle(color = defaultColor, fontWeight = defaultFontWeight), 0, number.length)
            }
        }

        // 1. Direct substring match
        val directIndex = number.indexOf(cleanQuery, ignoreCase = true)
        if (directIndex >= 0) {
            val end = (directIndex + cleanQuery.length).coerceAtMost(number.length)
            return buildAnnotatedString {
                append(number)
                addStyle(SpanStyle(color = defaultColor, fontWeight = defaultFontWeight), 0, number.length)
                if (directIndex < end) {
                    addStyle(SpanStyle(color = highlightColor, fontWeight = highlightFontWeight), directIndex, end)
                }
            }
        }

        // 2. Formatted number match (e.g. query "55512" matches "+1 (555) 123-4567")
        val cleanQueryDigits = cleanQuery.filter { it.isDigit() || it == '+' }
        if (cleanQueryDigits.isNotEmpty()) {
            val digitIndices = IntArray(number.length)
            var digitCount = 0
            val cleanNumber = buildString(number.length) {
                for (i in number.indices) {
                    val c = number[i]
                    if (c.isDigit() || c == '+') {
                        digitIndices[digitCount++] = i
                        append(c)
                    }
                }
            }

            val matchInClean = cleanNumber.indexOf(cleanQueryDigits, ignoreCase = true)
            if (matchInClean >= 0 && matchInClean + cleanQueryDigits.length <= digitCount) {
                val startCharIdx = digitIndices[matchInClean]
                val endCharIdx = (digitIndices[matchInClean + cleanQueryDigits.length - 1] + 1).coerceAtMost(number.length)
                return buildAnnotatedString {
                    append(number)
                    addStyle(SpanStyle(color = defaultColor, fontWeight = defaultFontWeight), 0, number.length)
                    if (startCharIdx < endCharIdx) {
                        addStyle(
                            SpanStyle(color = highlightColor, fontWeight = highlightFontWeight),
                            startCharIdx,
                            endCharIdx
                        )
                    }
                }
            }
        }

        return buildAnnotatedString {
            append(number)
            addStyle(SpanStyle(color = defaultColor, fontWeight = defaultFontWeight), 0, number.length)
        }
    }
}