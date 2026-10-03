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

package com.example.ui

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddContactDialog
import com.example.ui.components.AddToExistingContactSheet
import com.example.ui.viewmodel.DialerViewModel

@Composable
fun MainScreenContactDialogs(
    viewModel: DialerViewModel
) {
    var isAddContactDialogVisible by viewModel.isAddContactDialogVisible
    var isEditContactDialogVisible by viewModel.isEditContactDialogVisible
    var isAddToExistingSheetVisible by viewModel.isAddToExistingSheetVisible
    val addToExistingPendingNumber by viewModel.addToExistingPendingNumber
    val allContacts by viewModel.allContactsFlow.collectAsStateWithLifecycle()
    var oldContactToEdit by viewModel.oldContactToEdit
    val newContactName by viewModel.newContactName
    val newContactNumber by viewModel.newContactNumber
    val newContactLabel by viewModel.newContactLabel

    if (isAddToExistingSheetVisible) {
        AddToExistingContactSheet(
            contacts = allContacts,
            pendingNumber = addToExistingPendingNumber,
            onContactSelected = { contact ->
                isAddToExistingSheetVisible = false
                viewModel.openAddToExistingContactWithNumber(addToExistingPendingNumber, contact)
            },
            onDismiss = { isAddToExistingSheetVisible = false },
            viewModel = viewModel
        )
    }

    if (isAddContactDialogVisible) {
        AddContactDialog(
            initialName = newContactName,
            initialNumber = newContactNumber,
            initialLabel = newContactLabel,
            initialEmail = "",
            availableAccounts = viewModel.availableAccounts,
            selectedAccountFilter = viewModel.selectedAccountFilter.value,
            defaultAccountName = viewModel.defaultContactAccountName.value,
            onDismiss = { isAddContactDialogVisible = false },
            onConfirmWithDetails = { name, numbers, emails, addresses, accountName, accountType ->
                viewModel.addContactWithDetails(name, numbers, emails, addresses, accountName, accountType)
                isAddContactDialogVisible = false
            }
        )
    }

    val contactToEdit = oldContactToEdit
    if (isEditContactDialogVisible && contactToEdit != null) {
        AddContactDialog(
            initialName = contactToEdit.name,
            initialNumber = contactToEdit.number,
            initialLabel = contactToEdit.label,
            initialEmail = contactToEdit.email,
            initialNumbers = contactToEdit.getAllNumbers(),
            initialEmails = contactToEdit.getAllEmails(),
            initialAddresses = contactToEdit.getAllAddresses(),
            availableAccounts = viewModel.availableAccounts,
            selectedAccountFilter = contactToEdit.accountName,
            onDismiss = {
                isEditContactDialogVisible = false
                oldContactToEdit = null
            },
            onConfirmWithDetails = { name, numbers, emails, addresses, accountName, accountType ->
                viewModel.deleteContact(contactToEdit)
                viewModel.addContactWithDetails(name, numbers, emails, addresses, accountName, accountType)
                if (contactToEdit.favorite) {
                    val primaryNum = numbers.firstOrNull()?.number ?: contactToEdit.number
                    viewModel.toggleFavorite(primaryNum, true)
                }
                isEditContactDialogVisible = false
                oldContactToEdit = null
            }
        )
    }
}