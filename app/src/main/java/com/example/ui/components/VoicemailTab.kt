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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.CallRecord
import com.example.model.getAvatarShape
import com.example.model.getInitials
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.RichHapticEngine
import java.util.Locale

@Composable
fun VoicemailTabContent(
    voicemailRecords: List<CallRecord>,
    onPlayClick: (CallRecord) -> Unit,
    viewModel: DialerViewModel? = null
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.voicemail_header),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (voicemailRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_voicemails_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.no_voicemails_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(voicemailRecords, key = { it.id }) { record ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = record.name.ifBlank { record.number },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = {
                                    // FIXED: Formats real duration from record.duration instead of hardcoded 0:24
                                    val durationStr = if (record.duration > 0) {
                                        val mins = record.duration / 60
                                        val secs = record.duration % 60
                                        String.format(Locale.ROOT, "%02d:%02d", mins, secs)
                                    } else {
                                        "00:00"
                                    }
                                    Text(
                                        text = "${record.timestamp} • $durationStr",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                leadingContent = {
                                    val avatarShape = getAvatarShape(viewModel?.avatarShapeType?.value ?: "circular")
                                    Surface(
                                        modifier = Modifier.size(40.dp),
                                        shape = avatarShape,
                                        color = record.avatarBg
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (record.photoUri.isNotEmpty()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(record.photoUri)
                                                        .size(128, 128)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = record.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                val isSaved = record.name != record.number && record.name != "Unknown" && record.name.isNotBlank()
                                                if (isSaved) {
                                                    Text(
                                                        text = record.avatarText.ifEmpty { getInitials(record.name) },
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = record.avatarTextColor,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = record.avatarTextColor,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                trailingContent = {
                                    FilledIconButton(
                                        onClick = {
                                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                            onPlayClick(record)
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = stringResource(R.string.call_status_ongoing)
                                        )
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            Text(
                                text = stringResource(R.string.voicemail_sample_transcript),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}