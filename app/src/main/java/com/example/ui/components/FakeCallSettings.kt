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
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.FakeCallReceiver

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FakeCallSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanatory Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CallMerge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Fake Call Simulator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Escape awkward situations politely.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "A fully simulated high-fidelity incoming call screen will trigger after your chosen timer. Answering the call behaves like a real call with local elapsed timers and interactive keypad modules.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
                    text = "Caller Identity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = fakeName,
                    onValueChange = { fakeName = it },
                    label = { Text("Caller Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Quick presets
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf("Boss", "Mom", "Doctor", "Attorney", "Delivery Driver")
                    presets.forEach { preset ->
                        FilterChip(
                            selected = fakeName == preset,
                            onClick = {
                                fakeName = preset
                                when (preset) {
                                    "Boss" -> fakeNumber = "+1 (555) 492-0192"
                                    "Mom" -> fakeNumber = "+1 (555) 728-1100"
                                    "Doctor" -> fakeNumber = "+1 (800) 555-0133"
                                    "Attorney" -> fakeNumber = "+1 (888) 555-9128"
                                    "Delivery Driver" -> fakeNumber = "+1 (555) 233-1090"
                                }
                            },
                            label = { Text(preset) }
                        )
                    }
                }

                OutlinedTextField(
                    value = fakeNumber,
                    onValueChange = { fakeNumber = it },
                    label = { Text("Phone Number") },
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
                    text = "Trigger Delay Timer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
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
                        label = { Text("Duration") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("sec", "min", "hour").forEach { unit ->
                            FilterChip(
                                selected = delayUnit == unit,
                                onClick = { delayUnit = unit },
                                label = { Text(unit) }
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
                    text = "Sequential Multi-Call Options",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Triggers multiple calls in sequence so that if a single excuse call isn't sufficient, subsequent calls keep coming.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Call Count:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1, 2, 3, 5).forEach { num ->
                            FilterChip(
                                selected = repeatCount == num,
                                onClick = { repeatCount = num },
                                label = { Text(num.toString()) }
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
                        label = { Text("Repeat Interval") },
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
                                label = { Text(unit) }
                            )
                        }
                    }
                }

                if (repeatCount == 1) {
                    Text(
                        text = "Note: Repeat interval applies when Call Count is 2 or more.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.AccessTime, null)
                Spacer(Modifier.width(8.dp))
                val btnText = if (repeatCount > 1) {
                    "Schedule $repeatCount Sequential Calls"
                } else {
                    "Schedule Escape Call ($finalDelaySeconds sec)"
                }
                Text(btnText)
            }

            OutlinedButton(
                onClick = {
                    FakeCallReceiver.cancelFakeCall(context)
                    Toast.makeText(context, "Cancelled pending fake calls", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Cancel, null)
                Spacer(Modifier.width(8.dp))
                Text("Cancel Scheduled Escape Calls")
            }
        }
    }
}
