package com.example.data

import androidx.paging.PagingSource
import androidx.room.*
import com.example.model.CallRecord
import com.example.model.Contact

@Dao
interface DialerDao {
    // Contacts
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getContactsPaged(): PagingSource<Int, Contact>

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
}
