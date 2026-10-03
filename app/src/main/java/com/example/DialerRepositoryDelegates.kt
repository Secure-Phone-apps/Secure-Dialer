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

package com.example

import com.example.data.BackupRestoreManager
import com.example.model.*
import com.example.util.BlockedNumberContractManager
import kotlinx.coroutines.flow.Flow
import java.util.Locale

// Blocked Numbers
fun DialerRepository.getBlockedNumbers(): Flow<List<BlockedNumber>> = dao.getBlockedNumbersFlow()
suspend fun DialerRepository.addBlockedNumber(number: String) {
    BlockedNumberContractManager.blockNumber(context, number, dao)
}
suspend fun DialerRepository.removeBlockedNumber(number: String) {
    BlockedNumberContractManager.unblockNumber(context, number, dao)
}
suspend fun DialerRepository.isBlocked(number: String): Boolean {
    return BlockedNumberContractManager.isBlocked(context, number, dao)
}

// Backup & Restore
suspend fun DialerRepository.exportBackup(password: String = ""): String =
    BackupRestoreManager.exportBackup(context, password)

suspend fun DialerRepository.importBackup(rawData: String, password: String = ""): Boolean =
    BackupRestoreManager.importBackup(context, rawData, password)

// Speed Dial
fun DialerRepository.getSpeedDial(): Flow<List<SpeedDial>> = dao.getSpeedDialFlow()
suspend fun DialerRepository.saveSpeedDial(key: Int, number: String, name: String) =
    dao.insertSpeedDial(SpeedDial(key, number, name))
suspend fun DialerRepository.deleteSpeedDial(key: Int) = dao.deleteSpeedDial(key)

// Quick Responses
fun DialerRepository.getQuickResponses(): Flow<List<QuickResponse>> = dao.getQuickResponsesFlow()
suspend fun DialerRepository.addQuickResponse(message: String) =
    dao.insertQuickResponse(QuickResponse(message = message))
suspend fun DialerRepository.deleteQuickResponse(response: QuickResponse) =
    dao.deleteQuickResponse(response)

// Telephony Settings
suspend fun DialerRepository.getVoicemailNumber(): String = dao.getSetting("voicemail_number") ?: ""
suspend fun DialerRepository.saveVoicemailNumber(number: String) =
    dao.insertSetting(AppSetting("voicemail_number", number))

suspend fun DialerRepository.getPreferredSim(): String = dao.getSetting("preferred_sim") ?: "Ask"
suspend fun DialerRepository.savePreferredSim(sim: String) =
    dao.insertSetting(AppSetting("preferred_sim", sim))

// Call Notes
suspend fun DialerRepository.getCallNote(number: String): CallNote? = dao.getLatestCallNote(number)
fun DialerRepository.getAllCallNotes(): Flow<List<CallNote>> = dao.getAllCallNotesFlow()
suspend fun DialerRepository.saveCallNote(callNote: CallNote) = dao.insertCallNote(callNote)
suspend fun DialerRepository.saveCallNote(number: String, note: String) =
    dao.insertCallNote(CallNote(number = number, note = note, lastUpdated = System.currentTimeMillis()))
suspend fun DialerRepository.deleteCallNote(number: String) = dao.deleteCallNotesForNumber(number)
suspend fun DialerRepository.deleteCallNoteById(id: Long) = dao.deleteCallNoteById(id)

// Call Recordings
fun DialerRepository.getAllCallRecordings(): Flow<List<CallRecording>> = dao.getAllCallRecordingsFlow()
suspend fun DialerRepository.saveCallRecording(recording: CallRecording): Long {
    val existing = dao.getCallRecordingByPath(recording.filePath)
    if (existing != null) return existing.id.toLong()
    return dao.insertCallRecording(recording)
}
suspend fun DialerRepository.deleteCallRecording(id: Int) = dao.deleteCallRecording(id)

// Spam Management
fun DialerRepository.getAllSpamNumbers(): Flow<List<SpamNumber>> = dao.getAllSpamNumbersFlow()
suspend fun DialerRepository.addSpamNumber(number: String, label: String = "Spam") =
    dao.insertSpamNumber(SpamNumber(number.trim(), label.trim().ifBlank { "Spam" }))
suspend fun DialerRepository.deleteSpamNumber(spam: SpamNumber) = dao.deleteSpamNumber(spam)
suspend fun DialerRepository.clearAllSpam() = dao.clearAllSpam()
suspend fun DialerRepository.isSpamNumber(number: String): Boolean = dao.isSpamNumber(number)

suspend fun DialerRepository.importSpamNumbersFromCsv(csvContent: String): Int {
    val lines = csvContent.lines()
    val listToInsert = mutableListOf<SpamNumber>()

    for (rawLine in lines) {
        val line = rawLine.trim()
        // FIXED: Ignore comments, blank lines, and standard CSV column headers
        if (line.isBlank() || line.startsWith("#") || line.startsWith("//")) continue
        if (line.lowercase(Locale.ROOT).startsWith("number,") || line.lowercase(Locale.ROOT) == "number") continue

        val parts = line.split(",")
        val number = parts.getOrNull(0)?.trim()?.replace("\"", "") ?: continue
        if (number.isEmpty() || number.length < 2) continue

        val label = parts.getOrNull(1)?.trim()?.replace("\"", "")?.ifBlank { "Spam" } ?: "Spam"
        listToInsert.add(SpamNumber(number, label))
    }

    // FIXED: Chunk inserts into 1,000-item slices to prevent SQLite bind variable limit crashes & OOM
    if (listToInsert.isNotEmpty()) {
        listToInsert.chunked(1000).forEach { chunk ->
            dao.insertSpamNumbers(chunk)
        }
    }
    return listToInsert.size
}

suspend fun DialerRepository.exportSpamNumbersToCsv(): String {
    val list = dao.getAllSpamNumbersList()
    return buildString {
        append("# Dialer Offline Spam Database Export\n")
        append("# Format: Number,Label\n")
        for (item in list) {
            append("${item.number},${item.label}\n")
        }
    }
}

// Call Reminders
fun DialerRepository.getAllReminders(): Flow<List<CallReminder>> = dao.getAllRemindersFlow()
suspend fun DialerRepository.saveReminder(reminder: CallReminder): Long = dao.insertReminder(reminder)
suspend fun DialerRepository.updateReminder(reminder: CallReminder) = dao.updateReminder(reminder)
suspend fun DialerRepository.deleteReminder(reminder: CallReminder) = dao.deleteReminder(reminder)
suspend fun DialerRepository.deleteReminderById(id: Int) = dao.deleteReminderById(id)