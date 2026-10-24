/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.widget.Toast
import com.example.R
import java.util.Locale

object WhatsAppHelper {

    /**
     * Normalizes a phone number to standard E.164 digits without plus for wa.me links.
     * Uses device network country or default system locale as fallback.
     */
    fun formatToE164Digits(context: Context, rawNumber: String): String {
        val trimmed = rawNumber.trim()
        if (trimmed.isBlank()) return ""

        // If number already contains international prefix '+'
        if (trimmed.startsWith("+")) {
            return trimmed.filter { it.isDigit() }
        }

        // Detect country code from SIM or system locale
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val countryIso = tm?.networkCountryIso?.takeIf { it.isNotBlank() }
            ?: tm?.simCountryIso?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().country.takeIf { it.isNotBlank() }
            ?: "US"

        val formattedE164 = try {
            PhoneNumberUtils.formatNumberToE164(trimmed, countryIso.uppercase())
        } catch (_: Exception) {
            null
        }

        return if (!formattedE164.isNullOrBlank()) {
            formattedE164.filter { it.isDigit() }
        } else {
            trimmed.filter { it.isDigit() }
        }
    }

    /**
     * Checks if WhatsApp or WhatsApp Business is installed.
     */
    fun isWhatsAppInstalled(context: Context): Boolean {
        val pm = context.packageManager
        val packages = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in packages) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (_: Exception) {}
        }
        return false
    }

    /**
     * Directly launches a WhatsApp chat window with the specified number.
     * Works with unsaved contacts without requiring them in Google Contacts!
     */
    fun openWhatsAppChat(context: Context, rawNumber: String, customMessage: String = "") {
        val digits = formatToE164Digits(context, rawNumber)
        if (digits.isBlank()) {
            Toast.makeText(context, context.getString(R.string.unknown), Toast.LENGTH_SHORT).show()
            return
        }

        val encodedMsg = if (customMessage.isNotBlank()) "?text=${Uri.encode(customMessage)}" else ""
        val url = "https://wa.me/$digits$encodedMsg"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            // Target official WhatsApp package if installed
            if (isWhatsAppInstalled(context)) {
                setPackage("com.whatsapp")
            }
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback without package lock (e.g. WhatsApp Business or browser)
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "Could not open WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
