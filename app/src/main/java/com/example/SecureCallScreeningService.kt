/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.data.AppDatabase
import com.example.util.BlockedNumberContractManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Native Android Telecom CallScreeningService (API 24+ / Android 7-17+).
 * Screens incoming calls against the local encrypted Blocklist, Spam Database,
 * and system BlockedNumberContract before the ringer or lockscreen wakes up.
 * Strictly offline with zero INTERNET permission.
 */
class SecureCallScreeningService : CallScreeningService() {

    private val screeningScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
                respondToCall(callDetails, CallResponse.Builder().build())
                return
            }
        }

        val rawNumber = callDetails.handle?.schemeSpecificPart.orEmpty().trim()
        val presentation = callDetails.handlePresentation
        val isEmergencyCallback = callDetails.hasProperty(Call.Details.PROPERTY_EMERGENCY_CALLBACK_MODE)

        if (isEmergencyCallback || (rawNumber.isNotEmpty() && CallManager.isEmergencyNumber(rawNumber, this))) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        screeningScope.launch {
            val shouldBlock = try {
                withTimeoutOrNull(1800L) {
                    AppDatabase.retryOpenOnTransientRecoveryIfUnlocked(applicationContext)
                    val dao = AppDatabase.getDatabase(applicationContext).dialerDao()
                    BlockedNumberContractManager.shouldBlockIncomingCall(
                        context = applicationContext,
                        number = rawNumber,
                        dao = dao,
                        presentation = presentation
                    )
                } ?: false
            } catch (_: Exception) {
                false
            }

            val response = if (shouldBlock) {
                buildBlockResponse()
            } else {
                CallResponse.Builder().build()
            }

            try {
                respondToCall(callDetails, response)
            } catch (_: Exception) {}
        }
    }

    private fun buildBlockResponse(): CallResponse {
        val builder = CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setSilenceCall(true)
        }
        return builder.build()
    }

    override fun onDestroy() {
        screeningScope.cancel()
        super.onDestroy()
    }
}
