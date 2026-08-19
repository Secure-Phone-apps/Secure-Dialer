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
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CallRecord
import com.example.ui.viewmodel.DialerViewModel

@Composable
fun CallbackReminderDialog(
    context: Context,
    primaryRecord: CallRecord,
    viewModel: DialerViewModel,
    onDismiss: () -> Unit
) {
    var reminderNoteText by remember { mutableStateOf("") }
    var reminderDelayValue by remember { mutableStateOf("15") }
    var reminderDelayUnit by remember { mutableStateOf("min") }

    val finalDelaySeconds = remember(reminderDelayValue, reminderDelayUnit) {
        val value = reminderDelayValue.toIntOrNull() ?: 0
        when (reminderDelayUnit) {
            "sec" -> value
            "min" -> value * 60
            "hour" -> value * 3600
            "day" -> value * 86400
            else -> value * 60
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { 
            Text(
                text = stringResource(R.string.callback_reminders_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(
                        R.string.remind_call_back_prompt, 
                        if (primaryRecord.name == "Unknown") primaryRecord.number else primaryRecord.name
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = reminderDelayValue,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                            reminderDelayValue = newValue
                        }
                    },
                    label = { Text(stringResource(R.string.duration_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("sec", "min", "hour", "day").forEach { unit ->
                        FilterChip(
                            selected = reminderDelayUnit == unit,
                            onClick = { reminderDelayUnit = unit },
                            label = { 
                                Text(
                                    text = unit, 
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                ) 
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = reminderNoteText,
                    onValueChange = { reminderNoteText = it },
                    label = { Text(stringResource(R.string.optional_note_label)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val triggerTime = System.currentTimeMillis() + (finalDelaySeconds * 1000L)
                    viewModel.addReminder(
                        number = primaryRecord.number,
                        name = if (primaryRecord.name == "Unknown") primaryRecord.number else primaryRecord.name,
                        triggerTime = triggerTime,
                        note = reminderNoteText
                    )
                    Toast.makeText(context, "Callback alarm scheduled!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
