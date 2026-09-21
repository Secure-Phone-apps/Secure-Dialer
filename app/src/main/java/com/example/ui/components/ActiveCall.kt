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

package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.ui.input.pointer.pointerInput
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.CallManager
import com.example.model.*
import com.example.ui.theme.LocalM3Expressive
import com.example.ui.theme.LocalAmoledMode
import kotlinx.coroutines.delay

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.util.RichHapticEngine

@Composable
fun ActiveCallScreen(
    contactName: String,
    contactNumber: String,
    preferredSim: String,
    quickResponses: List<String>,
    onHangUp: () -> Unit,
    onQuickDecline: (String) -> Unit,
    isIncoming: Boolean = false,
    contacts: List<Contact> = emptyList(),
    onAnswer: () -> Unit = {},
    callState: Int = android.telecom.Call.STATE_DISCONNECTED,
    recordingEnabled: Boolean = false,
    autoTuneVolume: Boolean = true,
    recordingChimeEnabled: Boolean = false,
    alwaysRecordEnabled: Boolean = false,
    onSaveRecording: (Long, String) -> Unit = { _, _ -> },
    callNotesEnabled: Boolean = true,
    onSaveNote: (String) -> Unit = {},
    onMinimize: (() -> Unit)? = null,
    avatarShapeType: String = "circular",
    isPocketProtectionEnabled: Boolean = false,
    isFake: Boolean = false,
    fakeState: String = "RINGING",
    onFakeAnswer: () -> Unit = {},
    onFakeHangUp: () -> Unit = {}
) {
    androidx.activity.compose.BackHandler(enabled = true) {
        onMinimize?.invoke()
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val activeStartTimestamp by CallManager.activeStartTimestamp.collectAsStateWithLifecycle()
    var tickTrigger by remember { mutableStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isBluetoothOn by remember { mutableStateOf(false) }
    var isOnHold by remember { mutableStateOf(false) }
    var isAddCallDialogOpen by remember { mutableStateOf(false) }
    var addCallNumberInput by remember { mutableStateOf("") }
    var selectedAddCallContactName by remember { mutableStateOf("") }

    var isRecording by remember { mutableStateOf(false) }
    var recordingStartTime by remember { mutableLongStateOf(0L) }
    val recorderIsActive by com.example.util.CallAudioRecorder.isRecording.collectAsStateWithLifecycle()

    LaunchedEffect(recorderIsActive) {
        if (recorderIsActive) {
            isRecording = true
            if (recordingStartTime == 0L) {
                recordingStartTime = System.currentTimeMillis()
            }
        } else {
            isRecording = false
            recordingStartTime = 0L
        }
    }

    val currentIsRecording by rememberUpdatedState(isRecording)
    val currentRecordingStartTime by rememberUpdatedState(recordingStartTime)
    val currentOnSaveRecording by rememberUpdatedState(onSaveRecording)

    var fakeActiveStartTimestamp by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            if (currentIsRecording || com.example.util.CallAudioRecorder.isRecording.value) {
                val result = com.example.util.CallAudioRecorder.stopRecording()
                com.example.util.CallAudioHelper.restoreAudioState(context, CallManager.inCallService)
                val file = result.file
                if (file != null && file.exists() && file.length() > 0L) {
                    val duration = result.durationSeconds.coerceAtLeast(1L)
                    currentOnSaveRecording(duration, file.absolutePath)
                }
            } else {
                com.example.util.CallAudioHelper.restoreAudioState(context, CallManager.inCallService)
            }
        }
    }

    val currentCallState = if (isFake) {
        if (fakeState == "ACTIVE") android.telecom.Call.STATE_ACTIVE else android.telecom.Call.STATE_RINGING
    } else {
        callState
    }

    LaunchedEffect(isFake, fakeState, callState) {
        if (isFake) {
            if (fakeState == "ACTIVE") {
                fakeActiveStartTimestamp = System.currentTimeMillis()
                if (alwaysRecordEnabled && !com.example.util.CallAudioRecorder.isRecording.value) {
                    com.example.util.RecordingFeedbackHelper.triggerRecordingStartFeedback(context, recordingChimeEnabled)
                    com.example.util.CallAudioRecorder.startRecording(context, contactNumber)
                }
                while (true) {
                    delay(1000)
                    tickTrigger++
                }
            }
        } else {
            if (callState == android.telecom.Call.STATE_ACTIVE) {
                while (true) {
                    delay(1000)
                    tickTrigger++
                }
            }
        }
    }

    val callDuration = remember(activeStartTimestamp, tickTrigger, currentCallState, isFake, fakeActiveStartTimestamp) {
        if (isFake) {
            if (fakeState == "ACTIVE" && fakeActiveStartTimestamp > 0L) {
                val durationMs = System.currentTimeMillis() - fakeActiveStartTimestamp
                (durationMs / 1000).coerceAtLeast(0L).toInt()
            } else {
                0
            }
        } else {
            if (callState == android.telecom.Call.STATE_ACTIVE) {
                val start = if (activeStartTimestamp > 0L) activeStartTimestamp else System.currentTimeMillis()
                val durationMs = System.currentTimeMillis() - start
                (durationMs / 1000).coerceAtLeast(0L).toInt()
            } else {
                0
            }
        }
    }

    val formattedTime = remember(callDuration) {
        val mins = callDuration / 60
        val secs = callDuration % 60
        "%02d:%02d".format(mins, secs)
    }

    // Dynamic Surface Color based on state
    val isAmoled = LocalAmoledMode.current
    val surfaceColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    val audioState by CallManager.audioState.collectAsStateWithLifecycle()
    val waitingCall by CallManager.waitingCall.collectAsStateWithLifecycle()
    val allCalls by CallManager.calls.collectAsStateWithLifecycle()
    val currentCall by CallManager.currentCall.collectAsStateWithLifecycle()
    val heldCall = remember(allCalls) { allCalls.firstOrNull { it.state == android.telecom.Call.STATE_HOLDING } }

    LaunchedEffect(audioState) {
        audioState?.let {
            isBluetoothOn = (it.route == android.telecom.CallAudioState.ROUTE_BLUETOOTH)
            isSpeakerOn = (it.route == android.telecom.CallAudioState.ROUTE_SPEAKER)
        }
    }

    var isQuickDeclineMenuOpen by remember { mutableStateOf(false) }
    var isInCallDialpadOpen by remember { mutableStateOf(false) }
    var isNoteDialogOpen by remember { mutableStateOf(false) }
    var inCallDialpadInput by remember { mutableStateOf("") }
    var fakeParticipants by remember(contactName, contactNumber) {
        mutableStateOf(listOf(Pair(contactName, contactNumber)))
    }

    val participants = remember(isFake, currentCall, allCalls, contactName, contactNumber, contacts, fakeParticipants) {
        if (isFake) {
            fakeParticipants
        } else {
            val allCallsList = allCalls.filter { it.state != android.telecom.Call.STATE_DISCONNECTED }
            // Identify if there is a conference call in the list (either has children or has conference property)
            val conferenceCall = allCallsList.find { 
                it.children.isNotEmpty() || 
                it.details?.hasProperty(android.telecom.Call.Details.PROPERTY_CONFERENCE) == true 
            }
            if (conferenceCall != null) {
                val children = conferenceCall.children
                if (children.isNotEmpty()) {
                    children.map { child ->
                        val number = child.details?.handle?.schemeSpecificPart ?: ""
                        val name = contacts.find { it.number == number }?.name ?: number
                        Pair(name, number)
                    }
                } else {
                    listOf(Pair("Conference Call", ""))
                }
            } else {
                val number = currentCall?.details?.handle?.schemeSpecificPart ?: contactNumber
                val name = contacts.find { it.number == number }?.name ?: contactName.ifEmpty { number }
                listOf(Pair(name, number))
            }
        }
    }

    val isConference = remember(isFake, fakeParticipants, currentCall, allCalls) {
        if (isFake) {
            fakeParticipants.size > 1
        } else {
            val allCallsList = allCalls.filter { it.state != android.telecom.Call.STATE_DISCONNECTED }
            allCallsList.any { 
                it.children.isNotEmpty() || 
                it.details?.hasProperty(android.telecom.Call.Details.PROPERTY_CONFERENCE) == true 
            } || currentCall?.details?.hasProperty(android.telecom.Call.Details.PROPERTY_CONFERENCE) == true
        }
    }

    var isNear by remember { mutableStateOf(false) }

    // Proximity Screen-Off WakeLock: Turns off physical screen & digitizer when held to ear
    val powerManager = remember(context) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isProximitySupported = remember(powerManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerManager?.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) == true
        } else {
            false
        }
    }

    val isHeadsetOn = remember(audioState) {
        audioState?.route == android.telecom.CallAudioState.ROUTE_WIRED_HEADSET
    }
    val isEarpieceActive = (audioState?.route == android.telecom.CallAudioState.ROUTE_EARPIECE) ||
        (audioState == null && !isSpeakerOn && !isBluetoothOn && !isHeadsetOn)

    // Proximity screen-off should be active during calls ONLY when callAudioState is ROUTE_EARPIECE (not speaker, bluetooth, or wired headset)
    val shouldActivateProximity = isEarpieceActive && !isSpeakerOn && !isBluetoothOn && !isHeadsetOn &&
        (currentCallState == android.telecom.Call.STATE_ACTIVE ||
         currentCallState == android.telecom.Call.STATE_DIALING ||
         currentCallState == android.telecom.Call.STATE_CONNECTING ||
         currentCallState == android.telecom.Call.STATE_RINGING)

    DisposableEffect(shouldActivateProximity, powerManager) {
        var wakeLock: PowerManager.WakeLock? = null
        if (shouldActivateProximity && isProximitySupported && powerManager != null) {
            try {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "SecureDialer:InCallProximityWakeLock"
                )
                wakeLock.acquire()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onDispose {
            try {
                if (wakeLock != null && wakeLock.isHeld) {
                    wakeLock.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(context) {
        val gestureManager = com.example.util.CallGestureSensorManager(
            context = context,
            onProximityChanged = { near -> isNear = near },
            onFlipFaceDown = {
                if (callState == android.telecom.Call.STATE_RINGING) {
                    CallManager.silenceRinger(context)
                }
            }
        )
        gestureManager.startListening()

        onDispose {
            gestureManager.stopListening()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = surfaceColor
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            InCallHeader(
                isOnHold = isOnHold,
                callState = currentCallState,
                participants = participants,
                preferredSim = preferredSim,
                contactName = contactName,
                contactNumber = contactNumber,
                formattedTime = formattedTime,
                heldCall = heldCall,
                contacts = contacts,
                onMerge = {
                    if (isFake) {
                        Toast.makeText(context, "📞 Merged call (Simulation)", Toast.LENGTH_SHORT).show()
                    } else {
                        CallManager.mergeCalls()
                    }
                },
                isConference = isConference,
                isRecording = isRecording
            )

            waitingCall?.let { call ->
                InCallWaitingCallDialog(
                    waitingCall = call,
                    contacts = contacts,
                    avatarShapeType = avatarShapeType
                )
            }

            // Middle Call Screen options
            if (isIncoming && isQuickDeclineMenuOpen) {
                InCallQuickDeclineSheet(
                    contactNumber = contactNumber,
                    quickResponses = quickResponses,
                    onClose = { isQuickDeclineMenuOpen = false },
                    onQuickDecline = onQuickDecline
                )
            } else if (isInCallDialpadOpen) {
                // Interactive In-Call Keypad for DTMF entry
                InCallKeypad(
                    onClose = { isInCallDialpadOpen = false },
                    avatarShapeType = avatarShapeType
                )
            } else {
                InCallAvatarDisplay(
                    participants = participants,
                    contactName = contactName,
                    contactNumber = contactNumber,
                    contacts = contacts,
                    avatarShapeType = avatarShapeType
                )
            }

            // Add Call Dialog
            if (isAddCallDialogOpen) {
                InCallAddCallDialog(
                    contacts = contacts,
                    onDismiss = { isAddCallDialogOpen = false },
                    onAddCall = { finalName, number ->
                        if (isFake) {
                            fakeParticipants = fakeParticipants + Pair(finalName, number)
                        } else {
                            CallManager.placeCall(context, number)
                        }
                    },
                    avatarShapeType = avatarShapeType
                )
            }

            // Call Option buttons: keypad, mute, speaker, hold, bluetooth
            var isNoteDialogOpen by remember { mutableStateOf(false) }

            InCallControlGrid(
                isInCallDialpadOpen = isInCallDialpadOpen,
                onToggleDialpad = {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                    isInCallDialpadOpen = !isInCallDialpadOpen
                },
                isMuted = isMuted,
                onToggleMute = {
                    RichHapticEngine.performHaptic(context, if (!isMuted) RichHapticEngine.HapticStyle.WARNING else RichHapticEngine.HapticStyle.CLICK)
                    isMuted = !isMuted
                    CallManager.setMuted(isMuted)
                },
                isSpeakerOn = isSpeakerOn,
                onToggleSpeaker = {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                    isSpeakerOn = !isSpeakerOn
                    CallManager.setSpeaker(isSpeakerOn)
                },
                isOnHold = isOnHold,
                onToggleHold = {
                    RichHapticEngine.performHaptic(context, if (!isOnHold) RichHapticEngine.HapticStyle.WARNING else RichHapticEngine.HapticStyle.CLICK)
                    isOnHold = !isOnHold
                    CallManager.setHold(isOnHold)
                },
                isBluetoothOn = isBluetoothOn,
                onToggleBluetooth = {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                    isBluetoothOn = !isBluetoothOn
                    CallManager.setBluetooth(isBluetoothOn)
                },
                isAddCallDialogOpen = isAddCallDialogOpen,
                onOpenAddCallDialog = {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                    isAddCallDialogOpen = true
                },
                recordingEnabled = recordingEnabled,
                callNotesEnabled = callNotesEnabled,
                isRecording = isRecording,
                onToggleRecording = {
                    if (isRecording || com.example.util.CallAudioRecorder.isRecording.value) {
                        com.example.util.RecordingFeedbackHelper.triggerRecordingStopFeedback(context, recordingChimeEnabled)
                        val result = com.example.util.CallAudioRecorder.stopRecording()
                        if (autoTuneVolume) {
                            com.example.util.CallAudioHelper.restoreAudioState(context, CallManager.inCallService)
                        }
                        val file = result.file
                        if (file != null && file.exists() && file.length() > 0L) {
                            val duration = result.durationSeconds.coerceAtLeast(1L)
                            onSaveRecording(duration, file.absolutePath)
                            Toast.makeText(context, context.getString(R.string.recording_saved), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Call recording failed or file empty", Toast.LENGTH_SHORT).show()
                        }
                        isRecording = false
                    } else {
                        com.example.util.RecordingFeedbackHelper.triggerRecordingStartFeedback(context, recordingChimeEnabled)
                        val currentRoute = audioState?.route ?: android.telecom.CallAudioState.ROUTE_EARPIECE
                        val isHeadset = currentRoute == android.telecom.CallAudioState.ROUTE_BLUETOOTH || 
                                        currentRoute == android.telecom.CallAudioState.ROUTE_WIRED_HEADSET

                        // If auto-tune volume enabled and not wearing headset, prepare 55% sweet spot and route to speaker
                        if (autoTuneVolume && !isHeadset) {
                            com.example.util.CallAudioHelper.prepareSpeakerForRecording(
                                context = context,
                                inCallService = CallManager.inCallService,
                                currentAudioState = audioState
                            )
                        }
                        val started = com.example.util.CallAudioRecorder.startRecording(context, contactNumber)
                        if (started) {
                            recordingStartTime = System.currentTimeMillis()
                            isRecording = true
                            if (isHeadset) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.headset_recording_warning),
                                    Toast.LENGTH_LONG
                                ).show()
                            } else if (autoTuneVolume) {
                                Toast.makeText(
                                    context,
                                    "⏺️ Recording started. Speaker tuned to 55% for balanced two-way audio.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (!isSpeakerOn) {
                                Toast.makeText(
                                    context,
                                    "⏺️ Recording started. Tip: Turn ON Speakerphone to capture both sides clearly.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(context, context.getString(R.string.recording_started), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (autoTuneVolume && !isHeadset) {
                                com.example.util.CallAudioHelper.restoreAudioState(context, CallManager.inCallService)
                            }
                            Toast.makeText(context, "Cannot start recording. Grant Microphone permission in app settings.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                isNoteDialogOpen = isNoteDialogOpen,
                onOpenNoteDialog = {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                    isNoteDialogOpen = true
                },
                avatarShapeType = avatarShapeType
            )

            if (isNoteDialogOpen) {
                InCallNoteDialog(
                    onDismiss = { isNoteDialogOpen = false },
                    onSaveNote = onSaveNote
                )
            }

            InCallBottomBar(
                isIncoming = if (isFake) (fakeState == "RINGING") else isIncoming,
                onAnswer = if (isFake) onFakeAnswer else onAnswer,
                onHangUp = if (isFake) onFakeHangUp else onHangUp,
                onToggleQuickDeclineMenu = { isQuickDeclineMenuOpen = !isQuickDeclineMenuOpen },
                avatarShapeType = avatarShapeType
            )
        }
        }

        if (onMinimize != null) {
            IconButton(
                onClick = {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                    onMinimize()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimize Call",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        if (isNear && shouldActivateProximity) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isPocketProtectionEnabled) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.pocket_lock_active),
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.pocket_lock_active),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.pocket_lock_desc),
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
