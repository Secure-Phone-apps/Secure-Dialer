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
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.telephony.PhoneNumberUtils
import android.telephony.SubscriptionManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.example.data.AppDatabase
import com.example.model.AppSetting
import com.example.model.CallRecording
import com.example.ui.components.getCurrentLocale
import com.example.util.CallAudioHelper
import com.example.util.CallAudioRecorder
import com.example.util.RecordingFeedbackHelper
import com.example.util.SimCallTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

object CallManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var dtmfJob: Job? = null

    @Volatile
    var isAppInForeground: Boolean = false

    private val _currentCall = MutableStateFlow<Call?>(null)
    val currentCall: StateFlow<Call?> = _currentCall.asStateFlow()

    private val _waitingCall = MutableStateFlow<Call?>(null)
    val waitingCall: StateFlow<Call?> = _waitingCall.asStateFlow()

    private val _calls = MutableStateFlow<List<Call>>(emptyList())
    val calls: StateFlow<List<Call>> = _calls.asStateFlow()

    private val _callState = MutableStateFlow(Call.STATE_DISCONNECTED)
    val callState: StateFlow<Int> = _callState.asStateFlow()

    private val _audioState = MutableStateFlow<CallAudioState?>(null)
    val audioState: StateFlow<CallAudioState?> = _audioState.asStateFlow()

    private val _callerNumber = MutableStateFlow("")
    val callerNumber: StateFlow<String> = _callerNumber.asStateFlow()

    private val _callerName = MutableStateFlow("")
    val callerName: StateFlow<String> = _callerName.asStateFlow()

    private val _callerCnapName = MutableStateFlow("")
    val callerCnapName: StateFlow<String> = _callerCnapName.asStateFlow()

    private val _activeStartTimestamp = MutableStateFlow(0L)
    val activeStartTimestamp: StateFlow<Long> = _activeStartTimestamp.asStateFlow()

    private val _currentSimSlot = MutableStateFlow(1)
    val currentSimSlot: StateFlow<Int> = _currentSimSlot.asStateFlow()

    @Volatile
    var appContext: Context? = null

    var inCallService: InCallService? = null
        set(value) {
            field = value
            if (value != null) {
                appContext = value.applicationContext
            } else {
                _audioState.value = null
            }
        }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            notifyCallsChanged()

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
                    autoStartRecordingIfNeeded()
                }
            } else if (call == _waitingCall.value && state == Call.STATE_DISCONNECTED) {
                updateWaitingCall(null)
            }
            autoSelectCurrentCall()
        }

        override fun onChildrenChanged(call: Call, children: List<Call>) {
            super.onChildrenChanged(call, children)
            notifyCallsChanged()
            autoSelectCurrentCall()
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            super.onDetailsChanged(call, details)
            notifyCallsChanged()
            autoSelectCurrentCall()
        }
    }

    private fun notifyCallsChanged() {
        _calls.value = ArrayList(_calls.value)
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

    fun autoSelectCurrentCall() {
        val activeCalls = _calls.value.filter { it.state != Call.STATE_DISCONNECTED }
        if (activeCalls.isEmpty()) {
            updateCall(null)
            return
        }

        val target = activeCalls.find {
            it.children.isNotEmpty() || it.details?.hasProperty(Call.Details.PROPERTY_CONFERENCE) == true
        }
            ?: activeCalls.find { it.state == Call.STATE_ACTIVE }
            ?: activeCalls.find { it.state in listOf(Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_RINGING) }
            ?: activeCalls.find { it.state == Call.STATE_HOLDING }
            ?: activeCalls.firstOrNull()

        if (_currentCall.value != target) {
            updateCall(target)
        }
    }

    fun updateCall(call: Call?) {
        _currentCall.value = call
        if (call != null) {
            _callState.value = call.state
            if (call.state == Call.STATE_ACTIVE) {
                if (_activeStartTimestamp.value == 0L) {
                    val connect = call.details?.connectTimeMillis ?: 0L
                    _activeStartTimestamp.value = if (connect > 0L) connect else System.currentTimeMillis()
                }
            } else {
                _activeStartTimestamp.value = 0L
            }

            if (call.state == Call.STATE_HOLDING) {
                try {
                    call.unhold()
                } catch (_: Exception) {}
            }

            val number = call.details?.handle?.schemeSpecificPart ?: ""
            _callerNumber.value = number
            _callerName.value = ""
            val cnap = call.details?.callerDisplayName ?: ""

            if (cnap.isNotBlank() && number.isNotEmpty()) {
                ContactCache.putCnapName(number, cnap)
                _callerCnapName.value = cnap
                persistCnap(number, cnap)
            } else if (number.isNotEmpty()) {
                val cached = ContactCache.getCnapName(number)
                if (!cached.isNullOrBlank()) {
                    _callerCnapName.value = cached
                } else {
                    resolveCnapAsync(number)
                }
            } else {
                _callerCnapName.value = ""
            }
        } else {
            autoStopRecordingIfNeeded()
            stopDtmf()
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

    private fun persistCnap(number: String, cnap: String) {
        val ctx = appContext ?: inCallService?.applicationContext ?: return
        scope.launch {
            try {
                AppDatabase.getDatabase(ctx)
                    .dialerDao()
                    .insertSetting(AppSetting("cnap_" + number.filter { it.isDigit() }, cnap))
            } catch (_: Exception) {}
        }
    }

    private fun resolveCnapAsync(number: String) {
        val ctx = appContext ?: inCallService?.applicationContext ?: return
        scope.launch {
            val dbCnap = getSavedCnapName(ctx, number)
            if (!dbCnap.isNullOrBlank()) {
                _callerCnapName.value = dbCnap
            }
        }
    }

    fun autoStartRecordingIfNeeded() {
        if (CallAudioRecorder.isRecording.value) return
        val ctx = appContext ?: inCallService?.applicationContext ?: return
        val prefs = ctx.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_auto_record_calls_enabled", false)) {
            val chime = prefs.getBoolean("recording_chime_enabled", false)
            val autoTune = prefs.getBoolean("auto_tune_recording_volume", true)
            RecordingFeedbackHelper.triggerRecordingStartFeedback(ctx, chime)
            if (autoTune) {
                CallAudioHelper.prepareSpeakerForRecording(ctx, inCallService, _audioState.value)
            }
            val num = _callerNumber.value.ifEmpty { "Unknown" }
            CallAudioRecorder.startRecording(ctx, num)
        }
    }

    fun autoStopRecordingIfNeeded(context: Context? = null): Job? {
        val ctx = context?.applicationContext ?: appContext ?: inCallService?.applicationContext ?: return null
        val result = CallAudioRecorder.stopRecording()
        CallAudioHelper.restoreAudioState(ctx, inCallService)

        val file = result.file ?: return null
        if (!file.exists() || file.length() <= 128L) return null

        val durationSec = result.durationSeconds.coerceAtLeast(1L)
        val number = _callerNumber.value.ifEmpty {
            file.nameWithoutExtension
                .removePrefix("REC_")
                .split("_")
                .firstOrNull()
                ?.filter { it.isDigit() }
                ?.ifEmpty { "Unknown" } ?: "Unknown"
        }
        val name = _callerName.value.ifEmpty { number }
        val locale = getCurrentLocale(ctx)
        val timestamp = SimpleDateFormat("MMM d, HH:mm", locale).format(Date())

        val recording = CallRecording(
            number = number,
            name = name,
            timestamp = timestamp,
            duration = durationSec,
            filePath = file.absolutePath
        )

        return scope.launch {
            try {
                val db = AppDatabase.getDatabase(ctx)
                if (db.dialerDao().getCallRecordingByPath(file.absolutePath) == null) {
                    db.dialerDao().insertCallRecording(recording)
                }
                val autoExport = db.dialerDao().getSetting("is_auto_export_recordings_enabled")?.toBooleanStrictOrNull() ?: true
                if (autoExport) {
                    CallAudioRecorder.exportRecordingToPublicDownloads(ctx, file)
                }
            } catch (_: Exception) {}
        }
    }

    fun mergeCalls() {
        val all = _calls.value.filter { it.state != Call.STATE_DISCONNECTED }
        val active = all.find { it.state == Call.STATE_ACTIVE }
        val held = all.find { it.state == Call.STATE_HOLDING }

        if (active != null && held != null) {
            try {
                active.conference(held)
            } catch (_: Exception) {
                try {
                    held.conference(active)
                } catch (_: Exception) {}
            }
        } else {
            val current = _currentCall.value
            val other = all.firstOrNull { it != current }
            if (current != null && other != null) {
                try {
                    current.conference(other)
                } catch (_: Exception) {}
            }
        }
    }

    fun updateWaitingCall(call: Call?) {
        _waitingCall.value = call
    }

    fun updateAudioState(state: CallAudioState?) {
        _audioState.value = state
    }

    fun answer() {
        try {
            _currentCall.value?.answer(VideoProfile.STATE_AUDIO_ONLY)
        } catch (_: Exception) {}
    }

    fun disconnect() {
        try {
            val call = _currentCall.value ?: return
            if (call.state == Call.STATE_RINGING) {
                call.reject(false, null)
            } else {
                call.disconnect()
            }
            if (_calls.value.none { it != call && it.state != Call.STATE_DISCONNECTED }) {
                updateCall(null)
            }
        } catch (_: Exception) {}
    }

    fun setMuted(muted: Boolean) {
        inCallService?.setMuted(muted)
    }

    fun setSpeaker(speaker: Boolean) {
        inCallService?.setAudioRoute(if (speaker) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
    }

    fun setBluetooth(bluetooth: Boolean) {
        inCallService?.setAudioRoute(if (bluetooth) CallAudioState.ROUTE_BLUETOOTH else CallAudioState.ROUTE_EARPIECE)
    }

    fun setHold(hold: Boolean) {
        if (hold) {
            _currentCall.value?.hold()
        } else {
            _currentCall.value?.unhold()
        }
    }

    fun silenceRinger(context: Context) {
        try {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            tm?.silenceRinger()
        } catch (_: Exception) {}
    }

    fun playDtmf(key: Char) {
        val call = _currentCall.value ?: return
        dtmfJob?.cancel()
        try {
            call.playDtmfTone(key)
        } catch (_: Exception) {}
        dtmfJob = scope.launch(Dispatchers.Default) {
            delay(150)
            try {
                call.stopDtmfTone()
            } catch (_: Exception) {}
        }
    }

    fun stopDtmf() {
        dtmfJob?.cancel()
        try {
            _currentCall.value?.stopDtmfTone()
        } catch (_: Exception) {}
    }

    fun formatOutgoingNumberWithClir(number: String, isHideCallerId: Boolean, clirPrefix: String): String {
        if (!isHideCallerId || number.isBlank() || isEmergencyNumber(number)) return number
        val clir = clirPrefix.trim().ifBlank { "#31#" }
        return if (number.startsWith(clir)) number else "$clir$number"
    }

    private fun isEmergencyNumber(number: String): Boolean {
        return try {
            PhoneNumberUtils.isEmergencyNumber(number.trim())
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun placeCall(context: Context, number: String, preferredSim: String = "Ask") {
        appContext = context.applicationContext
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val activity = context as? Activity
            val windowToken = activity?.currentFocus?.windowToken ?: activity?.window?.decorView?.windowToken
            if (imm != null && windowToken != null) {
                imm.hideSoftInputFromWindow(windowToken, 0)
            }
        } catch (_: Exception) {}

        val isEmergency = isEmergencyNumber(number)
        val targetSlot = if (preferredSim.contains("2")) 2 else 1
        _currentSimSlot.value = targetSlot
        SimCallTracker.recordOutgoingCall(context, number, targetSlot)

        val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return
        try {
            _currentCall.value?.takeIf { it.state == Call.STATE_ACTIVE }?.hold()
        } catch (_: Exception) {}

        val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        val hideId = prefs.getBoolean("is_hide_caller_id_enabled", false)
        val clirPrefix = prefs.getString("clir_prefix", "#31#") ?: "#31#"
        val dialedNumber = formatOutgoingNumberWithClir(number, hideId, clirPrefix)

        val uri = Uri.fromParts("tel", dialedNumber, null)
        val extras = Bundle()

        // Critical safety rule: Emergency numbers must never be constrained to a single SIM
        if (!isEmergency && preferredSim != "Ask") {
            findPhoneAccountForSlot(context, tm, targetSlot)?.let {
                extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it)
            }
        }

        try {
            tm.placeCall(uri, extras)
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    private fun findPhoneAccountForSlot(context: Context, tm: TelecomManager, slotIndex: Int): PhoneAccountHandle? {
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val subInfoList = sm?.activeSubscriptionInfoList
            val subInfo = subInfoList?.find { it.simSlotIndex == (slotIndex - 1) }

            if (subInfo != null) {
                tm.callCapablePhoneAccounts.find { handle ->
                    handle.id.contains(subInfo.subscriptionId.toString()) || 
                    (subInfo.iccId != null && handle.id.contains(subInfo.iccId))
                } ?: tm.callCapablePhoneAccounts.getOrNull(slotIndex - 1)
            } else {
                tm.callCapablePhoneAccounts.getOrNull(slotIndex - 1)
            }
        } catch (_: Exception) {
            tm.callCapablePhoneAccounts.getOrNull(slotIndex - 1)
        }
    }

    fun rejectCallWithMessage(context: Context, number: String, textMessage: String) {
        val call = _currentCall.value
        if (call != null && call.state == Call.STATE_RINGING) {
            try {
                call.reject(true, textMessage)
                Toast.makeText(context, context.getString(R.string.sms_sent), Toast.LENGTH_SHORT).show()
                return
            } catch (_: Exception) {}
        }

        try {
            call?.disconnect()
        } catch (_: Exception) {}

        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
                putExtra("sms_body", textMessage)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.sms_failed), Toast.LENGTH_SHORT).show()
        }
    }
}