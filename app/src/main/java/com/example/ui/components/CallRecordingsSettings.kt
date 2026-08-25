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
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
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
import com.example.R
import com.example.model.CallRecording
import com.example.ui.viewmodel.DialerViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Composable
fun CallRecordingsSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val recordings by viewModel.recordingsFlow.collectAsState()
    var playingId by remember { mutableIntStateOf(-1) }
    var currentPosMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(1) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var noteEditingRecording by remember { mutableStateOf<CallRecording?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
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
        if (recordings.isEmpty()) {
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
                    onEditNote = {
                        noteEditingRecording = rec
                    },
                    onExport = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        exportRecordingToDownloads(context, rec.filePath)
                    },
                    onShare = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val file = File(rec.filePath)
                        if (file.exists()) {
                            try {
                                val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "audio/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Call Recording"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Unable to share audio file", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
                        }
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
    cardBgColor: Color,
    onPlayToggle: () -> Unit,
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

            // Inline Scrubbing Bar when playing
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val currentSec = currentPosMs / 1000
                        val totalSec = durationMs / 1000
                        Text(
                            text = String.format("%02d:%02d", currentSec / 60, currentSec % 60),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%02d:%02d", totalSec / 60, totalSec % 60),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
