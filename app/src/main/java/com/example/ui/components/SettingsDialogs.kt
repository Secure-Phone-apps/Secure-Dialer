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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.RichHapticEngine

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_about)) },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.about_version_label, com.example.BuildConfig.VERSION_NAME), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.about_app_desc), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.about_mission_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun PrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.privacy_policy_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.privacy_intro), style = MaterialTheme.typography.titleSmall)
                Text(
                    "• " + stringResource(R.string.privacy_section_1_desc) + "\n" +
                    "• " + stringResource(R.string.privacy_section_2_desc) + "\n" +
                    "• " + stringResource(R.string.privacy_section_3_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

fun exportRecordingToDownloads(context: android.content.Context, filePath: String) {
    try {
        val file = java.io.File(filePath)
        if (!file.exists()) {
            android.widget.Toast.makeText(context, context.getString(R.string.file_save_failed), android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val success = com.example.util.CallAudioRecorder.exportRecordingToPublicDownloads(context, file)
        val msg = if (success) context.getString(R.string.file_saved_success) else context.getString(R.string.file_save_failed)
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, context.getString(R.string.file_save_failed), android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun SimRenameDialog(
    viewModel: DialerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activeSims = remember(context) { com.example.util.MultiSimManager.getActiveSimAccounts(context) }
    val displaySims = remember(activeSims) {
        if (activeSims.size >= 2) activeSims.take(2)
        else {
            val sim1 = activeSims.getOrNull(0) ?: com.example.util.SimAccountInfo(0, 1, "SIM 1", "SIM 1", "", null)
            val sim2 = activeSims.getOrNull(1) ?: com.example.util.SimAccountInfo(1, 2, "SIM 2", "SIM 2", "", null)
            listOf(sim1, sim2)
        }
    }

    var editingSim by remember { mutableStateOf<com.example.util.SimAccountInfo?>(null) }
    var customLabelInput by remember(editingSim) {
        mutableStateOf(editingSim?.let { viewModel.customSimLabels[it.subscriptionId] } ?: "")
    }

    val simRenamedMsg = stringResource(R.string.toast_sim_renamed)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_rename_sims)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val activeEditingSim = editingSim
                if (activeEditingSim == null) {
                    Text(
                        text = stringResource(R.string.settings_rename_sims_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    displaySims.forEach { sim ->
                        val currentCustom = viewModel.customSimLabels[sim.subscriptionId]
                        val defaultLabel = "SIM ${sim.slotIndex + 1}"
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    editingSim = sim
                                    customLabelInput = currentCustom ?: ""
                                },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentCustom?.takeIf { it.isNotBlank() } ?: defaultLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(
                                            R.string.sim_system_slot_info,
                                            sim.carrierName.ifEmpty { sim.displayName },
                                            sim.slotIndex + 1
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    val defaultLabel = "SIM ${activeEditingSim.slotIndex + 1}"
                    Text(
                        text = stringResource(R.string.dialog_rename_sim_prompt, defaultLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customLabelInput,
                        onValueChange = { customLabelInput = it },
                        label = { Text(defaultLabel) },
                        placeholder = { Text(stringResource(R.string.sim_label_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            val activeEditingSim = editingSim
            if (activeEditingSim != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                            viewModel.updateSimCustomLabel(activeEditingSim.subscriptionId, "")
                            Toast.makeText(context, context.getString(R.string.toast_sim_label_reset), Toast.LENGTH_SHORT).show()
                            editingSim = null
                        }
                    ) {
                        Text(stringResource(R.string.btn_reset))
                    }
                    Button(
                        onClick = {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                            viewModel.updateSimCustomLabel(activeEditingSim.subscriptionId, customLabelInput.trim())
                            Toast.makeText(context, simRenamedMsg, Toast.LENGTH_SHORT).show()
                            editingSim = null
                        }
                    ) {
                        Text(stringResource(R.string.btn_save))
                    }
                }
            } else {
                TextButton(
                    onClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        },
        dismissButton = {
            if (editingSim != null) {
                TextButton(
                    onClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        editingSim = null
                    }
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        }
    )
}

