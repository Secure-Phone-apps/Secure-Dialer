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

import android.content.Intent
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.R
import com.example.model.CallRecording
import com.example.ui.theme.AppShapes
import com.example.ui.viewmodel.DialerViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

/**
 * Dedicated, isolated audio player card for call recordings.
 * Isolates playback progress updates to prevent recomposition churn on parent lists.
 */
@Composable
fun RecentCallAudioPlayerCard(
    isPlaying: Boolean,
    currentPositionMs: Int,
    totalDurationMs: Int,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = AppShapes.Card,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledIconButton(
                    onClick = onTogglePlay
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(if (isPlaying) R.string.btn_answer else R.string.call_status_ongoing)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    val progress = if (totalDurationMs > 0) currentPositionMs.toFloat() / totalDurationMs.toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(AppShapes.Chip)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val currentSec = currentPositionMs / 1000
                        val totalSec = totalDurationMs / 1000
                        Text(
                            text = String.format(Locale.ROOT, "%02d:%02d", currentSec / 60, currentSec % 60),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.ROOT, "%02d:%02d", totalSec / 60, totalSec % 60),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Direct Recording Player Dialog allowing playback, switching between multiple recordings,
 * noting, sharing, and deleting call recordings directly from the recent calls list.
 */
@Composable
fun DirectRecordingPlayerDialog(
    recordings: List<CallRecording>,
    onDismiss: () -> Unit,
    viewModel: DialerViewModel
) {
    val context = LocalContext.current
    var selectedIndex by remember { mutableIntStateOf(0) }
    val currentRecording = recordings.getOrNull(selectedIndex) ?: recordings.firstOrNull()

    if (currentRecording == null) {
        onDismiss()
        return
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf((currentRecording.duration * 1000).toInt()) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isEditingNote by remember { mutableStateOf(false) }
    var noteInput by remember(currentRecording.id) { mutableStateOf(currentRecording.note) }

    fun stopPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        isPlaying = false
        currentPositionMs = 0
    }

    fun startPlaying(rec: CallRecording) {
        stopPlayer()
        val file = File(rec.filePath)
        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(context, context.getString(R.string.toast_deleted_recording), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val mp = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                totalDurationMs = duration.coerceAtLeast(1000)
                setOnCompletionListener {
                    isPlaying = false
                    currentPositionMs = 0
                }
                start()
            }
            mediaPlayer = mp
            isPlaying = true
        } catch (_: Exception) {
            stopPlayer()
            Toast.makeText(context, context.getString(R.string.toast_deleted_recording), Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(currentRecording.id) {
        onDispose {
            stopPlayer()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(100)
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        currentPositionMs = mp.currentPosition
                    }
                } catch (_: Exception) {}
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            stopPlayer()
            onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.settings_recordings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${currentRecording.name.ifEmpty { currentRecording.number }} • ${currentRecording.timestamp}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (recordings.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        recordings.forEachIndexed { idx, rec ->
                            FilterChip(
                                selected = selectedIndex == idx,
                                onClick = {
                                    stopPlayer()
                                    selectedIndex = idx
                                },
                                label = { Text("#${idx + 1} (${rec.duration}s)") }
                            )
                        }
                    }
                }

                RecentCallAudioPlayerCard(
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    totalDurationMs = totalDurationMs,
                    onTogglePlay = {
                        if (isPlaying) {
                            stopPlayer()
                        } else {
                            startPlaying(currentRecording)
                        }
                    }
                )

                if (isEditingNote) {
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text(stringResource(R.string.jot_call_note_title)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { isEditingNote = false }) {
                            Text(stringResource(R.string.btn_cancel))
                        }
                        Button(
                            onClick = {
                                viewModel.updateCallRecordingNote(currentRecording.id, noteInput.trim())
                                if (noteInput.isNotBlank() && currentRecording.number.isNotBlank()) {
                                    viewModel.saveCallNote(currentRecording.number, noteInput.trim())
                                }
                                isEditingNote = false
                                Toast.makeText(context, context.getString(R.string.note_saved), Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(stringResource(R.string.btn_save_note))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                noteInput = currentRecording.note
                                isEditingNote = true
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (currentRecording.note.isNotBlank()) "📝 ${currentRecording.note}" else "➕ ${stringResource(R.string.jot_call_note_title)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (currentRecording.note.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = {
                            noteInput = currentRecording.note
                            isEditingNote = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.btn_edit),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val file = File(currentRecording.filePath)
                            if (file.exists()) {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "audio/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_recording)))
                            } else {
                                Toast.makeText(context, context.getString(R.string.toast_deleted_recording), Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.share_recording))
                    }

                    TextButton(
                        onClick = {
                            stopPlayer()
                            viewModel.deleteCallRecording(currentRecording.id)
                            try {
                                val file = File(currentRecording.filePath)
                                if (file.exists()) file.delete()
                            } catch (_: Exception) {}
                            Toast.makeText(context, context.getString(R.string.toast_deleted_recording), Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.delete_recording))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    stopPlayer()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
