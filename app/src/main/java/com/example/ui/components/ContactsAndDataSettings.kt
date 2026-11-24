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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ContactNameFormat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.PRIVATE_VAULT_ACCOUNT_NAME
import com.example.model.PRIVATE_VAULT_ACCOUNT_TYPE
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.RichHapticEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ContactsAndDataSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color,
    highlightedTitle: String? = null
) {
    val context = LocalContext.current
    val timestamp = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) }

    var showContactsToDisplayDialog by remember { mutableStateOf(false) }
    var showDefaultAccountDialog by remember { mutableStateOf(false) }
    var showNameFormatDialog by remember { mutableStateOf(false) }

    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var exportPasswordInput by remember { mutableStateOf("") }
    var pendingBackupData by remember { mutableStateOf<String?>(null) }
    var pendingEncryptedRestoreContent by remember { mutableStateOf<String?>(null) }
    var restorePasswordInput by remember { mutableStateOf("") }
    var showPasswordPromptDialog by remember { mutableStateOf(false) }
    var pendingVcfImportContent by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            exportPasswordInput = ""
            restorePasswordInput = ""
            pendingBackupData = null
            pendingEncryptedRestoreContent = null
        }
    }

    val saveBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val backupData = pendingBackupData
        if (uri != null && backupData != null) {
            viewModel.writeTextToUri(uri, backupData) { success ->
                if (success) {
                    Toast.makeText(context, context.getString(R.string.file_saved_success), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.file_save_failed), Toast.LENGTH_LONG).show()
                }
                pendingBackupData = null
            }
        }
    }

    val openBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.readTextFromUri(uri) { content ->
                if (!content.isNullOrBlank()) {
                    val isEncrypted = isBackupPayloadEncrypted(content)
                    if (isEncrypted) {
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
                    pendingVcfImportContent = rawVcf
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
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Account Display Filters", highlightedTitle) ||
                        isMatchTitle("Contacts & Data Settings", highlightedTitle) ||
                        isMatchTitle("Contacts to display", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                val currentFilter = viewModel.selectedAccountFilter.value
                val filterSub = when {
                    currentFilter.isBlank() -> stringResource(R.string.account_filter_all)
                    currentFilter == PRIVATE_VAULT_ACCOUNT_NAME -> stringResource(R.string.contact_source_private_vault)
                    else -> viewModel.availableAccounts.firstOrNull { it.name == currentFilter }?.displayName ?: currentFilter
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
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Default Save Account", highlightedTitle) ||
                        isMatchTitle("Default Account", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                val defaultAccName = viewModel.defaultContactAccountName.value
                val defaultAccSub = when {
                    defaultAccName.isBlank() -> stringResource(R.string.contact_source_phone)
                    defaultAccName == PRIVATE_VAULT_ACCOUNT_NAME -> stringResource(R.string.contact_source_private_vault)
                    else -> viewModel.availableAccounts.firstOrNull { it.name == defaultAccName }?.displayName ?: defaultAccName
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

        // Name format & sort order Card
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Name format & sort order", highlightedTitle) ||
                        isMatchTitle("Sort contacts", highlightedTitle) ||
                        isMatchTitle("Name Format", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                val currentFormat = viewModel.contactNameFormat.value
                val formatSub = if (currentFormat == ContactNameFormat.FIRST_NAME_FIRST) {
                    stringResource(R.string.name_format_first_name_first)
                } else {
                    stringResource(R.string.name_format_last_name_first)
                }
                SettingsRowNav(
                    title = stringResource(R.string.settings_name_format_title),
                    subtitle = formatSub,
                    onClick = { showNameFormatDialog = true },
                    icon = Icons.Default.SortByAlpha,
                    iconBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.tertiary
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
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Database Backup & Restore", highlightedTitle) ||
                        isMatchTitle("Backup", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.backup_export_title),
                    subtitle = stringResource(R.string.backup_export_desc),
                    onClick = {
                        exportPasswordInput = ""
                        showExportPasswordDialog = true
                    },
                    icon = Icons.Default.CloudDownload,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Restore Local Backup Card
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Restore", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.btn_restore_backup_file),
                    subtitle = stringResource(R.string.settings_backup_restore_sub),
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
            PreferenceHeader(stringResource(R.string.header_contacts_import_export))
        }

        // Export Contacts (.vcf) Card
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Export Contacts (vCard / .vcf)", highlightedTitle) ||
                        isMatchTitle("Export Contacts", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.settings_export_contacts_vcard),
                    subtitle = stringResource(R.string.vcf_migration_desc),
                    onClick = {
                        saveVcfLauncher.launch("contacts_$timestamp.vcf")
                    },
                    icon = Icons.Default.Contacts,
                    iconBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // Import Contacts (.vcf) Card
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Import Contacts", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.settings_import_contacts_vcard),
                    subtitle = stringResource(R.string.settings_import_contacts_vcard_sub),
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
    if (showExportPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                exportPasswordInput = ""
                showExportPasswordDialog = false
            },
            title = { Text(stringResource(R.string.backup_export_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.backup_export_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = exportPasswordInput,
                        onValueChange = { exportPasswordInput = it },
                        label = { Text(stringResource(R.string.backup_password_label)) },
                        placeholder = { Text(stringResource(R.string.optional_note_label)) },
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
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                        val chars = exportPasswordInput.toCharArray()
                        exportPasswordInput = ""
                        showExportPasswordDialog = false
                        viewModel.exportBackup(chars) { data ->
                            pendingBackupData = data
                            saveBackupFileLauncher.launch("dialer_backup_$timestamp.json")
                        }
                    }
                ) {
                    Text(stringResource(R.string.btn_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        exportPasswordInput = ""
                        showExportPasswordDialog = false
                    }
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (showContactsToDisplayDialog) {
        AlertDialog(
            onDismissRequest = { showContactsToDisplayDialog = false },
            title = { Text(stringResource(R.string.settings_contacts_to_display)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    val accounts = viewModel.availableAccounts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                viewModel.onAccountFilterChange("")
                                showContactsToDisplayDialog = false
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = viewModel.selectedAccountFilter.value.isBlank(),
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                viewModel.onAccountFilterChange("")
                                showContactsToDisplayDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.account_filter_all))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                viewModel.onAccountFilterChange(PRIVATE_VAULT_ACCOUNT_NAME)
                                showContactsToDisplayDialog = false
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = viewModel.selectedAccountFilter.value == PRIVATE_VAULT_ACCOUNT_NAME,
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                viewModel.onAccountFilterChange(PRIVATE_VAULT_ACCOUNT_NAME)
                                showContactsToDisplayDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.contact_source_private_vault))
                    }
                    accounts.forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    viewModel.onAccountFilterChange(acc.name)
                                    showContactsToDisplayDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = viewModel.selectedAccountFilter.value == acc.name,
                                onClick = {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    viewModel.onAccountFilterChange(acc.name)
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
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    val accounts = viewModel.availableAccounts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                viewModel.updateDefaultContactAccount("", "")
                                showDefaultAccountDialog = false
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = viewModel.defaultContactAccountName.value.isBlank(),
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                viewModel.updateDefaultContactAccount("", "")
                                showDefaultAccountDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.contact_source_phone))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                viewModel.updateDefaultContactAccount(PRIVATE_VAULT_ACCOUNT_NAME, PRIVATE_VAULT_ACCOUNT_TYPE)
                                showDefaultAccountDialog = false
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = viewModel.defaultContactAccountName.value == PRIVATE_VAULT_ACCOUNT_NAME,
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                viewModel.updateDefaultContactAccount(PRIVATE_VAULT_ACCOUNT_NAME, PRIVATE_VAULT_ACCOUNT_TYPE)
                                showDefaultAccountDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.contact_source_private_vault))
                    }
                    accounts.forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    viewModel.updateDefaultContactAccount(acc.name, acc.type)
                                    showDefaultAccountDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = viewModel.defaultContactAccountName.value == acc.name,
                                onClick = {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    viewModel.updateDefaultContactAccount(acc.name, acc.type)
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

    if (showNameFormatDialog) {
        val currentFormat = viewModel.contactNameFormat.value
        AlertDialog(
            onDismissRequest = { showNameFormatDialog = false },
            title = { Text(stringResource(R.string.settings_name_format_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                viewModel.setContactNameFormat(ContactNameFormat.FIRST_NAME_FIRST)
                                showNameFormatDialog = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentFormat == ContactNameFormat.FIRST_NAME_FIRST,
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                viewModel.setContactNameFormat(ContactNameFormat.FIRST_NAME_FIRST)
                                showNameFormatDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.name_format_first_name_first))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                viewModel.setContactNameFormat(ContactNameFormat.LAST_NAME_FIRST)
                                showNameFormatDialog = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentFormat == ContactNameFormat.LAST_NAME_FIRST,
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                viewModel.setContactNameFormat(ContactNameFormat.LAST_NAME_FIRST)
                                showNameFormatDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.name_format_last_name_first))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNameFormatDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showPasswordPromptDialog && pendingEncryptedRestoreContent != null) {
        AlertDialog(
            onDismissRequest = {
                restorePasswordInput = ""
                showPasswordPromptDialog = false
                pendingEncryptedRestoreContent = null
            },
            title = { Text(stringResource(R.string.backup_password_label)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.backup_restore_desc),
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
                        val chars = restorePasswordInput.toCharArray()
                        restorePasswordInput = ""
                        viewModel.importBackup(content, chars) { success ->
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
                        restorePasswordInput = ""
                        showPasswordPromptDialog = false
                        pendingEncryptedRestoreContent = null
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    val rawVcfToImport = pendingVcfImportContent
    if (rawVcfToImport != null) {
        VcfImportDestinationDialog(
            viewModel = viewModel,
            rawVcf = rawVcfToImport,
            onDismiss = { pendingVcfImportContent = null }
        )
    }
}

@Composable
internal fun VcfImportDestinationDialog(
    viewModel: DialerViewModel,
    rawVcf: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.UploadFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(stringResource(R.string.vcf_import_dest_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.vcf_import_dest_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    onClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                        onDismiss()
                        viewModel.importContactsVcfDetailed(
                            rawData = rawVcf,
                            saveToPrivateVault = true
                        ) { res ->
                            Toast.makeText(
                                context,
                                if (res.importedCount > 0) {
                                    context.getString(R.string.vcf_imported_summary, res.importedCount)
                                } else {
                                    context.getString(R.string.file_read_failed)
                                },
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth().testTag("vcf_import_dest_private_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.vcf_import_dest_private),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.vcf_import_dest_private_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                        onDismiss()
                        val defName = viewModel.defaultContactAccountName.value.takeUnless { it == PRIVATE_VAULT_ACCOUNT_NAME } ?: ""
                        val defType = viewModel.defaultContactAccountType.value.takeUnless { it == PRIVATE_VAULT_ACCOUNT_TYPE } ?: ""
                        viewModel.importContactsVcfDetailed(
                            rawData = rawVcf,
                            saveToPrivateVault = false,
                            accountName = defName,
                            accountType = defType
                        ) { res ->
                            Toast.makeText(
                                context,
                                if (res.importedCount > 0) {
                                    context.getString(R.string.vcf_imported_summary, res.importedCount)
                                } else {
                                    context.getString(R.string.file_read_failed)
                                },
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().testTag("vcf_import_dest_system_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.vcf_import_dest_system),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.vcf_import_dest_system_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}