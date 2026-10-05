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

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AppShapes

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
    callNotesEnabled: Boolean = true,
    isNoteDialogOpen: Boolean,
    onOpenNoteDialog: () -> Unit,
    avatarShapeType: String = "squircle"
) {
    val btnShape = AppShapes.Keypad
    val buttonHeight = 68.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Keypad, Mute, Speaker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InCallButton(
                icon = Icons.Default.Dialpad,
                label = stringResource(R.string.keypad),
                isActive = isInCallDialpadOpen,
                onClick = onToggleDialpad,
                shape = btnShape,
                modifier = Modifier.weight(1f).height(buttonHeight)
            )
            InCallButton(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = stringResource(R.string.mute),
                isActive = isMuted,
                onClick = onToggleMute,
                shape = btnShape,
                modifier = Modifier.weight(1f).height(buttonHeight)
            )
            InCallButton(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                label = stringResource(R.string.speaker),
                isActive = isSpeakerOn,
                onClick = onToggleSpeaker,
                shape = btnShape,
                modifier = Modifier.weight(1f).height(buttonHeight)
            )
        }

        // Row 2: Hold, Bluetooth, Add Call
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InCallButton(
                icon = Icons.Default.Pause,
                label = stringResource(R.string.hold),
                isActive = isOnHold,
                onClick = onToggleHold,
                shape = btnShape,
                modifier = Modifier.weight(1f).height(buttonHeight)
            )
            InCallButton(
                icon = Icons.Default.Bluetooth,
                label = stringResource(R.string.bluetooth),
                isActive = isBluetoothOn,
                onClick = onToggleBluetooth,
                shape = btnShape,
                modifier = Modifier.weight(1f).height(buttonHeight)
            )
            InCallButton(
                icon = Icons.Default.GroupAdd,
                label = stringResource(R.string.add_call),
                isActive = isAddCallDialogOpen,
                onClick = onOpenAddCallDialog,
                shape = btnShape,
                modifier = Modifier.weight(1f).height(buttonHeight)
            )
        }

        // Row 3: Recording & Note (Clean, balanced 3-slot row with no stretched buttons)
        if (recordingEnabled || callNotesEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (recordingEnabled) {
                    InCallButton(
                        icon = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        label = if (isRecording) stringResource(R.string.recording) else stringResource(R.string.record),
                        isActive = isRecording,
                        onClick = onToggleRecording,
                        shape = btnShape,
                        isPulsingRecording = isRecording,
                        modifier = Modifier.weight(1f).height(buttonHeight)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (callNotesEnabled) {
                    InCallButton(
                        icon = Icons.Default.EditNote,
                        label = stringResource(R.string.call_note),
                        isActive = isNoteDialogOpen,
                        onClick = onOpenNoteDialog,
                        shape = btnShape,
                        modifier = Modifier.weight(1f).height(buttonHeight)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}