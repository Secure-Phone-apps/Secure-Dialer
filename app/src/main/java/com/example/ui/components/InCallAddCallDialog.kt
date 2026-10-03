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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.Contact
import com.example.model.getAvatarShape
import com.example.model.getInitials
import com.example.util.RichHapticEngine

@Composable
fun InCallAddCallDialog(
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onAddCall: (name: String, number: String) -> Unit,
    avatarShapeType: String = "circular"
) {
    val context = LocalContext.current
    var addCallNumberInput by remember { mutableStateOf("") }
    var selectedAddCallContactName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val targetNum = addCallNumberInput.trim()
                    if (targetNum.isNotBlank()) {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                        val finalName = if (selectedAddCallContactName.isNotBlank()) {
                            selectedAddCallContactName
                        } else {
                            contacts.find { it.number == targetNum }?.name ?: targetNum
                        }
                        onAddCall(finalName, targetNum)
                        onDismiss()
                    } else {
                        Toast.makeText(context, context.getString(R.string.add_call_search_hint), Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text(stringResource(R.string.btn_add_merge))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
        title = {
            Text(
                stringResource(R.string.add_call),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = addCallNumberInput,
                    onValueChange = {
                        addCallNumberInput = it
                        selectedAddCallContactName = ""
                    },
                    label = { Text(stringResource(R.string.add_call_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // FIXED: Support name, primary number, secondary numbers, and T9 digit mapping
                val filteredContacts = remember(addCallNumberInput, contacts) {
                    val query = addCallNumberInput.trim()
                    if (query.isBlank()) {
                        contacts
                    } else {
                        contacts.filter { contact ->
                            contact.name.contains(query, ignoreCase = true) ||
                            contact.number.contains(query) ||
                            contact.t9Mapping.contains(query) ||
                            contact.getAllNumbers().any { it.number.contains(query) }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredContacts,
                        // FIXED: Guaranteed unique key prevents fatal Compose IllegalArgumentException crash
                        key = { "${it.id}_${it.number}" },
                        contentType = { "add_call_contact" }
                    ) { contact ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                addCallNumberInput = contact.number
                                selectedAddCallContactName = contact.name
                            },
                            shape = MaterialTheme.shapes.small,
                            color = if (addCallNumberInput == contact.number) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    shape = getAvatarShape(avatarShapeType),
                                    color = contact.avatarBg
                                ) {
                                    var imageFailed by remember(contact.photoUri) { mutableStateOf(false) }
                                    Box(contentAlignment = Alignment.Center) {
                                        if (contact.photoUri.isNotEmpty() && !imageFailed) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(contact.photoUri)
                                                    .size(128, 128)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = contact.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                onError = { imageFailed = true }
                                            )
                                        } else {
                                            Text(
                                                text = contact.avatarText.ifEmpty { getInitials(contact.name) },
                                                color = contact.avatarTextColor,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = contact.number,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        shape = MaterialTheme.shapes.large
    )
}