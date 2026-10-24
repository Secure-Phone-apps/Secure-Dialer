/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import com.example.*
import com.example.data.BackupRestoreManager
import com.example.model.CallNote
import com.example.model.CallReminder
import com.example.model.QuickResponse
import com.example.model.SpamNumber
import com.example.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DialerSpamAndBackupCoordinator(
    private val application: Application,
    private val repository: DialerRepository,
    private val scope: CoroutineScope,
    private val onDataChanged: () -> Unit
) {

    fun writeTextToUri(uri: Uri, content: String, onResult: (Boolean) -> Unit) {
        scope.launch {
            val success = BackupRestoreManager.writeTextToUri(application, uri, content)
            onResult(success)
        }
    }

    fun readTextFromUri(uri: Uri, onResult: (String?) -> Unit) {
        scope.launch {
            val content = BackupRestoreManager.readTextFromUri(application, uri)
            onResult(content)
        }
    }

    fun exportBlockedNumbers(onResult: (String) -> Unit) {
        scope.launch {
            val data = BackupRestoreManager.exportBlockedNumbers(application)
            onResult(data)
        }
    }

    fun importBlockedNumbers(rawData: String, onResult: (Int) -> Unit) {
        scope.launch {
            val count = BackupRestoreManager.importBlockedNumbers(application, rawData)
            if (count > 0) onDataChanged()
            onResult(count)
        }
    }

    fun exportBackup(password: String = "", onResult: (String) -> Unit) {
        scope.launch {
            val data = repository.exportBackup(password)
            onResult(data)
        }
    }

    fun importBackup(rawData: String, password: String = "", onResult: (Boolean) -> Unit) {
        scope.launch {
            val success = repository.importBackup(rawData, password)
            if (success) onDataChanged()
            onResult(success)
        }
    }

    fun exportContactsVcf(onResult: (String) -> Unit) {
        scope.launch {
            val data = BackupRestoreManager.exportContactsToVcf(application)
            onResult(data)
        }
    }

    fun importContactsVcf(rawData: String, onResult: (Boolean) -> Unit) {
        scope.launch {
            val success = BackupRestoreManager.importContactsFromVcf(application, rawData)
            if (success) onDataChanged()
            onResult(success)
        }
    }

    fun saveSpeedDial(key: Int, num: String, name: String) {
        scope.launch { repository.saveSpeedDial(key, num, name) }
    }

    fun deleteSpeedDial(key: Int) {
        scope.launch { repository.deleteSpeedDial(key) }
    }

    fun addQuickResponse(msg: String) {
        scope.launch { repository.addQuickResponse(msg) }
    }

    fun deleteQuickResponse(resp: QuickResponse) {
        scope.launch { repository.deleteQuickResponse(resp) }
    }

    fun addBlockedNumber(num: String) {
        scope.launch { repository.addBlockedNumber(num) }
    }

    fun removeBlockedNumber(num: String) {
        scope.launch { repository.removeBlockedNumber(num) }
    }

    fun saveCallNote(number: String, note: String) {
        if (note.isBlank()) return
        scope.launch {
            repository.saveCallNote(CallNote(number = number, note = note, lastUpdated = System.currentTimeMillis()))
        }
    }

    suspend fun getCallNote(number: String): CallNote? = repository.getCallNote(number)

    fun deleteCallNote(number: String) {
        scope.launch { repository.deleteCallNotesForNumber(number) }
    }

    fun deleteCallNoteById(id: Long) {
        scope.launch { repository.deleteCallNoteById(id) }
    }

    fun getCallNotesForNumberFlow(number: String): Flow<List<CallNote>> = repository.getCallNotesForNumberFlow(number)

    fun addSpamNumber(number: String, label: String = "Spam") {
        scope.launch { repository.addSpamNumber(number, label) }
    }

    fun deleteSpamNumber(spam: SpamNumber) {
        scope.launch { repository.deleteSpamNumber(spam) }
    }

    fun clearAllSpam() {
        scope.launch { repository.clearAllSpam() }
    }

    fun importSpamNumbersFromCsv(csvContent: String, onResult: (Int) -> Unit) {
        scope.launch {
            val count = repository.importSpamNumbersFromCsv(csvContent)
            onResult(count)
        }
    }

    fun exportSpamNumbersToCsv(onResult: (String) -> Unit) {
        scope.launch {
            val data = repository.exportSpamNumbersToCsv()
            onResult(data)
        }
    }

    fun addReminder(number: String, name: String, triggerTime: Long, note: String = "", onResult: (Long) -> Unit = {}) {
        scope.launch {
            val reminder = CallReminder(
                number = number,
                name = name,
                reminderTime = triggerTime,
                isCompleted = false,
                note = note
            )
            val id = repository.saveReminder(reminder)
            ReminderScheduler.schedule(application, id.toInt(), triggerTime)
            onResult(id)
        }
    }

    fun updateReminder(reminder: CallReminder) {
        scope.launch { repository.updateReminder(reminder) }
    }

    fun deleteReminder(reminder: CallReminder) {
        scope.launch {
            repository.deleteReminder(reminder)
            ReminderScheduler.cancel(application, reminder.id)
        }
    }

    fun deleteReminderById(id: Int) {
        scope.launch {
            repository.deleteReminderById(id)
            ReminderScheduler.cancel(application, id)
        }
    }
}
