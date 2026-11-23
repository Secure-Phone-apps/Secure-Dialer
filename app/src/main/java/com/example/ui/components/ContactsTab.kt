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

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.R
import com.example.model.Contact
import com.example.ui.theme.LocalAdaptiveLayout
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.RichHapticEngine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactsTabContent(
    viewModel: DialerViewModel,
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
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val allContacts by viewModel.allContactsFlow.collectAsStateWithLifecycle()
    val privateContactsCount by viewModel.privateContactsCountFlow.collectAsStateWithLifecycle()
    val selectedAccountFilter by viewModel.selectedAccountFilter
    val searchQuery by viewModel.searchQuery
    var showOnlyFavorites by remember { mutableStateOf(false) }
    val nameFormat by viewModel.contactNameFormat
    val sortedFavorites = remember(favoriteContacts, nameFormat) {
        favoriteContacts.sortedBy { com.example.getContactSortKey(it.name, nameFormat) }
    }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedContactIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pendingDeleteContacts by remember { mutableStateOf<List<Contact>?>(null) }
    var pendingExportVcfData by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedContactIds = emptySet()
    }

    val saveSelectedVcfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/x-vcard")
    ) { uri: Uri? ->
        val payload = pendingExportVcfData
        if (uri != null && payload != null) {
            viewModel.writeTextToUri(uri, payload) { success ->
                Toast.makeText(
                    context,
                    context.getString(if (success) R.string.file_saved_success else R.string.file_save_failed),
                    Toast.LENGTH_LONG
                ).show()
                pendingExportVcfData = null
                if (success) {
                    isSelectionMode = false
                    selectedContactIds = emptySet()
                }
            }
        } else {
            pendingExportVcfData = null
        }
    }

    // Unified contacts pipeline eliminates duplicate filtering
    val activeContactsList = remember(allContacts, showOnlyFavorites, sortedFavorites, selectedAccountFilter, searchQuery, nameFormat) {
        val baseList = if (showOnlyFavorites) {
            sortedFavorites
        } else if (selectedAccountFilter.isBlank()) {
            allContacts
        } else if (selectedAccountFilter.equals("Phone", ignoreCase = true)) {
            allContacts.filter {
                it.accountName.isBlank() || it.accountName.equals("Phone", ignoreCase = true) || it.accountType.contains("local", ignoreCase = true)
            }
        } else {
            allContacts.filter { it.accountName == selectedAccountFilter }
        }
        val queryTrimmed = searchQuery.trim()
        val queryFiltered = if (queryTrimmed.isEmpty()) {
            baseList
        } else {
            baseList.filter {
                it.name.contains(queryTrimmed, ignoreCase = true) ||
                    it.number.contains(queryTrimmed, ignoreCase = true) ||
                    it.t9Mapping.contains(queryTrimmed, ignoreCase = true) ||
                    it.note.contains(queryTrimmed, ignoreCase = true)
            }
        }
        queryFiltered.sortedBy { com.example.getContactSortKey(it.name, nameFormat) }
    }

    val selectedContactsList = remember(selectedContactIds, activeContactsList, sortedFavorites) {
        val pool = (activeContactsList + sortedFavorites).distinctBy { it.id }
        pool.filter { it.id in selectedContactIds }
    }

    val alphabet = remember(activeContactsList, nameFormat) {
        val uniqueLetters = activeContactsList.asSequence()
            .map { com.example.formatContactName(it.name, nameFormat).trim() }
            .filter { it.isNotEmpty() }
            .map { it.first().uppercaseChar() }
            .distinct()
            .toList()

        val letterChars = uniqueLetters.filter { it.isLetter() }.sorted()
        val otherChars = uniqueLetters.filter { !it.isLetter() }

        val result = mutableListOf<Char>()
        result.addAll(letterChars)
        if (otherChars.isNotEmpty() || result.isEmpty()) {
            result.add('#')
        }

        if (result.size <= 1) ('A'..'Z').toList() + '#' else result
    }

    val letterToIndex = remember(activeContactsList, nameFormat) {
        val map = HashMap<Char, Int>()
        for (i in activeContactsList.indices) {
            val contact = activeContactsList[i]
            val formatted = com.example.formatContactName(contact.name, nameFormat).trim()
            val firstChar = formatted.firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (firstChar.isLetter()) firstChar else '#'
            if (!map.containsKey(key)) {
                map[key] = i
            }
        }
        map
    }

    val activeLetter = remember(listState, alphabet, showOnlyFavorites, sortedFavorites, activeContactsList, contactsPaged, nameFormat) {
        derivedStateOf {
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val contact = if (showOnlyFavorites) {
                sortedFavorites.getOrNull(firstVisibleIndex)
            } else if (activeContactsList.isNotEmpty()) {
                activeContactsList.getOrNull(firstVisibleIndex)
            } else if (firstVisibleIndex < contactsPaged.itemCount) {
                contactsPaged.peek(firstVisibleIndex)
            } else {
                null
            }
            val formatted = if (contact != null) com.example.formatContactName(contact.name, nameFormat).trim() else ""
            val firstChar = formatted.firstOrNull()?.uppercaseChar() ?: '#'
            if (alphabet.contains(firstChar)) firstChar else if (!firstChar.isLetter() && alphabet.contains('#')) '#' else alphabet.firstOrNull() ?: '#'
        }
    }

    val adaptive = LocalAdaptiveLayout.current

    fun toggleSelectionFor(contact: Contact) {
        selectedContactIds = if (contact.id in selectedContactIds) {
            val next = selectedContactIds - contact.id
            if (next.isEmpty()) {
                isSelectionMode = false
            }
            next
        } else {
            isSelectionMode = true
            selectedContactIds + contact.id
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = adaptive.contentMaxWidth)
                .fillMaxSize()
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (adaptive.isVeryNarrowWidth) 8.dp else 12.dp)
        ) {
            if (!hasPermission && !isLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("contacts_permission_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.permissions_required),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.contacts_perm_desc),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(stringResource(R.string.enable_contacts_perm))
                        }
                    }
                }
            }

            if (!isLoading) {
                if (isSelectionMode) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("multi_select_bar"),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = {
                                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                            isSelectionMode = false
                                            selectedContactIds = emptySet()
                                        },
                                        modifier = Modifier.size(48.dp).testTag("multi_select_close_btn")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                                    }
                                    Text(
                                        text = stringResource(R.string.multi_select_title, selectedContactIds.size),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                val allVisibleIds = remember(activeContactsList) { activeContactsList.map { it.id }.toSet() }
                                val isAllSelected = allVisibleIds.isNotEmpty() && selectedContactIds.containsAll(allVisibleIds)
                                TextButton(
                                    onClick = {
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                        selectedContactIds = if (isAllSelected) emptySet() else allVisibleIds
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("multi_select_all_btn")
                                ) {
                                    Text(
                                        text = stringResource(
                                            if (isAllSelected) R.string.action_deselect_all else R.string.action_select_all
                                        ),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.WARNING)
                                        if (selectedContactsList.isNotEmpty()) {
                                            pendingDeleteContacts = selectedContactsList
                                        }
                                    },
                                    enabled = selectedContactsList.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("multi_select_delete_btn"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.btn_delete), maxLines = 1)
                                }

                                FilledTonalButton(
                                    onClick = {
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                                        if (selectedContactsList.isNotEmpty()) {
                                            viewModel.exportContactsVcf(selectedContactsList) { vcfData ->
                                                pendingExportVcfData = vcfData
                                                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                                saveSelectedVcfLauncher.launch("selected_contacts_$stamp.vcf")
                                            }
                                        }
                                    },
                                    enabled = selectedContactsList.isNotEmpty(),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("multi_select_export_vcf_btn"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.action_export_vcf), maxLines = 1)
                                }

                                val hasNonPrivateSelected = selectedContactsList.any { !it.isPrivateVault }
                                if (hasNonPrivateSelected) {
                                    FilledTonalButton(
                                        onClick = {
                                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                                            val toMove = selectedContactsList.filter { !it.isPrivateVault }
                                            if (toMove.isNotEmpty()) {
                                                viewModel.moveContactsToPrivateVault(toMove) { movedCount ->
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.toast_moved_to_private_vault, movedCount),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                isSelectionMode = false
                                                selectedContactIds = emptySet()
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("multi_select_move_vault_btn"),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.move_to_private_vault), maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showOnlyFavorites = !showOnlyFavorites },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showOnlyFavorites) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (showOnlyFavorites) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("favorites_toggle_fab")
                        ) {
                            Icon(
                                imageVector = if (showOnlyFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = stringResource(R.string.tab_favorites),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.tab_favorites),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (activeContactsList.isNotEmpty() || contactsPaged.itemCount > 0) {
                            FilledTonalButton(
                                onClick = {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    isSelectionMode = true
                                    selectedContactIds = emptySet()
                                },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("multi_select_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = stringResource(R.string.action_select_contacts),
                                    modifier = Modifier.size(18.dp)
                                )
                                if (!adaptive.isVeryNarrowWidth) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.action_select_contacts),
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onAddContactClick,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("add_contact_fab")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.action_add_contact),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.action_add_contact),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            val listItemsCount = if (showOnlyFavorites) sortedFavorites.size else maxOf(contactsPaged.itemCount, activeContactsList.size)
            val isRefreshNotLoading = contactsPaged.loadState.refresh is LoadState.NotLoading

            if (listItemsCount == 0 && (hasPermission || privateContactsCount > 0) && (showOnlyFavorites || (!showOnlyFavorites && isRefreshNotLoading))) {
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
            } else if (hasPermission || privateContactsCount > 0 || activeContactsList.isNotEmpty() || contactsPaged.itemCount > 0) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize().padding(end = 36.dp)
                ) {
                    if (showOnlyFavorites) {
                        // FIXED: itemsIndexed eliminates O(N^2) indexOf lookup on every scroll frame
                        itemsIndexed(
                            items = sortedFavorites,
                            key = { _, contact -> "${contact.id}_${contact.number}" },
                            contentType = { _, _ -> "contact" }
                        ) { index, contact ->
                            val currentDisplayName = com.example.formatContactName(contact.name, nameFormat).trim()
                            val firstLetter = currentDisplayName.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                            val prevContact = if (index > 0) sortedFavorites[index - 1] else null
                            val prevDisplayName = if (prevContact != null) com.example.formatContactName(prevContact.name, nameFormat).trim() else ""
                            val prevLetter = prevDisplayName.firstOrNull()?.uppercaseChar()?.toString() ?: ""

                            if (firstLetter != prevLetter) {
                                Text(
                                    text = firstLetter,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 2.dp)
                                )
                            }
                            ContactRow(
                                contact = contact,
                                onCallClick = onCallClick,
                                onToggleFavorite = onToggleFavorite,
                                onEditContact = onEditContact,
                                onDeleteContact = { pendingDeleteContacts = listOf(it) },
                                viewModel = viewModel,
                                isSelectionMode = isSelectionMode,
                                isSelected = contact.id in selectedContactIds,
                                onToggleSelect = { toggleSelectionFor(contact) },
                                onLongPress = {
                                    isSelectionMode = true
                                    selectedContactIds = selectedContactIds + contact.id
                                },
                                onMoveToPrivateVault = { c ->
                                    viewModel.moveContactsToPrivateVault(listOf(c)) { count ->
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_moved_to_private_vault, count),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onExportPrivateToSystem = { c ->
                                    viewModel.exportPrivateContactToSystem(c) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_copied_to_device, c.name),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                    } else if (activeContactsList.isEmpty() && contactsPaged.itemCount > 0) {
                        items(
                            count = contactsPaged.itemCount,
                            key = contactsPaged.itemKey { "${it.id}_${it.number}" },
                            contentType = contactsPaged.itemContentType { "contact" }
                        ) { index ->
                            val contact = contactsPaged[index]
                            if (contact != null) {
                                val currentDisplayName = com.example.formatContactName(contact.name, nameFormat).trim()
                                val firstLetter = currentDisplayName.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                                val prevContact = if (index > 0) contactsPaged[index - 1] else null
                                val prevDisplayName = if (prevContact != null) com.example.formatContactName(prevContact.name, nameFormat).trim() else ""
                                val prevLetter = prevDisplayName.firstOrNull()?.uppercaseChar()?.toString() ?: ""

                                if (firstLetter != prevLetter) {
                                    Text(
                                        text = firstLetter,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 2.dp)
                                    )
                                }
                                ContactRow(
                                    contact = contact,
                                    onCallClick = onCallClick,
                                    onToggleFavorite = onToggleFavorite,
                                    onEditContact = onEditContact,
                                    onDeleteContact = { pendingDeleteContacts = listOf(it) },
                                    viewModel = viewModel,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = contact.id in selectedContactIds,
                                    onToggleSelect = { toggleSelectionFor(contact) },
                                    onLongPress = {
                                        isSelectionMode = true
                                        selectedContactIds = selectedContactIds + contact.id
                                    },
                                    onMoveToPrivateVault = { c ->
                                        viewModel.moveContactsToPrivateVault(listOf(c)) { count ->
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.toast_moved_to_private_vault, count),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onExportPrivateToSystem = { c ->
                                        viewModel.exportPrivateContactToSystem(c) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.toast_copied_to_device, c.name),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = activeContactsList,
                            key = { _, contact -> "${contact.id}_${contact.number}" },
                            contentType = { _, _ -> "contact" }
                        ) { index, contact ->
                            val currentDisplayName = com.example.formatContactName(contact.name, nameFormat).trim()
                            val firstLetter = currentDisplayName.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                            val prevContact = if (index > 0) activeContactsList[index - 1] else null
                            val prevDisplayName = if (prevContact != null) com.example.formatContactName(prevContact.name, nameFormat).trim() else ""
                            val prevLetter = prevDisplayName.firstOrNull()?.uppercaseChar()?.toString() ?: ""

                            if (firstLetter != prevLetter) {
                                Text(
                                    text = firstLetter,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 2.dp)
                                )
                            }
                            ContactRow(
                                contact = contact,
                                onCallClick = onCallClick,
                                onToggleFavorite = onToggleFavorite,
                                onEditContact = onEditContact,
                                onDeleteContact = { pendingDeleteContacts = listOf(it) },
                                viewModel = viewModel,
                                isSelectionMode = isSelectionMode,
                                isSelected = contact.id in selectedContactIds,
                                onToggleSelect = { toggleSelectionFor(contact) },
                                onLongPress = {
                                    isSelectionMode = true
                                    selectedContactIds = selectedContactIds + contact.id
                                },
                                onMoveToPrivateVault = { c ->
                                    viewModel.moveContactsToPrivateVault(listOf(c)) { count ->
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_moved_to_private_vault, count),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onExportPrivateToSystem = { c ->
                                    viewModel.exportPrivateContactToSystem(c) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_copied_to_device, c.name),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
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
        val hasItems = if (showOnlyFavorites) sortedFavorites.isNotEmpty() else (activeContactsList.isNotEmpty() || contactsPaged.itemCount > 0)
        if (hasItems) {
            val haptic = LocalHapticFeedback.current
            val maxScrollTarget = (if (showOnlyFavorites) sortedFavorites.size else if (activeContactsList.isNotEmpty()) activeContactsList.size else contactsPaged.itemCount) - 1

            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(40.dp)
                    .padding(vertical = if (adaptive.isTightVerticalSpace) 8.dp else 28.dp)
            ) {
                val maxRailSlots = (maxHeight.value / 16f).toInt().coerceAtLeast(8)
                val step = if (alphabet.size > maxRailSlots) ((alphabet.size + maxRailSlots - 1) / maxRailSlots) else 1
                val visibleAlphabet = remember(alphabet, step) {
                    if (step <= 1) alphabet else alphabet.filterIndexed { i, _ -> i % step == 0 || i == alphabet.lastIndex }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(visibleAlphabet, letterToIndex, maxScrollTarget) {
                            detectTapGestures { offset ->
                                if (visibleAlphabet.isEmpty()) return@detectTapGestures
                                val index = (offset.y / size.height * visibleAlphabet.size).toInt().coerceIn(0, visibleAlphabet.size - 1)
                                val char = visibleAlphabet[index]
                                letterToIndex[char]?.let { targetIndex ->
                                    val safeIndex = targetIndex.coerceIn(0, maxScrollTarget.coerceAtLeast(0))
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(safeIndex)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            }
                        }
                        .pointerInput(visibleAlphabet, letterToIndex, maxScrollTarget) {
                            detectDragGestures { change, _ ->
                                if (visibleAlphabet.isEmpty()) return@detectDragGestures
                                val index = (change.position.y / size.height * visibleAlphabet.size).toInt().coerceIn(0, visibleAlphabet.size - 1)
                                val char = visibleAlphabet[index]
                                letterToIndex[char]?.let { targetIndex ->
                                    val safeIndex = targetIndex.coerceIn(0, maxScrollTarget.coerceAtLeast(0))
                                    coroutineScope.launch {
                                        listState.scrollToItem(safeIndex)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    visibleAlphabet.forEach { char ->
                        val isActive = char == activeLetter.value
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .sizeIn(minWidth = 18.dp, minHeight = 14.dp, maxHeight = 20.dp)
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
    }

    val targetsToDelete = pendingDeleteContacts
    if (targetsToDelete != null && targetsToDelete.isNotEmpty()) {
        val isBulk = targetsToDelete.size > 1
        AlertDialog(
            onDismissRequest = { pendingDeleteContacts = null },
            title = {
                Text(
                    text = if (isBulk) {
                        stringResource(R.string.confirm_delete_contacts_title, targetsToDelete.size)
                    } else {
                        stringResource(R.string.confirm_delete_contact_title)
                    }
                )
            },
            text = {
                Text(
                    text = if (isBulk) {
                        stringResource(R.string.confirm_delete_contacts_msg, targetsToDelete.size)
                    } else {
                        stringResource(R.string.confirm_delete_contact_msg, targetsToDelete.first().name)
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.WARNING)
                        val countToDelete = targetsToDelete.size
                        pendingDeleteContacts = null
                        if (countToDelete == 1) {
                            onDeleteContact(targetsToDelete.first())
                        } else {
                            viewModel.deleteContacts(targetsToDelete) { deleted ->
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_deleted_contacts, deleted),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        isSelectionMode = false
                        selectedContactIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_contacts_btn")
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteContacts = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}