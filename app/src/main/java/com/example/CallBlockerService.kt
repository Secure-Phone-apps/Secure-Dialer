package com.example

import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallBlockerService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: ""
        val db = AppDatabase.getDatabase(this)
        val dao = db.dialerDao()

        CoroutineScope(Dispatchers.IO).launch {
            val isBlocked = dao.isBlocked(number)
            if (isBlocked) {
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
        }
    }
}
