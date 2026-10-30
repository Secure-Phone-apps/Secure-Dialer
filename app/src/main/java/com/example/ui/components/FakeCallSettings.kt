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

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import com.example.CallManager
import com.example.R
import com.example.model.ScheduledFakeCall
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.FakeCallManager
import com.example.util.FakeCallReceiver
import com.example.util.FakeCallVoicePlayer
import com.example.util.RichHapticEngine
import java.text.SimpleDateFormat
import java.util.*

private enum class FakeCallDelayUnit(val key: String, val multiplier: Int) {
    SEC("sec", 1),
    MIN("min", 60),
    HOUR("hour", 3600)
}

private enum class ScheduleMode {
    TIMER_DELAY,
    DATE_TIME
}

@Composable
fun FakeCallSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current

    var fakeName by remember { mutableStateOf("Boss") }
    var fakeNumber by remember { mutableStateOf("+1 (555) 492-0192") }

    // Schedule Mode: Timer delay vs Specific Date & Time
    var scheduleMode by remember { mutableStateOf(ScheduleMode.TIMER_DELAY) }

    // Mode A: Timer Delay
    var delayValue by remember { mutableStateOf("10") }
    var selectedDelayUnit by remember { mutableStateOf(FakeCallDelayUnit.SEC) }

    val finalDelaySeconds = remember(delayValue, selectedDelayUnit) {
        val value = delayValue.toIntOrNull() ?: 0
        (value * selectedDelayUnit.multiplier).coerceAtLeast(0)
    }

    // Mode B: Specific Date & Time
    val now = remember { Calendar.getInstance() }
    var targetCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            add(Calendar.MINUTE, 15)
        })
    }

    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()) }
    val fullDateTimeFormatter = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }

    val computedDelayFromDateTime = remember(targetCalendar) {
        val diff = (targetCalendar.timeInMillis - System.currentTimeMillis()) / 1000
        diff.coerceAtLeast(1).toInt()
    }

    // Sequential repeat options
    var repeatCount by remember { mutableIntStateOf(1) }
    var intervalValue by remember { mutableStateOf("1") }
    var selectedIntervalUnit by remember { mutableStateOf(FakeCallDelayUnit.MIN) }

    val finalIntervalSeconds = remember(intervalValue, selectedIntervalUnit) {
        val value = intervalValue.toIntOrNull() ?: 0
        (value * selectedIntervalUnit.multiplier).coerceAtLeast(0)
    }

    // Voice Audio on Answer Options
    var selectedVoiceType by remember { mutableStateOf("OFFICE") }
    var customAudioUri by remember { mutableStateOf<String?>(null) }
    var customAudioName by remember { mutableStateOf<String?>(null) }
    var isVoicePreviewPlaying by remember { mutableStateOf(false) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            customAudioUri = uri.toString()
            var displayName = "Custom Audio File"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                }
            } catch (_: Exception) {}
            customAudioName = displayName
            selectedVoiceType = "CUSTOM"
            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
        }
    }

    // Stop voice preview when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            FakeCallVoicePlayer.stop()
        }
    }

    // Scheduled calls list refresh state
    var scheduledCallsVersion by remember { mutableIntStateOf(0) }
    val scheduledCalls = remember(scheduledCallsVersion) {
        FakeCallManager.getScheduledCalls(context)
    }

    val effectiveDelaySeconds = if (scheduleMode == ScheduleMode.TIMER_DELAY) {
        finalDelaySeconds
    } else {
        computedDelayFromDateTime
    }

    val isValidConfig = effectiveDelaySeconds >= 1 && fakeNumber.trim().isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Configuration Inputs: Caller Identity
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

                // Quick presets
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
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Timing Selection: Mode Selector (Timer Delay vs Date & Time)
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

                // Tab Switcher between Timer Delay and Date/Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = scheduleMode == ScheduleMode.TIMER_DELAY,
                        onClick = {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                            scheduleMode = ScheduleMode.TIMER_DELAY
                        },
                        label = { Text(stringResource(R.string.fake_call_mode_timer), maxLines = 1) },
                        leadingIcon = {
                            Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = scheduleMode == ScheduleMode.DATE_TIME,
                        onClick = {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                            scheduleMode = ScheduleMode.DATE_TIME
                        },
                        label = { Text(stringResource(R.string.fake_call_mode_datetime), maxLines = 1) },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (scheduleMode == ScheduleMode.TIMER_DELAY) {
                    // Quick duration presets
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val quickTimers = listOf(
                            "10s" to ("10" to FakeCallDelayUnit.SEC),
                            "30s" to ("30" to FakeCallDelayUnit.SEC),
                            "1m" to ("1" to FakeCallDelayUnit.MIN),
                            "5m" to ("5" to FakeCallDelayUnit.MIN),
                            "15m" to ("15" to FakeCallDelayUnit.MIN)
                        )
                        items(quickTimers) { (label, config) ->
                            FilterChip(
                                selected = delayValue == config.first && selectedDelayUnit == config.second,
                                onClick = {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    delayValue = config.first
                                    selectedDelayUnit = config.second
                                },
                                label = { Text(label, maxLines = 1) }
                            )
                        }
                    }

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
                            FakeCallDelayUnit.entries.forEach { unit ->
                                FilterChip(
                                    selected = selectedDelayUnit == unit,
                                    onClick = {
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                        selectedDelayUnit = unit
                                    },
                                    label = { Text(unit.key, maxLines = 1) }
                                )
                            }
                        }
                    }
                } else {
                    // Date & Time picker buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                                val year = targetCalendar.get(Calendar.YEAR)
                                val month = targetCalendar.get(Calendar.MONTH)
                                val day = targetCalendar.get(Calendar.DAY_OF_MONTH)

                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val newCal = (targetCalendar.clone() as Calendar).apply {
                                            set(Calendar.YEAR, y)
                                            set(Calendar.MONTH, m)
                                            set(Calendar.DAY_OF_MONTH, d)
                                        }
                                        targetCalendar = newCal
                                    },
                                    year,
                                    month,
                                    day
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(dateFormatter.format(targetCalendar.time), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        OutlinedButton(
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                                val hour = targetCalendar.get(Calendar.HOUR_OF_DAY)
                                val minute = targetCalendar.get(Calendar.MINUTE)

                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        val newCal = (targetCalendar.clone() as Calendar).apply {
                                            set(Calendar.HOUR_OF_DAY, h)
                                            set(Calendar.MINUTE, m)
                                            set(Calendar.SECOND, 0)
                                        }
                                        targetCalendar = newCal
                                    },
                                    hour,
                                    minute,
                                    false
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(timeFormatter.format(targetCalendar.time), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    val minutesUntil = (computedDelayFromDateTime / 60)
                    val triggerHint = if (minutesUntil > 60) {
                        "${minutesUntil / 60}h ${minutesUntil % 60}m"
                    } else if (minutesUntil > 0) {
                        "${minutesUntil}m"
                    } else {
                        "${computedDelayFromDateTime}s"
                    }

                    Text(
                        text = stringResource(R.string.fake_call_triggers_in, triggerHint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Voice Audio on Answer Options Card
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
                    text = stringResource(R.string.fake_call_voice_audio_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = stringResource(R.string.fake_call_voice_audio_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Voice preset chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val voices = listOf(
                        "OFFICE" to R.string.fake_call_voice_office,
                        "FAMILY" to R.string.fake_call_voice_family,
                        "FRIEND" to R.string.fake_call_voice_friend,
                        "NONE" to R.string.fake_call_voice_none,
                        "CUSTOM" to R.string.fake_call_voice_custom
                    )
                    items(voices) { (key, resId) ->
                        FilterChip(
                            selected = selectedVoiceType == key,
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                FakeCallVoicePlayer.stop()
                                isVoicePreviewPlaying = false
                                selectedVoiceType = key
                                if (key == "CUSTOM" && customAudioUri == null) {
                                    audioPickerLauncher.launch("audio/*")
                                }
                            },
                            label = { Text(stringResource(resId), maxLines = 1) }
                        )
                    }
                }

                if (selectedVoiceType == "CUSTOM") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                audioPickerLauncher.launch("audio/*")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = customAudioName ?: stringResource(R.string.fake_call_voice_choose_file),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Voice Preview Button
                if (selectedVoiceType != "NONE") {
                    OutlinedButton(
                        onClick = {
                            if (isVoicePreviewPlaying) {
                                FakeCallVoicePlayer.stop()
                                isVoicePreviewPlaying = false
                            } else {
                                isVoicePreviewPlaying = true
                                FakeCallVoicePlayer.playVoice(
                                    context = context,
                                    voiceType = selectedVoiceType,
                                    customAudioUri = customAudioUri,
                                    isSpeakerOn = true,
                                    onPlaybackCompleted = {
                                        isVoicePreviewPlaying = false
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val icon = if (isVoicePreviewPlaying) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        val text = if (isVoicePreviewPlaying) {
                            stringResource(R.string.fake_call_voice_stop)
                        } else {
                            stringResource(R.string.fake_call_voice_preview)
                        }
                        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
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
                    maxLines = 2,
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
                                onClick = {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    repeatCount = num
                                },
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
                        FakeCallDelayUnit.entries.forEach { unit ->
                            FilterChip(
                                selected = selectedIntervalUnit == unit,
                                enabled = repeatCount > 1,
                                onClick = {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    selectedIntervalUnit = unit
                                },
                                label = { Text(unit.key, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        }

        // Multi-Call & Call Waiting Testing Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneCallback,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Call Waiting & Multi-Call Testing",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Simulate receiving a secondary call while on an active call to test Call Waiting alerts or verify same-caller suppression.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                            val isCallCurrentlyActive = (CallManager.currentCall.value != null && CallManager.callState.value != android.telecom.Call.STATE_DISCONNECTED) ||
                                    (viewModel.isFakeCallActive.value && viewModel.fakeCallState.value == "ACTIVE")

                            if (!isCallCurrentlyActive) {
                                viewModel.isFakeCallActive.value = true
                                viewModel.fakeCallerName.value = fakeName.trim().ifBlank { "Boss" }
                                viewModel.fakeCallerNumber.value = fakeNumber.trim().ifBlank { "+1 (555) 492-0192" }
                                viewModel.fakeCallState.value = "ACTIVE"
                                viewModel.fakeCallStartTimestamp = System.currentTimeMillis()
                                viewModel.isCallMinimized.value = false
                                viewModel.isSettingsVisible.value = false
                            }
                            viewModel.triggerSimulatedCallWaiting(
                                name = "Alice Cooper",
                                number = "+1 (555) 987-6543",
                                delayMillis = 3500L,
                                context = context
                            )
                            Toast.makeText(context, "📞 Call Waiting simulated from Alice Cooper (+1 555-987-6543) in 3.5s", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Different Caller (Alert)", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                            val isCallCurrentlyActive = (CallManager.currentCall.value != null && CallManager.callState.value != android.telecom.Call.STATE_DISCONNECTED) ||
                                    (viewModel.isFakeCallActive.value && viewModel.fakeCallState.value == "ACTIVE")

                            val sameNum = if (viewModel.isFakeCallActive.value) {
                                viewModel.fakeCallerNumber.value
                            } else if (CallManager.currentCall.value != null) {
                                CallManager.callerNumber.value
                            } else {
                                fakeNumber.trim().ifBlank { "+1 (555) 492-0192" }
                            }

                            if (!isCallCurrentlyActive) {
                                viewModel.isFakeCallActive.value = true
                                viewModel.fakeCallerName.value = fakeName.trim().ifBlank { "Boss" }
                                viewModel.fakeCallerNumber.value = sameNum
                                viewModel.fakeCallState.value = "ACTIVE"
                                viewModel.fakeCallStartTimestamp = System.currentTimeMillis()
                                viewModel.isCallMinimized.value = false
                                viewModel.isSettingsVisible.value = false
                            }

                            viewModel.triggerSimulatedCallWaiting(
                                name = "Same Caller Retry",
                                number = sameNum,
                                delayMillis = 2000L,
                                context = context
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Same Caller (Suppress)", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Action Buttons: Schedule and Cancel
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                enabled = isValidConfig,
                onClick = {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                    FakeCallReceiver.scheduleFakeCall(
                        context = context,
                        name = fakeName.trim(),
                        number = fakeNumber.trim(),
                        delaySeconds = effectiveDelaySeconds,
                        repeatCount = repeatCount,
                        repeatIntervalSeconds = finalIntervalSeconds,
                        audioUri = customAudioUri,
                        audioVoiceType = selectedVoiceType
                    )
                    scheduledCallsVersion++
                    Toast.makeText(context, context.getString(R.string.fake_call_scheduled_success), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                val btnText = if (repeatCount > 1) {
                    stringResource(R.string.schedule_sequential_calls, repeatCount)
                } else if (scheduleMode == ScheduleMode.TIMER_DELAY) {
                    stringResource(R.string.schedule_escape_call, effectiveDelaySeconds)
                } else {
                    stringResource(R.string.schedule_call_btn)
                }
                Text(btnText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            if (scheduledCalls.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.WARNING)
                        FakeCallManager.cancelAll(context)
                        scheduledCallsVersion++
                        Toast.makeText(context, context.getString(R.string.toast_deleted_recording), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_cancel_scheduled_escape_calls), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Upcoming Scheduled Calls Section
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.fake_call_upcoming_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (scheduledCalls.size > 1) {
                        TextButton(
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                                FakeCallManager.cancelAll(context)
                                scheduledCallsVersion++
                            }
                        ) {
                            Text(stringResource(R.string.fake_call_cancel_all), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (scheduledCalls.isEmpty()) {
                    Text(
                        text = stringResource(R.string.fake_call_no_upcoming),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        scheduledCalls.forEach { call ->
                            val triggerDate = Date(call.triggerTimeMillis)
                            val formattedTime = fullDateTimeFormatter.format(triggerDate)
                            val voiceTag = when (call.audioVoiceType) {
                                "OFFICE" -> "Office"
                                "FAMILY" -> "Family"
                                "FRIEND" -> "Friend"
                                "CUSTOM" -> "Custom"
                                else -> "Silent"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = call.name.ifEmpty { call.number },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$formattedTime • $voiceTag",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                        FakeCallManager.cancelCall(context, call.id)
                                        scheduledCallsVersion++
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Cancel call",
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
}