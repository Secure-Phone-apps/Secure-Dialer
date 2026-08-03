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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Add
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
import com.example.model.*
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.theme.LocalM3Expressive
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import kotlinx.coroutines.launch

@Composable
fun ContactsTabContent(
    viewModel: com.example.ui.viewmodel.DialerViewModel,
    contactsPaged: LazyPagingItems<Contact>,
    favoriteContacts: List<Contact>,
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
    
    val allContacts by viewModel.allContactsFlow.collectAsState()
    var showOnlyFavorites by remember { mutableStateOf(false) }
    val sortedFavorites = remember(favoriteContacts) { favoriteContacts.sortedBy { it.name.uppercase() } }

    val alphabet = remember(allContacts, showOnlyFavorites, sortedFavorites) {
        val contactsList = if (showOnlyFavorites) sortedFavorites else allContacts
        val uniqueLetters = contactsList.asSequence()
            .map { it.name.trim() }
            .filter { it.isNotEmpty() }
            .map { it.first().uppercaseChar() }
            .distinct()
            .toMutableList()

        val letterChars = uniqueLetters.filter { it.isLetter() }.sorted()
        val otherChars = uniqueLetters.filter { !it.isLetter() }

        val result = mutableListOf<Char>()
        result.addAll(letterChars)
        if (otherChars.isNotEmpty() || result.isEmpty()) {
            result.add('#')
        }
        
        if (result.size <= 1) {
            ('A'..'Z').toList() + '#'
        } else {
            result
        }
    }

    val letterToIndex = remember(allContacts, showOnlyFavorites, sortedFavorites) {
        val contactsList = if (showOnlyFavorites) sortedFavorites else allContacts
        val map = HashMap<Char, Int>()
        for (i in contactsList.indices) {
            val contact = contactsList[i]
            val firstChar = contact.name.trim().firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (firstChar.isLetter()) firstChar else '#'
            if (!map.containsKey(key)) {
                map[key] = i
            }
        }
        map
    }

    val activeLetter = remember(listState, alphabet, showOnlyFavorites, sortedFavorites, contactsPaged) {
        derivedStateOf {
            val firstVisibleIndex = listState.firstVisibleItemIndex
            if (showOnlyFavorites) {
                if (firstVisibleIndex < sortedFavorites.size) {
                    val contact = sortedFavorites.getOrNull(firstVisibleIndex)
                    val firstChar = contact?.name?.trim()?.firstOrNull()?.uppercaseChar() ?: '#'
                    if (alphabet.contains(firstChar)) {
                        firstChar
                    } else if (!firstChar.isLetter() && alphabet.contains('#')) {
                        '#'
                    } else {
                        alphabet.firstOrNull() ?: '#'
                    }
                } else {
                    alphabet.firstOrNull() ?: '#'
                }
            } else {
                if (firstVisibleIndex < contactsPaged.itemCount) {
                    val contact = contactsPaged.peek(firstVisibleIndex)
                    val firstChar = contact?.name?.trim()?.firstOrNull()?.uppercaseChar() ?: '#'
                    if (alphabet.contains(firstChar)) {
                        firstChar
                    } else if (!firstChar.isLetter() && alphabet.contains('#')) {
                        '#'
                    } else {
                        alphabet.firstOrNull() ?: '#'
                    }
                } else {
                    alphabet.firstOrNull() ?: '#'
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (!hasPermission && !isLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.permissions_required),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.contacts_perm_desc),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text(stringResource(R.string.enable_contacts_perm))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (hasPermission && !isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showOnlyFavorites,
                        onClick = { showOnlyFavorites = false },
                        label = { Text(stringResource(R.string.filter_all_contacts)) },
                        shape = RoundedCornerShape(16.dp),
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    FilterChip(
                        selected = showOnlyFavorites,
                        onClick = { showOnlyFavorites = true },
                        label = { Text(stringResource(R.string.tab_favorites)) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (showOnlyFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Toggle Favorites",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = null,
                        modifier = Modifier.testTag("favorites_toggle_fab"),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 36.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAddContactClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("add_contact_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.action_add_contact),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_add_contact),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            val listItemsCount = if (showOnlyFavorites) sortedFavorites.size else contactsPaged.itemCount
            val isRefreshNotLoading = contactsPaged.loadState.refresh is LoadState.NotLoading

            if (listItemsCount == 0 && (showOnlyFavorites || (!showOnlyFavorites && isRefreshNotLoading))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateIllustration(
                        title = if (showOnlyFavorites) stringResource(R.string.no_favorites_title) else stringResource(R.string.no_contacts_title),
                        subtitle = if (showOnlyFavorites) stringResource(R.string.no_favorites_subtitle) else stringResource(R.string.no_contacts_subtitle)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().padding(end = 36.dp)
                ) {
                    if (showOnlyFavorites) {
                        items(
                            items = sortedFavorites,
                            key = { it.number }
                        ) { contact ->
                            val index = sortedFavorites.indexOf(contact)
                            val firstLetter = contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                            val prevContact = if (index > 0) sortedFavorites[index - 1] else null
                            val prevLetter = prevContact?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
                            
                            if (firstLetter != prevLetter) {
                                Text(
                                    text = firstLetter,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                                )
                            }
                            ContactRow(
                                contact = contact,
                                onCallClick = onCallClick,
                                onToggleFavorite = onToggleFavorite,
                                onEditContact = onEditContact,
                                onDeleteContact = onDeleteContact,
                                viewModel = viewModel
                            )
                        }
                    } else {
                        items(
                            count = contactsPaged.itemCount,
                            key = contactsPaged.itemKey { it.number },
                            contentType = contactsPaged.itemContentType { "contact" }
                        ) { index ->
                            val contact = contactsPaged[index]
                            if (contact != null) {
                                val firstLetter = contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                                val prevContact = if (index > 0) contactsPaged[index - 1] else null
                                val prevLetter = prevContact?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
                                
                                if (firstLetter != prevLetter) {
                                    Text(
                                        text = firstLetter,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                                    )
                                }
                                ContactRow(
                                    contact = contact,
                                    onCallClick = onCallClick,
                                    onToggleFavorite = onToggleFavorite,
                                    onEditContact = onEditContact,
                                    onDeleteContact = onDeleteContact,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }

                    if (!showOnlyFavorites && contactsPaged.loadState.append is LoadState.Loading) {
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
        val hasItems = if (showOnlyFavorites) sortedFavorites.isNotEmpty() else contactsPaged.itemCount > 0
        if (hasItems) {
            val haptic = LocalHapticFeedback.current
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(40.dp)
                    .padding(vertical = 40.dp)
                    .pointerInput(alphabet, letterToIndex) {
                        detectTapGestures { offset ->
                            val index = (offset.y / size.height * alphabet.size)
                                .toInt()
                                .coerceIn(0, alphabet.size - 1)
                            val char = alphabet[index]
                            letterToIndex[char]?.let { i ->
                                coroutineScope.launch {
                                    listState.animateScrollToItem(i)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }
                    }
                    .pointerInput(alphabet, letterToIndex) {
                        detectDragGestures { change, _ ->
                            val index = (change.position.y / size.height * alphabet.size)
                                .toInt()
                                .coerceIn(0, alphabet.size - 1)
                            val char = alphabet[index]
                            letterToIndex[char]?.let { i ->
                                coroutineScope.launch {
                                    listState.scrollToItem(i)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                alphabet.forEach { char ->
                    val isActive = char == activeLetter.value
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Text(
                            text = char.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = if (isActive) 11.sp else 9.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }
    }
}