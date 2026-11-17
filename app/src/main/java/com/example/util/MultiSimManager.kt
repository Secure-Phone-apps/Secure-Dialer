/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telecom.PhoneAccount
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

data class PhoneAccountMeta(
    val handle: PhoneAccountHandle,
    val label: String = "",
    val shortDescription: String = "",
    val isSimSubscription: Boolean = false,
    val resolvedSubscriptionId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID
)

data class SimSubscriptionDescriptor(
    val slotIndex: Int,
    val subscriptionId: Int,
    val iccId: String = "",
    val displayName: String = "",
    val carrierName: String = ""
)

object MultiSimManager {

    private val ISOLATED_SIM_DIGIT_REGEX = Regex("\\b(?:sim|slot|sub|#)?\\s*([12])\\b", RegexOption.IGNORE_CASE)
    private val TOKEN_SPLIT_REGEX = Regex("[^A-Za-z0-9_]+")
    private val OEM_SLOT2_REGEX = Regex("(?:^|[\\W_])(?:sim|slot|sub)[_-]?2(?:$|[\\W_])", RegexOption.IGNORE_CASE)
    private val OEM_SLOT1_REGEX = Regex("(?:^|[\\W_])(?:sim|slot|sub)[_-]?1(?:$|[\\W_])", RegexOption.IGNORE_CASE)

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

            val accountMetas: List<PhoneAccountMeta> = handles.map { handle ->
                buildPhoneAccountMeta(tm, telephonyManager, handle)
            }

            val activeSubs: List<SubscriptionInfo> = try {
                sm?.activeSubscriptionInfoList ?: emptyList()
            } catch (_: SecurityException) {
                emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            if (activeSubs.isNotEmpty()) {
                val descriptors = activeSubs.map { subInfo ->
                    SimSubscriptionDescriptor(
                        slotIndex = subInfo.simSlotIndex.coerceAtLeast(0),
                        subscriptionId = subInfo.subscriptionId,
                        iccId = safeIccId(subInfo),
                        displayName = subInfo.displayName?.toString().orEmpty(),
                        carrierName = subInfo.carrierName?.toString().orEmpty()
                    )
                }
                val matchedHandles = matchPhoneAccountsToSlots(descriptors, accountMetas)

                activeSubs.forEachIndexed { idx, subInfo ->
                    val hardwareSlot = subInfo.simSlotIndex.coerceAtLeast(0)
                    val handle = matchedHandles.getOrNull(idx)

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
            } else if (accountMetas.isNotEmpty()) {
                // FALLBACK: TelecomManager PhoneAccounts (Essential for eSIM & Delayed Permission states)
                val simOnlyMetas = accountMetas.filter { it.isSimSubscription }.ifEmpty { accountMetas }
                simOnlyMetas.forEachIndexed { index, meta ->
                    val label = meta.label.ifBlank { "SIM ${index + 1}" }
                    val subId = if (meta.resolvedSubscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID && meta.resolvedSubscriptionId > 0) {
                        meta.resolvedSubscriptionId
                    } else {
                        index + 1
                    }
                    simList.add(
                        SimAccountInfo(
                            slotIndex = index,
                            subscriptionId = subId,
                            displayName = label,
                            carrierName = meta.shortDescription.ifBlank { label.ifBlank { "Mobile Network" } },
                            number = "",
                            accountHandle = meta.handle,
                            isEmbedded = false
                        )
                    )
                }
            }

            // GUARANTEE: If hardware has multi-SIM capabilities or telephony phoneCount > 1, provide distinct slots
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
                val claimedHandles = simList.mapNotNull { it.accountHandle }.toHashSet()
                val existingSlots = simList.map { it.slotIndex }.toHashSet()
                var nextSlot = 0
                while (simList.size < 2) {
                    while (nextSlot in existingSlots) nextSlot++
                    val spareHandle = handles.firstOrNull { it !in claimedHandles }
                    if (spareHandle != null) claimedHandles.add(spareHandle)
                    simList.add(
                        SimAccountInfo(
                            slotIndex = nextSlot,
                            subscriptionId = nextSlot + 1,
                            displayName = "SIM ${nextSlot + 1}",
                            carrierName = "SIM ${nextSlot + 1}",
                            number = "",
                            accountHandle = spareHandle,
                            isEmbedded = false
                        )
                    )
                    existingSlots.add(nextSlot)
                    nextSlot++
                }
            }
        } catch (_: Exception) {}

        return simList.sortedBy { it.slotIndex }
    }

    @SuppressLint("MissingPermission")
    private fun buildPhoneAccountMeta(
        tm: TelecomManager?,
        telephonyManager: TelephonyManager?,
        handle: PhoneAccountHandle
    ): PhoneAccountMeta {
        val account = try {
            tm?.getPhoneAccount(handle)
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }

        val resolvedSubId = resolveSubscriptionIdForHandle(telephonyManager, handle)
        val isSim = account?.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION) ?: false
        return PhoneAccountMeta(
            handle = handle,
            label = account?.label?.toString()?.trim().orEmpty(),
            shortDescription = account?.shortDescription?.toString()?.trim().orEmpty(),
            isSimSubscription = isSim,
            resolvedSubscriptionId = resolvedSubId
        )
    }

    @SuppressLint("MissingPermission")
    private fun resolveSubscriptionIdForHandle(
        telephonyManager: TelephonyManager?,
        handle: PhoneAccountHandle
    ): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && telephonyManager != null) {
            try {
                return telephonyManager.getSubscriptionId(handle)
            } catch (_: Throwable) {}
        }
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID
    }

    private fun safeIccId(subInfo: SubscriptionInfo): String {
        val raw = try {
            subInfo.iccId?.trim().orEmpty()
        } catch (_: Exception) {
            ""
        }
        // Ignore redacted/placeholder ICCIDs on Android 10+ (e.g. "FFFFFFFFFFFFFFFF")
        if (raw.length < 4 || raw.all { it == 'F' || it == 'f' || it == '0' }) return ""
        return raw
    }

    /**
     * Deterministically and exclusively maps OS [PhoneAccountHandle]s onto SIM subscription slots.
     * Guarantees two subscriptions never collide onto the same [PhoneAccountHandle].
     */
    fun matchPhoneAccountsToSlots(
        subDescriptors: List<SimSubscriptionDescriptor>,
        accountMetas: List<PhoneAccountMeta>
    ): List<PhoneAccountHandle?> {
        if (subDescriptors.isEmpty()) return emptyList()
        if (accountMetas.isEmpty()) return List(subDescriptors.size) { null }

        val candidatePool = accountMetas.filter { it.isSimSubscription }.ifEmpty { accountMetas }
        val result = MutableList<PhoneAccountHandle?>(subDescriptors.size) { null }
        val usedHandles = HashSet<PhoneAccountHandle>()

        fun claim(index: Int, meta: PhoneAccountMeta?) {
            if (meta == null) return
            if (usedHandles.add(meta.handle)) {
                result[index] = meta.handle
            }
        }

        // Pass 1: Official TelephonyManager.getSubscriptionId(handle) mapping (API 30+)
        subDescriptors.forEachIndexed { index, sub ->
            if (sub.subscriptionId > 0) {
                val exactSub = candidatePool.firstOrNull {
                    it.handle !in usedHandles && it.resolvedSubscriptionId == sub.subscriptionId
                }
                claim(index, exactSub)
            }
        }

        // Pass 2: Handle ID exact subscriptionId match or long ICCID match (Pixel / AOSP / Qualcomm / Xiaomi)
        subDescriptors.forEachIndexed { index, sub ->
            if (result[index] != null) return@forEachIndexed
            val subIdStr = sub.subscriptionId.toString()
            val icc = sub.iccId
            val match = candidatePool.firstOrNull { meta ->
                if (meta.handle in usedHandles) return@firstOrNull false
                val id = meta.handle.id
                id == subIdStr ||
                    id == "sub_$subIdStr" ||
                    (icc.length >= 6 && (id == icc || id.startsWith(icc) || icc.startsWith(id)))
            }
            claim(index, match)
        }

        // Pass 3: PhoneAccount label / shortDescription matching SIM display or carrier name
        subDescriptors.forEachIndexed { index, sub ->
            if (result[index] != null) return@forEachIndexed
            val match = candidatePool.firstOrNull { meta ->
                if (meta.handle in usedHandles) return@firstOrNull false
                (sub.displayName.isNotBlank() &&
                    (meta.label.equals(sub.displayName, ignoreCase = true) ||
                        meta.shortDescription.equals(sub.displayName, ignoreCase = true))) ||
                    (sub.carrierName.isNotBlank() &&
                        (meta.label.equals(sub.carrierName, ignoreCase = true) ||
                            meta.shortDescription.equals(sub.carrierName, ignoreCase = true)))
            }
            claim(index, match)
        }

        // Pass 4: Exact MediaTek / Spreadtrum / vendor slot suffix tokens (strictly distinguishing 0-based vs 1-based)
        val hasZeroBasedSuffix = candidatePool.any { it.handle.id.endsWith("_0") || it.handle.id.equals("0") }
        subDescriptors.forEachIndexed { index, sub ->
            if (result[index] != null) return@forEachIndexed
            val hwSlot = sub.slotIndex.coerceAtLeast(0)
            val expectedSuffix = if (hasZeroBasedSuffix) "_$hwSlot" else "_${hwSlot + 1}"
            val expectedExact = if (hasZeroBasedSuffix) "$hwSlot" else "${hwSlot + 1}"
            val match = candidatePool.firstOrNull { meta ->
                if (meta.handle in usedHandles) return@firstOrNull false
                val id = meta.handle.id
                id.endsWith(expectedSuffix) ||
                    id.equals(expectedExact) ||
                    id.contains("slot_$hwSlot", ignoreCase = true) ||
                    id.contains("sim_${hwSlot + 1}", ignoreCase = true)
            }
            claim(index, match)
        }

        // Pass 5: Positional assignment from ONLY remaining unclaimed candidate handles (never reuse claimed handles!)
        subDescriptors.forEachIndexed { index, sub ->
            if (result[index] != null) return@forEachIndexed
            val remaining = candidatePool.filter { it.handle !in usedHandles }
            val fallback = remaining.getOrNull(sub.slotIndex) ?: remaining.firstOrNull()
            claim(index, fallback)
        }

        return result
    }

    /**
     * Resolves a user preference ("SIM 1", "SIM 2", "O2", "Tele2", custom display label) to a concrete
     * [SimAccountInfo] and 1-based slot number without falling prey to carrier-name substring bugs like `"O2".contains("2")`.
     */
    fun resolvePreferredSimTarget(
        preferredSim: String,
        simAccounts: List<SimAccountInfo>
    ): Pair<SimAccountInfo?, Int> {
        val trimmed = preferredSim.trim()
        if (trimmed.isEmpty() || trimmed.equals("Ask", ignoreCase = true)) {
            val single = simAccounts.singleOrNull()
            return single to ((single?.slotIndex ?: 0) + 1)
        }

        // 1. Exact explicit "SIM 1" / "SIM 2" slot selection takes immediate precedence
        if (trimmed.equals("SIM 1", ignoreCase = true) || trimmed == "1") {
            val account = simAccounts.find { it.slotIndex == 0 } ?: simAccounts.firstOrNull()
            return account to 1
        }
        if (trimmed.equals("SIM 2", ignoreCase = true) || trimmed == "2") {
            val account = simAccounts.find { it.slotIndex == 1 } ?: simAccounts.getOrNull(1)
            return account to 2
        }

        // 2. Exact match on displayName or carrierName BEFORE checking digits (fixes "O2", "Tele2", "Plus2")
        val exactByLabel = simAccounts.find {
            it.displayName.equals(trimmed, ignoreCase = true) ||
                it.carrierName.equals(trimmed, ignoreCase = true)
        }
        if (exactByLabel != null) {
            return exactByLabel to (exactByLabel.slotIndex + 1)
        }

        // 3. Isolated SIM/Slot token extraction ("Slot 2", "SIM 2 (Work)", "#2") — ignores attached digits inside "O2"
        val digitMatch = ISOLATED_SIM_DIGIT_REGEX.find(trimmed)
        if (digitMatch != null) {
            val slot = digitMatch.groupValues[1].toIntOrNull() ?: 1
            val account = simAccounts.find { it.slotIndex == (slot - 1) } ?: simAccounts.getOrNull(slot - 1)
            return account to slot
        }

        val fallback = simAccounts.firstOrNull()
        return fallback to ((fallback?.slotIndex ?: 0) + 1)
    }

    @SuppressLint("MissingPermission")
    fun inferSlotFromPhoneAccountHandle(
        handle: PhoneAccountHandle?,
        context: Context,
        simAccounts: List<SimAccountInfo>
    ): Int? {
        if (handle == null) return null

        val directMatch = simAccounts.find {
            it.accountHandle == handle ||
                (it.accountHandle?.id == handle.id && it.accountHandle?.componentName == handle.componentName)
        }
        if (directMatch != null) return directMatch.slotIndex + 1

        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val resolvedSubId = resolveSubscriptionIdForHandle(tm, handle)
        if (resolvedSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID && resolvedSubId > 0) {
            simAccounts.find { it.subscriptionId == resolvedSubId }?.let { return it.slotIndex + 1 }
        }

        val subMap = simAccounts.associate { it.subscriptionId.toString() to (it.slotIndex + 1) }
        if (handle.id.isNotBlank()) {
            return resolveCallLogSimSlot(
                trackedSimSlot = null,
                phoneAccountId = handle.id,
                subIdToSlotMap = subMap,
                defaultIfUnknown = null
            )
        }
        return null
    }

    /**
     * Resolves 1-based SIM slot (1 or 2) from tracked call data and CallLog.Calls.PHONE_ACCOUNT_ID.
     * Avoids short-key substring collisions such as `"12".contains("1")`.
     */
    fun resolveCallLogSimSlot(
        trackedSimSlot: Int?,
        phoneAccountId: String,
        subIdToSlotMap: Map<String, Int>,
        defaultIfUnknown: Int? = 1
    ): Int {
        if (trackedSimSlot != null && trackedSimSlot in 1..2) return trackedSimSlot
        val rawId = phoneAccountId.trim()
        if (rawId.isEmpty()) return defaultIfUnknown ?: 1

        // 1. Exact key match (case-sensitive, then case-insensitive)
        subIdToSlotMap[rawId]?.let { return it }
        subIdToSlotMap.entries.firstOrNull { it.key.equals(rawId, ignoreCase = true) }?.let { return it.value }

        // 2. Tokenized exact match on delimiters (handles "tel:12", "sub_12", "12, 1", "E12;slot1")
        val tokens = TOKEN_SPLIT_REGEX.split(rawId).filter { it.isNotBlank() }
        for (token in tokens) {
            subIdToSlotMap[token]?.let { return it }
            subIdToSlotMap.entries.firstOrNull { it.key.equals(token, ignoreCase = true) }?.let { return it.value }
        }

        // 3. Substring / prefix match ONLY for long unique identifiers (e.g., ICCIDs length >= 6)
        // Sorted longest-first so a shorter prefix can never shadow a longer ICCID.
        val longKeyMatch = subIdToSlotMap.entries
            .filter { it.key.length >= 6 }
            .sortedByDescending { it.key.length }
            .firstOrNull { (key, _) -> rawId.contains(key, ignoreCase = true) }
        if (longKeyMatch != null) return longKeyMatch.value

        // 4. Standardized OEM slot patterns
        if (OEM_SLOT2_REGEX.containsMatchIn(rawId) ||
            rawId.contains(", 2,") ||
            rawId.endsWith("_1") ||
            (rawId == "1" && !subIdToSlotMap.containsKey("1"))
        ) {
            return 2
        }

        if (OEM_SLOT1_REGEX.containsMatchIn(rawId) ||
            rawId.contains(", 1,") ||
            rawId == "0" ||
            rawId.endsWith("_0")
        ) {
            return 1
        }

        return defaultIfUnknown ?: 1
    }

    @SuppressLint("MissingPermission")
    fun getPhysicalSimCount(context: Context): Int {
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val subCount = try {
                (sm?.activeSubscriptionInfoList ?: emptyList()).size
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