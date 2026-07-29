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
 * World-class native integration for system-wide and local call blocking.
 * Integrates with Android's system BlockedNumberContract with automatic fallback
 * to Room DB for maximum reliability across custom ROMs and older Android versions.
 */
object BlockedNumberContractManager {

    suspend fun isBlocked(context: Context, number: String, dao: DialerDao? = null): Boolean =
        withContext(Dispatchers.IO) {
            if (number.isBlank()) return@withContext false
            val cleanNum = number.filter { it.isDigit() || it == '+' }

            // 1. Check System BlockedNumberContract if supported
            try {
                if (BlockedNumberContract.canCurrentUserBlockNumbers(context)) {
                    val isSystemBlocked = BlockedNumberContract.isBlocked(context, cleanNum)
                    if (isSystemBlocked) return@withContext true
                }
            } catch (e: Exception) {
                // System query restricted or unsupported on custom ROM
            }

            // 2. Check Local Room Database
            try {
                if (dao != null) {
                    val localBlocked = dao.isBlocked(cleanNum)
                    if (localBlocked) return@withContext true
                }
            } catch (e: Exception) {
                // DB error fallback
            }

            false
        }

    suspend fun blockNumber(context: Context, number: String, dao: DialerDao? = null): Boolean =
        withContext(Dispatchers.IO) {
            val cleanNum = number.trim()
            if (cleanNum.isEmpty()) return@withContext false

            var systemSuccess = false

            // Try System BlockedNumberContract first
            try {
                if (BlockedNumberContract.canCurrentUserBlockNumbers(context)) {
                    val values = ContentValues().apply {
                        put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, cleanNum)
                    }
                    val uri: Uri? = context.contentResolver.insert(
                        BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                        values
                    )
                    systemSuccess = (uri != null)
                }
            } catch (e: Exception) {
                systemSuccess = false
            }

            // Always sync to Local DB for ultra-fast local lookups
            try {
                dao?.insertBlockedNumber(BlockedNumber(number = cleanNum))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            systemSuccess
        }

    suspend fun unblockNumber(context: Context, number: String, dao: DialerDao? = null): Boolean =
        withContext(Dispatchers.IO) {
            val cleanNum = number.trim()
            if (cleanNum.isEmpty()) return@withContext false

            var systemSuccess = false

            try {
                if (BlockedNumberContract.canCurrentUserBlockNumbers(context)) {
                    val count = context.contentResolver.delete(
                        BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                        "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?",
                        arrayOf(cleanNum)
                    )
                    systemSuccess = (count > 0)
                }
            } catch (e: Exception) {
                systemSuccess = false
            }

            try {
                dao?.deleteBlockedNumber(BlockedNumber(number = cleanNum))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            systemSuccess
        }
}
