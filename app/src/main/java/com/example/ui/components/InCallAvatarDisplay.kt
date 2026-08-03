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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Contact
import com.example.model.getInitials
import com.example.model.getAvatarShape

@Composable
fun InCallAvatarDisplay(
    participants: List<Pair<String, String>>,
    contactName: String,
    contactNumber: String,
    contacts: List<Contact>,
    avatarShapeType: String = "circular"
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.size(120.dp),
        shape = getAvatarShape(avatarShapeType),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (participants.size > 1) {
                Text(text = "👥", fontSize = 64.sp)
            } else {
                val matchedContact = remember(contactNumber, contacts) {
                    contacts.find { it.number == contactNumber }
                }
                if (matchedContact != null && matchedContact.photoUri.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(matchedContact.photoUri)
                            .size(256, 256)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Contact Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val pName = participants.firstOrNull()?.first ?: contactName
                    val isSaved = matchedContact != null || (pName != contactNumber && pName != "Unknown" && pName.isNotBlank() && pName.any { it.isLetter() })
                    if (isSaved) {
                        val avatarText = getInitials(pName)
                        Text(
                            text = avatarText,
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Unsaved Contact Icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
        }
    }
}
