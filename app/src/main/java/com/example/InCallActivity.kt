/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.model.CallRecording
import com.example.model.Contact
import com.example.model.QuickResponse
import com.example.ui.components.ActiveCallScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.CallAudioRecorder
import com.example.util.LockScreenCallPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dedicated lockscreen-isolated Activity hosting exclusively the active/incoming call UI.
 * Keeps MainActivity (Contacts, Recents, Recordings, Settings) strictly behind the device lock
 * while allowing instant incoming-call answering without an automatic keyguard prompt.
 */
class InCallActivity : ComponentActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isDeviceLockedState = mutableStateOf(false)
    private var keyguardReceiverRegistered = false

    private val keyguardStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshLockState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CallManager.appContext = applicationContext
        volumeControlStream = AudioManager.STREAM_VOICE_CALL

        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        enableEdgeToEdge()
        LockScreenCallPolicy.applyLockScreenWindowFlags(this, showWhenLocked = true, keepScreenOn = true)
        refreshLockState()
        handleLaunchIntent(intent)

        val prefs = getSharedPreferences(LockScreenCallPolicy.PREFS_NAME, Context.MODE_PRIVATE)
        val dao = AppDatabase.getDatabase(applicationContext).dialerDao()
        val repository = DialerRepository(applicationContext)

        setContent {
            val isDarkTheme by remember { mutableStateOf(prefs.getBoolean("is_dark_theme", true)) }
            val isAmoledMode by remember { mutableStateOf(prefs.getBoolean("is_amoled_mode", false)) }
            val isM3Expressive by remember { mutableStateOf(prefs.getBoolean("is_m3_expressive", true)) }
            val useDynamicColor by remember {
                mutableStateOf(prefs.getBoolean("use_dynamic_color", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S))
            }
            val themeColor by remember { mutableStateOf(prefs.getString("theme_color", "expressive_lime") ?: "expressive_lime") }
            val customColorHex by remember { mutableStateOf(prefs.getString("custom_color_hex", "#68A500") ?: "#68A500") }
            val preferredSim by remember { mutableStateOf(prefs.getString("preferred_sim", "Ask") ?: "Ask") }
            val avatarShapeType by remember { mutableStateOf(prefs.getString("avatar_shape_type", "squircle") ?: "squircle") }
            val recordingEnabled by remember { mutableStateOf(prefs.getBoolean("recording_enabled", false)) }
            val autoTuneVolume by remember { mutableStateOf(prefs.getBoolean("auto_tune_recording_volume", true)) }
            val recordingChimeEnabled by remember { mutableStateOf(prefs.getBoolean("recording_chime_enabled", false)) }
            val alwaysRecordEnabled by remember { mutableStateOf(prefs.getBoolean("is_auto_record_calls_enabled", false)) }
            val callNotesEnabled by remember { mutableStateOf(prefs.getBoolean("is_call_notes_enabled", true)) }
            val isPocketProtectionEnabled by remember { mutableStateOf(prefs.getBoolean("is_pocket_protection_enabled", false)) }

            val currentCall by CallManager.currentCall.collectAsStateWithLifecycle()
            val allCalls by CallManager.calls.collectAsStateWithLifecycle()
            val callState by CallManager.callState.collectAsStateWithLifecycle()
            val callerNumber by CallManager.callerNumber.collectAsStateWithLifecycle()
            val callerName by CallManager.callerName.collectAsStateWithLifecycle()
            val callerCnapName by CallManager.callerCnapName.collectAsStateWithLifecycle()
            val isDeviceLocked by isDeviceLockedState

            val quickResponseEntities by dao.getQuickResponsesFlow()
                .collectAsStateWithLifecycle(initialValue = emptyList<QuickResponse>())
            val quickResponses = remember(quickResponseEntities) { quickResponseEntities.map { it.message } }

            // Only expose full contact list for Add Call picker once the keyguard is unlocked
            val isLastNameFirst = remember { prefs.getString("contact_name_format", "FIRST_NAME_FIRST") == "LAST_NAME_FIRST" }
            val allContacts by dao.getAllContactsFlow(isLastNameFirst)
                .collectAsStateWithLifecycle(initialValue = emptyList<Contact>())
            val visibleContacts = if (isDeviceLocked) emptyList() else allContacts

            var resolvedName by remember { mutableStateOf("") }
            LaunchedEffect(callerNumber, callerName, callerCnapName) {
                if (callerName.isNotBlank() && callerName != callerNumber && callerName != "Unknown") {
                    resolvedName = callerName
                } else if (callerNumber.isNotBlank()) {
                    val contactMatch = getContactNameFromNumber(applicationContext, callerNumber)
                    resolvedName = when {
                        !contactMatch.isNullOrBlank() -> contactMatch
                        callerCnapName.isNotBlank() -> callerCnapName
                        else -> getSavedCnapName(applicationContext, callerNumber) ?: callerNumber
                    }
                    if (resolvedName.isNotBlank() && resolvedName != callerNumber) {
                        CallManager.setCallerName(resolvedName)
                    }
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                        refreshLockState()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Close InCallActivity smoothly once all Telecom calls are disconnected
            LaunchedEffect(currentCall, allCalls, callState) {
                val hasActiveCalls = currentCall != null || allCalls.any { it.state != Call.STATE_DISCONNECTED }
                if (!hasActiveCalls && !CallManager.isInPreAttachPlacementWindow()) {
                    if (CallManager.hasPendingOutgoingCancel()) {
                        finishCallScreen()
                    } else {
                        delay(420)
                        val stillEmpty = CallManager.currentCall.value == null &&
                            CallManager.calls.value.none { it.state != Call.STATE_DISCONNECTED }
                        if (stillEmpty) {
                            finishCallScreen()
                        }
                    }
                }
            }

            // Never leak MainActivity from back press; send only InCallActivity to back
            BackHandler(enabled = true) {
                moveTaskToBack(true)
            }

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                dynamicColor = useDynamicColor,
                themeColor = themeColor,
                customColorHex = customColorHex,
                isAmoledMode = isAmoledMode,
                isM3Expressive = isM3Expressive
            ) {
                ActiveCallScreen(
                    contactName = resolvedName.ifBlank { callerName.ifBlank { callerNumber.ifBlank { getString(R.string.notification_incoming_call_title) } } },
                    contactNumber = callerNumber,
                    preferredSim = preferredSim,
                    quickResponses = quickResponses,
                    onHangUp = {
                        CallManager.disconnect()
                        if (CallManager.currentCall.value == null && CallManager.calls.value.isEmpty()) {
                            finishCallScreen()
                        }
                    },
                    onAnswer = {
                        answerWithLockPolicy()
                    },
                    onQuickDecline = {
                        CallManager.disconnect()
                        if (CallManager.currentCall.value == null && CallManager.calls.value.isEmpty()) {
                            finishCallScreen()
                        }
                    },
                    isIncoming = callState == Call.STATE_RINGING,
                    contacts = visibleContacts,
                    callState = callState,
                    recordingEnabled = recordingEnabled,
                    autoTuneVolume = autoTuneVolume,
                    recordingChimeEnabled = recordingChimeEnabled,
                    alwaysRecordEnabled = alwaysRecordEnabled,
                    callNotesEnabled = callNotesEnabled,
                    onSaveRecording = { duration, filePath ->
                        val number = CallManager.callerNumber.value.ifEmpty { callerNumber.ifEmpty { "Unknown" } }
                        val name = resolvedName.ifBlank { CallManager.callerName.value.ifBlank { number } }
                        activityScope.launch {
                            try {
                                val recording = CallRecording(
                                    name = name,
                                    number = number,
                                    timestamp = CallAudioRecorder.formatRecordingTimestamp(applicationContext),
                                    duration = duration,
                                    filePath = filePath
                                )
                                repository.saveCallRecording(recording)
                                val autoExport = prefs.getBoolean("is_auto_export_recordings_enabled", true)
                                if (autoExport) {
                                    CallAudioRecorder.exportRecordingToPublicDownloads(applicationContext, java.io.File(filePath))
                                }
                            } catch (_: Exception) {}
                        }
                    },
                    onSaveNote = { content ->
                        val number = CallManager.callerNumber.value.ifEmpty { callerNumber }
                        if (number.isNotBlank() && content.isNotBlank()) {
                            activityScope.launch {
                                try {
                                    repository.saveCallNote(number, content)
                                } catch (_: Exception) {}
                            }
                        }
                    },
                    onMinimize = if (isDeviceLocked) null else { { moveTaskToBack(true) } },
                    isDeviceLocked = isDeviceLocked,
                    onRequestUnlock = { actionAfterUnlock ->
                        LockScreenCallPolicy.requestUnlockThen(this@InCallActivity) {
                            refreshLockState()
                            actionAfterUnlock()
                        }
                    },
                    avatarShapeType = avatarShapeType,
                    isPocketProtectionEnabled = isPocketProtectionEnabled
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        CallManager.setInCallUiVisible(true)
        LockScreenCallPolicy.applyLockScreenWindowFlags(this, showWhenLocked = true, keepScreenOn = true)
        registerKeyguardReceiver()
        refreshLockState()
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        CallManager.setInCallUiVisible(true)
        refreshLockState()
    }

    override fun onPause() {
        super.onPause()
        CallManager.setInCallUiVisible(false)
    }

    override fun onStop() {
        super.onStop()
        CallManager.setInCallUiVisible(false)
        unregisterKeyguardReceiver()
    }

    override fun onDestroy() {
        CallManager.setInCallUiVisible(false)
        unregisterKeyguardReceiver()
        activityScope.cancel()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        LockScreenCallPolicy.applyLockScreenWindowFlags(this, showWhenLocked = true, keepScreenOn = true)
        refreshLockState()
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent == null) return
        val shouldAnswer = intent.getBooleanExtra(MyInCallService.EXTRA_ANSWER, false) ||
            intent.action == MyInCallService.ACTION_ANSWER
        if (shouldAnswer) {
            answerWithLockPolicy()
        }
    }

    private fun answerWithLockPolicy() {
        if (LockScreenCallPolicy.shouldPromptUnlockBeforeAnswering(this)) {
            LockScreenCallPolicy.requestUnlockThen(this) {
                refreshLockState()
                CallManager.answer()
            }
        } else {
            CallManager.answer()
        }
    }

    private fun refreshLockState() {
        isDeviceLockedState.value = LockScreenCallPolicy.isDeviceLocked(this)
    }

    private fun finishCallScreen() {
        CallManager.setInCallUiVisible(false)
        LockScreenCallPolicy.applyLockScreenWindowFlags(this, showWhenLocked = false, keepScreenOn = false)
        if (!isFinishing && !isDestroyed) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else {
                finish()
            }
        }
    }

    private fun registerKeyguardReceiver() {
        if (!keyguardReceiverRegistered) {
            try {
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_USER_PRESENT)
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                }
                ContextCompat.registerReceiver(
                    this,
                    keyguardStateReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
                keyguardReceiverRegistered = true
            } catch (_: Exception) {}
        }
    }

    private fun unregisterKeyguardReceiver() {
        if (keyguardReceiverRegistered) {
            try {
                unregisterReceiver(keyguardStateReceiver)
            } catch (_: Exception) {}
            keyguardReceiverRegistered = false
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                event.keyCode == KeyEvent.KEYCODE_VOLUME_UP
            val isRinging = CallManager.currentCall.value?.state == Call.STATE_RINGING ||
                CallManager.calls.value.any { it.state == Call.STATE_RINGING } ||
                CallManager.waitingCall.value?.state == Call.STATE_RINGING

            if (isVolumeKey && isRinging) {
                CallManager.silenceRinger(this)
                return true
            }

            val isActiveCall = CallManager.currentCall.value?.state == Call.STATE_ACTIVE ||
                CallManager.calls.value.any { it.state == Call.STATE_ACTIVE }
            if (isVolumeKey && isActiveCall) {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val direction = if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    AudioManager.ADJUST_RAISE
                } else {
                    AudioManager.ADJUST_LOWER
                }
                audioManager?.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, direction, AudioManager.FLAG_SHOW_UI)
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
