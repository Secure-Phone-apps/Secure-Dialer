package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.Contact
import com.example.model.getInitials

@Composable
fun InCallAddCallDialog(
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onAddCall: (name: String, number: String) -> Unit
) {
    val context = LocalContext.current
    var addCallNumberInput by remember { mutableStateOf("") }
    var selectedAddCallContactName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (addCallNumberInput.isNotBlank()) {
                        val finalName = if (selectedAddCallContactName.isNotBlank()) {
                            selectedAddCallContactName
                        } else {
                            contacts.find { it.number == addCallNumberInput }?.name ?: addCallNumberInput
                        }
                        onAddCall(finalName, addCallNumberInput)
                        Toast.makeText(context, "📞 Merged call with $finalName", Toast.LENGTH_LONG).show()
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Please select or enter a valid number", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text(stringResource(R.string.btn_add_merge))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
        title = {
            Text(
                stringResource(R.string.add_call),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = addCallNumberInput,
                    onValueChange = {
                        addCallNumberInput = it
                        selectedAddCallContactName = ""
                    },
                    label = { Text(stringResource(R.string.add_call_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val filteredContacts = if (addCallNumberInput.isBlank()) {
                    contacts
                } else {
                    contacts.filter {
                        it.name.contains(addCallNumberInput, ignoreCase = true) ||
                                it.number.contains(addCallNumberInput)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredContacts,
                        key = { it.number },
                        contentType = { "add_call_contact" }
                    ) { contact ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                addCallNumberInput = contact.number
                                selectedAddCallContactName = contact.name
                            },
                            shape = MaterialTheme.shapes.small,
                            color = if (addCallNumberInput == contact.number) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = contact.avatarBg
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (contact.photoUri.isNotEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(contact.photoUri)
                                                    .size(256, 256)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Contact Photo",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = contact.avatarText.ifEmpty { getInitials(contact.name) },
                                                color = contact.avatarTextColor,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = contact.number,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        shape = MaterialTheme.shapes.large
    )
}
