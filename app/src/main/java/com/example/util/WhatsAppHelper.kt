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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.example.R
import java.util.Locale

enum class WhatsAppFlavor(
    val key: String,
    val packageName: String,
    val titleRes: Int,
    val subtitleRes: Int,
    val isBusiness: Boolean
) {
    STANDARD("STANDARD", "com.whatsapp", R.string.whatsapp_standard, R.string.whatsapp_standard_desc, false),
    BUSINESS("BUSINESS", "com.whatsapp.w4b", R.string.whatsapp_business, R.string.whatsapp_business_desc, true);

    companion object {
        fun fromKey(key: String?): WhatsAppFlavor? =
            entries.find { it.key.equals(key, ignoreCase = true) }

        fun fromPackage(pkg: String?): WhatsAppFlavor? =
            entries.find { it.packageName.equals(pkg, ignoreCase = true) }
    }
}

data class WhatsAppChooserRequest(
    val rawNumber: String,
    val customMessage: String = "",
    val availableFlavors: List<WhatsAppFlavor>,
    val onSelected: (WhatsAppFlavor, Boolean) -> Unit
)

object WhatsAppHelper {

    private const val PREFS_NAME = "dialer_prefs"
    private const val PREF_KEY_WHATSAPP_PKG = "preferred_whatsapp_package"

    val pendingChooserRequest: MutableState<WhatsAppChooserRequest?> = mutableStateOf(null)

    fun dismissChooser() {
        pendingChooserRequest.value = null
    }

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
     * Inspects which WhatsApp packages are installed on device.
     */
    fun getInstalledFlavors(context: Context): List<WhatsAppFlavor> {
        val pm = context.packageManager
        val list = mutableListOf<WhatsAppFlavor>()
        for (flavor in WhatsAppFlavor.entries) {
            try {
                pm.getPackageInfo(flavor.packageName, 0)
                list.add(flavor)
            } catch (_: Exception) {}
        }
        return list
    }

    fun isWhatsAppInstalled(context: Context): Boolean {
        return getInstalledFlavors(context).isNotEmpty()
    }

    fun isStandardInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(WhatsAppFlavor.STANDARD.packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isBusinessInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(WhatsAppFlavor.BUSINESS.packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getPreferredPackageKey(context: Context): String {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREF_KEY_WHATSAPP_PKG, "ASK") ?: "ASK"
        } catch (_: Exception) {
            "ASK"
        }
    }

    fun setPreferredPackageKey(context: Context, key: String) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_KEY_WHATSAPP_PKG, key)
                .apply()
        } catch (_: Exception) {}
    }

    /**
     * Directly launches wa.me intent targeting a specific package or system browser.
     */
    fun launchDirect(
        context: Context,
        rawNumber: String,
        customMessage: String = "",
        targetPackage: String? = null
    ) {
        val digits = formatToE164Digits(context, rawNumber)
        if (digits.isBlank()) {
            Toast.makeText(context, context.getString(R.string.unknown), Toast.LENGTH_SHORT).show()
            return
        }

        val encodedMsg = if (customMessage.isNotBlank()) "?text=${Uri.encode(customMessage)}" else ""
        val url = "https://wa.me/$digits$encodedMsg"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (!targetPackage.isNullOrBlank()) {
                setPackage(targetPackage)
            }
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback without package lock (e.g. browser fallback)
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                Toast.makeText(context, context.getString(R.string.cannot_open_whatsapp), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Smart launcher for WhatsApp chats:
     * - If 0 apps installed -> launches wa.me browser link
     * - If 1 app installed -> launches that specific package immediately (auto-detects Business vs Standard)
     * - If 2 apps installed and preference configured -> launches preferred package
     * - If 2 apps installed and preference is ASK -> shows in-app account chooser dialog
     */
    fun openWhatsAppChat(
        context: Context,
        rawNumber: String,
        customMessage: String = "",
        preferredOverride: String? = null
    ) {
        val installed = getInstalledFlavors(context)
        if (installed.isEmpty()) {
            launchDirect(context, rawNumber, customMessage, null)
            return
        }

        if (installed.size == 1) {
            // Intelligent auto-detection: launch whichever flavor is installed
            launchDirect(context, rawNumber, customMessage, installed[0].packageName)
            return
        }

        // Multi-account scenario: both Standard and Business are installed
        val activePref = preferredOverride ?: getPreferredPackageKey(context)
        val configuredFlavor = WhatsAppFlavor.fromKey(activePref)

        if (configuredFlavor != null && installed.contains(configuredFlavor)) {
            launchDirect(context, rawNumber, customMessage, configuredFlavor.packageName)
            return
        }

        // Present in-app chooser dialog
        pendingChooserRequest.value = WhatsAppChooserRequest(
            rawNumber = rawNumber,
            customMessage = customMessage,
            availableFlavors = installed,
            onSelected = { chosenFlavor, rememberChoice ->
                if (rememberChoice) {
                    setPreferredPackageKey(context, chosenFlavor.key)
                }
                launchDirect(context, rawNumber, customMessage, chosenFlavor.packageName)
                dismissChooser()
            }
        )
    }
}
