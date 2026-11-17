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

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.BlockedNumberContract
import com.example.data.DialerDao
import com.example.model.BlockedNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Native integration for system-wide and local call blocking + offline spam screening.
 * Combines Android's BlockedNumberContract with Room DB blocklist and spam_numbers
 * using normalized digits, suffix matching, and safe wildcard matching for maximum
 * compatibility across Android 9-17 and all OEM ROMs.
 */
object BlockedNumberContractManager {

    /**
     * Pure deterministic matcher supporting:
     * - Exact trimmed matches
     * - Formatted vs digit-normalized matches (e.g. "+1 (555) 123-4567" vs "5551234567")
     * - Trailing country-code agnostic suffix comparison when both have >= 7 digits
     * - Safe wildcard rules using '*' (e.g. "+1800*", "*555*", "800*")
     */
    fun matchesRule(candidateNumber: String, rulePattern: String): Boolean {
        val candidate = candidateNumber.trim()
        val rule = rulePattern.trim()
        if (candidate.isEmpty() || rule.isEmpty()) return false

        if (candidate.equals(rule, ignoreCase = true)) return true

        val candidateDialable = candidate.filter { it.isDigit() || it == '+' }
        val candidateDigits = candidate.filter { it.isDigit() }

        if (rule.contains('*')) {
            if (matchesWildcard(candidate, rule)) return true
            val ruleDialable = rule.filter { it.isDigit() || it == '+' || it == '*' }
            val ruleDigitsWild = rule.filter { it.isDigit() || it == '*' }
            if (ruleDialable.isNotEmpty() && candidateDialable.isNotEmpty() && matchesWildcard(candidateDialable, ruleDialable)) {
                return true
            }
            if (ruleDigitsWild.isNotEmpty() && candidateDigits.isNotEmpty() && matchesWildcard(candidateDigits, ruleDigitsWild)) {
                return true
            }
            return false
        }

        val ruleDialable = rule.filter { it.isDigit() || it == '+' }
        val ruleDigits = rule.filter { it.isDigit() }

        if (candidateDialable.isNotEmpty() && candidateDialable == ruleDialable) return true
        if (candidateDigits.isNotEmpty() && candidateDigits == ruleDigits) return true

        if (candidateDigits.length >= 7 && ruleDigits.length >= 7) {
            val minLen = minOf(candidateDigits.length, ruleDigits.length, 10)
            if (minLen >= 7 && candidateDigits.takeLast(minLen) == ruleDigits.takeLast(minLen)) {
                return true
            }
        }
        return false
    }

    private fun matchesWildcard(value: String, wildcardPattern: String): Boolean {
        val regexString = wildcardPattern
            .split('*')
            .joinToString(".*") { Regex.escape(it) }
        return try {
            Regex("^$regexString$", RegexOption.IGNORE_CASE).matches(value)
        } catch (_: Exception) {
            false
        }
    }

    fun matchesAnyRule(
        number: String,
        rules: List<String>,
        context: Context? = null
    ): Boolean {
        val trimmed = number.trim()
        if (trimmed.isEmpty() || rules.isEmpty()) return false
        try {
            if (context != null && com.example.CallManager.isEmergencyNumber(trimmed, context)) {
                return false
            }
        } catch (_: Exception) {}

        return rules.any { rule -> matchesRule(trimmed, rule) }
    }

    suspend fun shouldBlockIncomingCall(
        context: Context,
        number: String,
        dao: DialerDao? = null
    ): Boolean = isBlocked(context = context, number = number, dao = dao, includeSpam = true)

    suspend fun isBlocked(
        context: Context,
        number: String,
        dao: DialerDao? = null,
        includeSpam: Boolean = false
    ): Boolean =
        withContext(Dispatchers.IO) {
            val trimmed = number.trim()
            if (trimmed.isEmpty()) return@withContext false

            // CRITICAL SAFETY FIX: Emergency services must NEVER be blocked under any condition
            try {
                if (com.example.CallManager.isEmergencyNumber(trimmed, context)) return@withContext false
            } catch (_: Exception) {}

            val cleanNum = trimmed.filter { it.isDigit() || it == '+' }

            // 1. Check System BlockedNumberContract (requires default dialer or screening role)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && BlockedNumberContract.canCurrentUserBlockNumbers(context)) {
                    val isSystemBlocked = BlockedNumberContract.isBlocked(context, cleanNum) ||
                        BlockedNumberContract.isBlocked(context, trimmed)
                    if (isSystemBlocked) return@withContext true
                }
            } catch (_: Exception) {}

            // 2. Check Local Room Blocklist (SQL fast path + normalized/wildcard fallback)
            if (dao != null) {
                try {
                    val isLocalBlocked = dao.isBlocked(cleanNum) ||
                        dao.isBlocked(trimmed) ||
                        dao.isBlockedSql(cleanNum) ||
                        dao.isBlockedSql(trimmed)
                    if (isLocalBlocked) return@withContext true

                    val allBlocked = dao.getBlockedNumbersList()
                    if (allBlocked.any { matchesRule(trimmed, it.number) }) {
                        return@withContext true
                    }
                } catch (_: Exception) {}

                // 3. Check Offline Spam Database when screening incoming calls
                if (includeSpam) {
                    try {
                        if (dao.isSpamNumber(cleanNum) || dao.isSpamNumber(trimmed)) {
                            return@withContext true
                        }
                        val allSpam = dao.getAllSpamNumbersList()
                        if (allSpam.any { matchesRule(trimmed, it.number) }) {
                            return@withContext true
                        }
                    } catch (_: Exception) {}
                }
            }

            false
        }

    suspend fun blockNumber(context: Context, number: String, dao: DialerDao? = null): Boolean =
        withContext(Dispatchers.IO) {
            val trimmed = number.trim()
            if (trimmed.isEmpty()) return@withContext false

            // Never block emergency numbers
            try {
                if (com.example.CallManager.isEmergencyNumber(trimmed, context)) return@withContext false
            } catch (_: Exception) {}

            val cleanNum = if (trimmed.contains("*")) trimmed else trimmed.filter { it.isDigit() || it == '+' }
            var systemSuccess = false

            // Try System BlockedNumberContract first
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && BlockedNumberContract.canCurrentUserBlockNumbers(context)) {
                    val values = ContentValues().apply {
                        put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, trimmed)
                    }
                    val uri: Uri? = context.contentResolver.insert(
                        BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                        values
                    )
                    systemSuccess = (uri != null)
                }
            } catch (_: Exception) {
                systemSuccess = false
            }

            // Always insert both normalized and raw formats into Room to ensure calls match across OEMs
            try {
                if (cleanNum.isNotEmpty()) {
                    dao?.insertBlockedNumber(BlockedNumber(number = cleanNum))
                }
                if (cleanNum != trimmed) {
                    dao?.insertBlockedNumber(BlockedNumber(number = trimmed))
                }
            } catch (_: Exception) {}

            systemSuccess
        }

    suspend fun unblockNumber(context: Context, number: String, dao: DialerDao? = null): Boolean =
        withContext(Dispatchers.IO) {
            val trimmed = number.trim()
            if (trimmed.isEmpty()) return@withContext false

            val cleanNum = if (trimmed.contains("*")) trimmed else trimmed.filter { it.isDigit() || it == '+' }
            var systemSuccess = false

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && BlockedNumberContract.canCurrentUserBlockNumbers(context)) {
                    val count = context.contentResolver.delete(
                        BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                        "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ? OR ${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?",
                        arrayOf(trimmed, cleanNum)
                    )
                    systemSuccess = (count > 0)
                }
            } catch (_: Exception) {
                systemSuccess = false
            }

            // Delete matching entries from local DB (including formatted equivalents)
            try {
                if (cleanNum.isNotEmpty()) {
                    dao?.deleteBlockedNumber(BlockedNumber(number = cleanNum))
                }
                dao?.deleteBlockedNumber(BlockedNumber(number = trimmed))
                dao?.getBlockedNumbersList()?.filter {
                    it.number == trimmed || it.number == cleanNum || (!trimmed.contains("*") && matchesRule(trimmed, it.number) && !it.number.contains("*"))
                }?.forEach { existing ->
                    dao.deleteBlockedNumber(existing)
                }
            } catch (_: Exception) {}

            systemSuccess
        }
}