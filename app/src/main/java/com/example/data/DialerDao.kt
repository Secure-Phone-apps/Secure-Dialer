package com.example.data

import androidx.paging.PagingSource
import androidx.room.*
import com.example.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DialerDao {
    // Contacts
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getContactsPaged(): PagingSource<Int, Contact>

    @Query("SELECT * FROM contacts WHERE favorite = 1 ORDER BY name ASC")
    fun getFavoriteContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE name LIKE :query OR number LIKE :query OR t9Mapping LIKE :query ORDER BY name ASC")
    fun searchContacts(query: String): PagingSource<Int, Contact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Query("DELETE FROM contacts")
    suspend fun clearContacts()

    @Update
    suspend fun updateContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("SELECT * FROM contacts WHERE number = :number LIMIT 1")
    suspend fun getContactByNumber(number: String): Contact?

    // Call History
    @Query("SELECT * FROM call_history ORDER BY id DESC")
    fun getCallHistoryPaged(): PagingSource<Int, CallRecord>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedNumber(blockedNumber: BlockedNumber)

    @Delete
    suspend fun deleteBlockedNumber(blockedNumber: BlockedNumber)

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE number = :number)")
    suspend fun isBlocked(number: String): Boolean

    // Speed Dial
    @Query("SELECT * FROM speed_dial")
    fun getSpeedDialFlow(): Flow<List<SpeedDial>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeedDial(speedDial: SpeedDial)

    @Query("DELETE FROM speed_dial WHERE `key` = :key")
    suspend fun deleteSpeedDial(key: Int)

    // Quick Responses
    @Query("SELECT * FROM quick_responses")
    fun getQuickResponsesFlow(): Flow<List<QuickResponse>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickResponse(response: QuickResponse)

    @Delete
    suspend fun deleteQuickResponse(response: QuickResponse)

    // App Settings
    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)
}
