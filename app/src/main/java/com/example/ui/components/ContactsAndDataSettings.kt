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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.viewmodel.DialerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ContactsAndDataSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current
    val timestamp = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) }

    var showContactsToDisplayDialog by remember { mutableStateOf(false) }
    var showDefaultAccountDialog by remember { mutableStateOf(false) }

    var exportPassword by remember { mutableStateOf("") }
    var pendingBackupData by remember { mutableStateOf<String?>(null) }
    var pendingEncryptedRestoreContent by remember { mutableStateOf<String?>(null) }
    var restorePasswordInput by remember { mutableStateOf("") }
    var showPasswordPromptDialog by remember { mutableStateOf(false) }

    val saveBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && pendingBackupData != null) {
            viewModel.writeTextToUri(uri, pendingBackupData!!) { success ->
                if (success) {
                    Toast.makeText(context, context.getString(R.string.file_saved_success), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.file_save_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val openBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.readTextFromUri(uri) { content ->
                if (!content.isNullOrBlank()) {
                    if (content.contains("\"encrypted\":true")) {
                        pendingEncryptedRestoreContent = content
                        showPasswordPromptDialog = true
                    } else {
                        viewModel.importBackup(content, "") { success ->
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.backup_restored_success_toast), Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, context.getString(R.string.backup_restore_failed_toast), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.file_read_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val saveVcfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/x-vcard")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportContactsVcf { vcfData ->
                viewModel.writeTextToUri(uri, vcfData) { success ->
                    if (success) {
                        Toast.makeText(context, context.getString(R.string.file_saved_success), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.file_save_failed), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val openVcfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.readTextFromUri(uri) { rawVcf ->
                if (!rawVcf.isNullOrBlank()) {
                    viewModel.importContactsVcf(rawVcf) { success ->
                        if (success) {
                            Toast.makeText(context, context.getString(R.string.vcf_import_count_success), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.file_read_failed), Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.file_read_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // [Header] CONTACTS MANAGEMENT
        item {
            PreferenceHeader(stringResource(R.string.header_contacts_management))
        }

        // Contacts to display Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                val currentFilter = viewModel.selectedAccountFilter.value
                val filterSub = if (currentFilter.isBlank()) {
                    stringResource(R.string.account_filter_all)
                } else {
                    viewModel.availableAccounts.firstOrNull { it.name == currentFilter }?.displayName ?: currentFilter
                }
                SettingsRowNav(
                    title = stringResource(R.string.settings_contacts_to_display),
                    subtitle = filterSub,
                    onClick = { showContactsToDisplayDialog = true },
                    icon = Icons.Default.FilterList,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Default Account Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                val defaultAccName = viewModel.defaultContactAccountName.value
                val defaultAccSub = if (defaultAccName.isBlank()) {
                    "Default / Same as Filter"
                } else {
                    viewModel.availableAccounts.firstOrNull { it.name == defaultAccName }?.displayName ?: defaultAccName
                }
                SettingsRowNav(
                    title = stringResource(R.string.settings_default_account_for_new),
                    subtitle = defaultAccSub,
                    onClick = { showDefaultAccountDialog = true },
                    icon = Icons.Default.PersonAdd,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // [Header] BACKUP & RESTORE
        item {
            Spacer(modifier = Modifier.height(12.dp))
            PreferenceHeader(stringResource(R.string.header_data_backup))
        }

        // Export Local Backup Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.backup_export_title),
                    subtitle = stringResource(R.string.backup_export_desc),
                    onClick = {
                        viewModel.exportBackup(exportPassword) { data ->
                            pendingBackupData = data
                            val defaultName = "dialer_backup_$timestamp.json"
                            saveBackupFileLauncher.launch(defaultName)
                        }
                    },
                    icon = Icons.Default.CloudDownload,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Restore Local Backup Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.btn_restore_backup_file),
                    subtitle = "Restore call logs and settings from encrypted JSON file",
                    onClick = {
                        openBackupFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    icon = Icons.Default.Restore,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // [Header] CONTACTS IMPORT & EXPORT
        item {
            Spacer(modifier = Modifier.height(12.dp))
            PreferenceHeader("CONTACTS IMPORT & EXPORT")
        }

        // Export Contacts (.vcf) Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = "Export Contacts (vCard)",
                    subtitle = stringResource(R.string.vcf_migration_desc),
                    onClick = {
                        val defaultName = "contacts_$timestamp.vcf"
                        saveVcfLauncher.launch(defaultName)
                    },
                    icon = Icons.Default.Contacts,
                    iconBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // Import Contacts (.vcf) Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = "Import Contacts (vCard)",
                    subtitle = "Restore contacts from standard .vcf card file",
                    onClick = {
                        openVcfLauncher.launch(arrayOf("text/x-vcard", "text/vcard", "text/plain", "*/*"))
                    },
                    icon = Icons.Default.UploadFile,
                    iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Dialogs
    if (showContactsToDisplayDialog) {
        AlertDialog(
            onDismissRequest = { showContactsToDisplayDialog = false },
            title = { Text(stringResource(R.string.settings_contacts_to_display)) },
            text = {
                Column {
                    val accounts = viewModel.availableAccounts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = viewModel.selectedAccountFilter.value.isBlank(),
                            onClick = {
                                viewModel.selectedAccountFilter.value = ""
                                showContactsToDisplayDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.account_filter_all))
                    }
                    accounts.forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = viewModel.selectedAccountFilter.value == acc.name,
                                onClick = {
                                    viewModel.selectedAccountFilter.value = acc.name
                                    showContactsToDisplayDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(acc.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContactsToDisplayDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showDefaultAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDefaultAccountDialog = false },
            title = { Text(stringResource(R.string.settings_default_account_for_new)) },
            text = {
                Column {
                    val accounts = viewModel.availableAccounts
                    accounts.forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = viewModel.defaultContactAccountName.value == acc.name,
                                onClick = {
                                    viewModel.defaultContactAccountName.value = acc.name
                                    showDefaultAccountDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(acc.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDefaultAccountDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showPasswordPromptDialog && pendingEncryptedRestoreContent != null) {
        AlertDialog(
            onDismissRequest = {
                showPasswordPromptDialog = false
                pendingEncryptedRestoreContent = null
            },
            title = { Text(stringResource(R.string.backup_password_label)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This backup file is encrypted. Enter the decryption password to restore your data:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = restorePasswordInput,
                        onValueChange = { restorePasswordInput = it },
                        label = { Text(stringResource(R.string.backup_password_label)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val content = pendingEncryptedRestoreContent ?: ""
                        viewModel.importBackup(content, restorePasswordInput) { success ->
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.backup_restored_success_toast), Toast.LENGTH_LONG).show()
                                showPasswordPromptDialog = false
                                pendingEncryptedRestoreContent = null
                            } else {
                                Toast.makeText(context, context.getString(R.string.backup_restore_failed_toast), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.backup_restore_btn))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordPromptDialog = false
                        pendingEncryptedRestoreContent = null
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
