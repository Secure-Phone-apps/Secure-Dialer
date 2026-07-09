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
import com.example.model.Contact

@Composable
fun ContactsTabContent(
    contacts: List<Contact>,
    onCallClick: (Contact) -> Unit,
    onAddContactClick: () -> Unit,
    onToggleFavorite: (Contact) -> Unit,
    primaryText: Color,
    secondaryText: Color,
    activePill: Color,
    brandBlue: Color,
    hasPermission: Boolean = true,
    isLoading: Boolean = false,
    onRequestPermission: () -> Unit = {},
    onEditContact: (Contact) -> Unit = {},
    onDeleteContact: (Contact) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (!hasPermission && !isLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = activePill.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Permissions Required", fontWeight = FontWeight.Bold, color = brandBlue, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To access, load, edit, and call the real contacts on your phone, please enable the Contacts Permission.",
                        fontSize = 11.sp,
                        color = secondaryText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Enable Phone Contacts", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Contacts",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = secondaryText
            )
            Button(
                onClick = onAddContactClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = activePill,
                    contentColor = if (activePill == Color(0xFF004A77)) Color.White else Color(0xFF041E49)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("add_contact_button")
            ) {
                Text("+ Add New", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👥", fontSize = 48.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Text("No contacts found", fontWeight = FontWeight.Medium, color = secondaryText)
                    Text("Tap '+ Add New' to create a contact", fontSize = 12.sp, color = secondaryText)
                }
            }
        } else {
            val favorites = contacts.filter { it.favorite }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (favorites.isNotEmpty()) {
                    item {
                        Text(
                            text = "⭐ Favorites",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            items(favorites, key = { "${it.name}_${it.number}" }) { contact ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(72.dp)
                                        .clickable { onCallClick(contact) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(contact.avatarBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (contact.name.length >= 2) contact.name.substring(0, 2).uppercase() else contact.name.take(1).uppercase(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = contact.avatarTextColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = contact.name.split(" ").firstOrNull() ?: contact.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            color = secondaryText.copy(alpha = 0.12f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                        )
                    }
                }

                item {
                    Text(
                        text = "All Contacts",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = secondaryText,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                    )
                }

                items(contacts, key = { "${it.name}_${it.number}" }) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onCallClick(contact) }
                            .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(contact.avatarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (contact.name.length >= 2) contact.name.substring(0, 2).uppercase() else contact.name.take(1).uppercase(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = contact.avatarTextColor
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = primaryText
                            )
                            Text(
                                text = "${contact.label} • ${contact.number}",
                                fontSize = 13.sp,
                                color = secondaryText
                            )
                        }

                        // Star/Unstar toggle
                        IconButton(
                            onClick = { onToggleFavorite(contact) },
                            modifier = Modifier.width(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Favorite Toggle",
                                tint = if (contact.favorite) Color(0xFFEAB308) else Color.LightGray
                            )
                        }

                        // More options dropdown
                        var menuExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.padding(horizontal = 0.dp)) {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.width(30.dp)
                            ) {
                                Text("⋮", fontSize = 18.sp, color = secondaryText, fontWeight = FontWeight.Bold)
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("✏️ Edit Contact") },
                                    onClick = {
                                        menuExpanded = false
                                        onEditContact(contact)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🗑️ Delete Contact") },
                                    onClick = {
                                        menuExpanded = false
                                        onDeleteContact(contact)
                                    }
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(activePill)
                                .clickable { onCallClick(contact) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = if (activePill == Color(0xFF004A77)) Color.White else Color(0xFF041E49),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
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
    onConfirm: () -> Unit,
    softBlueBg: Color,
    activeBluePill: Color,
    searchBarBg: Color,
    primaryDarkText: Color,
    brandBlue: Color,
    grayText: Color
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Create Contact",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryDarkText,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("dialog_name_input")
                )

                OutlinedTextField(
                    value = number,
                    onValueChange = onNumberChange,
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("dialog_phone_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Mobile", "Work", "Home").forEach { option ->
                        val isSelected = label == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) activeBluePill else searchBarBg)
                                .clickable { onLabelChange(option) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                fontSize = 12.sp,
                                color = if (isSelected) brandBlue else grayText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel", color = brandBlue, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = brandBlue)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
