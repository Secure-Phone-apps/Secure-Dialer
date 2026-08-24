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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.viewmodel.DialerViewModel

@Composable
fun AdvancedFeaturesSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val scrollState = rememberScrollState()
    val reminders by viewModel.remindersFlow.collectAsState()
    val activeReminders = remember(reminders) { reminders.filter { !it.isCompleted } }
    val allNotes by viewModel.notesFlow.collectAsState()

    val isCallbackRemindersEnabled by viewModel.isCallbackRemindersEnabled
    val isCallNotesEnabled by viewModel.isCallNotesEnabled
    val isFakeCallSimulatorEnabled by viewModel.isFakeCallSimulatorEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Feature Container 1: Callback Reminder Dashboard (Expandable with Switch)
        ExpandableSettingsCard(
            title = stringResource(R.string.settings_callback_reminders),
            subtitle = stringResource(R.string.settings_callback_reminders_sub),
            icon = Icons.Default.NotificationsActive,
            iconBgColor = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            cardBgColor = cardBgColor,
            badgeText = if (activeReminders.isNotEmpty()) "${activeReminders.size} active" else null,
            hasSwitch = true,
            isSwitchChecked = isCallbackRemindersEnabled,
            onSwitchChange = { viewModel.updateCallbackRemindersEnabled(it) },
            initiallyExpanded = isCallbackRemindersEnabled
        ) {
            ScheduledRemindersSettings(viewModel = viewModel, cardBgColor = cardBgColor)
        }

        // Feature Container 2: Call Notes (Expandable with Switch)
        ExpandableSettingsCard(
            title = stringResource(R.string.settings_all_call_notes),
            subtitle = stringResource(R.string.settings_all_call_notes_sub),
            icon = Icons.Default.Description,
            iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
            cardBgColor = cardBgColor,
            badgeText = if (allNotes.isNotEmpty()) "${allNotes.size} notes" else null,
            hasSwitch = true,
            isSwitchChecked = isCallNotesEnabled,
            onSwitchChange = { viewModel.updateCallNotesEnabled(it) },
            initiallyExpanded = false
        ) {
            CallNotesSettings(viewModel = viewModel, cardBgColor = cardBgColor)
        }

        // Feature Container 3: Fake Call Simulator (Expandable with Switch)
        ExpandableSettingsCard(
            title = stringResource(R.string.settings_fake_call_sim),
            subtitle = stringResource(R.string.settings_fake_call_sim_sub),
            icon = Icons.Default.PhoneCallback,
            iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
            cardBgColor = cardBgColor,
            hasSwitch = true,
            isSwitchChecked = isFakeCallSimulatorEnabled,
            onSwitchChange = { viewModel.updateFakeCallSimulatorEnabled(it) },
            initiallyExpanded = false
        ) {
            FakeCallSettings(viewModel = viewModel, cardBgColor = cardBgColor)
        }
    }
}
