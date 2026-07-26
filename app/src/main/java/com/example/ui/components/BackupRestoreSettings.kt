package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
    val serviceHealth by viewModel.serviceHealth

    var exportPassword by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    var importDataInput by remember { mutableStateOf("") }

    var exportedDataDialog by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshServiceHealth()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Watchdog & Service Health Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (serviceHealth?.isHealthy == true) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.errorContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (serviceHealth?.isHealthy == true) Icons.Default.Shield else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (serviceHealth?.isHealthy == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.service_health_header),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (serviceHealth?.isHealthy == true) stringResource(R.string.service_health_active) else stringResource(R.string.service_health_action_req),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (serviceHealth?.isHealthy == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.refreshServiceHealth() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Health")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (serviceHealth?.isRoleGranted == true) stringResource(R.string.service_health_role_granted) else stringResource(R.string.service_health_role_not_granted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
}
