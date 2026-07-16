package com.example

import android.telecom.Call
import android.telecom.VideoProfile
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object CallManager {
    private val _currentCall = MutableStateFlow<Call?>(null)
    val currentCall: StateFlow<Call?> = _currentCall

    private val _waitingCall = MutableStateFlow<Call?>(null)
    val waitingCall: StateFlow<Call?> = _waitingCall

    private val _calls = MutableStateFlow<List<Call>>(emptyList())
    val calls: StateFlow<List<Call>> = _calls

    private val _callState = MutableStateFlow<Int>(Call.STATE_DISCONNECTED)
    val callState: StateFlow<Int> = _callState

    private val _audioState = MutableStateFlow<android.telecom.CallAudioState?>(null)
    val audioState: StateFlow<android.telecom.CallAudioState?> = _audioState

    private val _callerNumber = MutableStateFlow<String>("")
    val callerNumber: StateFlow<String> = _callerNumber

    private val _callerName = MutableStateFlow<String>("")
    val callerName: StateFlow<String> = _callerName

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            val list = _calls.value.toList()
            _calls.value = list // force emit
            if (call == _currentCall.value) {
                _callState.value = state
                if (state == Call.STATE_DISCONNECTED) {
                    removeCall(call)
                }
            } else if (call == _waitingCall.value && state == Call.STATE_DISCONNECTED) {
                updateWaitingCall(null)
            }
        }
    }

    var inCallService: android.telecom.InCallService? = null
        set(value) {
            field = value
            if (value == null) {
                _audioState.value = null
            }
        }

    fun addCall(call: Call) {
        val currentList = _calls.value.toMutableList()
        if (!currentList.contains(call)) {
            currentList.add(call)
            _calls.value = currentList
            call.registerCallback(callCallback)
        }
        if (_currentCall.value == null) {
            updateCall(call)
        } else if (call.state == Call.STATE_RINGING && _currentCall.value != call) {
            updateWaitingCall(call)
        }
    }

    fun removeCall(call: Call) {
        val currentList = _calls.value.toMutableList()
        if (currentList.contains(call)) {
            currentList.remove(call)
            _calls.value = currentList
            call.unregisterCallback(callCallback)
        }
        if (_currentCall.value == call) {
            val nextCall = _calls.value.firstOrNull { it.state != Call.STATE_DISCONNECTED }
            updateCall(nextCall)
        }
        if (_waitingCall.value == call) {
            updateWaitingCall(null)
        }
    }

    fun updateCall(call: android.telecom.Call?) {
        _currentCall.value = call
        if (call != null) {
            _callState.value = call.state
            // Extract phone number
            _callerNumber.value = call.details?.handle?.schemeSpecificPart ?: ""
            _callerName.value = "" 
        } else {
            _callState.value = android.telecom.Call.STATE_DISCONNECTED
            _callerNumber.value = ""
            _callerName.value = ""
            if (_calls.value.isEmpty()) {
                inCallService = null
            }
        }
    }

    fun updateWaitingCall(call: Call?) {
        _waitingCall.value = call
    }

    fun updateAudioState(audioState: android.telecom.CallAudioState?) {
        _audioState.value = audioState
    }

    fun answer() {
        try {
            _currentCall.value?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setMuted(muted: Boolean) {
        inCallService?.setMuted(muted)
    }

    fun setSpeaker(speaker: Boolean) {
        inCallService?.setAudioRoute(if (speaker) android.telecom.CallAudioState.ROUTE_SPEAKER else android.telecom.CallAudioState.ROUTE_EARPIECE)
    }

    fun setHold(hold: Boolean) {
        if (hold) {
            _currentCall.value?.hold()
        } else {
            _currentCall.value?.unhold()
        }
    }

    fun placeCall(context: Context, number: String, preferredSim: String = "Ask") {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            val activity = context as? android.app.Activity
            val windowToken = activity?.currentFocus?.windowToken ?: activity?.window?.decorView?.windowToken
            if (imm != null && windowToken != null) {
                imm.hideSoftInputFromWindow(windowToken, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
            if (telecomManager != null) {
                val uri = android.net.Uri.fromParts("tel", number, null)
                val extras = android.os.Bundle()
                
                if (preferredSim != "Ask") {
                    val accounts = telecomManager.callCapablePhoneAccounts
                    val index = if (preferredSim == "SIM 1") 0 else 1
                    if (index < accounts.size) {
                        extras.putParcelable(android.telecom.TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accounts[index])
                    }
                }
                
                telecomManager.placeCall(uri, extras)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
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

    fun playDtmf(key: Char) {
        _currentCall.value?.playDtmfTone(key)
    }

    fun stopDtmf() {
        _currentCall.value?.stopDtmfTone()
    }

    fun setBluetooth(bluetooth: Boolean) {
        inCallService?.setAudioRoute(if (bluetooth) android.telecom.CallAudioState.ROUTE_BLUETOOTH else android.telecom.CallAudioState.ROUTE_EARPIECE)
    }
}
