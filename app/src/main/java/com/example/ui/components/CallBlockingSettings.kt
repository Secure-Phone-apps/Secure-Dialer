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

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.CallScreenPermissionHelper
import com.example.util.RichHapticEngine

@Composable
fun CallBlockingSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color,
    highlightedTitle: String? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val blockedNumbersEntities by viewModel.blockedNumbersFlow.collectAsStateWithLifecycle()
    val spamList by viewModel.spamFlow.collectAsStateWithLifecycle()

    var isScreeningGranted by remember { mutableStateOf(CallScreenPermissionHelper.isCallScreeningRoleHeld(context)) }
    val screeningRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        isScreeningGranted = CallScreenPermissionHelper.isCallScreeningRoleHeld(context)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isScreeningGranted = CallScreenPermissionHelper.isCallScreeningRoleHeld(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isScreeningGranted) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                    }
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (isScreeningGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.service_health_header),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    if (isScreeningGranted) R.string.service_health_role_granted
                                    else R.string.service_health_role_not_granted
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!isScreeningGranted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                CallScreenPermissionHelper.requestCallScreeningRole(context, screeningRoleLauncher)
                            }
                        ) {
                            Text(stringResource(R.string.btn_configure))
                        }
                    }
                }
            }
        }

        val isBlockUnknownPrivateEnabled by viewModel.isBlockUnknownPrivateEnabled
        HighlightableCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            cardBgColor = cardBgColor,
            isHighlighted = isMatchTitle("Block Unknown & Private Calls", highlightedTitle),
            shape = MaterialTheme.shapes.medium,
            onClick = {
                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                viewModel.updateBlockUnknownPrivateEnabled(!isBlockUnknownPrivateEnabled)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.search_item_block_unknown_private),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.search_desc_block_unknown_private),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isBlockUnknownPrivateEnabled,
                    onCheckedChange = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        viewModel.updateBlockUnknownPrivateEnabled(it)
                    }
                )
            }
        }

        // Expandable Blocklist
        HighlightableCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            cardBgColor = cardBgColor,
            isHighlighted = isMatchTitle("Blocked Numbers & Blacklist", highlightedTitle) ||
                isMatchTitle("Blocked Numbers", highlightedTitle),
            shape = MaterialTheme.shapes.medium
        ) {
            ExpandableSettingsCard(
                title = stringResource(R.string.settings_blocked_numbers),
                subtitle = stringResource(R.string.settings_blocked_numbers_sub),
                icon = Icons.Default.Block,
                iconBgColor = MaterialTheme.colorScheme.errorContainer,
                iconTint = MaterialTheme.colorScheme.onErrorContainer,
                cardBgColor = Color.Transparent,
                badgeText = if (blockedNumbersEntities.isNotEmpty()) "${blockedNumbersEntities.size}" else null,
                initiallyExpanded = isMatchTitle("Blocked Numbers", highlightedTitle) || isMatchTitle("Block Unknown", highlightedTitle)
            ) {
                BlockListSettings(viewModel = viewModel, cardBgColor = cardBgColor)
            }
        }

        // Expandable Spam Defense Database
        HighlightableCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            cardBgColor = cardBgColor,
            isHighlighted = isMatchTitle("Offline Spam Database & Protection", highlightedTitle) ||
                isMatchTitle("Spam", highlightedTitle),
            shape = MaterialTheme.shapes.medium
        ) {
            ExpandableSettingsCard(
                title = stringResource(R.string.settings_spam_protection),
                subtitle = stringResource(R.string.local_offline_protection),
                icon = Icons.Default.Shield,
                iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                cardBgColor = Color.Transparent,
                badgeText = if (spamList.isNotEmpty()) "${spamList.size}" else null,
                initiallyExpanded = isMatchTitle("Offline Spam Database", highlightedTitle) || isMatchTitle("Spam", highlightedTitle)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    SpamDatabaseSettings(viewModel = viewModel, cardBgColor = cardBgColor)
                }
            }
        }
    }
}