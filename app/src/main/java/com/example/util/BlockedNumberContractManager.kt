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
import android.telephony.PhoneNumberUtils
import com.example.data.DialerDao
import com.example.model.BlockedNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Native integration for system-wide and local call blocking.
 * Integrates with Android's system BlockedNumberContract with automatic fallback
 * to Room DB for maximum reliability across custom ROMs and older Android versions.
 */
object BlockedNumberContractManager {

    suspend fun isBlocked(context: Context, number: String, dao: DialerDao? = null): Boolean =
        withContext(Dispatchers.IO) {
            val trimmed = number.trim()
            if (trimmed.isEmpty()) return@withContext false

            // CRITICAL SAFETY FIX: Emergency services must NEVER be blocked under any condition
            try {
                if (PhoneNumberUtils.isEmergencyNumber(trimmed)) return@withContext false
            } catch (_: Exception) {}

            val cleanNum = trimmed.filter { it.isDigit() || it == '+' }

            // 1. Check System BlockedNumberContract (requires default dialer role)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && BlockedNumberContract.canCurrentUserBlockNumbers(context)) {
                    val isSystemBlocked = BlockedNumberContract.isBlocked(context, cleanNum) || 
                                          BlockedNumberContract.isBlocked(context, trimmed)
                    if (isSystemBlocked) return@withContext true
                }
            } catch (_: Exception) {}

            // 2. Check Local Room Database (handles exact match, formatted match, and wildcard rules)
            try {
                if (dao != null) {
                    val isLocalBlocked = dao.isBlocked(cleanNum) || 
                                         dao.isBlocked(trimmed) || 
                                         dao.isBlockedSql(cleanNum) ||
                                         dao.isBlockedSql(trimmed)
                    if (isLocalBlocked) return@withContext true
                }
            } catch (_: Exception) {}

            false
        }

    suspend fun blockNumber(context: Context, number: String, dao: DialerDao? = null): Boolean =
        withContext(Dispatchers.IO) {
            val trimmed = number.trim()
            if (trimmed.isEmpty()) return@withContext false

            // Never block emergency numbers
            try {
                if (PhoneNumberUtils.isEmergencyNumber(trimmed)) return@withContext false
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

            // FIXED: Always insert both normalized and raw formats into Room to ensure incoming calls match 100%
            try {
                dao?.insertBlockedNumber(BlockedNumber(number = cleanNum))
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

            val cleanNum = trimmed.filter { it.isDigit() || it == '+' }
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

            // Delete both raw and normalized entries from local DB
            try {
                dao?.deleteBlockedNumber(BlockedNumber(number = cleanNum))
                dao?.deleteBlockedNumber(BlockedNumber(number = trimmed))
            } catch (_: Exception) {}

            systemSuccess
        }
}