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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.DialerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val timestamp = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date()) }

    var exportPassword by remember { mutableStateOf("") }
    var pendingBackupData by remember { mutableStateOf<String?>(null) }
    var pendingEncryptedRestoreContent by remember { mutableStateOf<String?>(null) }
    var restorePasswordInput by remember { mutableStateOf("") }
    var showPasswordPromptDialog by remember { mutableStateOf(false) }

    // Text fallback dialog states
    var exportedDataDialog by remember { mutableStateOf<String?>(null) }
    var showManualTextImportDialog by remember { mutableStateOf(false) }
    var manualTextInput by remember { mutableStateOf("") }
    var manualTextPassword by remember { mutableStateOf("") }

    // --- File Pickers (SAF) ---
    val saveBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && pendingBackupData != null) {
            viewModel.writeTextToUri(uri, pendingBackupData!!) { success ->
                if (success) {
                    Toast.makeText(context, context.getString(R.string.file_saved_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.file_save_failed), Toast.LENGTH_SHORT).show()
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
                    val trimmed = content.trim()
                    if (trimmed.startsWith("{")) {
                        viewModel.importBackup(trimmed) { success ->
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.backup_restored_success_toast), Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, context.getString(R.string.backup_restore_failed_toast), Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        pendingEncryptedRestoreContent = trimmed
                        restorePasswordInput = ""
                        showPasswordPromptDialog = true
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.file_read_failed), Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, context.getString(R.string.file_saved_success), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.file_save_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val openVcfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.readTextFromUri(uri) { content ->
                if (!content.isNullOrBlank()) {
                    viewModel.importContactsVcf(content) { success ->
                        if (success) {
                            Toast.makeText(context, context.getString(R.string.vcf_import_count_success), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Failed to import contacts. Please verify .vcf format.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.file_read_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val saveBlocklistLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBlockedNumbers { blocklistData ->
                viewModel.writeTextToUri(uri, blocklistData) { success ->
                    if (success) {
                        Toast.makeText(context, context.getString(R.string.file_saved_success), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.file_save_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val openBlocklistLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.readTextFromUri(uri) { content ->
                if (!content.isNullOrBlank()) {
                    viewModel.importBlockedNumbers(content) { count ->
                        Toast.makeText(context, context.getString(R.string.blocklist_import_count_success, count), Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.file_read_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Full App Data & Settings Backup ---
        Card(
            modifier = Modifier.fillMaxWidth(),
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

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showManualTextImportDialog = true }
                    ) {
                        Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Paste Raw Text", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // --- Contacts vCard (.vcf) Migration Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
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

        // --- Blocked Numbers File Migration Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.blocklist_file_action_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.blocklist_file_action_desc),
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
                            val defaultName = "blocked_numbers_$timestamp.txt"
                            saveBlocklistLauncher.launch(defaultName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_export_blocklist), fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            openBlocklistLauncher.launch(arrayOf("text/plain", "text/csv", "application/json", "*/*"))
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_import_blocklist), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Encrypted Restore Password Prompt Dialog
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
                                restorePasswordInput = ""
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
                TextButton(onClick = {
                    showPasswordPromptDialog = false
                    pendingEncryptedRestoreContent = null
                }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Manual Text Import Dialog
    if (showManualTextImportDialog) {
        AlertDialog(
            onDismissRequest = { showManualTextImportDialog = false },
            title = { Text(stringResource(R.string.backup_import_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.backup_import_dialog_desc))
                    OutlinedTextField(
                        value = manualTextInput,
                        onValueChange = { manualTextInput = it },
                        label = { Text(stringResource(R.string.backup_string_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = MaterialTheme.shapes.small
                    )
                    OutlinedTextField(
                        value = manualTextPassword,
                        onValueChange = { manualTextPassword = it },
                        label = { Text(stringResource(R.string.backup_password_label)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (manualTextInput.isNotBlank()) {
                        viewModel.importBackup(manualTextInput.trim(), manualTextPassword) { success ->
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.backup_restored_success_toast), Toast.LENGTH_LONG).show()
                                showManualTextImportDialog = false
                                manualTextInput = ""
                                manualTextPassword = ""
                            } else {
                                Toast.makeText(context, context.getString(R.string.backup_restore_failed_toast), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }) {
                    Text(stringResource(R.string.backup_restore_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualTextImportDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}
