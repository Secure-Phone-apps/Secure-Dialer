/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UssdDialogData(
    val title: String,
    val message: String,
    val code: String
)

object UssdManager {

    private val _ussdDialogData = MutableStateFlow<UssdDialogData?>(null)
    val ussdDialogData: StateFlow<UssdDialogData?> = _ussdDialogData.asStateFlow()

    fun dismissDialog() {
        _ussdDialogData.value = null
    }

    /**
     * Checks if dialed string is an MMI or USSD code (e.g. *#06#, *123#, *#21#).
     */
    fun isMmiOrUssdCode(number: String): Boolean {
        val trimmed = number.trim()
        if (trimmed.isBlank()) return false
        return (trimmed.startsWith("*") || trimmed.startsWith("#")) && trimmed.endsWith("#")
    }

    /**
     * Executes the USSD code or IMEI inquiry directly.
     * Returns true if intercepted and handled internally; false if standard telephony call should proceed.
     */
    @SuppressLint("MissingPermission", "HardwareIds")
    fun handleUssdOrMmi(context: Context, code: String): Boolean {
        val trimmed = code.trim()
        if (!isMmiOrUssdCode(trimmed)) return false

        // 1. Intercept *#06# (IMEI display)
        if (trimmed == "*#06#") {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val imeiList = mutableListOf<String>()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val imei1 = tm?.getImei(0)
                    if (!imei1.isNullOrBlank()) imeiList.add("IMEI 1: $imei1")
                    val imei2 = tm?.getImei(1)
                    if (!imei2.isNullOrBlank()) imeiList.add("IMEI 2: $imei2")
                } catch (_: Exception) {}
            }

            if (imeiList.isEmpty()) {
                try {
                    @Suppress("DEPRECATION")
                    val deviceId = tm?.deviceId
                    if (!deviceId.isNullOrBlank()) {
                        imeiList.add("Device ID / MEID: $deviceId")
                    }
                } catch (_: Exception) {}
            }

            val message = if (imeiList.isNotEmpty()) {
                imeiList.joinToString("\n\n")
            } else {
                "Device Model: ${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}\nHardware: ${Build.HARDWARE}\nAndroid Version: ${Build.VERSION.RELEASE}"
            }

            _ussdDialogData.value = UssdDialogData(
                title = "Device Information",
                message = message,
                code = trimmed
            )
            return true
        }

        // 2. Carrier USSD Request (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                try {
                    val mainHandler = Handler(Looper.getMainLooper())
                    _ussdDialogData.value = UssdDialogData(
                        title = "Carrier USSD",
                        message = "Running USSD code $trimmed...",
                        code = trimmed
                    )

                    tm.sendUssdRequest(
                        trimmed,
                        object : TelephonyManager.UssdResponseCallback() {
                            override fun onReceiveUssdResponse(
                                telephonyManager: TelephonyManager,
                                request: String,
                                response: CharSequence
                            ) {
                                _ussdDialogData.value = UssdDialogData(
                                    title = "Carrier Response ($trimmed)",
                                    message = response.toString(),
                                    code = trimmed
                                )
                            }

                            override fun onReceiveUssdResponseFailed(
                                telephonyManager: TelephonyManager,
                                request: String,
                                failureCode: Int
                            ) {
                                val errorDesc = when (failureCode) {
                                    TelephonyManager.USSD_RETURN_FAILURE -> "Carrier returned failure."
                                    TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL -> "Carrier USSD service unavailable."
                                    else -> "USSD code failed or unsupported by SIM."
                                }
                                _ussdDialogData.value = UssdDialogData(
                                    title = "USSD Failed",
                                    message = "$errorDesc\nCode: $trimmed",
                                    code = trimmed
                                )
                            }
                        },
                        mainHandler
                    )
                    return true
                } catch (_: Exception) {
                    // Fall through to native dialed intent fallback
                }
            }
        }

        // 3. Fallback: Launch ACTION_CALL with proper Uri.fromParts encoding for '#' character
        try {
            val uri = Uri.fromParts("tel", trimmed, null)
            val intent = Intent(Intent.ACTION_CALL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return true
        } catch (_: Exception) {
            return false
        }
    }
}
