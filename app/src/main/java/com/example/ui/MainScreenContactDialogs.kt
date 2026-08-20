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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.AddContactDialog
import com.example.ui.viewmodel.DialerViewModel

@Composable
fun MainScreenContactDialogs(
    viewModel: DialerViewModel
) {
    var isAddContactDialogVisible by viewModel.isAddContactDialogVisible
    var isEditContactDialogVisible by viewModel.isEditContactDialogVisible
    var oldContactToEdit by viewModel.oldContactToEdit
    val newContactName by viewModel.newContactName
    val newContactNumber by viewModel.newContactNumber
    val newContactLabel by viewModel.newContactLabel

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
            onConfirm = { name, number, label, email, accountName, accountType ->
                viewModel.addContact(name, number, label, email, accountName, accountType)
                isAddContactDialogVisible = false
            }
        )
    }

    if (isEditContactDialogVisible && oldContactToEdit != null) {
        val currentOldContact = oldContactToEdit
        if (currentOldContact != null) {
            AddContactDialog(
                initialName = currentOldContact.name,
                initialNumber = currentOldContact.number,
                initialLabel = currentOldContact.label,
                initialEmail = currentOldContact.email,
                availableAccounts = viewModel.availableAccounts,
                selectedAccountFilter = currentOldContact.accountName,
                onDismiss = { isEditContactDialogVisible = false },
                onConfirm = { name, number, label, email, accountName, accountType ->
                    viewModel.deleteContact(currentOldContact)
                    viewModel.addContact(name, number, label, email, accountName, accountType)
                    isEditContactDialogVisible = false
                }
            )
        }
    }
}
