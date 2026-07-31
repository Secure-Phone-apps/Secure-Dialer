package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.DialerViewModel
import kotlinx.coroutines.launch

@Composable
fun MergeDuplicateSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val allContacts by viewModel.allContactsFlow.collectAsState()
    val duplicates = remember(allContacts) {
        allContacts.groupBy { contact -> contact.number.filter { it.isDigit() || it == '+' } }
            .filter { it.key.isNotEmpty() && it.value.size > 1 }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_dedup_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.dedup_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (duplicates.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                SettingsEmptyState(
                    icon = Icons.Default.FilterNone,
                    title = stringResource(R.string.no_duplicates_title),
                    description = stringResource(R.string.no_duplicates_desc),
                    tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
        } else {
            val coroutineScope = rememberCoroutineScope()
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch {
                        duplicates.forEach { (_, group) ->
                            val primary = group.first()
                            // delete the rest
                            group.drop(1).forEach { dup ->
                                viewModel.deleteContact(dup.number)
                            }
                        }
                        Toast.makeText(context, context.getString(R.string.toast_merged_all_duplicates), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(Icons.Default.MergeType, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.merge_all_duplicates, duplicates.size), fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(duplicates.keys.toList(), key = { it }) { key ->
                    val group = duplicates[key] ?: emptyList()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = cardBgColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.first().number,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        coroutineScope.launch {
                                            group.drop(1).forEach { dup ->
                                                viewModel.deleteContact(dup.number)
                                            }
                                            Toast.makeText(context, context.getString(R.string.toast_merged_duplicates_for, group.first().number), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Text(stringResource(R.string.merge_group))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            group.forEach { contact ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (contact.email.isNotEmpty()) {
                                        Text(
                                            text = " • ${contact.email}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
}
