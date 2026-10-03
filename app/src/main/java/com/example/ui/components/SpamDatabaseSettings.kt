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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.RichHapticEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SpamDatabaseSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current
    val timestamp = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date()) }
    val spamList by viewModel.spamFlow.collectAsStateWithLifecycle()

    var manualNumber by remember { mutableStateOf("") }
    var manualLabel by remember { mutableStateOf("") }

    val saveSpamCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportSpamNumbersToCsv { csvData ->
                viewModel.writeTextToUri(uri, csvData) { success ->
                    if (success) {
                        Toast.makeText(context, context.getString(R.string.file_saved_success), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.file_save_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val openSpamFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.readTextFromUri(uri) { content ->
                if (!content.isNullOrBlank()) {
                    viewModel.importSpamNumbersFromCsv(content) { count ->
                        Toast.makeText(context, context.getString(R.string.spam_import_count_success, count), Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.file_read_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status info & Export/Import Controls
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(
                    text = stringResource(R.string.spam_entries_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.spam_numbers_blocked, spamList.size),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        saveSpamCsvLauncher.launch("spam_database_$timestamp.csv")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.btn_export_spam_csv),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        openSpamFileLauncher.launch(arrayOf("text/csv", "text/plain", "text/comma-separated-values", "*/*"))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.btn_import_spam_file),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }

        // Add Manual Entry Item
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_spam_manually_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // FIXED: Numerical keyboard for phone number entry
                    OutlinedTextField(
                        value = manualNumber,
                        onValueChange = { manualNumber = it },
                        label = { Text(stringResource(R.string.label_phone_number_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = manualLabel,
                        onValueChange = { manualLabel = it },
                        label = { Text(stringResource(R.string.label_spam_label_hint)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Button(
                    onClick = {
                        val trimmedNum = manualNumber.trim()
                        if (trimmedNum.isNotBlank()) {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                            viewModel.addSpamNumber(trimmedNum, manualLabel.ifBlank { "Spam" })
                            Toast.makeText(context, context.getString(R.string.file_saved_success), Toast.LENGTH_SHORT).show()
                            manualNumber = ""
                            manualLabel = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_add_to_offline_db))
                }
            }
        }

        // List block
        if (spamList.isEmpty()) {
            SettingsEmptyState(
                icon = Icons.Default.VerifiedUser,
                title = stringResource(R.string.blocklist_empty_title),
                description = stringResource(R.string.blocklist_empty_desc),
                tintColor = MaterialTheme.colorScheme.primary
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.blocked_offline_numbers_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                TextButton(onClick = {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.WARNING)
                    viewModel.clearAllSpam()
                    Toast.makeText(context, context.getString(R.string.toast_deleted_recording), Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_clear_all))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                spamList.forEach { spam ->
                    ListItem(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        headlineContent = { Text(spam.number, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(spam.label) },
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                viewModel.deleteSpamNumber(spam)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }
        }
    }
}