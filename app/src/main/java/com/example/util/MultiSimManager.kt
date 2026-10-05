/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.compose.runtime.Immutable

@Immutable
data class SimAccountInfo(
    val slotIndex: Int,
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val number: String,
    val accountHandle: PhoneAccountHandle?,
    val isEmbedded: Boolean = false,
    val mccString: String = "",
    val mncString: String = "",
    val countryIso: String = ""
)

object MultiSimManager {

    @SuppressLint("MissingPermission")
    fun getActiveSimAccounts(context: Context): List<SimAccountInfo> {
        val simList = mutableListOf<SimAccountInfo>()
        try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

            val handles: List<PhoneAccountHandle> = try {
                tm?.callCapablePhoneAccounts ?: emptyList()
            } catch (_: SecurityException) {
                emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            val activeSubs: List<SubscriptionInfo> = try {
                sm?.activeSubscriptionInfoList ?: emptyList()
            } catch (_: SecurityException) {
                emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            if (activeSubs.isNotEmpty()) {
                for (subInfo in activeSubs) {
                    val hardwareSlot = subInfo.simSlotIndex.coerceAtLeast(0)
                    val subIdStr = subInfo.subscriptionId.toString()

                    val handle = handles.find { h ->
                        h.id == subIdStr ||
                        (!subInfo.iccId.isNullOrBlank() && (h.id == subInfo.iccId || h.id.startsWith(subInfo.iccId!!)))
                    } ?: handles.find { h ->
                        h.id.endsWith("_$hardwareSlot") || h.id.endsWith("_${hardwareSlot + 1}")
                    } ?: handles.getOrNull(hardwareSlot) ?: handles.firstOrNull()

                    val displayLabel = subInfo.displayName?.toString()?.takeIf { it.isNotBlank() }
                        ?: subInfo.carrierName?.toString()?.takeIf { it.isNotBlank() }
                        ?: "SIM ${hardwareSlot + 1}"
                    val carrier = subInfo.carrierName?.toString()?.takeIf { it.isNotBlank() }
                        ?: subInfo.displayName?.toString()?.takeIf { it.isNotBlank() }
                        ?: "Carrier"

                    val isEmbedded = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            subInfo.isEmbedded
                        } else false
                    } catch (_: Exception) { false }

                    val mcc = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            subInfo.mccString ?: ""
                        } else {
                            @Suppress("DEPRECATION")
                            subInfo.mcc.toString().takeIf { it != "0" } ?: ""
                        }
                    } catch (_: Exception) { "" }

                    val mnc = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            subInfo.mncString ?: ""
                        } else {
                            @Suppress("DEPRECATION")
                            subInfo.mnc.toString().takeIf { it != "0" } ?: ""
                        }
                    } catch (_: Exception) { "" }

                    val countryIso = try {
                        subInfo.countryIso ?: ""
                    } catch (_: Exception) { "" }

                    val phoneNum = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            sm?.getPhoneNumber(subInfo.subscriptionId) ?: ""
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
                            accountHandle = handle,
                            isEmbedded = isEmbedded,
                            mccString = mcc,
                            mncString = mnc,
                            countryIso = countryIso
                        )
                    )
                }
            } else if (handles.isNotEmpty()) {
                // FALLBACK: TelecomManager PhoneAccounts (Essential for eSIM & Delayed Permission states)
                handles.forEachIndexed { index, handle ->
                    simList.add(
                        SimAccountInfo(
                            slotIndex = index,
                            subscriptionId = index + 1,
                            displayName = "SIM ${index + 1}",
                            carrierName = "Mobile Network",
                            number = "",
                            accountHandle = handle,
                            isEmbedded = false
                        )
                    )
                }
            }

            // GUARANTEE: If hardware has multi-SIM capabilities or telephony phoneCount > 1, provide slots
            val phoneCount = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    telephonyManager?.activeModemCount ?: 1
                } else {
                    @Suppress("DEPRECATION")
                    telephonyManager?.phoneCount ?: 1
                }
            } catch (_: SecurityException) {
                1
            } catch (_: Exception) {
                1
            }

            if (simList.size < 2 && phoneCount >= 2) {
                while (simList.size < 2) {
                    val nextSlot = simList.size
                    simList.add(
                        SimAccountInfo(
                            slotIndex = nextSlot,
                            subscriptionId = nextSlot + 1,
                            displayName = "SIM ${nextSlot + 1}",
                            carrierName = "SIM ${nextSlot + 1}",
                            number = "",
                            accountHandle = handles.getOrNull(nextSlot),
                            isEmbedded = false
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return simList.sortedBy { it.slotIndex }
    }

    @SuppressLint("MissingPermission")
    fun getPhysicalSimCount(context: Context): Int {
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val subCount = try {
                sm?.activeSubscriptionInfoList?.size ?: 0
            } catch (_: SecurityException) { 0 } catch (_: Exception) { 0 }
            if (subCount > 0) return subCount

            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                telephonyManager?.activeModemCount ?: 1
            } else {
                @Suppress("DEPRECATION")
                telephonyManager?.phoneCount ?: 1
            }
        } catch (_: Exception) {
            1
        }
    }
}