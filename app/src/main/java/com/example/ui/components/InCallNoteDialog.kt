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

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.util.RichHapticEngine

@Composable
fun InCallNoteDialog(
    initialNote: String = "",
    onDismiss: () -> Unit,
    onSaveNote: (String) -> Unit
) {
    val context = LocalContext.current
    var noteText by remember { mutableStateOf(initialNote) }
    var isHandled by remember { mutableStateOf(false) }

    val currentNoteText by rememberUpdatedState(noteText)
    val currentOnSaveNote by rememberUpdatedState(onSaveNote)

    fun performSaveAndDismiss() {
        if (!isHandled) {
            isHandled = true
            val trimmed = currentNoteText.trim()
            if (trimmed.isNotBlank()) {
                currentOnSaveNote(trimmed)
                Toast.makeText(context, context.getString(R.string.note_saved), Toast.LENGTH_SHORT).show()
            }
        }
        onDismiss()
    }

    fun performDiscardAndDismiss() {
        isHandled = true // Marks as explicitly discarded to prevent onDispose from saving
        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
        onDismiss()
    }

    // Auto-save draft on unexpected call disconnect or background dismissal (unless explicitly discarded)
    DisposableEffect(Unit) {
        onDispose {
            if (!isHandled) {
                val trimmed = currentNoteText.trim()
                if (trimmed.isNotBlank()) {
                    currentOnSaveNote(trimmed)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { performSaveAndDismiss() },
        title = { Text(stringResource(R.string.jot_call_note_title)) },
        text = {
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text(stringResource(R.string.note_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                maxLines = 5
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                    performSaveAndDismiss()
                }
            ) {
                Text(stringResource(R.string.btn_save_note))
            }
        },
        dismissButton = {
            TextButton(
                // FIXED: Discard actually discards the note without secretly saving it to database
                onClick = { performDiscardAndDismiss() }
            ) {
                Text(stringResource(R.string.btn_discard))
            }
        }
    )
}