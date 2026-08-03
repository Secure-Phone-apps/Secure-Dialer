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

package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.view.inputmethod.InputMethodManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object CallManager {
    private val _currentCall = MutableStateFlow<Call?>(null)
    val currentCall: StateFlow<Call?> = _currentCall

    private val _waitingCall = MutableStateFlow<Call?>(null)
    val waitingCall: StateFlow<Call?> = _waitingCall

    private val _calls = MutableStateFlow<List<Call>>(emptyList())
    val calls: StateFlow<List<Call>> = _calls

    private val _callState = MutableStateFlow(Call.STATE_DISCONNECTED)
    val callState: StateFlow<Int> = _callState

    private val _audioState = MutableStateFlow<CallAudioState?>(null)
    val audioState: StateFlow<CallAudioState?> = _audioState

    private val _callerNumber = MutableStateFlow("")
    val callerNumber: StateFlow<String> = _callerNumber

    private val _callerName = MutableStateFlow("")
    val callerName: StateFlow<String> = _callerName

    private val _callerCnapName = MutableStateFlow("")
    val callerCnapName: StateFlow<String> = _callerCnapName

    private val _activeStartTimestamp = MutableStateFlow<Long>(0L)
    val activeStartTimestamp: StateFlow<Long> = _activeStartTimestamp

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            _calls.value = _calls.value // force emit
            if (call == _currentCall.value) {
                _callState.value = state
                if (state == Call.STATE_ACTIVE) {
                    if (_activeStartTimestamp.value == 0L) {
                        val connectTime = call.details?.connectTimeMillis ?: 0L
                        _activeStartTimestamp.value = if (connectTime > 0L) connectTime else System.currentTimeMillis()
                    }
                }
                if (state == Call.STATE_DISCONNECTED) {
                    removeCall(call)
                }
            } else if (call == _waitingCall.value && state == Call.STATE_DISCONNECTED) {
                updateWaitingCall(null)
            }
        }
    }

    var inCallService: InCallService? = null
        set(value) {
            field = value
            if (value == null) {
                _audioState.value = null
            }
        }

    fun addCall(call: Call) {
        if (call !in _calls.value) {
            _calls.value = _calls.value + call
            call.registerCallback(callCallback)
        }
        if (_currentCall.value == null) {
            updateCall(call)
        } else if (call.state == Call.STATE_RINGING && _currentCall.value != call) {
            updateWaitingCall(call)
        }
    }

    fun autoStopRecordingIfNeeded() {
        if (com.example.util.CallAudioRecorder.isRecording.value) {
            val file = com.example.util.CallAudioRecorder.stopRecording()
            if (file != null && file.exists()) {
                val durationSec = com.example.util.CallAudioRecorder.recordingDuration.value.toLong()
                val number = _callerNumber.value.ifEmpty { "Unknown" }
                val name = _callerName.value.ifEmpty { number }
                val locale = inCallService?.let { com.example.ui.components.getCurrentLocale(it) } ?: java.util.Locale.getDefault()
                val sdf = java.text.SimpleDateFormat("MMM d, HH:mm", locale)
                val timestamp = sdf.format(java.util.Date())

                val recording = com.example.model.CallRecording(
                    number = number,
                    name = name,
                    timestamp = timestamp,
                    duration = durationSec,
                    filePath = file.absolutePath
                )

                inCallService?.let { ctx ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = com.example.data.AppDatabase.getDatabase(ctx)
                            db.dialerDao().insertCallRecording(recording)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    fun removeCall(call: Call) {
        if (call in _calls.value) {
            _calls.value = _calls.value - call
            call.unregisterCallback(callCallback)
        }
        if (_currentCall.value == call) {
            autoStopRecordingIfNeeded()
            val nextCall = _calls.value.firstOrNull { it.state != Call.STATE_DISCONNECTED }
            updateCall(nextCall)
        }
        if (_waitingCall.value == call) {
            updateWaitingCall(null)
        }
    }

    fun updateCall(call: Call?) {
        _currentCall.value = call
        if (call != null) {
            _callState.value = call.state
            if (call.state == Call.STATE_ACTIVE) {
                if (_activeStartTimestamp.value == 0L) {
                    val connectTime = call.details?.connectTimeMillis ?: 0L
                    _activeStartTimestamp.value = if (connectTime > 0L) connectTime else System.currentTimeMillis()
                }
            } else {
                _activeStartTimestamp.value = 0L
            }
            val number = call.details?.handle?.schemeSpecificPart ?: ""
            _callerNumber.value = number
            _callerName.value = "" 
            val cnap = call.details?.callerDisplayName ?: ""
            _callerCnapName.value = cnap
            
            if (cnap.isNotBlank() && number.isNotEmpty()) {
                inCallService?.let { context ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = com.example.data.AppDatabase.getDatabase(context)
                            db.dialerDao().insertSetting(com.example.model.AppSetting("cnap_" + number.filter { it.isDigit() }, cnap))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } else {
            autoStopRecordingIfNeeded()
            dtmfJob?.cancel()
            dtmfJob = null
            _callState.value = Call.STATE_DISCONNECTED
            _callerNumber.value = ""
            _callerName.value = ""
            _callerCnapName.value = ""
            _activeStartTimestamp.value = 0L
            if (_calls.value.isEmpty()) {
                inCallService = null
            }
        }
    }

    fun updateWaitingCall(call: Call?) {
        _waitingCall.value = call
    }

    fun updateAudioState(audioState: CallAudioState?) {
        _audioState.value = audioState
    }

    fun answer() {
        try {
            _currentCall.value?.answer(VideoProfile.STATE_AUDIO_ONLY)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setMuted(muted: Boolean) {
        inCallService?.setMuted(muted)
    }

    fun setSpeaker(speaker: Boolean) {
        inCallService?.setAudioRoute(if (speaker) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
    }

    fun setHold(hold: Boolean) {
        if (hold) {
            _currentCall.value?.hold()
        } else {
            _currentCall.value?.unhold()
        }
    }

    @SuppressLint("MissingPermission")
    fun placeCall(context: Context, number: String, preferredSim: String = "Ask") {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val activity = context as? Activity
            val windowToken = activity?.currentFocus?.windowToken ?: activity?.window?.decorView?.windowToken
            if (imm != null && windowToken != null) {
                imm.hideSoftInputFromWindow(windowToken, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null) {
                val uri = Uri.fromParts("tel", number, null)
                val extras = Bundle()
                
                if (preferredSim != "Ask") {
                    val accounts = telecomManager.callCapablePhoneAccounts
                    val index = if (preferredSim == "SIM 1") 0 else 1
                    if (index < accounts.size) {
                        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accounts[index])
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

    private var dtmfJob: kotlinx.coroutines.Job? = null

    fun playDtmf(key: Char) {
        _currentCall.value?.let { call ->
            dtmfJob?.cancel()
            try {
                call.playDtmfTone(key)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            dtmfJob = CoroutineScope(Dispatchers.Default).launch {
                kotlinx.coroutines.delay(150)
                try {
                    call.stopDtmfTone()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun stopDtmf() {
        dtmfJob?.cancel()
        try {
            _currentCall.value?.stopDtmfTone()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBluetooth(bluetooth: Boolean) {
        inCallService?.setAudioRoute(if (bluetooth) CallAudioState.ROUTE_BLUETOOTH else CallAudioState.ROUTE_EARPIECE)
    }
}
