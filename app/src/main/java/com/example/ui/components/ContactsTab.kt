package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.model.Contact

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import kotlinx.coroutines.launch

@Composable
fun ContactsTabContent(
    contactsPaged: LazyPagingItems<Contact>,
    onCallClick: (Contact) -> Unit,
    onAddContactClick: () -> Unit,
    onToggleFavorite: (Contact) -> Unit,
    hasPermission: Boolean = true,
    isLoading: Boolean = false,
    onRequestPermission: () -> Unit = {},
    onEditContact: (Contact) -> Unit = {},
    onDeleteContact: (Contact) -> Unit = {}
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val alphabet = ('A'..'Z').toList() + '#'

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (!hasPermission && !isLoading) {
                // ... (keep permission card)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Permissions Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To access, load, edit, and call the real contacts on your phone, please enable the Contacts Permission.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Enable Phone Contacts")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Contacts",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onAddContactClick,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("add_contact_button")
                ) {
                    Icon(Icons.Default.Call, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add New", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (contactsPaged.itemCount == 0 && contactsPaged.loadState.refresh is LoadState.NotLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateIllustration(
                        title = "No contacts found",
                        subtitle = "Tap 'Add New' to create a contact"
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize().padding(end = 24.dp)
                ) {
                    items(
                        count = contactsPaged.itemCount,
                        key = contactsPaged.itemKey { it.number },
                        contentType = contactsPaged.itemContentType { "contact" }
                    ) { index ->
                        val contact = contactsPaged[index]
                        if (contact != null) {
                            ContactRow(
                                contact = contact,
                                onCallClick = onCallClick,
                                onToggleFavorite = onToggleFavorite,
                                onEditContact = onEditContact,
                                onDeleteContact = onDeleteContact
                            )
                        }
                    }

                    if (contactsPaged.loadState.append is LoadState.Loading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }

        // A-Z Scroller Rail
        if (contactsPaged.itemCount > 0) {
            val haptic = LocalHapticFeedback.current
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(32.dp)
                    .padding(vertical = 60.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, _ ->
                            val index = (change.position.y / size.height * alphabet.size)
                                .toInt()
                                .coerceIn(0, alphabet.size - 1)
                            val letter = alphabet[index].toString()
                            
                            // Find first contact starting with this letter
                            for (i in 0 until contactsPaged.itemCount) {
                                val c = contactsPaged[i]
                                if (c != null && (c.name.startsWith(letter, ignoreCase = true) || (letter == "#" && !c.name[0].isLetter()))) {
                                    coroutineScope.launch {
                                        listState.scrollToItem(i)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    break
                                }
                            }
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                alphabet.forEach { char ->
                    Text(
                        text = char.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.clickable {
                            val letter = char.toString()
                            for (i in 0 until contactsPaged.itemCount) {
                                val c = contactsPaged[i]
                                if (c != null && (c.name.startsWith(letter, ignoreCase = true) || (letter == "#" && !c.name[0].isLetter()))) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(i)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    break
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ContactRow(
    contact: Contact,
    onCallClick: (Contact) -> Unit,
    onToggleFavorite: (Contact) -> Unit,
    onEditContact: (Contact) -> Unit,
    onDeleteContact: (Contact) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCallClick(contact)
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            },
            supportingContent = {
                Text(
                    text = "${contact.label} • ${contact.number}",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            leadingContent = {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = contact.avatarBg
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (contact.name.length >= 2) contact.name.substring(0, 2).uppercase() else contact.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = contact.avatarTextColor
                        )
                    }
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleFavorite(contact)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = if (contact.favorite) Color(0xFFEAB308) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    }
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            menuExpanded = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More"
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuExpanded = false
                                    onEditContact(contact)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuExpanded = false
                                    onDeleteContact(contact)
                                }
                            )
                        }
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun AddContactDialog(
    name: String,
    onNameChange: (String) -> Unit,
    number: String,
    onNumberChange: (String) -> Unit,
    label: String,
    onLabelChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Contact",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_name_input")
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = onNumberChange,
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_phone_input")
                )
                Column {
                    Text(
                        text = "Label",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Mobile", "Work", "Home").forEach { option ->
                            FilterChip(
                                selected = label == option,
                                onClick = { onLabelChange(option) },
                                label = { Text(option) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
