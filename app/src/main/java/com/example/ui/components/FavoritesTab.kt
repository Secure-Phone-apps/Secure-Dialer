package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Contact

@Composable
fun FavoritesTabContent(
    contacts: List<Contact>,
    onCallClick: (String, String) -> Unit,
    onToggleFavorite: (Contact) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Favorites",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No favorites yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Star contacts to find them easily",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp) // Space for Dialpad overlay
            ) {
                items(contacts, key = { "${it.name}_${it.number}" }) { contact ->
                    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                    val containerColor = if (isDark) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCallClick(contact.name, contact.number) },
                        colors = CardDefaults.cardColors(
                            containerColor = containerColor
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        ListItem(
                            headlineContent = {
                                Column(
                                    modifier = Modifier.offset(x = (-8).dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 18.sp
                                    )
                                    Text(
                                        text = "${contact.label} • ${contact.number}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            supportingContent = null,
                            leadingContent = {
                                Surface(
                                    modifier = Modifier
                                        .offset(x = (-8).dp)
                                        .size(44.dp),
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(onClick = { onToggleFavorite(contact) }) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Unstar",
                                            tint = Color(0xFFEAB308)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onCallClick(contact.name, contact.number) },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "Call",
                                            modifier = Modifier.size(18.dp)
                                        )
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
