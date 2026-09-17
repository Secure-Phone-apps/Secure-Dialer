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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.model.CallRecording
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.VaultBiometricAuthHelper
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallRecordingsSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val haptic = LocalHapticFeedback.current
    val recordings by viewModel.recordingsFlow.collectAsState()
    val isVaultLockEnabled by viewModel.isRecordingsBiometricLockEnabled
    var isVaultUnlocked by remember { mutableStateOf(!isVaultLockEnabled) }

    var playingId by remember { mutableIntStateOf(-1) }
    var currentPosMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(1) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var noteEditingRecording by remember { mutableStateOf<CallRecording?>(null) }
    var shareSheetRecording by remember { mutableStateOf<CallRecording?>(null) }

    // Synchronize unlock state when lock toggle changes
    LaunchedEffect(isVaultLockEnabled) {
        if (!isVaultLockEnabled) {
            isVaultUnlocked = true
        }
    }

    // Automatically prompt when opening screen if biometric vault is enabled and locked
    LaunchedEffect(Unit) {
        if (isVaultLockEnabled && !isVaultUnlocked && activity != null) {
            VaultBiometricAuthHelper.authenticate(
                activity = activity,
                title = context.getString(R.string.vault_locked_title),
                subtitle = context.getString(R.string.vault_locked_desc),
                onSuccess = {
                    isVaultUnlocked = true
                }
            )
        }
    }

    // Manage media player and lock reset on lifecycle stop
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, isVaultLockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && isVaultLockEnabled) {
                isVaultUnlocked = false
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                playingId = -1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mediaPlayer?.release()
            mediaPlayer = null
            if (isVaultLockEnabled) {
                isVaultUnlocked = false
            }
        }
    }

    // M3 Bottom Sheet for Exporting and Sharing Call Recordings
    if (shareSheetRecording != null) {
        val rec = shareSheetRecording!!
        val file = remember(rec.filePath) { File(rec.filePath) }
        val fileSizeStr = remember(file) {
            if (file.exists()) "${file.length() / 1024} KB" else "0 KB"
        }

        ModalBottomSheet(
            onDismissRequest = { shareSheetRecording = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 36.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Recording Meta
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.share_recording_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${rec.name.ifEmpty { rec.number }} • ${rec.timestamp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // File metadata badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBgColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.recording_details),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Duration: ${rec.duration}s  •  Size: $fileSizeStr",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (rec.note.isNotBlank()) {
                                Text(
                                    text = "Note: ${rec.note}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = ".M4A",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Action 1: Share via Apps
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBgColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            shareSheetRecording = null
                            if (file.exists()) {
                                try {
                                    val uri: Uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/mp4"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_recording_title)))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Unable to share audio file", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.share_via_apps),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.share_via_apps_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action 2: Save to Downloads
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBgColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            shareSheetRecording = null
                            exportRecordingToDownloads(context, rec.filePath)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.save_to_downloads),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.save_to_downloads_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (noteEditingRecording != null) {
        val rec = noteEditingRecording!!
        var noteInput by remember { mutableStateOf(rec.note) }
        AlertDialog(
            onDismissRequest = { noteEditingRecording = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.jot_call_note_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${rec.name.ifEmpty { rec.number }} • ${rec.timestamp}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        placeholder = { Text(stringResource(R.string.note_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateCallRecordingNote(rec.id, noteInput.trim())
                    if (noteInput.isNotBlank() && rec.number.isNotBlank()) {
                        viewModel.saveCallNote(rec.number, noteInput.trim())
                    }
                    noteEditingRecording = null
                    Toast.makeText(context, context.getString(R.string.note_saved), Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.btn_save_note))
                }
            },
            dismissButton = {
                TextButton(onClick = { noteEditingRecording = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Android Call Recording Guide & Architectural Notice
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp).padding(top = 2.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Android Call Recording Notice",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Due to Android 10+ system privacy restrictions on non-system apps, direct line capture is restricted by Google. To record both parties clearly, turn ON Speakerphone during the call.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Auto-tune Speaker Volume Option
        val autoTuneVolume by viewModel.autoTuneRecordingVolume
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = cardBgColor,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_auto_tune_volume),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_auto_tune_volume_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
                Switch(
                    checked = autoTuneVolume,
                    onCheckedChange = { viewModel.updateAutoTuneRecordingVolume(it) }
                )
            }
        }

        // Recording Audio Chime & Haptics Option
        val recordingChime by viewModel.recordingChimeEnabled
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = cardBgColor,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_recording_chime),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_recording_chime_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
                Switch(
                    checked = recordingChime,
                    onCheckedChange = { viewModel.updateRecordingChimeEnabled(it) }
                )
            }
        }

        // Lock Recordings with Biometrics Switch Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = cardBgColor,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_recordings_biometric_lock),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_recordings_biometric_lock_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
                Switch(
                    checked = isVaultLockEnabled,
                    onCheckedChange = { enabled ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (enabled && activity != null) {
                            VaultBiometricAuthHelper.authenticate(
                                activity = activity,
                                title = context.getString(R.string.vault_locked_title),
                                subtitle = context.getString(R.string.vault_locked_desc),
                                onSuccess = {
                                    viewModel.updateRecordingsBiometricLockEnabled(true)
                                    isVaultUnlocked = true
                                }
                            )
                        } else {
                            viewModel.updateRecordingsBiometricLockEnabled(enabled)
                            if (!enabled) isVaultUnlocked = true
                        }
                    }
                )
            }
        }

        if (isVaultLockEnabled && !isVaultUnlocked) {
            // Locked Vault Guard View
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Vault Locked",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.vault_locked_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.vault_locked_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (activity != null) {
                                VaultBiometricAuthHelper.authenticate(
                                    activity = activity,
                                    title = context.getString(R.string.vault_locked_title),
                                    subtitle = context.getString(R.string.vault_locked_desc),
                                    onSuccess = {
                                        isVaultUnlocked = true
                                    }
                                )
                            } else {
                                isVaultUnlocked = true
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_unlock_vault))
                    }
                }
            }
        } else if (recordings.isEmpty()) {
            SettingsEmptyState(
                icon = Icons.Default.Mic,
                title = stringResource(R.string.no_recordings_title),
                description = stringResource(R.string.no_recordings_desc),
                tintColor = MaterialTheme.colorScheme.primary
            )
        } else {
            recordings.forEach { rec ->
                val isPlaying = playingId == rec.id
                CompactRecordingRow(
                    recording = rec,
                    isPlaying = isPlaying,
                    currentPosMs = currentPosMs,
                    durationMs = durationMs,
                    playbackSpeed = playbackSpeed,
                    cardBgColor = cardBgColor,
                    onPlayToggle = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isPlaying) {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            mediaPlayer = null
                            playingId = -1
                        } else {
                            mediaPlayer?.release()
                            val file = File(rec.filePath)
                            if (!file.exists()) {
                                Toast.makeText(context, "Audio file not found on disk", Toast.LENGTH_SHORT).show()
                                return@CompactRecordingRow
                            }
                            if (file.length() == 0L) {
                                Toast.makeText(context, "Audio file is empty", Toast.LENGTH_SHORT).show()
                                return@CompactRecordingRow
                            }
                            try {
                                val mp = MediaPlayer().apply {
                                    setDataSource(rec.filePath)
                                    prepare()
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && playbackSpeed != 1.0f) {
                                        try {
                                            playbackParams = playbackParams.setSpeed(playbackSpeed)
                                        } catch (_: Exception) {}
                                    }
                                    start()
                                    setOnCompletionListener {
                                        playingId = -1
                                        currentPosMs = 0
                                    }
                                }
                                mediaPlayer = mp
                                durationMs = mp.duration.coerceAtLeast(1)
                                playingId = rec.id
                            } catch (e: Exception) {
                                Toast.makeText(context, "Unable to play audio: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onSpeedChange = { speed ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        playbackSpeed = speed
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(speed) ?: android.media.PlaybackParams().apply { this.speed = speed }
                            } catch (_: Exception) {}
                        }
                    },
                    onEditNote = {
                        noteEditingRecording = rec
                    },
                    onExport = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        shareSheetRecording = rec
                    },
                    onShare = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        shareSheetRecording = rec
                    },
                    onDelete = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isPlaying) {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            mediaPlayer = null
                            playingId = -1
                        }
                        viewModel.deleteCallRecording(rec.id)
                        try { File(rec.filePath).delete() } catch (_: Exception) {}
                        Toast.makeText(context, context.getString(R.string.toast_deleted_recording), Toast.LENGTH_SHORT).show()
                    }
                )

                if (isPlaying) {
                    LaunchedEffect(playingId) {
                        while (playingId == rec.id && mediaPlayer?.isPlaying == true) {
                            currentPosMs = mediaPlayer?.currentPosition ?: 0
                            delay(150)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactRecordingRow(
    recording: CallRecording,
    isPlaying: Boolean,
    currentPosMs: Int,
    durationMs: Int,
    playbackSpeed: Float,
    cardBgColor: Color,
    onPlayToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onEditNote: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else cardBgColor,
        border = if (isPlaying) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Play / Pause round button
                FilledIconButton(
                    onClick = onPlayToggle,
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Info: Contact/Number, Time, Duration
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.name.ifEmpty { recording.number },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val fileSizeStr = remember(recording.filePath) {
                        val f = File(recording.filePath)
                        if (f.exists()) "${f.length() / 1024} KB" else ""
                    }
                    Text(
                        text = "${recording.timestamp} • ${recording.duration}s${if (fileSizeStr.isNotEmpty()) " • $fileSizeStr" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    if (recording.note.isNotBlank()) {
                        Text(
                            text = "📝 ${recording.note}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }

                // Compact Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((-4).dp)
                ) {
                    IconButton(
                        onClick = onEditNote,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Note",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onExport,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Inline Scrubbing Bar & Playback Speed Controls when playing
            AnimatedVisibility(
                visible = isPlaying,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val progress = (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentSec = currentPosMs / 1000
                        val totalSec = durationMs / 1000
                        Text(
                            text = "${String.format("%02d:%02d", currentSec / 60, currentSec % 60)} / ${String.format("%02d:%02d", totalSec / 60, totalSec % 60)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 1.0x, 1.5x, 2.0x Playback Speed Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(1.0f, 1.5f, 2.0f).forEach { speed ->
                                val selected = playbackSpeed == speed
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { onSpeedChange(speed) }
                                ) {
                                    Text(
                                        text = "${speed}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 10.sp,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
