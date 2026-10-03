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

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.RichHapticEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BlockListSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current
    val timestamp = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date()) }
    val blockedNumbersEntities by viewModel.blockedNumbersFlow.collectAsStateWithLifecycle()
    var newBlockedInput by remember { mutableStateOf("") }

    val saveBlocklistLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBlockedNumbers { blocklistData ->
                viewModel.writeTextToUri(uri, blocklistData) { success ->
                    if (success) {
                        Toast.makeText(context, context.getString(R.string.file_saved_success), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.file_save_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val openBlocklistLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.readTextFromUri(uri) { content ->
                if (!content.isNullOrBlank()) {
                    viewModel.importBlockedNumbers(content) { count ->
                        Toast.makeText(context, context.getString(R.string.blocklist_import_count_success, count), Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.file_read_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // FIXED: Added Phone keyboard type for numerical input
        OutlinedTextField(
            value = newBlockedInput,
            onValueChange = { newBlockedInput = it },
            label = { Text(stringResource(R.string.enter_number_to_block)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (newBlockedInput.isNotEmpty()) {
                    IconButton(onClick = { newBlockedInput = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.dialpad_clear))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val trimmed = newBlockedInput.trim()
                if (trimmed.isNotBlank()) {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.WARNING)
                    viewModel.addBlockedNumber(trimmed)
                    newBlockedInput = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = MaterialTheme.shapes.small
        ) {
            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.block_this_number), fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                    saveBlocklistLauncher.launch("blocked_numbers_$timestamp.txt")
                },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_export_blocklist), style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
                onClick = {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                    openBlocklistLauncher.launch(arrayOf("text/plain", "text/csv", "application/json", "*/*"))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_import_blocklist), style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.blocked_callers_header),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (blockedNumbersEntities.isEmpty()) {
            SettingsEmptyState(
                icon = Icons.Default.Block,
                title = stringResource(R.string.no_blocked_numbers_title),
                description = stringResource(R.string.no_blocked_numbers_desc),
                tintColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // FIXED: Direct entity iteration eliminates redundant list mapping allocations
                blockedNumbersEntities.forEach { blockedItem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = cardBgColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = blockedItem.number,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                viewModel.removeBlockedNumber(blockedItem.number)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.unblock),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}