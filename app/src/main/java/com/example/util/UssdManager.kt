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
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UssdDialogData(
    val title: String,
    val message: String,
    val code: String,
    val isLoading: Boolean = false
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

        val appContext = context.applicationContext

        // 1. Intercept *#06# (IMEI display)
        if (trimmed == "*#06#") {
            val tm = appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val imeiList = mutableListOf<String>()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val imei1 = tm?.getImei(0)
                    if (!imei1.isNullOrBlank()) imeiList.add(context.getString(R.string.ussd_imei_line, 1, imei1))
                    val imei2 = tm?.getImei(1)
                    if (!imei2.isNullOrBlank()) imeiList.add(context.getString(R.string.ussd_imei_line, 2, imei2))
                } catch (_: Exception) {}
            }

            if (imeiList.isEmpty()) {
                try {
                    @Suppress("DEPRECATION")
                    val deviceId = tm?.deviceId
                    if (!deviceId.isNullOrBlank()) {
                        imeiList.add(context.getString(R.string.ussd_device_id_line, deviceId))
                    }
                } catch (_: Exception) {}
            }

            val message = if (imeiList.isNotEmpty()) {
                imeiList.joinToString("\n\n")
            } else {
                context.getString(
                    R.string.ussd_device_fallback_info,
                    Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
                    Build.MODEL,
                    Build.HARDWARE,
                    Build.VERSION.RELEASE
                )
            }

            _ussdDialogData.value = UssdDialogData(
                title = context.getString(R.string.ussd_device_info_title),
                message = message,
                code = trimmed
            )
            return true
        }

        // 2. Carrier USSD Request (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val tm = appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                try {
                    val mainHandler = Handler(Looper.getMainLooper())
                    val carrierResponseTitle = context.getString(R.string.ussd_carrier_response_title, trimmed)
                    val failedTitle = context.getString(R.string.ussd_failed_title)
                    val errReturnFailureMsg = context.getString(
                        R.string.ussd_failed_message,
                        context.getString(R.string.ussd_error_return_failure),
                        trimmed
                    )
                    val errServiceUnavailMsg = context.getString(
                        R.string.ussd_failed_message,
                        context.getString(R.string.ussd_error_service_unavail),
                        trimmed
                    )
                    val errUnsupportedMsg = context.getString(
                        R.string.ussd_failed_message,
                        context.getString(R.string.ussd_error_unsupported),
                        trimmed
                    )

                    _ussdDialogData.value = UssdDialogData(
                        title = context.getString(R.string.ussd_carrier_title),
                        message = context.getString(R.string.ussd_running_code, trimmed),
                        code = trimmed,
                        isLoading = true
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
                                    title = carrierResponseTitle,
                                    message = response.toString(),
                                    code = trimmed
                                )
                            }

                            override fun onReceiveUssdResponseFailed(
                                telephonyManager: TelephonyManager,
                                request: String,
                                failureCode: Int
                            ) {
                                val errorMsg = when (failureCode) {
                                    TelephonyManager.USSD_RETURN_FAILURE -> errReturnFailureMsg
                                    TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL -> errServiceUnavailMsg
                                    else -> errUnsupportedMsg
                                }
                                _ussdDialogData.value = UssdDialogData(
                                    title = failedTitle,
                                    message = errorMsg,
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
