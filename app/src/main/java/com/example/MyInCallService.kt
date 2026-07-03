package com.example

import android.telecom.Call
import android.telecom.InCallService

class MyInCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        // This service is required by Android OS to list the app in Default Dialer selection.
        // It binds to active calls when selected as the default phone app.
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
    }
}
