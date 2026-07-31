package com.example.ui.components

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun BackupRestoreSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var exportPassword by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    var importDataInput by remember { mutableStateOf("") }

    var exportedDataDialog by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    var exportedVcfDialog by remember { mutableStateOf<String?>(null) }
    var showImportVcfDialog by remember { mutableStateOf(false) }
    var importVcfInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Encrypted Backup Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.backup_export_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
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

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.exportBackup(exportPassword) { data ->
                            exportedDataDialog = data
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.backup_export_btn), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // --- Encrypted Restore Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.backup_restore_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.backup_restore_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.backup_import_btn), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // --- vCard (VCF) Contacts Migration Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "vCard (VCF) Contacts Migration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Export your local contacts as a standard vCard (.vcf) file or import contacts from an existing vCard file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.exportContactsVcf { data ->
                                exportedVcfDialog = data
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export VCF", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { showImportVcfDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import VCF", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Exported Dialog
    if (exportedDataDialog != null) {
        val dataStr = exportedDataDialog ?: ""
        AlertDialog(
            onDismissRequest = { exportedDataDialog = null },
            title = { Text(stringResource(R.string.backup_exported_dialog_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.backup_exported_dialog_desc),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dataStr,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = MaterialTheme.shapes.small
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString(dataStr))
                    Toast.makeText(context, context.getString(R.string.backup_copied_toast), Toast.LENGTH_SHORT).show()
                    exportedDataDialog = null
                }) {
                    Text(stringResource(R.string.backup_copy_clipboard))
                }
            },
            dismissButton = {
                TextButton(onClick = { exportedDataDialog = null }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(stringResource(R.string.backup_import_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.backup_import_dialog_desc))
                    OutlinedTextField(
                        value = importDataInput,
                        onValueChange = { importDataInput = it },
                        label = { Text(stringResource(R.string.backup_string_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = MaterialTheme.shapes.small
                    )
                    OutlinedTextField(
                        value = importPassword,
                        onValueChange = { importPassword = it },
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
                    if (importDataInput.isNotBlank()) {
                        viewModel.importBackup(importDataInput.trim(), importPassword) { success ->
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.backup_restored_success_toast), Toast.LENGTH_LONG).show()
                                showImportDialog = false
                                importDataInput = ""
                                importPassword = ""
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
                TextButton(onClick = { showImportDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Exported VCF Dialog
    if (exportedVcfDialog != null) {
        val dataStr = exportedVcfDialog ?: ""
        AlertDialog(
            onDismissRequest = { exportedVcfDialog = null },
            title = { Text("vCard (VCF) Contacts Exported") },
            text = {
                Column {
                    Text(
                        "Your contacts have been exported to standard vCard format. Copy this text and save it to a .vcf file to import into other devices.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dataStr,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = MaterialTheme.shapes.small
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString(dataStr))
                    Toast.makeText(context, "vCard content copied to clipboard!", Toast.LENGTH_SHORT).show()
                    exportedVcfDialog = null
                }) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportedVcfDialog = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Import VCF Dialog
    if (showImportVcfDialog) {
        AlertDialog(
            onDismissRequest = { showImportVcfDialog = false },
            title = { Text("Import Contacts from vCard") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste the content of a standard .vcf (vCard) file below to import the contacts into your system database.")
                    OutlinedTextField(
                        value = importVcfInput,
                        onValueChange = { importVcfInput = it },
                        label = { Text("vCard (VCF) Content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = MaterialTheme.shapes.small
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (importVcfInput.isNotBlank()) {
                        viewModel.importContactsVcf(importVcfInput.trim()) { success ->
                            if (success) {
                                Toast.makeText(context, "Contacts imported successfully!", Toast.LENGTH_LONG).show()
                                showImportVcfDialog = false
                                importVcfInput = ""
                            } else {
                                Toast.makeText(context, "Failed to import contacts. Please verify the vCard format.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }) {
                    Text("Import Contacts")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportVcfDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
