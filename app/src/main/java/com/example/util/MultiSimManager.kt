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
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
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
 * World-class Dual-SIM / Multi-SIM Carrier Management.
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

            activeSubs.forEachIndexed { index, subInfo ->
                val handle = handles.find { h ->
                    h.id.contains(subInfo.subscriptionId.toString()) || 
                    (!subInfo.iccId.isNullOrBlank() && h.id.contains(subInfo.iccId))
                } ?: handles.getOrNull(index)

                val displayLabel = subInfo.displayName?.toString()?.takeIf { it.isNotBlank() } ?: "SIM ${index + 1}"
                val carrier = subInfo.carrierName?.toString()?.takeIf { it.isNotBlank() } ?: "Carrier"

                val simInfo = SimAccountInfo(
                    slotIndex = index,
                    subscriptionId = subInfo.subscriptionId,
                    displayName = displayLabel,
                    carrierName = carrier,
                    number = subInfo.number ?: "",
                    accountHandle = handle
                )
                simList.add(simInfo)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Ensure at least 2 SIM options exist so Dual SIM (SIM 1 & SIM 2) selection is always available
        if (simList.isEmpty()) {
            simList.add(SimAccountInfo(0, 1, "SIM 1", "SIM 1 Carrier", "", null))
            simList.add(SimAccountInfo(1, 2, "SIM 2", "SIM 2 Carrier", "", null))
        } else if (simList.size == 1) {
            val first = simList[0]
            val secondSlot = if (first.slotIndex == 0) 1 else 0
            val secondSub = if (first.subscriptionId == 1) 2 else 1
            simList.add(
                SimAccountInfo(
                    slotIndex = secondSlot,
                    subscriptionId = secondSub,
                    displayName = "SIM 2",
                    carrierName = "SIM 2 Carrier",
                    number = "",
                    accountHandle = null
                )
            )
        }

        return simList
    }

    @SuppressLint("MissingPermission")
    fun getPhysicalSimCount(context: Context): Int {
        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val activeSubs = subscriptionManager?.activeSubscriptionInfoList
            if (activeSubs != null && activeSubs.isNotEmpty()) {
                return activeSubs.size
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 1
    }
}
