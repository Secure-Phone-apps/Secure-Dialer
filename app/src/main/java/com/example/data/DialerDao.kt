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

package com.example.data

import androidx.paging.PagingSource
import androidx.room.*
import com.example.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DialerDao {
    // Contacts
    @Query("SELECT * FROM contacts WHERE (:accountName = '' OR (:accountName = 'Phone' AND (accountName = '' OR accountName IS NULL OR LOWER(accountName) = 'phone' OR LOWER(accountType) LIKE '%local%')) OR accountName = :accountName) ORDER BY name ASC")
    fun getContactsPaged(accountName: String = ""): PagingSource<Int, Contact>

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContactsFlow(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts")
    suspend fun getAllContactsList(): List<Contact>

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun getContactsCount(): Int

    @Query("SELECT * FROM contacts WHERE favorite = 1 ORDER BY name ASC")
    fun getFavoriteContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE (:accountName = '' OR (:accountName = 'Phone' AND (accountName = '' OR accountName IS NULL OR LOWER(accountName) = 'phone' OR LOWER(accountType) LIKE '%local%')) OR accountName = :accountName) AND (name LIKE :query OR number LIKE :query OR t9Mapping LIKE :query) ORDER BY name ASC")
    fun searchContacts(query: String, accountName: String = ""): PagingSource<Int, Contact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Query("DELETE FROM contacts")
    suspend fun clearContacts()

    @Update
    suspend fun updateContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)

    @Query("SELECT * FROM contacts WHERE number = :number LIMIT 1")
    suspend fun getContactByNumber(number: String): Contact?

    // Call History
    @Query("SELECT * FROM call_history ORDER BY id DESC")
    fun getCallHistoryPaged(): PagingSource<Int, CallRecord>

    @Query("SELECT * FROM call_history ORDER BY id DESC LIMIT 1500")
    fun getAllCallHistoryFlow(): Flow<List<CallRecord>>

    @Query("SELECT COUNT(*) FROM call_history")
    suspend fun getCallLogCount(): Int

    @Query("SELECT MAX(id) FROM call_history")
    suspend fun getMaxCallLogId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLogs(logs: List<CallRecord>)

    @Query("DELETE FROM call_history")
    suspend fun clearCallLogs()

    @Query("DELETE FROM call_history WHERE id = :id")
    suspend fun deleteCallLog(id: Int)

    @Query("SELECT * FROM call_history WHERE number = :number ORDER BY id DESC")
    suspend fun getCallHistoryByNumber(number: String): List<CallRecord>

    // Blocked Numbers
    @Query("SELECT * FROM blocked_numbers")
    fun getBlockedNumbersFlow(): Flow<List<BlockedNumber>>

    @Query("SELECT * FROM blocked_numbers")
    suspend fun getBlockedNumbersList(): List<BlockedNumber>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedNumber(blockedNumber: BlockedNumber)

    @Delete
    suspend fun deleteBlockedNumber(blockedNumber: BlockedNumber)

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE number = :number OR (:number LIKE REPLACE(number, '*', '%')))")
    suspend fun isBlockedSql(number: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE number = :number)")
    suspend fun isBlocked(number: String): Boolean

    // Speed Dial
    @Query("SELECT * FROM speed_dial")
    fun getSpeedDialFlow(): Flow<List<SpeedDial>>

    @Query("SELECT * FROM speed_dial")
    suspend fun getSpeedDialList(): List<SpeedDial>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeedDial(speedDial: SpeedDial)

    @Query("DELETE FROM speed_dial WHERE `key` = :key")
    suspend fun deleteSpeedDial(key: Int)

    // Quick Responses
    @Query("SELECT * FROM quick_responses")
    fun getQuickResponsesFlow(): Flow<List<QuickResponse>>

    @Query("SELECT * FROM quick_responses")
    suspend fun getQuickResponsesList(): List<QuickResponse>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickResponse(response: QuickResponse)

    @Delete
    suspend fun deleteQuickResponse(response: QuickResponse)

    // App Settings
    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun getSetting(key: String): String?

    @Query("SELECT * FROM app_settings")
    suspend fun getAllSettingsList(): List<AppSetting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)

    // Call Notes
    @Query("SELECT * FROM call_notes WHERE number = :number ORDER BY id DESC")
    fun getCallNotesForNumberFlow(number: String): Flow<List<CallNote>>

    @Query("SELECT * FROM call_notes WHERE number = :number ORDER BY id DESC LIMIT 1")
    suspend fun getLatestCallNote(number: String): CallNote?

    @Query("SELECT * FROM call_notes ORDER BY id DESC")
    fun getAllCallNotesFlow(): Flow<List<CallNote>>

    @Query("SELECT * FROM call_notes ORDER BY id DESC")
    suspend fun getAllCallNotesList(): List<CallNote>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallNote(callNote: CallNote)

    @Query("DELETE FROM call_notes WHERE id = :id")
    suspend fun deleteCallNoteById(id: Long)

    @Query("DELETE FROM call_notes WHERE number = :number")
    suspend fun deleteCallNotesForNumber(number: String)

    // Call Recordings
    @Query("SELECT * FROM call_recordings ORDER BY id DESC")
    fun getAllCallRecordingsFlow(): Flow<List<CallRecording>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallRecording(recording: CallRecording)

    @Query("DELETE FROM call_recordings WHERE id = :id")
    suspend fun deleteCallRecording(id: Int)

    // Spam Numbers
    @Query("SELECT * FROM spam_numbers ORDER BY number ASC")
    fun getAllSpamNumbersFlow(): Flow<List<SpamNumber>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpamNumber(spam: SpamNumber)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpamNumbers(spam: List<SpamNumber>)

    @Delete
    suspend fun deleteSpamNumber(spam: SpamNumber)

    @Query("DELETE FROM spam_numbers")
    suspend fun clearAllSpam()

    @Query("SELECT EXISTS(SELECT 1 FROM spam_numbers WHERE number = :number)")
    suspend fun isSpamNumber(number: String): Boolean

    // Call Reminders
    @Query("SELECT * FROM call_reminders ORDER BY reminderTime ASC")
    fun getAllRemindersFlow(): Flow<List<CallReminder>>

    @Query("SELECT * FROM call_reminders ORDER BY reminderTime ASC")
    suspend fun getAllRemindersList(): List<CallReminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: CallReminder): Long

    @Update
    suspend fun updateReminder(reminder: CallReminder)

    @Delete
    suspend fun deleteReminder(reminder: CallReminder)

    @Query("DELETE FROM call_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Int)
}
