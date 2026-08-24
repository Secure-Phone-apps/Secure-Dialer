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
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.R
import com.example.ui.theme.LocalM3Expressive
import com.example.ui.viewmodel.DialerViewModel
import kotlinx.coroutines.delay
import java.io.File

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

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (recordings.isEmpty()) {
            SettingsEmptyState(
                icon = Icons.Default.Mic,
                title = stringResource(R.string.no_recordings_title),
                description = stringResource(R.string.no_recordings_desc),
                tintColor = MaterialTheme.colorScheme.primary
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recordings.forEach { rec ->
                    val isPlaying = playingId == rec.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPlaying) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            } else {
                                cardBgColor
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(if (LocalM3Expressive.current) MaterialTheme.shapes.medium else CircleShape)
                                            .background(
                                                if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(onClick = {
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
                                                    return@IconButton
                                                }
                                                if (file.length() == 0L) {
                                                    Toast.makeText(context, "Audio file is empty. Please re-record during a call.", Toast.LENGTH_SHORT).show()
                                                    return@IconButton
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
                                                    Toast.makeText(context, "Unable to play audio file. Invalid format or empty recording.", Toast.LENGTH_LONG).show()
                                                    e.printStackTrace()
                                                }
                                            }
                                        }) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isPlaying) "Pause" else "Play",
                                                tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = rec.name.ifEmpty { rec.number },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (rec.name.isNotEmpty() && rec.name != rec.number) {
                                            Text(
                                                text = rec.number,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        val formattedSize = remember(rec.filePath) {
                                            val f = File(rec.filePath)
                                            if (f.exists()) "${f.length() / 1024} KB" else ""
                                        }
                                        Text(
                                            text = "${rec.timestamp} • ${rec.duration}s${if (formattedSize.isNotEmpty()) " • $formattedSize" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row {
                                    IconButton(onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        exportRecordingToDownloads(context, rec.filePath)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Export to Downloads",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val file = File(rec.filePath)
                                        if (file.exists()) {
                                            try {
                                                val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share Recording",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (isPlaying) {
                                            mediaPlayer?.stop()
                                            mediaPlayer?.release()
                                            mediaPlayer = null
                                            playingId = -1
                                        }
                                        viewModel.deleteCallRecording(rec.id)
                                        try { File(rec.filePath).delete() } catch (e: Exception) {}
                                        Toast.makeText(context, context.getString(R.string.toast_deleted_recording), Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete recording",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            if (isPlaying) {
                                LaunchedEffect(playingId) {
                                    while (playingId == rec.id && mediaPlayer?.isPlaying == true) {
                                        currentPosMs = mediaPlayer?.currentPosition ?: 0
                                        delay(200)
                                    }
                                }
                                val progress = (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(MaterialTheme.shapes.extraSmall),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "${currentPosMs / 1000}s / ${durationMs / 1000}s",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
