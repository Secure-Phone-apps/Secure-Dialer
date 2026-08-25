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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ContactAccount
import com.example.ui.theme.LocalM3Expressive

data class Country(val code: String, val name: String, val prefix: String, val flag: String)

val COUNTRIES = listOf(
    Country("IN", "India", "+91", "🇮🇳"),
    Country("US", "United States", "+1", "🇺🇸"),
    Country("GB", "United Kingdom", "+44", "🇬🇧"),
    Country("CA", "Canada", "+1", "🇨🇦"),
    Country("AU", "Australia", "+61", "🇦🇺"),
    Country("DE", "Germany", "+49", "🇩🇪"),
    Country("FR", "France", "+33", "🇫🇷"),
    Country("IT", "Italy", "+39", "🇮🇹"),
    Country("ES", "Spain", "+34", "🇪🇸"),
    Country("JP", "Japan", "+81", "🇯🇵"),
    Country("CN", "China", "+86", "🇨🇳"),
    Country("BR", "Brazil", "+55", "🇧🇷"),
    Country("RU", "Russia", "+7", "🇷🇺"),
    Country("ZA", "South Africa", "+27", "🇿🇦"),
    Country("SG", "Singapore", "+65", "🇸🇬"),
    Country("MY", "Malaysia", "+60", "🇲🇾"),
    Country("ID", "Indonesia", "+62", "🇮🇩"),
    Country("AE", "United Arab Emirates", "+971", "🇦🇪"),
    Country("SA", "Saudi Arabia", "+966", "🇸🇦"),
    Country("PK", "Pakistan", "+92", "🇵🇰"),
    Country("BD", "Bangladesh", "+880", "🇧🇩"),
    Country("LK", "Sri Lanka", "+94", "🇱🇰"),
    Country("NP", "Nepal", "+977", "🇳🇵"),
    Country("MX", "Mexico", "+52", "🇲🇽"),
    Country("NZ", "New Zealand", "+64", "🇳🇿"),
    Country("NL", "Netherlands", "+31", "🇳🇱"),
    Country("CH", "Switzerland", "+41", "🇨🇭"),
    Country("SE", "Sweden", "+46", "🇸🇪"),
    Country("NO", "Norway", "+47", "🇳🇴")
)

data class PhoneInputState(
    val id: String = java.util.UUID.randomUUID().toString(),
    var country: Country? = null,
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
    val context = LocalContext.current
    val dialogShape = RoundedCornerShape(20.dp)
    val fieldShape = RoundedCornerShape(14.dp)
    val buttonShape = RoundedCornerShape(12.dp)
    val isExpressive = LocalM3Expressive.current

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

    val countryIso = remember {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            tm?.networkCountryIso?.uppercase() ?: tm?.simCountryIso?.uppercase() ?: java.util.Locale.getDefault().country.uppercase()
        } catch (e: Exception) {
            java.util.Locale.getDefault().country.uppercase()
        }
    }
    
    val defaultCountry = remember(countryIso) {
        COUNTRIES.firstOrNull { it.code == countryIso } ?: COUNTRIES.first { it.code == "IN" }
    }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf(initialAddresses.firstOrNull()?.address ?: "") }

    val phoneInputs = remember {
        mutableStateListOf<PhoneInputState>().apply {
            if (initialNumbers.isNotEmpty()) {
                initialNumbers.forEach { item ->
                    val num = item.number
                    val matchedCountry = if (num.startsWith("+")) {
                        COUNTRIES.filter { num.startsWith(it.prefix) }.maxByOrNull { it.prefix.length } ?: defaultCountry
                    } else defaultCountry
                    val raw = if (num.startsWith("+") && matchedCountry != null) num.substring(matchedCountry.prefix.length) else num
                    add(PhoneInputState(country = matchedCountry, rawNumber = raw, label = item.label, isPrimary = item.isPrimary))
                }
            } else if (initialNumber.isNotBlank()) {
                val matchedCountry = if (initialNumber.startsWith("+")) {
                    COUNTRIES.filter { initialNumber.startsWith(it.prefix) }.maxByOrNull { it.prefix.length } ?: defaultCountry
                } else defaultCountry
                val raw = if (initialNumber.startsWith("+") && matchedCountry != null) initialNumber.substring(matchedCountry.prefix.length) else initialNumber
                add(PhoneInputState(country = matchedCountry, rawNumber = raw, label = initialLabel.ifBlank { "Mobile" }, isPrimary = true))
            } else {
                add(PhoneInputState(country = defaultCountry, rawNumber = "", label = "Mobile", isPrimary = true))
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

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = dialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isExpressive) 6.dp else 3.dp,
        title = {
            Text(
                text = if (initialName.isEmpty() && initialNumber.isEmpty()) stringResource(R.string.add_contact_dialog_title) else stringResource(R.string.edit_contact_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val accountsToPick = availableAccounts.filter { it.name.isNotBlank() }
                if (accountsToPick.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { accountMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = fieldShape,
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.contact_source_label),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = selectedAccount?.displayName ?: stringResource(R.string.contact_source_phone),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
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
                                onDismissRequest = { accountMenuExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
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

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text(stringResource(R.string.label_first_name)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("dialog_first_name_input"),
                            shape = fieldShape
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text(stringResource(R.string.label_last_name)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("dialog_last_name_input"),
                            shape = fieldShape
                        )
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.label_phone_number),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(phoneInputs.size) { index ->
                    val phoneItem = phoneInputs[index]
                    var expandedCountryMenu by remember { mutableStateOf(false) }
                    var expandedLabelMenu by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = fieldShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box {
                                    OutlinedButton(
                                        onClick = { expandedCountryMenu = true },
                                        modifier = Modifier.height(52.dp),
                                        shape = fieldShape,
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = "${phoneItem.country?.flag ?: "🌐"} ${phoneItem.country?.prefix ?: ""}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Country",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    
                                    DropdownMenu(
                                        expanded = expandedCountryMenu,
                                        onDismissRequest = { expandedCountryMenu = false },
                                        modifier = Modifier.heightIn(max = 280.dp)
                                    ) {
                                        COUNTRIES.forEach { country ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(country.flag)
                                                        Text(country.name)
                                                        Spacer(modifier = Modifier.weight(1f))
                                                        Text(country.prefix, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                },
                                                onClick = {
                                                    phoneInputs[index] = phoneItem.copy(country = country)
                                                    expandedCountryMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = phoneItem.rawNumber,
                                    onValueChange = { phoneInputs[index] = phoneItem.copy(rawNumber = it) },
                                    placeholder = { Text("Phone number") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("dialog_phone_input_$index"),
                                    shape = fieldShape
                                )

                                if (phoneInputs.size > 1) {
                                    IconButton(
                                        onClick = { phoneInputs.removeAt(index) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                                            contentDescription = "Remove number",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("Mobile", "Work", "Home", "Other").forEach { opt ->
                                    FilterChip(
                                        selected = phoneItem.label.equals(opt, ignoreCase = true),
                                        onClick = { phoneInputs[index] = phoneItem.copy(label = opt) },
                                        label = { Text(opt, style = MaterialTheme.typography.labelSmall) },
                                        shape = fieldShape
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    TextButton(
                        onClick = {
                            phoneInputs.add(PhoneInputState(country = defaultCountry, rawNumber = "", label = "Mobile", isPrimary = false))
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.label_add_number))
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.label_email),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(emailInputs.size) { index ->
                    val emailItem = emailInputs[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = emailItem.email,
                            onValueChange = { emailInputs[index] = emailItem.copy(email = it) },
                            placeholder = { Text("Email address") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("dialog_email_input_$index"),
                            shape = fieldShape,
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") }
                        )
                        IconButton(
                            onClick = { emailInputs.removeAt(index) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                                contentDescription = "Remove email",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                item {
                    TextButton(
                        onClick = {
                            emailInputs.add(EmailInputState(email = "", label = "Home"))
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.label_add_email))
                    }
                }

                item {
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text(stringResource(R.string.label_address)) },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = fieldShape,
                        leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.LocationOn, contentDescription = "Address") }
                    )
                }
            }
        },
        confirmButton = {
            val hasValidNumber = phoneInputs.any { it.rawNumber.trim().isNotEmpty() }
            Button(
                onClick = {
                    val finalName = "${firstName.trim()} ${lastName.trim()}".trim()
                    val numbersList = phoneInputs.filter { it.rawNumber.trim().isNotEmpty() }.mapIndexed { idx, p ->
                        val pfx = p.country?.prefix ?: ""
                        val formattedNum = if (p.rawNumber.startsWith("+")) p.rawNumber.trim() else "$pfx${p.rawNumber.trim()}"
                        com.example.model.LabeledNumber(
                            number = formattedNum,
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
                enabled = firstName.trim().isNotEmpty() && hasValidNumber,
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
