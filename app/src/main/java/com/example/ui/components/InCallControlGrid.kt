package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.CallManager
import com.example.R
import com.example.model.getAvatarShape

@Composable
fun InCallControlGrid(
    isInCallDialpadOpen: Boolean,
    onToggleDialpad: () -> Unit,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    isSpeakerOn: Boolean,
    onToggleSpeaker: () -> Unit,
    isOnHold: Boolean,
    onToggleHold: () -> Unit,
    isBluetoothOn: Boolean,
    onToggleBluetooth: () -> Unit,
    isAddCallDialogOpen: Boolean,
    onOpenAddCallDialog: () -> Unit,
    recordingEnabled: Boolean,
    isRecording: Boolean,
    onToggleRecording: () -> Unit,
    isNoteDialogOpen: Boolean,
    onOpenNoteDialog: () -> Unit,
    avatarShapeType: String = "circular"
) {
    val haptic = LocalHapticFeedback.current
    val btnShape = getAvatarShape(avatarShapeType)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Row 1: Keypad, Mute, Speaker
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                InCallButton(
                    icon = Icons.Default.Dialpad,
                    label = stringResource(R.string.keypad),
                    isActive = isInCallDialpadOpen,
                    onClick = onToggleDialpad,
                    shape = btnShape
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                InCallButton(
                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = stringResource(R.string.mute),
                    isActive = isMuted,
                    onClick = onToggleMute,
                    shape = btnShape
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                InCallButton(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    label = stringResource(R.string.speaker),
                    isActive = isSpeakerOn,
                    onClick = onToggleSpeaker,
                    shape = btnShape
                )
            }
        }

        // Row 2: Hold, Bluetooth, Add Call
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                InCallButton(
                    icon = Icons.Default.Pause,
                    label = stringResource(R.string.hold),
                    isActive = isOnHold,
                    onClick = onToggleHold,
                    shape = btnShape
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                InCallButton(
                    icon = Icons.Default.Bluetooth,
                    label = stringResource(R.string.bluetooth),
                    isActive = isBluetoothOn,
                    onClick = onToggleBluetooth,
                    shape = btnShape
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                InCallButton(
                    icon = Icons.Default.GroupAdd,
                    label = stringResource(R.string.add_call),
                    isActive = isAddCallDialogOpen,
                    onClick = onOpenAddCallDialog,
                    shape = btnShape
                )
            }
        }

        // Row 3: Record & Note
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (recordingEnabled) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    InCallButton(
                        icon = Icons.Default.Mic,
                        label = if (isRecording) stringResource(R.string.recording) else stringResource(R.string.record),
                        isActive = isRecording,
                        onClick = onToggleRecording,
                        shape = btnShape
                    )
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    InCallButton(
                        icon = Icons.Default.EditNote,
                        label = stringResource(R.string.call_note),
                        isActive = isNoteDialogOpen,
                        onClick = onOpenNoteDialog,
                        shape = btnShape
                    )
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    // Spacer to align symmetrically
                }
            } else {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {}

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    InCallButton(
                        icon = Icons.Default.EditNote,
                        label = stringResource(R.string.call_note),
                        isActive = isNoteDialogOpen,
                        onClick = onOpenNoteDialog,
                        shape = btnShape
                    )
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {}
            }
        }
    }
}
