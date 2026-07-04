package com.example

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

class MyInCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.inCallService = this
        CallManager.updateCall(call)
        
        // Start the MainActivity to display the incoming/outgoing call screen
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("SHOW_CALL_SCREEN", true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        if (CallManager.currentCall.value == call) {
            CallManager.updateCall(null)
        }
    }
}

