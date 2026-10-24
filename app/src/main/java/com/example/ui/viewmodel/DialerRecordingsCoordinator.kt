/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import com.example.*
import com.example.model.CallRecording
import com.example.util.CallAudioRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class DialerRecordingsCoordinator(
    private val repository: DialerRepository,
    private val prefs: SharedPreferences,
    private val scope: CoroutineScope,
    private val onRecordingCreated: (CallRecording) -> Unit
) {

    fun toggleCallRecording(
        context: Context,
        phoneNumber: String,
        callerName: String,
        isAutoExportEnabled: Boolean,
        isCallNotesEnabled: Boolean
    ) {
        if (CallAudioRecorder.isRecording.value) {
            val result = CallAudioRecorder.stopRecording()
            val file = result.file
            if (file != null && file.exists() && file.length() > 0L) {
                val durationSec = result.durationSeconds.coerceAtLeast(1L)
                val sdf = SimpleDateFormat("MMM d, HH:mm", com.example.ui.components.getCurrentLocale(context))
                val timestamp = sdf.format(Date())
                val recording = CallRecording(
                    number = phoneNumber,
                    name = if (callerName.isBlank()) phoneNumber else callerName,
                    timestamp = timestamp,
                    duration = durationSec,
                    filePath = file.absolutePath
                )
                saveCallRecording(recording, isAutoExportEnabled, isCallNotesEnabled)
            }
        } else {
            CallAudioRecorder.startRecording(context, phoneNumber)
        }
    }

    fun saveCallRecording(
        recording: CallRecording,
        isAutoExportEnabled: Boolean,
        isCallNotesEnabled: Boolean
    ) {
        scope.launch {
            val insertedId = repository.saveCallRecording(recording)
            if (isAutoExportEnabled) {
                withContext(Dispatchers.IO) {
                    val f = File(recording.filePath)
                    if (f.exists() && f.length() > 128L) {
                        CallAudioRecorder.exportRecordingToPublicDownloads(repository.context, f)
                    }
                }
            }
            if (isCallNotesEnabled) {
                onRecordingCreated(recording.copy(id = insertedId.toInt()))
            }
        }
    }

    fun deleteCallRecording(id: Int) {
        scope.launch { repository.deleteCallRecording(id) }
    }

    fun updateCallRecordingNote(id: Int, note: String) {
        scope.launch { repository.updateCallRecordingNote(id, note) }
    }

    fun syncRecordingsFromDisk(context: Context, onComplete: ((Int) -> Unit)? = null) {
        scope.launch(Dispatchers.IO) {
            try {
                val existing = repository.getAllCallRecordings().first()
                val existingPaths = existing.map { it.filePath }.toSet()
                val existingNames = existing.map { File(it.filePath).name }.toSet()
                val recovered = CallAudioRecorder.recoverRecordingsFromDisk(context)
                var newCount = 0
                for (rec in recovered) {
                    val f = File(rec.filePath)
                    if (f.exists() && f.length() > 128L && !existingPaths.contains(rec.filePath) && !existingNames.contains(f.name)) {
                        val contactName = getContactNameFromNumber(context, rec.number) ?: rec.name
                        repository.saveCallRecording(rec.copy(name = contactName))
                        newCount++
                    }
                }
                withContext(Dispatchers.Main) { onComplete?.invoke(newCount) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onComplete?.invoke(0) }
            }
        }
    }

    fun recoverRecordingsFromDisk(context: Context, onComplete: ((Int) -> Unit)? = null) {
        syncRecordingsFromDisk(context, onComplete)
    }

    fun exportRecordingToDownloads(context: Context, filePath: String): Boolean {
        val f = File(filePath)
        return CallAudioRecorder.exportRecordingToPublicDownloads(context, f)
    }

    fun exportAllRecordingsToDownloads(context: Context): Int =
        CallAudioRecorder.exportAllRecordingsToDownloads(context)

    fun cleanupCorruptOrEmptyRecordings(context: Context): Int {
        val count = CallAudioRecorder.cleanupCorruptOrEmptyFiles(context)
        syncRecordingsFromDisk(context)
        return count
    }
}
