package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun InCallQuickDeclineSheet(
    contactNumber: String,
    quickResponses: List<String>,
    onClose: () -> Unit,
    onQuickDecline: (String) -> Unit
) {
    val context = LocalContext.current

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.quick_responses),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickResponses, key = { it }) { resp ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val smsManager = context.getSystemService(android.telephony.SmsManager::class.java)
                                    smsManager.sendTextMessage(contactNumber, null, resp, null, null)
                                    Toast.makeText(context, context.getString(R.string.sms_sent), Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.sms_failed), Toast.LENGTH_SHORT).show()
                                }
                                onQuickDecline(resp)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            resp,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    }
}
