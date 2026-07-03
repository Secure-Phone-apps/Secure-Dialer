package com.example

import android.telecom.Call
import android.telecom.VideoProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object CallManager {
    private val _currentCall = MutableStateFlow<Call?>(null)
    val currentCall: StateFlow<Call?> = _currentCall

    private val _callState = MutableStateFlow<Int>(Call.STATE_DISCONNECTED)
    val callState: StateFlow<Int> = _callState

    private val _callerNumber = MutableStateFlow<String>("")
    val callerNumber: StateFlow<String> = _callerNumber

    private val _callerName = MutableStateFlow<String>("")
    val callerName: StateFlow<String> = _callerName

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            _callState.value = state
            if (state == Call.STATE_DISCONNECTED) {
                updateCall(null)
            }
        }
    }

    fun updateCall(call: Call?) {
        _currentCall.value?.unregisterCallback(callCallback)
        _currentCall.value = call
        if (call != null) {
            _callState.value = call.state
            call.registerCallback(callCallback)
            
            // Extract phone number from call details
            val handle = call.details?.handle
            val number = handle?.schemeSpecificPart ?: ""
            _callerNumber.value = number
            _callerName.value = "" // Name can be resolved from contacts later in UI
        } else {
            _callState.value = Call.STATE_DISCONNECTED
            _callerNumber.value = ""
            _callerName.value = ""
        }
    }

    fun answer() {
        try {
            _currentCall.value?.answer(VideoProfile.STATE_AUDIO_ONLY)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            val call = _currentCall.value
            if (call != null) {
                if (call.state == Call.STATE_RINGING) {
                    call.reject(false, null)
                } else {
                    call.disconnect()
                }
            }
            updateCall(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
