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
    private val scope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)

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

    private val _currentSimSlot = MutableStateFlow<Int>(1)
    val currentSimSlot: StateFlow<Int> = _currentSimSlot

    fun autoSelectCurrentCall() {
        val allCallsList = _calls.value.filter { it.state != Call.STATE_DISCONNECTED }
        
        if (allCallsList.isEmpty()) {
            updateCall(null)
            return
        }

        // 1. If there's an active conference call (has children), prefer it.
        val conferenceCall = allCallsList.find { 
            it.children.isNotEmpty() || 
            it.details?.hasProperty(Call.Details.PROPERTY_CONFERENCE) == true 
        }
        if (conferenceCall != null) {
            if (_currentCall.value != conferenceCall) {
                updateCall(conferenceCall)
            }
            return
        }

        // 2. If there's an active call, prefer it.
        val activeCall = allCallsList.find { it.state == Call.STATE_ACTIVE }
        if (activeCall != null) {
            if (_currentCall.value != activeCall) {
                updateCall(activeCall)
            }
            return
        }

        // 3. If there's a dialing/connecting/ringing call, show it.
        val progressCall = allCallsList.find { 
            it.state == Call.STATE_DIALING || 
            it.state == Call.STATE_CONNECTING || 
            it.state == Call.STATE_RINGING 
        }
        if (progressCall != null) {
            if (_currentCall.value != progressCall) {
                updateCall(progressCall)
            }
            return
        }

        // 4. If there's a held call, pick it
        val heldCall = allCallsList.find { it.state == Call.STATE_HOLDING }
        if (heldCall != null) {
            if (_currentCall.value != heldCall) {
                updateCall(heldCall)
            }
            return
        }

        // 5. Fallback to any non-disconnected call
        if (_currentCall.value == null || _currentCall.value?.state == Call.STATE_DISCONNECTED) {
            updateCall(allCallsList.first())
        }
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            _calls.value = _calls.value // force emit
            
            if (state == Call.STATE_DISCONNECTED) {
                removeCall(call)
                return
            }

            if (call == _currentCall.value) {
                _callState.value = state
                if (state == Call.STATE_ACTIVE) {
                    if (_activeStartTimestamp.value == 0L) {
                        val connectTime = call.details?.connectTimeMillis ?: 0L
                        _activeStartTimestamp.value = if (connectTime > 0L) connectTime else System.currentTimeMillis()
                    }
                }
            } else if (call == _waitingCall.value && state == Call.STATE_DISCONNECTED) {
                updateWaitingCall(null)
            }
            
            autoSelectCurrentCall()
        }

        override fun onChildrenChanged(call: Call, children: List<Call>) {
            super.onChildrenChanged(call, children)
            _calls.value = _calls.value // force emit to update conference UI
            autoSelectCurrentCall()
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            super.onDetailsChanged(call, details)
            _calls.value = _calls.value // force emit
            autoSelectCurrentCall()
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
        if (call.state == Call.STATE_RINGING && _currentCall.value != null && _currentCall.value != call) {
            updateWaitingCall(call)
        } else {
            autoSelectCurrentCall()
        }
    }

    fun autoStopRecordingIfNeeded() {
        if (com.example.util.CallAudioRecorder.isRecording.value) {
            val result = com.example.util.CallAudioRecorder.stopRecording()
            val file = result.file
            if (file != null && file.exists() && file.length() > 0L) {
                val durationSec = result.durationSeconds.coerceAtLeast(1L)
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
                    scope.launch {
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
        }
        if (_waitingCall.value == call) {
            updateWaitingCall(null)
        }
        autoSelectCurrentCall()
    }

    fun mergeCalls() {
        val allCallsList = _calls.value.filter { it.state != Call.STATE_DISCONNECTED }
        val activeCall = allCallsList.find { it.state == Call.STATE_ACTIVE }
        val heldCall = allCallsList.find { it.state == Call.STATE_HOLDING }
        
        if (activeCall != null && heldCall != null) {
            try {
                activeCall.conference(heldCall)
            } catch (e: Exception) {
                try {
                    heldCall.conference(activeCall)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        } else {
            // Fallback: merge any other active call with another call if not strictly STATE_HOLDING yet
            val current = _currentCall.value
            val other = allCallsList.firstOrNull { it != current }
            if (current != null && other != null) {
                try {
                    current.conference(other)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
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
            if (call.state == Call.STATE_HOLDING) {
                try {
                    call.unhold()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val number = call.details?.handle?.schemeSpecificPart ?: ""
            _callerNumber.value = number
            _callerName.value = "" 
            val cnap = call.details?.callerDisplayName ?: ""
            
            if (cnap.isNotBlank() && number.isNotEmpty()) {
                ContactCache.putCnapName(number, cnap)
                _callerCnapName.value = cnap
                inCallService?.let { context ->
                    scope.launch {
                        try {
                            val db = com.example.data.AppDatabase.getDatabase(context)
                            db.dialerDao().insertSetting(com.example.model.AppSetting("cnap_" + number.filter { it.isDigit() }, cnap))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } else if (number.isNotEmpty()) {
                val savedCnap = ContactCache.getCnapName(number)
                if (!savedCnap.isNullOrBlank()) {
                    _callerCnapName.value = savedCnap
                } else {
                    inCallService?.let { context ->
                        scope.launch {
                            val dbCnap = getSavedCnapName(context, number)
                            if (!dbCnap.isNullOrBlank()) {
                                _callerCnapName.value = dbCnap
                            }
                        }
                    }
                }
            } else {
                _callerCnapName.value = ""
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

        val targetSlot = if (preferredSim.contains("2")) 2 else 1
        _currentSimSlot.value = targetSlot
        com.example.util.SimCallTracker.recordOutgoingCall(context, number, targetSlot)

        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null) {
                // Hold any active call first to avoid race conditions/collisions
                val activeCall = _currentCall.value
                if (activeCall != null && activeCall.state == Call.STATE_ACTIVE) {
                    try {
                        activeCall.hold()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

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
                
                val remainingCalls = _calls.value.filter { it != call && it.state != Call.STATE_DISCONNECTED }
                if (remainingCalls.isEmpty()) {
                    updateCall(null)
                }
            }
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
            dtmfJob = scope.launch(Dispatchers.Default) {
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
