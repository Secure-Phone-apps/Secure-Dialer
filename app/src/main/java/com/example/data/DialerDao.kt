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

    // ================= CONTACTS (SYSTEM CACHE + PRIVATE VAULT) =================

    @Query("""
        SELECT * FROM (
            SELECT id, rawContactId, contactId, number, name, label, favorite, avatarText, avatarBgValue, avatarTextColorValue, t9Mapping, email, photoUri, accountName, accountType, numbers, emails, addresses, note
            FROM contacts
            UNION ALL
            SELECT -id AS id, 0 AS rawContactId, 0 AS contactId, number, name, label, favorite, avatarText, avatarBgValue, avatarTextColorValue, t9Mapping, email, photoUri, accountName, accountType, numbers, emails, addresses, note
            FROM private_contacts
        )
        WHERE (:accountName = '' 
           OR (:accountName = 'Phone' AND ((accountName = '' OR accountName IS NULL OR accountName = 'Phone' COLLATE NOCASE OR accountType LIKE '%local%' COLLATE NOCASE) AND accountType != 'com.securedialer.privatevault')) 
           OR accountName = :accountName COLLATE NOCASE) 
        ORDER BY 
            CASE WHEN :isLastNameFirst = 1 AND instr(name, ' ') > 0 
                 THEN substr(name, instr(name, ' ') + 1) 
                 ELSE name 
            END COLLATE NOCASE ASC
    """)
    fun getContactsPaged(accountName: String = "", isLastNameFirst: Boolean = false): PagingSource<Int, Contact>

    @Query("""
        SELECT * FROM (
            SELECT id, rawContactId, contactId, number, name, label, favorite, avatarText, avatarBgValue, avatarTextColorValue, t9Mapping, email, photoUri, accountName, accountType, numbers, emails, addresses, note
            FROM contacts
            UNION ALL
            SELECT -id AS id, 0 AS rawContactId, 0 AS contactId, number, name, label, favorite, avatarText, avatarBgValue, avatarTextColorValue, t9Mapping, email, photoUri, accountName, accountType, numbers, emails, addresses, note
            FROM private_contacts
        )
        ORDER BY 
            CASE WHEN :isLastNameFirst = 1 AND instr(name, ' ') > 0 
                 THEN substr(name, instr(name, ' ') + 1) 
                 ELSE name 
            END COLLATE NOCASE ASC
    """)
    fun getAllContactsFlow(isLastNameFirst: Boolean = false): Flow<List<Contact>>

    @Query("""
        SELECT id, rawContactId, contactId, number, name, label, favorite, avatarText, avatarBgValue, avatarTextColorValue, t9Mapping, email, photoUri, accountName, accountType, numbers, emails, addresses, note
        FROM contacts
        UNION ALL
        SELECT -id AS id, 0 AS rawContactId, 0 AS contactId, number, name, label, favorite, avatarText, avatarBgValue, avatarTextColorValue, t9Mapping, email, photoUri, accountName, accountType, numbers, emails, addresses, note
        FROM private_contacts
    """)
    suspend fun getAllContactsList(): List<Contact>

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun getContactsCount(): Int

    @Query("""
        SELECT * FROM (
            SELECT id, rawContactId, contactId, number, name, label, favorite, avatarText, avatarBgValue, avatarTextColorValue, t9Mapping, email, photoUri, accountName, accountType, numbers, emails, addresses, note
            FROM contacts
            UNION ALL
            SELECT -id AS id, 0 AS rawContactId, 0 AS contactId, number, name, label, favorite, avatarText, avatarBgValue, avatarTextColorValue, t9Mapping, email, photoUri, accountName, accountType, numbers, emails, addresses, note
            FROM private_contacts
        )
        WHERE favorite = 1 
        ORDER BY 
            CASE WHEN :isLastNameFirst = 1 AND instr(name, ' ') > 0 
                 THEN substr(name, instr(name, ' ') + 1) 
                 ELSE name 
            END COLLATE NOCASE ASC
    """)
    fun getFavoriteContacts(isLastNameFirst: Boolean = false): Flow<List<Contact>>

    @Query("""
        SELECT * FROM (
            SELECT id, rawContactId, contactId, number, name, label, favorite, avatarText, avatarBgValue, avatarTextColorValue, t9Mapping, email, photoUri, accountName, accountType, numbers, emails, addresses, note
            FROM contacts
            UNION ALL
            SELECT -id AS id, 0 AS rawContactId, 0 AS contactId, number, name, label, favorite, avatarText, avatarBgValue, avatarTextColorValue, t9Mapping, email, photoUri, accountName, accountType, numbers, emails, addresses, note
            FROM private_contacts
        )
        WHERE (:accountName = '' 
           OR (:accountName = 'Phone' AND ((accountName = '' OR accountName IS NULL OR accountName = 'Phone' COLLATE NOCASE OR accountType LIKE '%local%' COLLATE NOCASE) AND accountType != 'com.securedialer.privatevault')) 
           OR accountName = :accountName COLLATE NOCASE) 
          AND (name LIKE :query ESCAPE '\' OR number LIKE :query ESCAPE '\' OR t9Mapping LIKE :query ESCAPE '\' OR note LIKE :query ESCAPE '\') 
        ORDER BY 
            CASE WHEN :isLastNameFirst = 1 AND instr(name, ' ') > 0 
                 THEN substr(name, instr(name, ' ') + 1) 
                 ELSE name 
            END COLLATE NOCASE ASC
    """)
    fun searchContacts(query: String, accountName: String = "", isLastNameFirst: Boolean = false): PagingSource<Int, Contact>

    @Transaction
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

    @Query("DELETE FROM contacts WHERE id IN (:ids)")
    suspend fun deleteContactsByIds(ids: List<Long>)

    @Query("""
        SELECT * FROM (
            SELECT id, rawContactId, contactId, number, name, label, favorite, avatarText, avatarBgValue, avatarTextColorValue, t9Mapping, email, photoUri, accountName, accountType, numbers, emails, addresses, note
            FROM contacts
            UNION ALL
            SELECT -id AS id, 0 AS rawContactId, 0 AS contactId, number, name, label, favorite, avatarText, avatarBgValue, avatarTextColorValue, t9Mapping, email, photoUri, accountName, accountType, numbers, emails, addresses, note
            FROM private_contacts
        )
        WHERE number = :number 
           OR REPLACE(REPLACE(REPLACE(REPLACE(number, ' ', ''), '-', ''), '(', ''), ')', '') = REPLACE(REPLACE(REPLACE(REPLACE(:number, ' ', ''), '-', ''), '(', ''), ')', '')
           OR (length(REPLACE(REPLACE(REPLACE(REPLACE(:number, ' ', ''), '-', ''), '(', ''), ')', '')) >= 7 
               AND REPLACE(REPLACE(REPLACE(REPLACE(number, ' ', ''), '-', ''), '(', ''), ')', '') LIKE '%' || substr(REPLACE(REPLACE(REPLACE(REPLACE(:number, ' ', ''), '-', ''), '(', ''), ')', ''), -7)) 
        LIMIT 1
    """)
    suspend fun getContactByNumber(number: String): Contact?

    // ================= PRIVATE ENCRYPTED VAULT CONTACTS =================

    @Query("SELECT * FROM private_contacts ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllPrivateContactsList(): List<PrivateContact>

    @Query("SELECT COUNT(*) FROM private_contacts")
    suspend fun getPrivateContactsCount(): Int

    @Query("SELECT COUNT(*) FROM private_contacts")
    fun getPrivateContactsCountFlow(): Flow<Int>

    @Query("SELECT * FROM private_contacts WHERE id = :id LIMIT 1")
    suspend fun getPrivateContactById(id: Long): PrivateContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrivateContact(contact: PrivateContact): Long

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrivateContacts(contacts: List<PrivateContact>): List<Long>

    @Update
    suspend fun updatePrivateContact(contact: PrivateContact)

    @Query("DELETE FROM private_contacts WHERE id = :id")
    suspend fun deletePrivateContactById(id: Long)

    @Query("DELETE FROM private_contacts WHERE id IN (:ids)")
    suspend fun deletePrivateContactsByIds(ids: List<Long>)

    @Query("UPDATE private_contacts SET favorite = :isFavorite WHERE id = :id")
    suspend fun updatePrivateFavoriteById(id: Long, isFavorite: Boolean)

    @Query("""
        UPDATE private_contacts SET favorite = :isFavorite 
        WHERE number = :number 
           OR REPLACE(REPLACE(REPLACE(REPLACE(number, ' ', ''), '-', ''), '(', ''), ')', '') = REPLACE(REPLACE(REPLACE(REPLACE(:number, ' ', ''), '-', ''), '(', ''), ')', '')
           OR (length(REPLACE(REPLACE(REPLACE(REPLACE(:number, ' ', ''), '-', ''), '(', ''), ')', '')) >= 7 
               AND REPLACE(REPLACE(REPLACE(REPLACE(number, ' ', ''), '-', ''), '(', ''), ')', '') LIKE '%' || substr(REPLACE(REPLACE(REPLACE(REPLACE(:number, ' ', ''), '-', ''), '(', ''), ')', ''), -7))
    """)
    suspend fun updatePrivateFavoriteByNumber(number: String, isFavorite: Boolean)

    // ================= CALL HISTORY =================

    @Query("SELECT * FROM call_history ORDER BY id DESC")
    fun getCallHistoryPaged(): PagingSource<Int, CallRecord>

    @Query("SELECT * FROM call_history ORDER BY timestampMs DESC, id DESC LIMIT 5000")
    fun getAllCallHistoryFlow(): Flow<List<CallRecord>>

    @Query("SELECT COUNT(*) FROM call_history")
    suspend fun getCallLogCount(): Int

    @Query("SELECT MAX(id) FROM call_history")
    suspend fun getMaxCallLogId(): Int?

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLogs(logs: List<CallRecord>)

    @Query("DELETE FROM call_history")
    suspend fun clearCallLogs()

    @Query("DELETE FROM call_history WHERE id = :id")
    suspend fun deleteCallLog(id: Int)

    @Query("SELECT * FROM call_history WHERE number = :number ORDER BY id DESC")
    suspend fun getCallHistoryByNumber(number: String): List<CallRecord>

    // ================= BLOCKED NUMBERS =================

    @Query("SELECT * FROM blocked_numbers")
    fun getBlockedNumbersFlow(): Flow<List<BlockedNumber>>

    @Query("SELECT * FROM blocked_numbers")
    suspend fun getBlockedNumbersList(): List<BlockedNumber>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedNumber(blockedNumber: BlockedNumber)

    @Delete
    suspend fun deleteBlockedNumber(blockedNumber: BlockedNumber)

    @Query(
        "SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE number = :number " +
            "OR (:number LIKE REPLACE(REPLACE(REPLACE(REPLACE(number, '\\', '\\\\'), '%', '\\%'), '_', '\\_'), '*', '%') ESCAPE '\\'))"
    )
    suspend fun isBlockedSql(number: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE number = :number)")
    suspend fun isBlocked(number: String): Boolean

    // ================= SPEED DIAL =================

    @Query("SELECT * FROM speed_dial")
    fun getSpeedDialFlow(): Flow<List<SpeedDial>>

    @Query("SELECT * FROM speed_dial")
    suspend fun getSpeedDialList(): List<SpeedDial>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeedDial(speedDial: SpeedDial)

    @Query("DELETE FROM speed_dial WHERE `key` = :key")
    suspend fun deleteSpeedDial(key: Int)

    // ================= QUICK RESPONSES =================

    @Query("SELECT * FROM quick_responses")
    fun getQuickResponsesFlow(): Flow<List<QuickResponse>>

    @Query("SELECT * FROM quick_responses")
    suspend fun getQuickResponsesList(): List<QuickResponse>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickResponse(response: QuickResponse)

    @Delete
    suspend fun deleteQuickResponse(response: QuickResponse)

    // ================= APP SETTINGS =================

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun getSetting(key: String): String?

    @Query("SELECT * FROM app_settings")
    suspend fun getAllSettingsList(): List<AppSetting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)

    @Query("""
        DELETE FROM app_settings
        WHERE `key` LIKE 'cnap\_%' ESCAPE '\'
          AND rowid NOT IN (
              SELECT rowid FROM app_settings
              WHERE `key` LIKE 'cnap\_%' ESCAPE '\'
              ORDER BY rowid DESC
              LIMIT :maxEntries
          )
    """)
    suspend fun pruneOldCnapSettings(maxEntries: Int = MAX_CACHED_CNAP_ENTRIES)

    @Transaction
    suspend fun upsertCnapSettingCapped(
        key: String,
        value: String,
        maxEntries: Int = MAX_CACHED_CNAP_ENTRIES
    ) {
        insertSetting(AppSetting(key, value))
        pruneOldCnapSettings(maxEntries.coerceAtLeast(1))
    }

    companion object {
        const val MAX_CACHED_CNAP_ENTRIES = 200
    }

    // ================= CALL NOTES =================

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

    // ================= CALL RECORDINGS =================

    @Query("SELECT * FROM call_recordings ORDER BY id DESC")
    fun getAllCallRecordingsFlow(): Flow<List<CallRecording>>

    @Query("SELECT * FROM call_recordings ORDER BY id DESC")
    suspend fun getAllCallRecordingsList(): List<CallRecording>

    @Query("SELECT * FROM call_recordings WHERE filePath = :path LIMIT 1")
    suspend fun getCallRecordingByPath(path: String): CallRecording?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallRecording(recording: CallRecording): Long

    @Query("UPDATE call_recordings SET note = :note WHERE id = :id")
    suspend fun updateCallRecordingNote(id: Int, note: String)

    @Query("DELETE FROM call_recordings WHERE id = :id")
    suspend fun deleteCallRecording(id: Int)

    // ================= SPAM NUMBERS =================

    @Query("SELECT * FROM spam_numbers ORDER BY number ASC")
    fun getAllSpamNumbersFlow(): Flow<List<SpamNumber>>

    @Query("SELECT * FROM spam_numbers ORDER BY number ASC")
    suspend fun getAllSpamNumbersList(): List<SpamNumber>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpamNumber(spam: SpamNumber)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpamNumbers(spam: List<SpamNumber>)

    @Delete
    suspend fun deleteSpamNumber(spam: SpamNumber)

    @Query("DELETE FROM spam_numbers")
    suspend fun clearAllSpam()

    @Query("SELECT EXISTS(SELECT 1 FROM spam_numbers WHERE number = :number)")
    suspend fun isSpamNumber(number: String): Boolean

    // ================= CALL REMINDERS =================

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