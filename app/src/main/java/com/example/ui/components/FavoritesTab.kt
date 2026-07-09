package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    onToggleFavorite: (Contact) -> Unit,
    primaryText: Color,
    secondaryText: Color,
    activePill: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Favorites",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = secondaryText,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⭐", fontSize = 48.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Text("No favorites added yet", fontWeight = FontWeight.Medium, color = secondaryText)
                    Text("Star contacts to access them instantly", fontSize = 12.sp, color = secondaryText)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(contacts, key = { "${it.name}_${it.number}" }) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onCallClick(contact.name, contact.number) }
                            .padding(12.dp),
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
                        Spacer(modifier = Modifier.width(16.dp))
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

                        // Unstar button
                        IconButton(onClick = { onToggleFavorite(contact) }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Unstar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(activePill)
                                .clickable { onCallClick(contact.name, contact.number) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📞", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
