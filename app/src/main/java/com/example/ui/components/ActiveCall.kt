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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.CallManager
import com.example.R
import com.example.model.CallRecording
import com.example.model.Contact
import com.example.ui.theme.LocalAmoledMode
import com.example.util.CallAudioHelper
import com.example.util.CallAudioRecorder
import com.example.util.CallGestureSensorManager
import com.example.util.RecordingFeedbackHelper
import com.example.util.RichHapticEngine
import kotlinx.coroutines.delay

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
    callState: Int = Call.STATE_DISCONNECTED,
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
    BackHandler(enabled = true) {
        onMinimize?.invoke()
    }

    val context = LocalContext.current
    val activeStartTimestamp by CallManager.activeStartTimestamp.collectAsStateWithLifecycle()
    var tickTrigger by remember { mutableIntStateOf(0) }

    val audioState by CallManager.audioState.collectAsStateWithLifecycle()
    val waitingCall by CallManager.waitingCall.collectAsStateWithLifecycle()
    val allCalls by CallManager.calls.collectAsStateWithLifecycle()
    val currentCall by CallManager.currentCall.collectAsStateWithLifecycle()
    val recorderIsActive by CallAudioRecorder.isRecording.collectAsStateWithLifecycle()

    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isBluetoothOn by remember { mutableStateOf(false) }
    var isOnHold by remember { mutableStateOf(false) }
    var isAddCallDialogOpen by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingStartTime by remember { mutableLongStateOf(0L) }
    var fakeActiveStartTimestamp by remember { mutableLongStateOf(0L) }

    // FIXED: Sync mute, bluetooth, and speaker routes with system audio updates (e.g. notifications / watch)
    LaunchedEffect(audioState) {
        audioState?.let {
            isBluetoothOn = (it.route == CallAudioState.ROUTE_BLUETOOTH)
            isSpeakerOn = (it.route == CallAudioState.ROUTE_SPEAKER)
            isMuted = it.isMuted
        }
    }

    // FIXED: Sync hold status with real carrier network state
    val currentCallState = if (isFake) {
        if (fakeState == "ACTIVE") Call.STATE_ACTIVE else Call.STATE_RINGING
    } else {
        callState
    }

    LaunchedEffect(currentCallState) {
        isOnHold = (currentCallState == Call.STATE_HOLDING)
    }

    LaunchedEffect(recorderIsActive) {
        if (recorderIsActive) {
            isRecording = true
            if (recordingStartTime == 0L) recordingStartTime = System.currentTimeMillis()
        } else {
            isRecording = false
            recordingStartTime = 0L
        }
    }

    val currentIsRecording by rememberUpdatedState(isRecording)
    val currentOnSaveRecording by rememberUpdatedState(onSaveRecording)

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Microphone permission granted. Tap Record to start.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission required for call recording", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (currentIsRecording || CallAudioRecorder.isRecording.value) {
                val result = CallAudioRecorder.stopRecording()
                CallAudioHelper.restoreAudioState(context, CallManager.inCallService)
                val file = result.file
                if (file != null && file.exists() && file.length() > 0L) {
                    currentOnSaveRecording(result.durationSeconds.coerceAtLeast(1L), file.absolutePath)
                }
            } else {
                CallAudioHelper.restoreAudioState(context, CallManager.inCallService)
            }
        }
    }

    LaunchedEffect(isFake, fakeState, callState) {
        if (isFake) {
            if (fakeState == "ACTIVE") {
                fakeActiveStartTimestamp = System.currentTimeMillis()
                if (alwaysRecordEnabled && !CallAudioRecorder.isRecording.value) {
                    RecordingFeedbackHelper.triggerRecordingStartFeedback(context, recordingChimeEnabled)
                    CallAudioRecorder.startRecording(context, contactNumber)
                }
                while (true) {
                    delay(1000)
                    tickTrigger++
                }
            }
        } else {
            if (callState == Call.STATE_ACTIVE) {
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
                ((System.currentTimeMillis() - fakeActiveStartTimestamp) / 1000).coerceAtLeast(0L).toInt()
            } else {
                0
            }
        } else {
            if (callState == Call.STATE_ACTIVE) {
                val start = if (activeStartTimestamp > 0L) activeStartTimestamp else System.currentTimeMillis()
                ((System.currentTimeMillis() - start) / 1000).coerceAtLeast(0L).toInt()
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

    val isAmoled = LocalAmoledMode.current
    val surfaceColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    val heldCall = remember(allCalls) { allCalls.firstOrNull { it.state == Call.STATE_HOLDING } }

    var isQuickDeclineMenuOpen by remember { mutableStateOf(false) }
    var isInCallDialpadOpen by remember { mutableStateOf(false) }
    var isNoteDialogOpen by remember { mutableStateOf(false) }

    var fakeParticipants by remember(contactName, contactNumber) {
        mutableStateOf(listOf(Pair(contactName, contactNumber)))
    }

    val participants = remember(isFake, currentCall, allCalls, contactName, contactNumber, contacts, fakeParticipants) {
        if (isFake) {
            fakeParticipants
        } else {
            val activeList = allCalls.filter { it.state != Call.STATE_DISCONNECTED }
            val conferenceCall = activeList.find {
                it.children.isNotEmpty() || it.details?.hasProperty(Call.Details.PROPERTY_CONFERENCE) == true
            }
            if (conferenceCall != null && conferenceCall.children.isNotEmpty()) {
                conferenceCall.children.map { child ->
                    val num = child.details?.handle?.schemeSpecificPart ?: ""
                    val name = contacts.find { it.number == num }?.name ?: num
                    Pair(name, num)
                }
            } else {
                val num = currentCall?.details?.handle?.schemeSpecificPart ?: contactNumber
                val name = contacts.find { it.number == num }?.name ?: contactName.ifEmpty { num }
                listOf(Pair(name, num))
            }
        }
    }

    val isConference = remember(isFake, fakeParticipants, currentCall, allCalls) {
        if (isFake) {
            fakeParticipants.size > 1
        } else {
            allCalls.any {
                it.state != Call.STATE_DISCONNECTED && (it.children.isNotEmpty() || it.details?.hasProperty(Call.Details.PROPERTY_CONFERENCE) == true)
            } || currentCall?.details?.hasProperty(Call.Details.PROPERTY_CONFERENCE) == true
        }
    }

    var isNear by remember { mutableStateOf(false) }
    val powerManager = remember(context) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isProximitySupported = remember(powerManager) {
        powerManager?.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) == true
    }

    val isHeadsetOn = remember(audioState) {
        audioState?.route == CallAudioState.ROUTE_WIRED_HEADSET
    }
    val isEarpieceActive = (audioState?.route == CallAudioState.ROUTE_EARPIECE) ||
            (audioState == null && !isSpeakerOn && !isBluetoothOn && !isHeadsetOn)

    val shouldActivateProximity = isEarpieceActive && !isSpeakerOn && !isBluetoothOn && !isHeadsetOn &&
            (currentCallState in listOf(Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_RINGING))

    DisposableEffect(shouldActivateProximity, powerManager) {
        var wakeLock: PowerManager.WakeLock? = null
        if (shouldActivateProximity && isProximitySupported && powerManager != null) {
            try {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "SecureDialer:InCallProximityWakeLock"
                )
                wakeLock.acquire(12 * 60 * 60 * 1000L)
            } catch (_: Exception) {}
        }
        onDispose {
            try {
                if (wakeLock?.isHeld == true) wakeLock?.release()
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(Unit) {
        val gestureManager = CallGestureSensorManager(
            context = context,
            onProximityChanged = { near -> isNear = near },
            onFlipFaceDown = {
                if (callState == Call.STATE_RINGING) {
                    CallManager.silenceRinger(context)
                }
            }
        )
        gestureManager.startListening()
        onDispose { gestureManager.stopListening() }
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

                if (isIncoming && isQuickDeclineMenuOpen) {
                    InCallQuickDeclineSheet(
                        contactNumber = contactNumber,
                        quickResponses = quickResponses,
                        onClose = { isQuickDeclineMenuOpen = false },
                        onQuickDecline = onQuickDecline
                    )
                } else if (isInCallDialpadOpen) {
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

                InCallControlGrid(
                    isInCallDialpadOpen = isInCallDialpadOpen,
                    onToggleDialpad = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        isInCallDialpadOpen = !isInCallDialpadOpen
                    },
                    isMuted = isMuted,
                    onToggleMute = {
                        val next = !isMuted
                        RichHapticEngine.performHaptic(context, if (next) RichHapticEngine.HapticStyle.WARNING else RichHapticEngine.HapticStyle.CLICK)
                        isMuted = next
                        CallManager.setMuted(next)
                    },
                    isSpeakerOn = isSpeakerOn,
                    onToggleSpeaker = {
                        val next = !isSpeakerOn
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                        isSpeakerOn = next
                        CallManager.setSpeaker(next)
                    },
                    isOnHold = isOnHold,
                    onToggleHold = {
                        val next = !isOnHold
                        RichHapticEngine.performHaptic(context, if (next) RichHapticEngine.HapticStyle.WARNING else RichHapticEngine.HapticStyle.CLICK)
                        isOnHold = next
                        CallManager.setHold(next)
                    },
                    isBluetoothOn = isBluetoothOn,
                    onToggleBluetooth = {
                        val next = !isBluetoothOn
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                        isBluetoothOn = next
                        CallManager.setBluetooth(next)
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
                        if (isRecording || CallAudioRecorder.isRecording.value) {
                            RecordingFeedbackHelper.triggerRecordingStopFeedback(context, recordingChimeEnabled)
                            val result = CallAudioRecorder.stopRecording()
                            if (autoTuneVolume) {
                                CallAudioHelper.restoreAudioState(context, CallManager.inCallService)
                            }
                            val file = result.file
                            if (file != null && file.exists() && file.length() > 0L) {
                                onSaveRecording(result.durationSeconds.coerceAtLeast(1L), file.absolutePath)
                                Toast.makeText(context, context.getString(R.string.recording_saved), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Call recording failed or file empty", Toast.LENGTH_SHORT).show()
                            }
                            isRecording = false
                        } else {
                            val hasAudioPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasAudioPermission) {
                                Toast.makeText(context, "Microphone permission required for call recording", Toast.LENGTH_SHORT).show()
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@InCallControlGrid
                            }

                            RecordingFeedbackHelper.triggerRecordingStartFeedback(context, recordingChimeEnabled)
                            val currentRoute = audioState?.route ?: CallAudioState.ROUTE_EARPIECE
                            val isHeadset = currentRoute == CallAudioState.ROUTE_BLUETOOTH || currentRoute == CallAudioState.ROUTE_WIRED_HEADSET

                            if (autoTuneVolume && !isHeadset) {
                                CallAudioHelper.prepareSpeakerForRecording(
                                    context = context,
                                    inCallService = CallManager.inCallService,
                                    currentAudioState = audioState
                                )
                            }
                            val started = CallAudioRecorder.startRecording(context, contactNumber)
                            if (started) {
                                recordingStartTime = System.currentTimeMillis()
                                isRecording = true
                                if (isHeadset) {
                                    Toast.makeText(context, context.getString(R.string.headset_recording_warning), Toast.LENGTH_LONG).show()
                                } else if (autoTuneVolume) {
                                    Toast.makeText(context, "⏺️ Recording started. Speaker tuned for balanced audio.", Toast.LENGTH_SHORT).show()
                                } else if (!isSpeakerOn) {
                                    Toast.makeText(context, "⏺️ Recording started. Tip: Turn ON Speakerphone.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, context.getString(R.string.recording_started), Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                if (autoTuneVolume && !isHeadset) {
                                    CallAudioHelper.restoreAudioState(context, CallManager.inCallService)
                                }
                                Toast.makeText(context, "Cannot start recording. Grant Microphone permission.", Toast.LENGTH_LONG).show()
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