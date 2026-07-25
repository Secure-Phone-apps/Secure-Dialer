package com.example

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.data.AppDatabase
import com.example.model.AppSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CallBlockerService : CallScreeningService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: ""
        val context = this
        val db = AppDatabase.getDatabase(context)
        val dao = db.dialerDao()

        serviceScope.launch {
            try {
                // Service Watchdog Heartbeat
                dao.insertSetting(AppSetting(KEY_LAST_ACTIVE, System.currentTimeMillis().toString()))

                val blocked = isNumberBlocked(dao, number)
                if (blocked) {
                    val response = CallResponse.Builder()
                        .setDisallowCall(true)
                        .setRejectCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(true)
                        .build()
                    respondToCall(callDetails, response)
                } else {
                    respondToCall(callDetails, CallResponse.Builder().build())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    respondToCall(callDetails, CallResponse.Builder().build())
                } catch (inner: Exception) {
                    inner.printStackTrace()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val KEY_LAST_ACTIVE = "call_blocker_last_active"

        suspend fun isNumberBlocked(dao: com.example.data.DialerDao, rawNumber: String): Boolean {
            if (rawNumber.isBlank()) return false
            val cleanNum = rawNumber.replace("[^0-9+]".toRegex(), "")

            // 1. Direct SQL check (exact & wildcard LIKE match)
            if (dao.isBlockedSql(rawNumber) || (cleanNum.isNotEmpty() && dao.isBlockedSql(cleanNum))) {
                return true
            }

            // 2. Advanced Regex & Wildcard Match
            val blockedList = dao.getBlockedNumbersList()
            for (blocked in blockedList) {
                val pattern = blocked.number.trim()
                if (pattern.isEmpty()) continue

                // Exact match
                if (pattern == rawNumber || pattern == cleanNum) return true

                // Wildcard match e.g. "+1800*", "800*", "555????"
                if (pattern.contains("*") || pattern.contains("?")) {
                    val regexStr = "^" + Regex.escape(pattern)
                        .replace("\\*", ".*")
                        .replace("\\?", ".") + "$"
                    val regex = Regex(regexStr)
                    if (rawNumber.matches(regex) || cleanNum.matches(regex)) {
                        return true
                    }
                }

                // Area code / Prefix match (e.g. pattern "+1800" or "800")
                val cleanPattern = pattern.replace("[^0-9+]".toRegex(), "")
                if (cleanPattern.isNotEmpty() && cleanNum.startsWith(cleanPattern)) {
                    return true
                }
            }

            return false
        }

        fun isCallScreeningRoleHeld(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) ?: false
            } else {
                true
            }
        }

        suspend fun getServiceHealthStatus(context: Context): ServiceHealth {
            val db = AppDatabase.getDatabase(context)
            val dao = db.dialerDao()
            val lastActiveStr = dao.getSetting(KEY_LAST_ACTIVE)
            val lastActiveTime = lastActiveStr?.toLongOrNull() ?: 0L
            val isRoleHeld = isCallScreeningRoleHeld(context)
            return ServiceHealth(
                isRoleGranted = isRoleHeld,
                lastActiveTimestamp = lastActiveTime,
                isHealthy = isRoleHeld
            )
        }
    }
}

data class ServiceHealth(
    val isRoleGranted: Boolean,
    val lastActiveTimestamp: Long,
    val isHealthy: Boolean
)
