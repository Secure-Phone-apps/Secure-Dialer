/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.util

import android.content.Context
import android.telecom.Call
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

    /**
     * Extracts a normalized, clean telephone number from a Call object or handle string.
     */
    fun extractPhoneNumber(call: Call?): String {
        if (call == null) return ""
        val handleUri = call.details?.handle
        val schemeSpecific = handleUri?.schemeSpecificPart
        if (!schemeSpecific.isNullOrBlank()) return schemeSpecific
        val uriStr = handleUri?.toString() ?: ""
        if (uriStr.isNotBlank()) {
            val stripped = uriStr.removePrefix("tel:").removePrefix("TEL:")
            val clean = stripped.substringBefore('?').substringBefore(';')
            if (clean.isNotBlank()) return clean
        }
        val gatewayUri = call.details?.gatewayInfo?.originalAddress
        val gatewaySpecific = gatewayUri?.schemeSpecificPart
        if (!gatewaySpecific.isNullOrBlank()) return gatewaySpecific
        return ""
    }

    /**
     * Compares two phone numbers or caller IDs defensively to determine if they refer to the SAME caller.
     * Handles:
     * - Null / blank checks
     * - Exact string matches
     * - URI prefixes (tel:, sip:)
     * - Android's native PhoneNumberUtils.compare (with Context if available)
     * - Normalized digit-only comparisons stripping all punctuation, formatting, and international trunk/country prefixes
     * - Suffix matching for national vs international numbers (e.g. matching last 7 to 10 digits)
     */
    fun isSameCaller(
        num1: String?,
        num2: String?,
        context: Context? = null
    ): Boolean {
        if (num1.isNullOrBlank() || num2.isNullOrBlank()) {
            return false
        }
        val clean1 = num1.trim().removePrefix("tel:").removePrefix("TEL:").substringBefore('?').substringBefore(';')
        val clean2 = num2.trim().removePrefix("tel:").removePrefix("TEL:").substringBefore('?').substringBefore(';')
        if (clean1.equals(clean2, ignoreCase = true)) return true

        // 1. Android native PhoneNumberUtils comparison
        try {
            if (context != null) {
                if (PhoneNumberUtils.compare(context, clean1, clean2)) return true
            }
            if (PhoneNumberUtils.compare(clean1, clean2)) return true
        } catch (_: Exception) {}

        // 2. Normalized digit-only comparison (strip dashes, spaces, brackets, dots, plus)
        val digits1 = clean1.filter { it.isDigit() }
        val digits2 = clean2.filter { it.isDigit() }
        if (digits1.isNotEmpty() && digits1 == digits2) return true

        // 3. Significant digit suffix comparison (handles international vs local dialing, e.g., +1 555-234-5678 vs 5552345678)
        val minLen = minOf(digits1.length, digits2.length)
        if (minLen >= 7) {
            val compareLen = minOf(minLen, 10)
            val suffix1 = digits1.takeLast(compareLen)
            val suffix2 = digits2.takeLast(compareLen)
            if (suffix1 == suffix2) return true
        }

        return false
    }

    /**
     * Checks if a Call represents the same caller as another Call or reference number/callerName.
     */
    fun isSameCallOrCaller(
        callA: Call?,
        callB: Call?,
        referenceNumber: String? = null,
        referenceName: String? = null,
        context: Context? = null
    ): Boolean {
        if (callA == null && callB == null) return false
        if (callA != null && callB != null && callA === callB) return true

        val numA = extractPhoneNumber(callA).ifBlank { referenceNumber ?: "" }
        val numB = extractPhoneNumber(callB).ifBlank { referenceNumber ?: "" }

        if (numA.isNotBlank() && numB.isNotBlank() && isSameCaller(numA, numB, context)) {
            return true
        }

        // Check callerDisplayName / CNAP if available
        val nameA = callA?.details?.callerDisplayName?.takeIf { it.isNotBlank() } ?: referenceName ?: ""
        val nameB = callB?.details?.callerDisplayName?.takeIf { it.isNotBlank() } ?: referenceName ?: ""
        if (nameA.isNotBlank() && nameB.isNotBlank() && nameA.equals(nameB, ignoreCase = true)) {
            return true
        }

        return false
    }
}
