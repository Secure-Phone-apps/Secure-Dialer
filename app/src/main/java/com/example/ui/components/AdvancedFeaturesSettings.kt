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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.RichHapticEngine
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.viewmodel.DialerViewModel

@Composable
fun AdvancedFeaturesSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color,
    highlightedTitle: String? = null
) {
    val scrollState = rememberScrollState()
    val reminders by viewModel.remindersFlow.collectAsStateWithLifecycle()
    val activeReminders = remember(reminders) { reminders.filter { !it.isCompleted } }
    val allNotes by viewModel.notesFlow.collectAsStateWithLifecycle()
    val recordings by viewModel.recordingsFlow.collectAsStateWithLifecycle()

    val isCallbackRemindersEnabled by viewModel.isCallbackRemindersEnabled
    val isCallNotesEnabled by viewModel.isCallNotesEnabled
    val isFakeCallSimulatorEnabled by viewModel.isFakeCallSimulatorEnabled
    val isRecordingEnabled by viewModel.recordingEnabled
    val isPostCallActionsEnabled by viewModel.isPostCallActionsEnabled
    val postCallDismissDuration by viewModel.postCallDismissDurationSeconds
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Feature Container 1: Automatic Call Recording (Expandable with Switch)
        HighlightableCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            cardBgColor = cardBgColor,
            isHighlighted = isMatchTitle("Call Recording & Local Audio Storage", highlightedTitle) ||
                    isMatchTitle("Advanced Tools Settings", highlightedTitle) ||
                    isMatchTitle("Call Recording", highlightedTitle) ||
                    isMatchTitle("Lock Recordings with Biometrics", highlightedTitle) ||
                    isMatchTitle("Recording Vault", highlightedTitle),
            shape = MaterialTheme.shapes.medium
        ) {
            ExpandableSettingsCard(
                title = stringResource(R.string.header_call_recording),
                subtitle = stringResource(R.string.settings_call_recording_sub),
                icon = Icons.Default.Mic,
                iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                cardBgColor = Color.Transparent,
                badgeText = if (recordings.isNotEmpty()) "${recordings.size}" else null,
                hasSwitch = true,
                isSwitchChecked = isRecordingEnabled,
                onSwitchChange = { viewModel.updateRecordingEnabled(it) },
                initiallyExpanded = isMatchTitle("Call Recording", highlightedTitle) ||
                        isMatchTitle("Call Recording & Local Audio Storage", highlightedTitle) ||
                        isMatchTitle("Lock Recordings with Biometrics", highlightedTitle) ||
                        isMatchTitle("Recording Vault", highlightedTitle)
            ) {
                CallRecordingsSettings(viewModel = viewModel, cardBgColor = cardBgColor)
            }
        }

        // Feature Container 2: Callback Reminder Dashboard (Expandable with Switch)
        HighlightableCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            cardBgColor = cardBgColor,
            isHighlighted = isMatchTitle("Scheduled Callback Reminders Dashboard", highlightedTitle) ||
                    isMatchTitle("Callback Reminders & Alerts", highlightedTitle) ||
                    isMatchTitle("Callback Reminder", highlightedTitle),
            shape = MaterialTheme.shapes.medium
        ) {
            ExpandableSettingsCard(
                title = stringResource(R.string.settings_callback_reminders),
                subtitle = stringResource(R.string.settings_callback_reminders_sub),
                icon = Icons.Default.NotificationsActive,
                iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                cardBgColor = Color.Transparent,
                badgeText = if (activeReminders.isNotEmpty()) "${activeReminders.size}" else null,
                hasSwitch = true,
                isSwitchChecked = isCallbackRemindersEnabled,
                onSwitchChange = { viewModel.updateCallbackRemindersEnabled(it) },
                initiallyExpanded = isCallbackRemindersEnabled ||
                        isMatchTitle("Callback Reminder", highlightedTitle) ||
                        isMatchTitle("Scheduled Callback Reminders Dashboard", highlightedTitle)
            ) {
                ScheduledRemindersSettings(viewModel = viewModel, cardBgColor = cardBgColor)
            }
        }

        // Feature Container 3: Call Notes (Expandable with Switch)
        HighlightableCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            cardBgColor = cardBgColor,
            isHighlighted = isMatchTitle("Call Notes & Memos", highlightedTitle) ||
                    isMatchTitle("Call Notes & Contact Memos", highlightedTitle) ||
                    isMatchTitle("Call Notes", highlightedTitle),
            shape = MaterialTheme.shapes.medium
        ) {
            ExpandableSettingsCard(
                title = stringResource(R.string.settings_all_call_notes),
                subtitle = stringResource(R.string.settings_all_call_notes_sub),
                icon = Icons.Default.Description,
                iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                cardBgColor = Color.Transparent,
                badgeText = if (allNotes.isNotEmpty()) "${allNotes.size}" else null,
                hasSwitch = true,
                isSwitchChecked = isCallNotesEnabled,
                onSwitchChange = { viewModel.updateCallNotesEnabled(it) },
                initiallyExpanded = isMatchTitle("Call Notes", highlightedTitle) ||
                        isMatchTitle("Call Notes & Memos", highlightedTitle)
            ) {
                CallNotesSettings(viewModel = viewModel, cardBgColor = cardBgColor)
            }
        }

        // Feature Container 4: Fake Call Simulator (Expandable with Switch)
        HighlightableCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            cardBgColor = cardBgColor,
            isHighlighted = isMatchTitle("Fake Call Simulator", highlightedTitle) ||
                    isMatchTitle("Fake Call Generator", highlightedTitle) ||
                    isMatchTitle("Fake Call", highlightedTitle),
            shape = MaterialTheme.shapes.medium
        ) {
            ExpandableSettingsCard(
                title = stringResource(R.string.settings_fake_call_sim),
                subtitle = stringResource(R.string.settings_fake_call_sim_sub),
                icon = Icons.AutoMirrored.Filled.PhoneCallback,
                iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                cardBgColor = Color.Transparent,
                hasSwitch = true,
                isSwitchChecked = isFakeCallSimulatorEnabled,
                onSwitchChange = { viewModel.updateFakeCallSimulatorEnabled(it) },
                initiallyExpanded = isMatchTitle("Fake Call", highlightedTitle) ||
                        isMatchTitle("Fake Call Simulator", highlightedTitle)
            ) {
                FakeCallSettings(viewModel = viewModel, cardBgColor = cardBgColor)
            }
        }

        // Feature Container 5: Post-Call Quick Actions
        HighlightableCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            cardBgColor = cardBgColor,
            isHighlighted = isMatchTitle("Post-Call Quick Actions", highlightedTitle) ||
                    isMatchTitle("Post-Call Actions", highlightedTitle) ||
                    isMatchTitle("Call Actions", highlightedTitle) ||
                    isMatchTitle("Auto-Dismiss Countdown", highlightedTitle) ||
                    isMatchTitle("Auto-Dismiss Timer", highlightedTitle),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsRowToggle(
                    title = stringResource(R.string.settings_post_call_actions),
                    subtitle = stringResource(R.string.settings_post_call_actions_sub),
                    checked = isPostCallActionsEnabled,
                    onCheckedChange = { viewModel.updatePostCallActionsEnabled(it) },
                    icon = Icons.Default.CallEnd,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.primary
                )

                AnimatedVisibility(visible = isPostCallActionsEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.settings_post_call_timer),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val timerLabel = when (postCallDismissDuration) {
                                0 -> stringResource(R.string.timer_never)
                                else -> stringResource(R.string.timer_seconds_format, postCallDismissDuration)
                            }
                            Text(
                                text = timerLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = stringResource(R.string.settings_post_call_timer_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val durations = listOf(3, 5, 10, 0)
                            durations.forEach { seconds ->
                                val isSelected = (postCallDismissDuration == seconds)
                                val label = if (seconds == 0) {
                                    stringResource(R.string.timer_never)
                                } else {
                                    "${seconds}s"
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                        viewModel.updatePostCallDismissDurationSeconds(seconds)
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}