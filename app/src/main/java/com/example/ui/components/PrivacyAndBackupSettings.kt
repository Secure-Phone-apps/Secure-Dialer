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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyAndBackupSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        Pair("Privacy & Security", Icons.Default.Lock),
        Pair("Contacts Source", Icons.Default.Contacts),
        Pair("Data & Backup", Icons.Default.Backup)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, (title, icon) ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = { Text(title, maxLines = 1) },
                    icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedSubTab) {
                0 -> PrivacyProtectionSection(viewModel = viewModel, cardBgColor = cardBgColor)
                1 -> ContactsManagementSection(viewModel = viewModel, cardBgColor = cardBgColor)
                2 -> DataAndBackupSection(viewModel = viewModel, cardBgColor = cardBgColor)
            }
        }
    }
}

@Composable
private fun PrivacyProtectionSection(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val isBiometricLockEnabled by viewModel.isBiometricLockEnabled
    val isPocketProtectionEnabled by viewModel.isPocketProtectionEnabled

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // [Header] PRIVACY PROTECTION
        item {
            PreferenceHeader(stringResource(R.string.header_privacy_protection))
        }

        // Biometric Lock Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowToggle(
                    title = stringResource(R.string.settings_biometric_lock),
                    subtitle = stringResource(R.string.settings_biometric_lock_sub),
                    checked = isBiometricLockEnabled,
                    onCheckedChange = { viewModel.updateBiometricLockEnabled(it) },
                    icon = Icons.Default.Lock,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Pocket Protection Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowToggle(
                    title = stringResource(R.string.settings_pocket_protection),
                    subtitle = stringResource(R.string.settings_pocket_protection_sub),
                    checked = isPocketProtectionEnabled,
                    onCheckedChange = { viewModel.updatePocketProtectionEnabled(it) },
                    icon = Icons.Default.ScreenLockPortrait,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Fake Call Simulator
        item {
            Spacer(modifier = Modifier.height(16.dp))
            PreferenceHeader("FAKE CALL SIMULATOR")
            FakeCallSettings(viewModel = viewModel, cardBgColor = cardBgColor)
        }
    }
}

@Composable
private fun ContactsManagementSection(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current
    var showContactsToDisplayDialog by remember { mutableStateOf(false) }
    var showDefaultAccountDialog by remember { mutableStateOf(false) }
    var isMergingDuplicates by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
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

        // Deduplication Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.settings_dedup),
                    subtitle = stringResource(R.string.settings_dedup_sub),
                    onClick = {
                        isMergingDuplicates = true
                        viewModel.refreshAvailableAccounts()
                        Toast.makeText(context, "Scanning local contacts database for duplicates...", Toast.LENGTH_SHORT).show()
                        isMergingDuplicates = false
                    },
                    icon = Icons.Default.MergeType,
                    iconBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }

    if (showContactsToDisplayDialog) {
        val availableAccounts = viewModel.availableAccounts
        val currentFilter = viewModel.selectedAccountFilter.value
        AlertDialog(
            onDismissRequest = { showContactsToDisplayDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_contacts_to_display),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.onAccountFilterChange("")
                                showContactsToDisplayDialog = false
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentFilter.isBlank(), onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.account_filter_all),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (currentFilter.isBlank()) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    availableAccounts.forEach { account ->
                        val isSelected = currentFilter == account.name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.onAccountFilterChange(account.name)
                                    showContactsToDisplayDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = account.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
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
        val availableAccounts = viewModel.availableAccounts
        val defaultAccName = viewModel.defaultContactAccountName.value
        AlertDialog(
            onDismissRequest = { showDefaultAccountDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_default_account_for_new),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.updateDefaultContactAccount("", "")
                                showDefaultAccountDialog = false
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = defaultAccName.isBlank(), onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Default / Same as Filter",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (defaultAccName.isBlank()) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    availableAccounts.filter { it.name.isNotBlank() }.forEach { account ->
                        val isSelected = defaultAccName == account.name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.updateDefaultContactAccount(account.name, account.type)
                                    showDefaultAccountDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = account.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
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
}

@Composable
private fun DataAndBackupSection(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current
    val timestamp = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date()) }

    var exportPassword by remember { mutableStateOf("") }
    var pendingBackupData by remember { mutableStateOf<String?>(null) }
    var showPasswordPromptDialog by remember { mutableStateOf(false) }
    var restorePasswordInput by remember { mutableStateOf("") }
    var pendingEncryptedRestoreContent by remember { mutableStateOf<String?>(null) }

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
                    if (content.contains("\"encrypted\":true")) {
                        pendingEncryptedRestoreContent = content
                        restorePasswordInput = ""
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
        item {
            PreferenceHeader(stringResource(R.string.header_data_backup))
        }

        // Encrypted Local Backup Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(R.string.backup_export_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.backup_export_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text(stringResource(R.string.backup_export_password_label)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.exportBackup(exportPassword) { data ->
                                    pendingBackupData = data
                                    val defaultName = "dialer_backup_$timestamp.json"
                                    saveBackupFileLauncher.launch(defaultName)
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_save_backup_file), fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                openBackupFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_restore_backup_file), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Contacts vCard (.vcf) Migration Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(R.string.vcf_migration_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.vcf_migration_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val defaultName = "contacts_$timestamp.vcf"
                                saveVcfLauncher.launch(defaultName)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_save_vcf_file), fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                openVcfLauncher.launch(arrayOf("text/x-vcard", "text/vcard", "text/plain", "*/*"))
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_choose_vcf_file), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
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
