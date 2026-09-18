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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.*
import com.example.model.Contact
import com.example.ui.theme.LocalAmoledMode
import com.example.ui.theme.LocalM3Expressive
import com.example.util.RichHapticEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToExistingContactSheet(
    contacts: List<Contact>,
    pendingNumber: String,
    onContactSelected: (Contact) -> Unit,
    onDismiss: () -> Unit,
    viewModel: com.example.ui.viewmodel.DialerViewModel? = null
) {
    val context = LocalContext.current
    val isAmoled = LocalAmoledMode.current
    val isExpressive = LocalM3Expressive.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts.sortedBy { it.name.lowercase() }
        } else {
            contacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.number.contains(searchQuery, ignoreCase = true) ||
                it.getAllNumbers().any { num -> num.number.contains(searchQuery, ignoreCase = true) }
            }.sortedBy { it.name.lowercase() }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isAmoled) Color(0xFF444444) else MaterialTheme.colorScheme.outlineVariant
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.dialpad_select_contact_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Adding $pendingNumber",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.dialpad_search_contact_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isAmoled) Color(0xFF141414) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = if (isAmoled) Color(0xFF141414) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isAmoled) Color(0xFF262626) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Contact List
            if (filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.dialpad_no_contacts_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = filteredContacts,
                        key = { it.id }
                    ) { contact ->
                        val avatarShape = viewModel?.let { getAvatarShape(it.avatarShapeType.value) } ?: CircleShape
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (viewModel?.vibrateOnClickEnabled?.value != false) {
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                                    }
                                    onContactSelected(contact)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = contact.name.ifBlank { "Unknown" },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = "${localizeContactLabel(contact.label)} • ${contact.number}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingContent = {
                                    Surface(
                                        modifier = Modifier.size(40.dp),
                                        shape = avatarShape,
                                        color = contact.avatarBg.copy(alpha = 0.85f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (contact.photoUri.isNotEmpty()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(contact.photoUri)
                                                        .size(256, 256)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = "Contact Avatar",
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                val hasName = contact.name.isNotBlank() && contact.name != contact.number
                                                if (hasName) {
                                                    Text(
                                                        text = contact.avatarText.ifEmpty { getInitials(contact.name) },
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = contact.avatarTextColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = "Avatar",
                                                        tint = contact.avatarTextColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}
