/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
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
import com.example.util.MultiSimManager
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

    private val _callerPhotoUri = MutableStateFlow("")
    val callerPhotoUri: StateFlow<String> = _callerPhotoUri.asStateFlow()

    private val _callerLabel = MutableStateFlow("")
    val callerLabel: StateFlow<String> = _callerLabel.asStateFlow()

    fun setCallerName(name: String) {
        if (name.isNotBlank()) {
            _callerName.value = name
        }
    }

    fun setCallerDetails(name: String, photoUri: String = "", label: String = "") {
        if (name.isNotBlank()) _callerName.value = name
        if (photoUri.isNotBlank()) _callerPhotoUri.value = photoUri
        if (label.isNotBlank()) _callerLabel.value = label
    }

    private val _activeStartTimestamp = MutableStateFlow(0L)
    val activeStartTimestamp: StateFlow<Long> = _activeStartTimestamp.asStateFlow()

    private val _currentSimSlot = MutableStateFlow(1)
    val currentSimSlot: StateFlow<Int> = _currentSimSlot.asStateFlow()

    private val _isConferenceActive = MutableStateFlow(false)
    val isConferenceActive: StateFlow<Boolean> = _isConferenceActive.asStateFlow()

    fun isConference(call: Call?): Boolean {
        if (call == null) return false
        if (call.children.isNotEmpty() || call.parent != null) return true
        val details = call.details ?: return false
        val properties = details.callProperties
        val hasConferenceProperty = (properties and Call.Details.PROPERTY_CONFERENCE != 0) ||
                (properties and Call.Details.PROPERTY_GENERIC_CONFERENCE != 0) ||
                details.hasProperty(Call.Details.PROPERTY_CONFERENCE) ||
                details.hasProperty(Call.Details.PROPERTY_GENERIC_CONFERENCE)
        val capabilities = details.callCapabilities
        val hasConferenceCapability = (capabilities and Call.Details.CAPABILITY_SWAP_CONFERENCE != 0) ||
                (capabilities and Call.Details.CAPABILITY_MERGE_CONFERENCE != 0) ||
                (capabilities and Call.Details.CAPABILITY_MANAGE_CONFERENCE != 0) ||
                (capabilities and Call.Details.CAPABILITY_SEPARATE_FROM_CONFERENCE != 0) ||
                (capabilities and Call.Details.CAPABILITY_DISCONNECT_FROM_CONFERENCE != 0)
        val hasConferenceExtra = details.extras?.getBoolean("android.telecom.extra.IS_CONFERENCE", false) == true ||
                details.extras?.containsKey("android.telecom.extra.CONFERENCE_PARTICIPANTS") == true
        return hasConferenceProperty || hasConferenceCapability || hasConferenceExtra
    }

    @Volatile
    var appContext: Context? = null

    var inCallService: InCallService? = null
        set(value) {
            field = value
            if (value != null) appContext = value.applicationContext else _audioState.value = null
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
                        val connect = call.details?.connectTimeMillis ?: 0L
                        _activeStartTimestamp.value = if (connect > 0L) connect else System.currentTimeMillis()
                    }
                    autoStartRecordingIfNeeded()
                }
            } else if (call == _waitingCall.value && state == Call.STATE_DISCONNECTED) {
                updateWaitingCall(null)
            }
            autoSelectCurrentCall()
        }

        override fun onParentChanged(call: Call, parent: Call?) {
            super.onParentChanged(call, parent)
            notifyCallsChanged()
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

        override fun onConferenceableCallsChanged(call: Call, conferenceableCalls: List<Call>) {
            super.onConferenceableCallsChanged(call, conferenceableCalls)
            notifyCallsChanged()
        }

        override fun onConnectionEvent(call: Call, event: String, extras: Bundle?) {
            super.onConnectionEvent(call, event, extras)
            notifyCallsChanged()
            autoSelectCurrentCall()
        }
    }

    private fun notifyCallsChanged() {
        val currentList = ArrayList(_calls.value)
        _calls.value = currentList
        _isConferenceActive.value = currentList.any { it.state != Call.STATE_DISCONNECTED && isConference(it) } ||
                isConference(_currentCall.value)
    }

    fun addCall(call: Call) {
        if (call !in _calls.value) {
            _calls.value = _calls.value + call
            call.registerCallback(callCallback)
        }
        notifyCallsChanged()
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
        notifyCallsChanged()
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

        val target = activeCalls.find { it.children.isNotEmpty() || isConference(it) }
            ?: activeCalls.find { it.state == Call.STATE_ACTIVE }
            ?: activeCalls.find { it.state in listOf(Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_RINGING) }
            ?: activeCalls.find { it.state == Call.STATE_HOLDING }
            ?: activeCalls.firstOrNull()

        if (_currentCall.value != target) {
            updateCall(target)
        } else {
            // Re-evaluate conference status
            _isConferenceActive.value = activeCalls.any { isConference(it) } || isConference(target)
        }
    }

    fun updateCall(call: Call?) {
        _currentCall.value = call
        if (call != null) {
            _callState.value = call.state
            _isConferenceActive.value = _calls.value.any { it.state != Call.STATE_DISCONNECTED && isConference(it) } || isConference(call)
            if (call.state == Call.STATE_ACTIVE) {
                if (_activeStartTimestamp.value == 0L) {
                    val connect = call.details?.connectTimeMillis ?: 0L
                    _activeStartTimestamp.value = if (connect > 0L) connect else System.currentTimeMillis()
                }
            } else {
                _activeStartTimestamp.value = 0L
            }

            if (call.state == Call.STATE_HOLDING) {
                try { call.unhold() } catch (_: Exception) {}
            }

            val number = call.details?.handle?.schemeSpecificPart ?: ""
            _callerNumber.value = number
            val cnap = call.details?.callerDisplayName ?: ""

            // 1. Immediate resolution from in-memory ContactCache
            val cachedContact = if (number.isNotEmpty()) ContactCache.getContact(number) else null
            if (cachedContact != null && cachedContact.name.isNotBlank()) {
                _callerName.value = cachedContact.name
                _callerPhotoUri.value = cachedContact.photoUri
                _callerLabel.value = cachedContact.label
            } else if (cnap.isNotBlank()) {
                _callerName.value = cnap
                _callerPhotoUri.value = ""
                _callerLabel.value = ""
            } else {
                val cachedCnap = if (number.isNotEmpty()) ContactCache.getCnapName(number) else null
                _callerName.value = cachedCnap ?: ""
                _callerPhotoUri.value = ""
                _callerLabel.value = ""
            }

            // SIM Slot resolution
            val accountHandle = call.details?.accountHandle
            val ctx = appContext ?: inCallService?.applicationContext
            if (accountHandle != null && ctx != null) {
                try {
                    val activeSims = MultiSimManager.getActiveSimAccounts(ctx)
                    val matchedSim = activeSims.find { it.accountHandle == accountHandle }
                    if (matchedSim != null) {
                        _currentSimSlot.value = matchedSim.slotIndex + 1
                    }
                } catch (_: Exception) {}
            }

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

            // 2. Asynchronous contact resolution via system ContactsProvider if needed
            if (number.isNotEmpty() && (_callerName.value.isEmpty() || _callerName.value == number)) {
                resolveCallerDetailsAsync(number, cnap)
            }
        } else {
            autoStopRecordingIfNeeded()
            stopDtmf()
            _callState.value = Call.STATE_DISCONNECTED
            _callerNumber.value = ""
            _callerName.value = ""
            _callerCnapName.value = ""
            _callerPhotoUri.value = ""
            _callerLabel.value = ""
            _activeStartTimestamp.value = 0L
            _isConferenceActive.value = false
            if (_calls.value.isEmpty()) inCallService = null
        }
    }

    private fun resolveCallerDetailsAsync(number: String, cnap: String) {
        val ctx = appContext ?: inCallService?.applicationContext ?: return
        scope.launch {
            val resolvedName = getContactNameFromNumber(ctx, number)
            if (!resolvedName.isNullOrBlank()) {
                _callerName.value = resolvedName
                val contact = ContactCache.getContact(number)
                if (contact != null) {
                    if (contact.photoUri.isNotBlank()) _callerPhotoUri.value = contact.photoUri
                    if (contact.label.isNotBlank()) _callerLabel.value = contact.label
                }
                return@launch
            }
            if (cnap.isNotBlank()) {
                _callerName.value = cnap
                return@launch
            }
            val dbCnap = getSavedCnapName(ctx, number)
            if (!dbCnap.isNullOrBlank()) {
                _callerName.value = dbCnap
            }
        }
    }

    private fun persistCnap(number: String, cnap: String) {
        val ctx = appContext ?: inCallService?.applicationContext ?: return
        scope.launch {
            try {
                AppDatabase.getDatabase(ctx).dialerDao().insertSetting(AppSetting("cnap_" + number.filter { it.isDigit() }, cnap))
            } catch (_: Exception) {}
        }
    }

    private fun resolveCnapAsync(number: String) {
        val ctx = appContext ?: inCallService?.applicationContext ?: return
        scope.launch {
            val dbCnap = getSavedCnapName(ctx, number)
            if (!dbCnap.isNullOrBlank()) _callerCnapName.value = dbCnap
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
            if (autoTune) CallAudioHelper.prepareSpeakerForRecording(ctx, inCallService, _audioState.value)
            CallAudioRecorder.startRecording(ctx, _callerNumber.value.ifEmpty { "Unknown" })
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
            file.nameWithoutExtension.removePrefix("REC_").split("_").firstOrNull()?.filter { it.isDigit() }?.ifEmpty { "Unknown" } ?: "Unknown"
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
                if (autoExport) CallAudioRecorder.exportRecordingToPublicDownloads(ctx, file)
            } catch (_: Exception) {}
        }
    }

    fun mergeCalls() {
        val all = _calls.value.filter { it.state != Call.STATE_DISCONNECTED }
        val active = all.find { it.state == Call.STATE_ACTIVE }
        val held = all.find { it.state == Call.STATE_HOLDING }

        if (active != null && held != null) {
            try { active.conference(held) } catch (_: Exception) { held.conference(active) }
        } else {
            val current = _currentCall.value
            val other = all.firstOrNull { it != current }
            if (current != null && other != null) {
                try { current.conference(other) } catch (_: Exception) {}
            }
        }
    }

    fun updateWaitingCall(call: Call?) { _waitingCall.value = call }
    fun updateAudioState(state: CallAudioState?) { _audioState.value = state }

    fun answer() {
        try { _currentCall.value?.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (_: Exception) {}
    }

    fun disconnect() {
        try {
            val call = _currentCall.value ?: return
            if (call.state == Call.STATE_RINGING) call.reject(false, null) else call.disconnect()
            if (_calls.value.none { it != call && it.state != Call.STATE_DISCONNECTED }) updateCall(null)
        } catch (_: Exception) {}
    }

    fun setMuted(muted: Boolean) { inCallService?.setMuted(muted) }
    fun setSpeaker(speaker: Boolean) {
        inCallService?.setAudioRoute(if (speaker) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
    }
    fun setBluetooth(bluetooth: Boolean) {
        inCallService?.setAudioRoute(if (bluetooth) CallAudioState.ROUTE_BLUETOOTH else CallAudioState.ROUTE_EARPIECE)
    }
    fun setHold(hold: Boolean) {
        if (hold) _currentCall.value?.hold() else _currentCall.value?.unhold()
    }

    fun silenceRinger(context: Context) {
        (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.silenceRinger()
    }

    fun playDtmf(key: Char) {
        val call = _currentCall.value ?: return
        dtmfJob?.cancel()
        try { call.playDtmfTone(key) } catch (_: Exception) {}
        dtmfJob = scope.launch(Dispatchers.Default) {
            delay(150)
            try { call.stopDtmfTone() } catch (_: Exception) {}
        }
    }

    fun stopDtmf() {
        dtmfJob?.cancel()
        try { _currentCall.value?.stopDtmfTone() } catch (_: Exception) {}
    }

    fun formatOutgoingNumberWithClir(number: String, isHideCallerId: Boolean, clirPrefix: String): String {
        if (!isHideCallerId || number.isBlank() || isEmergencyNumber(number)) return number
        val clir = clirPrefix.trim().ifBlank { "#31#" }
        return if (number.startsWith(clir)) number else "$clir$number"
    }

    fun isEmergencyNumber(number: String): Boolean {
        val trimmed = number.trim()
        if (trimmed.isBlank()) return false
        val digits = trimmed.filter { it.isDigit() }
        val standardEmergency = setOf("911", "112", "999", "000", "108", "110", "119", "995", "100", "101", "102")
        if (digits in standardEmergency) return true
        if (digits.length > 3 && (digits.startsWith("1") || digits.startsWith("0"))) {
            val withoutPrefix = digits.substring(1)
            if (withoutPrefix in standardEmergency) return true
        }
        return try {
            PhoneNumberUtils.isEmergencyNumber(trimmed)
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun placeCall(context: Context, number: String, preferredSim: String = "Ask") {
        appContext = context.applicationContext
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.let { imm ->
            (context as? Activity)?.currentFocus?.windowToken?.let { imm.hideSoftInputFromWindow(it, 0) }
        }

        val isEmergency = isEmergencyNumber(number)
        val targetSlot = if (preferredSim.contains("2")) 2 else 1
        _currentSimSlot.value = targetSlot
        SimCallTracker.recordOutgoingCall(context, number, targetSlot)

        _currentCall.value?.takeIf { it.state == Call.STATE_ACTIVE }?.hold()

        val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        val hideId = prefs.getBoolean("is_hide_caller_id_enabled", false)
        val clirPrefix = prefs.getString("clir_prefix", "#31#") ?: "#31#"
        val dialedNumber = formatOutgoingNumberWithClir(number, hideId, clirPrefix)

        val uri = Uri.fromParts("tel", dialedNumber, null)
        val extras = Bundle()

        if (!isEmergency && preferredSim != "Ask") {
            val matchedAccount = findPhoneAccountForSlot(context, targetSlot, preferredSim)
            if (matchedAccount?.accountHandle != null) {
                // Official AOSP Telecom binding
                extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, matchedAccount.accountHandle)

                // Inject OEM-specific Multi-SIM extras so Samsung, Xiaomi, and MediaTek modems don't drop back to SIM 1
                extras.putInt("android.telephony.extra.SUBSCRIPTION_INDEX", matchedAccount.subscriptionId)
                extras.putInt("subscription", matchedAccount.subscriptionId)
                extras.putInt("phone_subscription", matchedAccount.subscriptionId)
                extras.putInt("slot", matchedAccount.slotIndex)
                extras.putInt("simSlot", matchedAccount.slotIndex)
                extras.putInt("com.android.phone.extra.slot", matchedAccount.slotIndex)
            }
        }

        val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        var placedSuccessfully = false
        if (tm != null) {
            try {
                tm.placeCall(uri, extras)
                placedSuccessfully = true
            } catch (_: Exception) {
                placedSuccessfully = false
            }
        }

        if (!placedSuccessfully) {
            try {
                val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                    if (extras.size() > 0) putExtras(extras)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(callIntent)
            } catch (_: Exception) {
                try {
                    val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(dialIntent)
                } catch (_: Exception) {}
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun findPhoneAccountForSlot(context: Context, slotIndex: Int, preferredSimLabel: String): com.example.util.SimAccountInfo? {
        val simAccounts = MultiSimManager.getActiveSimAccounts(context)
        if (simAccounts.isEmpty()) return null

        val matchByLabel = simAccounts.find {
            it.displayName.equals(preferredSimLabel, ignoreCase = true) ||
            it.carrierName.equals(preferredSimLabel, ignoreCase = true)
        }
        if (matchByLabel != null) return matchByLabel

        return simAccounts.find { it.slotIndex == (slotIndex - 1) } ?: simAccounts.firstOrNull()
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

        call?.disconnect()
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