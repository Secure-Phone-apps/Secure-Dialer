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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager

data class SimAccountInfo(
    val slotIndex: Int,
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val number: String,
    val accountHandle: PhoneAccountHandle?
)

/**
 * Dual-SIM / Multi-SIM Carrier Management.
 * Dynamically queries SubscriptionManager and TelecomManager for active phone accounts.
 */
object MultiSimManager {

    @SuppressLint("MissingPermission")
    fun getActiveSimAccounts(context: Context): List<SimAccountInfo> {
        val simList = mutableListOf<SimAccountInfo>()
        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

            val handles = telecomManager?.callCapablePhoneAccounts ?: emptyList()
            val activeSubs = subscriptionManager?.activeSubscriptionInfoList ?: emptyList()

            for (subInfo in activeSubs) {
                // FIXED: Use actual hardware slot index (subInfo.simSlotIndex) instead of loop index
                val hardwareSlot = subInfo.simSlotIndex

                val handle = handles.find { h ->
                    h.id.contains(subInfo.subscriptionId.toString()) || 
                    (!subInfo.iccId.isNullOrBlank() && h.id.contains(subInfo.iccId))
                } ?: handles.getOrNull(hardwareSlot)

                val displayLabel = subInfo.displayName?.toString()?.takeIf { it.isNotBlank() } ?: "SIM ${hardwareSlot + 1}"
                val carrier = subInfo.carrierName?.toString()?.takeIf { it.isNotBlank() } ?: "Carrier"

                // FIXED: Use modern API 33+ getPhoneNumber with safe fallback to avoid OEM SecurityExceptions
                val phoneNum = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        subscriptionManager?.getPhoneNumber(subInfo.subscriptionId) ?: ""
                    } else {
                        @Suppress("DEPRECATION")
                        subInfo.number ?: ""
                    }
                } catch (_: Exception) {
                    ""
                }

                simList.add(
                    SimAccountInfo(
                        slotIndex = hardwareSlot,
                        subscriptionId = subInfo.subscriptionId,
                        displayName = displayLabel,
                        carrierName = carrier,
                        number = phoneNum,
                        accountHandle = handle
                    )
                )
            }
        } catch (_: Exception) {}

        // FIXED: Never fabricate fake "Phantom SIMs" with null handles. Return real hardware state.
        return simList.sortedBy { it.slotIndex }
    }

    @SuppressLint("MissingPermission")
    fun getPhysicalSimCount(context: Context): Int {
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            sm?.activeSubscriptionInfoList?.size ?: 0
        } catch (_: Exception) {
            0
        }
    }
}