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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R

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

