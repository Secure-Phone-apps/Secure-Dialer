package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun InCallNoteDialog(
    onDismiss: () -> Unit,
    onSaveNote: (String) -> Unit
) {
    val context = LocalContext.current
    var noteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.jot_call_note_title)) },
        text = {
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text(stringResource(R.string.note_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (noteText.isNotBlank()) {
                        onSaveNote(noteText)
                        Toast.makeText(context, context.getString(R.string.note_saved), Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.btn_save_note))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_discard))
            }
        }
    )
}
