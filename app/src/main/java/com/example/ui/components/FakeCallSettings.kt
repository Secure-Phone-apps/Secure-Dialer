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

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.FakeCallReceiver

@Composable
fun FakeCallSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current

    var fakeName by remember { mutableStateOf("Boss") }
    var fakeNumber by remember { mutableStateOf("+1 (555) 492-0192") }
    
    var delayValue by remember { mutableStateOf("10") }
    var delayUnit by remember { mutableStateOf("sec") } // "sec", "min", "hour"

    val finalDelaySeconds = remember(delayValue, delayUnit) {
        val value = delayValue.toIntOrNull() ?: 0
        when (delayUnit) {
            "min" -> value * 60
            "hour" -> value * 3600
            else -> value // "sec"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Configuration Inputs
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.caller_identity),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                OutlinedTextField(
                    value = fakeName,
                    onValueChange = { fakeName = it },
                    label = { Text(stringResource(R.string.caller_name_label)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Quick presets in sleek single row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(
                        "Boss" to "+1 (555) 492-0192",
                        "Mom" to "+1 (555) 728-1100",
                        "Doctor" to "+1 (800) 555-0133",
                        "Lawyer" to "+1 (888) 555-9128",
                        "Driver" to "+1 (555) 233-1090"
                    )
                    items(presets) { (presetName, presetNum) ->
                        FilterChip(
                            selected = fakeName == presetName,
                            onClick = {
                                fakeName = presetName
                                fakeNumber = presetNum
                            },
                            label = { Text(presetName, maxLines = 1) }
                        )
                    }
                }

                OutlinedTextField(
                    value = fakeNumber,
                    onValueChange = { fakeNumber = it },
                    label = { Text(stringResource(R.string.label_phone_number_hint)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Delay timer Selection
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.trigger_delay_timer),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = delayValue,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                                delayValue = newValue
                            }
                        },
                        label = { Text(stringResource(R.string.duration_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("sec", "min", "hour").forEach { unit ->
                            FilterChip(
                                selected = delayUnit == unit,
                                onClick = { delayUnit = unit },
                                label = { Text(unit, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        }

        var repeatCount by remember { mutableStateOf(1) }
        var intervalValue by remember { mutableStateOf("1") }
        var intervalUnit by remember { mutableStateOf("min") }

        val finalIntervalSeconds = remember(intervalValue, intervalUnit) {
            val value = intervalValue.toIntOrNull() ?: 0
            when (intervalUnit) {
                "min" -> value * 60
                "hour" -> value * 3600
                else -> value // "sec"
            }
        }

        // Sequential multi-call repeat options Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.sequential_multi_call_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = stringResource(R.string.sequential_multi_call_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.call_count_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1, 2, 3, 5).forEach { num ->
                            FilterChip(
                                selected = repeatCount == num,
                                onClick = { repeatCount = num },
                                label = { Text(num.toString(), maxLines = 1) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = intervalValue,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                                intervalValue = newValue
                            }
                        },
                        label = { Text(stringResource(R.string.repeat_interval_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = repeatCount > 1,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("sec", "min", "hour").forEach { unit ->
                            FilterChip(
                                selected = intervalUnit == unit,
                                enabled = repeatCount > 1,
                                onClick = { intervalUnit = unit },
                                label = { Text(unit, maxLines = 1) }
                            )
                        }
                    }
                }

                if (repeatCount == 1) {
                    Text(
                        text = stringResource(R.string.multi_call_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    FakeCallReceiver.scheduleFakeCall(
                        context,
                        fakeName.trim(),
                        fakeNumber.trim(),
                        finalDelaySeconds,
                        repeatCount,
                        finalIntervalSeconds
                    )
                    val msg = if (repeatCount > 1) {
                        "Scheduled $repeatCount fake calls (first in $finalDelaySeconds sec, then every $intervalValue $intervalUnit)!"
                    } else {
                        "Fake call scheduled in $finalDelaySeconds seconds!"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.AccessTime, null)
                Spacer(Modifier.width(8.dp))
                val btnText = if (repeatCount > 1) {
                    stringResource(R.string.schedule_sequential_calls, repeatCount)
                } else {
                    stringResource(R.string.schedule_escape_call, finalDelaySeconds)
                }
                Text(btnText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            OutlinedButton(
                onClick = {
                    FakeCallReceiver.cancelFakeCall(context)
                    Toast.makeText(context, "Cancelled pending fake calls", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Cancel, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_cancel_scheduled_escape_calls), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
