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

import com.example.model.AppSetting
import com.example.model.BlockedNumber
import com.example.model.CallNote
import com.example.model.CallRecording
import com.example.model.QuickResponse
import com.example.model.SpeedDial
import kotlinx.coroutines.flow.Flow

fun DialerRepository.getBlockedNumbers(): Flow<List<BlockedNumber>> = dao.getBlockedNumbersFlow()
suspend fun DialerRepository.addBlockedNumber(number: String) {
    com.example.util.BlockedNumberContractManager.blockNumber(context, number, dao)
}
suspend fun DialerRepository.removeBlockedNumber(number: String) {
    com.example.util.BlockedNumberContractManager.unblockNumber(context, number, dao)
}
suspend fun DialerRepository.isBlocked(number: String): Boolean {
    return com.example.util.BlockedNumberContractManager.isBlocked(context, number, dao)
}

suspend fun DialerRepository.exportBackup(password: String = ""): String = com.example.data.BackupRestoreManager.exportBackup(context, password)
suspend fun DialerRepository.importBackup(rawData: String, password: String = ""): Boolean = com.example.data.BackupRestoreManager.importBackup(context, rawData, password)

fun DialerRepository.getSpeedDial(): Flow<List<SpeedDial>> = dao.getSpeedDialFlow()
suspend fun DialerRepository.saveSpeedDial(key: Int, number: String, name: String) = dao.insertSpeedDial(SpeedDial(key, number, name))
suspend fun DialerRepository.deleteSpeedDial(key: Int) = dao.deleteSpeedDial(key)

fun DialerRepository.getQuickResponses(): Flow<List<QuickResponse>> = dao.getQuickResponsesFlow()
suspend fun DialerRepository.addQuickResponse(message: String) = dao.insertQuickResponse(QuickResponse(message = message))
suspend fun DialerRepository.deleteQuickResponse(response: QuickResponse) = dao.deleteQuickResponse(response)

suspend fun DialerRepository.getVoicemailNumber(): String = dao.getSetting("voicemail_number") ?: ""
suspend fun DialerRepository.saveVoicemailNumber(number: String) = dao.insertSetting(AppSetting("voicemail_number", number))

suspend fun DialerRepository.getPreferredSim(): String = dao.getSetting("preferred_sim") ?: "Ask"
suspend fun DialerRepository.savePreferredSim(sim: String) = dao.insertSetting(AppSetting("preferred_sim", sim))

suspend fun DialerRepository.getCallNote(number: String): CallNote? = dao.getLatestCallNote(number)
fun DialerRepository.getAllCallNotes(): Flow<List<CallNote>> = dao.getAllCallNotesFlow()
suspend fun DialerRepository.saveCallNote(callNote: CallNote) = dao.insertCallNote(callNote)
suspend fun DialerRepository.saveCallNote(number: String, note: String) = dao.insertCallNote(CallNote(number = number, note = note, lastUpdated = System.currentTimeMillis()))
suspend fun DialerRepository.deleteCallNote(number: String) = dao.deleteCallNotesForNumber(number)
suspend fun DialerRepository.deleteCallNoteById(id: Long) = dao.deleteCallNoteById(id)

fun DialerRepository.getAllCallRecordings(): Flow<List<CallRecording>> = dao.getAllCallRecordingsFlow()
suspend fun DialerRepository.saveCallRecording(recording: CallRecording) = dao.insertCallRecording(recording)
suspend fun DialerRepository.deleteCallRecording(id: Int) = dao.deleteCallRecording(id)

// Spam Number Delegates
fun DialerRepository.getAllSpamNumbers(): Flow<List<com.example.model.SpamNumber>> = dao.getAllSpamNumbersFlow()
suspend fun DialerRepository.addSpamNumber(number: String, label: String = "Spam") = dao.insertSpamNumber(com.example.model.SpamNumber(number, label))
suspend fun DialerRepository.deleteSpamNumber(spam: com.example.model.SpamNumber) = dao.deleteSpamNumber(spam)
suspend fun DialerRepository.clearAllSpam() = dao.clearAllSpam()
suspend fun DialerRepository.isSpamNumber(number: String): Boolean = dao.isSpamNumber(number)
suspend fun DialerRepository.importSpamNumbersFromCsv(csvContent: String): Int {
    val lines = csvContent.lines()
    val listToInsert = mutableListOf<com.example.model.SpamNumber>()
    for (line in lines) {
        if (line.isBlank()) continue
        val parts = line.split(",")
        val number = parts.getOrNull(0)?.trim() ?: continue
        if (number.isEmpty()) continue
        val label = parts.getOrNull(1)?.trim() ?: "Spam"
        listToInsert.add(com.example.model.SpamNumber(number, label))
    }
    if (listToInsert.isNotEmpty()) {
        dao.insertSpamNumbers(listToInsert)
    }
    return listToInsert.size
}

suspend fun DialerRepository.exportSpamNumbersToCsv(): String {
    val list = dao.getAllSpamNumbersList()
    val sb = StringBuilder()
    sb.append("# Dialer Offline Spam Database Export\n")
    sb.append("# Format: Number,Label\n")
    for (item in list) {
        sb.append("${item.number},${item.label}\n")
    }
    return sb.toString()
}

// Call Reminder Delegates
fun DialerRepository.getAllReminders(): Flow<List<com.example.model.CallReminder>> = dao.getAllRemindersFlow()
suspend fun DialerRepository.saveReminder(reminder: com.example.model.CallReminder): Long = dao.insertReminder(reminder)
suspend fun DialerRepository.updateReminder(reminder: com.example.model.CallReminder) = dao.updateReminder(reminder)
suspend fun DialerRepository.deleteReminder(reminder: com.example.model.CallReminder) = dao.deleteReminder(reminder)
suspend fun DialerRepository.deleteReminderById(id: Int) = dao.deleteReminderById(id)
