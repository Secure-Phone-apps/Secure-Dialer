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

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ContactAccount
import com.example.ui.theme.LocalM3Expressive

data class PhoneInputState(
    val id: String = java.util.UUID.randomUUID().toString(),
    var rawNumber: String = "",
    var label: String = "Mobile",
    var isPrimary: Boolean = false
)

data class EmailInputState(
    val id: String = java.util.UUID.randomUUID().toString(),
    var email: String = "",
    var label: String = "Home"
)

@Composable
fun AddContactDialog(
    initialName: String,
    initialNumber: String,
    initialLabel: String,
    initialEmail: String = "",
    initialNumbers: List<com.example.model.LabeledNumber> = emptyList(),
    initialEmails: List<com.example.model.LabeledEmail> = emptyList(),
    initialAddresses: List<com.example.model.LabeledAddress> = emptyList(),
    availableAccounts: List<ContactAccount> = emptyList(),
    selectedAccountFilter: String = "",
    defaultAccountName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, number: String, label: String, email: String, accountName: String, accountType: String) -> Unit = { _, _, _, _, _, _ -> },
    onConfirmWithDetails: ((name: String, numbers: List<com.example.model.LabeledNumber>, emails: List<com.example.model.LabeledEmail>, addresses: List<com.example.model.LabeledAddress>, accountName: String, accountType: String) -> Unit)? = null
) {
    val fieldShape = RoundedCornerShape(18.dp)
    val buttonShape = RoundedCornerShape(20.dp)
    var showMoreDetails by remember { mutableStateOf(initialAddresses.isNotEmpty()) }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf(initialAddresses.firstOrNull()?.address ?: "") }

    val defaultSaveAccount = remember(availableAccounts, defaultAccountName, selectedAccountFilter) {
        val specificAccounts = availableAccounts.filter { it.name.isNotBlank() }
        if (defaultAccountName.isNotBlank()) {
            specificAccounts.firstOrNull { it.name == defaultAccountName }
                ?: specificAccounts.firstOrNull { it.type == "com.google" }
                ?: specificAccounts.firstOrNull()
        } else if (selectedAccountFilter.isNotBlank()) {
            specificAccounts.firstOrNull { it.name == selectedAccountFilter }
                ?: specificAccounts.firstOrNull { it.type == "com.google" }
                ?: specificAccounts.firstOrNull()
        } else {
            specificAccounts.firstOrNull { it.type == "com.google" }
                ?: specificAccounts.firstOrNull()
        }
    }

    var selectedAccount by remember(defaultSaveAccount) { mutableStateOf<ContactAccount?>(defaultSaveAccount) }
    var accountMenuExpanded by remember { mutableStateOf(false) }

    var showEmailField by remember { mutableStateOf(initialEmails.isNotEmpty() || initialEmail.isNotBlank()) }
    var showAddressField by remember { mutableStateOf(initialAddresses.isNotEmpty() || addressInput.isNotBlank()) }

    val phoneInputs = remember {
        mutableStateListOf<PhoneInputState>().apply {
            if (initialNumbers.isNotEmpty()) {
                initialNumbers.forEach { item ->
                    add(PhoneInputState(rawNumber = item.number, label = item.label, isPrimary = item.isPrimary))
                }
            } else if (initialNumber.isNotBlank()) {
                add(PhoneInputState(rawNumber = initialNumber, label = initialLabel.ifBlank { "Mobile" }, isPrimary = true))
            } else {
                add(PhoneInputState(rawNumber = "", label = "Mobile", isPrimary = true))
            }
        }
    }

    val emailInputs = remember {
        mutableStateListOf<EmailInputState>().apply {
            if (initialEmails.isNotEmpty()) {
                initialEmails.forEach { item ->
                    add(EmailInputState(email = item.email, label = item.label))
                }
            } else if (initialEmail.isNotBlank()) {
                add(EmailInputState(email = initialEmail, label = "Home"))
            }
        }
    }

    LaunchedEffect(initialName) {
        val trimmed = initialName.trim()
        if (trimmed.isNotEmpty()) {
            val parts = trimmed.split("\\s+".toRegex())
            if (parts.isNotEmpty()) {
                firstName = parts[0]
                if (parts.size > 1) {
                    lastName = parts.subList(1, parts.size).joinToString(" ")
                }
            }
        }
    }

    val expressiveFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = {
            Text(
                text = if (initialName.isEmpty() && initialNumber.isEmpty()) stringResource(R.string.add_contact_dialog_title) else stringResource(R.string.edit_contact_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Full-width Account Banner displaying full email address
                val accountsToPick = availableAccounts.filter { it.name.isNotBlank() }
                if (accountsToPick.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = { accountMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = fieldShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column {
                                            Text(
                                                text = stringResource(R.string.contact_source_label),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = selectedAccount?.displayName ?: stringResource(R.string.contact_source_phone),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = stringResource(R.string.contact_source_select),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = accountMenuExpanded,
                                onDismissRequest = { accountMenuExpanded = false }
                            ) {
                                accountsToPick.forEach { account ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = account.displayName,
                                                fontWeight = if (account.name == selectedAccount?.name) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedAccount = account
                                            accountMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // First & Last Name
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text(stringResource(R.string.label_first_name)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("dialog_first_name_input"),
                            shape = fieldShape,
                            colors = expressiveFieldColors
                        )
                        TextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text(stringResource(R.string.label_last_name)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("dialog_last_name_input"),
                            shape = fieldShape,
                            colors = expressiveFieldColors
                        )
                    }
                }

                // Phone Numbers (Label selector at beginning, delete button ALWAYS on far right)
                items(phoneInputs.size) { index ->
                    val phoneItem = phoneInputs[index]
                    var labelMenuExpanded by remember { mutableStateOf(false) }

                    TextField(
                        value = phoneItem.rawNumber,
                        onValueChange = { phoneInputs[index] = phoneItem.copy(rawNumber = it) },
                        label = { Text(stringResource(R.string.label_phone_number)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = expressiveFieldColors,
                        shape = fieldShape,
                        modifier = Modifier.fillMaxWidth().testTag("dialog_phone_input_$index"),
                        leadingIcon = {
                            Box {
                                TextButton(
                                    onClick = { labelMenuExpanded = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Text(phoneItem.label, style = MaterialTheme.typography.labelSmall)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = labelMenuExpanded,
                                    onDismissRequest = { labelMenuExpanded = false }
                                ) {
                                    listOf("Mobile", "Work", "Home", "Main", "Other").forEach { lbl ->
                                        DropdownMenuItem(
                                            text = { Text(lbl) },
                                            onClick = {
                                                phoneInputs[index] = phoneItem.copy(label = lbl)
                                                labelMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (phoneInputs.size > 1) {
                                        phoneInputs.removeAt(index)
                                    } else {
                                        phoneInputs[0] = phoneItem.copy(rawNumber = "")
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove number",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }

                item {
                    FilledTonalButton(
                        onClick = {
                            phoneInputs.add(PhoneInputState(rawNumber = "", label = "Mobile", isPrimary = false))
                        },
                        shape = buttonShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add phone number", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Emails (On-demand, label at beginning, delete button ALWAYS on far right)
                if (showEmailField) {
                    items(emailInputs.size) { index ->
                        val emailItem = emailInputs[index]
                        var labelMenuExpanded by remember { mutableStateOf(false) }

                        TextField(
                            value = emailItem.email,
                            onValueChange = { emailInputs[index] = emailItem.copy(email = it) },
                            label = { Text(stringResource(R.string.label_email)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = expressiveFieldColors,
                            shape = fieldShape,
                            modifier = Modifier.fillMaxWidth().testTag("dialog_email_input_$index"),
                            leadingIcon = {
                                Box {
                                    TextButton(
                                        onClick = { labelMenuExpanded = true },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text(emailItem.label, style = MaterialTheme.typography.labelSmall)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    DropdownMenu(
                                        expanded = labelMenuExpanded,
                                        onDismissRequest = { labelMenuExpanded = false }
                                    ) {
                                        listOf("Home", "Work", "Other").forEach { lbl ->
                                            DropdownMenuItem(
                                                text = { Text(lbl) },
                                                onClick = {
                                                    emailInputs[index] = emailItem.copy(label = lbl)
                                                    labelMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (emailInputs.size > 1) {
                                            emailInputs.removeAt(index)
                                        } else {
                                            emailInputs.clear()
                                            showEmailField = false
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove email",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                // Address (On-demand)
                if (showAddressField) {
                    item {
                        TextField(
                            value = addressInput,
                            onValueChange = { addressInput = it },
                            label = { Text(stringResource(R.string.label_address)) },
                            singleLine = false,
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            shape = fieldShape,
                            colors = expressiveFieldColors,
                            leadingIcon = {
                                Text(
                                    text = "Home",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        addressInput = ""
                                        showAddressField = false
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove address",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                // Symmetrical On-Demand Action Chips for Email and Address
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!showEmailField || emailInputs.isEmpty()) {
                            FilledTonalButton(
                                onClick = {
                                    if (emailInputs.isEmpty()) {
                                        emailInputs.add(EmailInputState(email = "", label = "Home"))
                                    }
                                    showEmailField = true
                                },
                                shape = buttonShape,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Email", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            FilledTonalButton(
                                onClick = {
                                    emailInputs.add(EmailInputState(email = "", label = "Home"))
                                },
                                shape = buttonShape,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add email", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (!showAddressField) {
                            FilledTonalButton(
                                onClick = { showAddressField = true },
                                shape = buttonShape,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Address", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val hasValidNumber = phoneInputs.any { it.rawNumber.trim().isNotEmpty() }
            Button(
                onClick = {
                    val finalName = "${firstName.trim()} ${lastName.trim()}".trim()
                    val numbersList = phoneInputs.filter { it.rawNumber.trim().isNotEmpty() }.mapIndexed { idx, p ->
                        com.example.model.LabeledNumber(
                            number = p.rawNumber.trim(),
                            label = p.label.ifBlank { "Mobile" },
                            isPrimary = idx == 0
                        )
                    }
                    val emailsList = emailInputs.filter { it.email.trim().isNotEmpty() }.map {
                        com.example.model.LabeledEmail(email = it.email.trim(), label = it.label.ifBlank { "Home" })
                    }
                    val addressesList = if (addressInput.trim().isNotEmpty()) {
                        listOf(com.example.model.LabeledAddress(address = addressInput.trim(), label = "Home"))
                    } else emptyList()

                    val accName = selectedAccount?.name ?: ""
                    val accType = selectedAccount?.type ?: ""

                    if (onConfirmWithDetails != null) {
                        onConfirmWithDetails(finalName, numbersList, emailsList, addressesList, accName, accType)
                    } else {
                        val primaryNum = numbersList.firstOrNull()?.number ?: ""
                        val primaryLabel = numbersList.firstOrNull()?.label ?: "Mobile"
                        val primaryEmail = emailsList.firstOrNull()?.email ?: ""
                        onConfirm(finalName, primaryNum, primaryLabel, primaryEmail, accName, accType)
                    }
                },
                enabled = (firstName.trim().isNotEmpty() || lastName.trim().isNotEmpty()) && hasValidNumber,
                shape = buttonShape
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = buttonShape
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
