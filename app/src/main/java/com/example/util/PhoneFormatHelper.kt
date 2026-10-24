/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.util

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.util.Locale

object PhoneFormatHelper {

    /**
     * Resolves the primary network/SIM country ISO code, falling back to system locale or "US".
     */
    fun getCountryIso(context: Context): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val iso = tm?.networkCountryIso?.takeIf { it.isNotBlank() }
            ?: tm?.simCountryIso?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().country.takeIf { it.isNotBlank() }
            ?: "US"
        return iso.uppercase(Locale.ROOT)
    }

    /**
     * Formats phone number for display without altering USSD/MMI codes (*#06#, *123#).
     */
    fun formatDisplayNumber(number: String, countryIso: String): String {
        if (number.isBlank() || number.contains('*') || number.contains('#')) {
            return number
        }
        return try {
            val formatted = PhoneNumberUtils.formatNumber(number, countryIso)
            if (!formatted.isNullOrBlank()) formatted else number
        } catch (_: Exception) {
            number
        }
    }

    /**
     * Pure Compose VisualTransformation that renders dynamic as-you-type phone grouping
     * while preserving raw digits in the underlying TextFieldValue/model and preventing cursor jumps.
     */
    fun createVisualTransformation(countryIso: String): VisualTransformation {
        return object : VisualTransformation {
            override fun filter(text: AnnotatedString): TransformedText {
                val original = text.text
                if (original.isBlank() || original.contains('*') || original.contains('#')) {
                    return TransformedText(text, OffsetMapping.Identity)
                }

                val formatted = try {
                    PhoneNumberUtils.formatNumber(original, countryIso) ?: original
                } catch (_: Exception) {
                    original
                }

                if (formatted == original) {
                    return TransformedText(text, OffsetMapping.Identity)
                }

                val offsetMapping = object : OffsetMapping {
                    override fun originalToTransformed(offset: Int): Int {
                        val clamped = offset.coerceIn(0, original.length)
                        var matchedCount = 0
                        for (i in formatted.indices) {
                            if (matchedCount == clamped) return i
                            if (formatted[i] == original[matchedCount]) {
                                matchedCount++
                            }
                        }
                        return formatted.length
                    }

                    override fun transformedToOriginal(offset: Int): Int {
                        val clamped = offset.coerceIn(0, formatted.length)
                        var matchedCount = 0
                        for (i in 0 until clamped) {
                            if (matchedCount < original.length && formatted[i] == original[matchedCount]) {
                                matchedCount++
                            }
                        }
                        return matchedCount.coerceIn(0, original.length)
                    }
                }

                return TransformedText(AnnotatedString(formatted), offsetMapping)
            }
        }
    }
}
