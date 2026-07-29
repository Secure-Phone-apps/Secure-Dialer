package com.example

import android.content.*
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.paging.*
import com.example.data.AppDatabase
import com.example.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class DialerRepository(rawContext: Context) {
    val context: Context = rawContext.applicationContext
    val db = AppDatabase.getDatabase(context)
    val dao = db.dialerDao()

    // --- Paging ---

    fun getContactsPaged(query: String): Flow<PagingData<Contact>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = {
                if (query.isEmpty()) dao.getContactsPaged()
                else dao.searchContacts("%$query%")
            }
        ).flow
    }

    fun getFavoriteContacts(): Flow<List<Contact>> {
        return dao.getFavoriteContacts()
    }

    fun getAllContactsFlow(): Flow<List<Contact>> {
        return dao.getAllContactsFlow()
    }

    fun getCallHistoryPaged(): Flow<PagingData<CallRecord>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = { dao.getCallHistoryPaged() }
        ).flow
    }

    fun getAllCallHistoryFlow(): Flow<List<CallRecord>> {
        return dao.getAllCallHistoryFlow()
    }

    // --- Sync Logic ---

    private var contentObserver: android.database.ContentObserver? = null
    private var lastSyncTimestamp = 0L

    fun startObservingChanges(onChanged: () -> Unit) {
        if (contentObserver != null) return
        try {
            val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    val now = System.currentTimeMillis()
                    if (now - lastSyncTimestamp > 1500) {
                        lastSyncTimestamp = now
                        onChanged()
                    }
                }
            }
            contentObserver = observer
            context.contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
            context.contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun stopObservingChanges() {
        contentObserver?.let { observer ->
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            contentObserver = null
        }
    }

    suspend fun syncContacts() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
            var systemContactsCount = 0
            var maxTimestamp = 0L
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP),
                null, null, null
            )?.use { cursor ->
                systemContactsCount = cursor.count
                val tsCol = cursor.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)
                if (tsCol != -1) {
                    while (cursor.moveToNext()) {
                        val ts = cursor.getLong(tsCol)
                        if (ts > maxTimestamp) {
                            maxTimestamp = ts
                        }
                    }
                }
            }
            
            val localCount = dao.getContactsCount()
            val lastSyncedCount = prefs.getInt("last_synced_contacts_count", -1)
            val lastSyncedTimestamp = prefs.getLong("last_synced_contacts_timestamp", -1L)
            
            if (localCount > 0 && systemContactsCount == lastSyncedCount && maxTimestamp == lastSyncedTimestamp) {
                // No changes in system contacts; skip heavy sync
                return@withContext
            }
            
            val systemContacts = fetchSystemContacts()
            dao.insertContacts(systemContacts)
            
            prefs.edit()
                .putInt("last_synced_contacts_count", systemContactsCount)
                .putLong("last_synced_contacts_timestamp", maxTimestamp)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val systemContacts = fetchSystemContacts()
                if (systemContacts.isNotEmpty()) {
                    dao.insertContacts(systemContacts)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    suspend fun syncCallLogs() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
            var systemCount = 0
            var systemMaxId = 0
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID),
                null, null, "${CallLog.Calls._ID} DESC"
            )?.use { cursor ->
                systemCount = cursor.count
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndex(CallLog.Calls._ID)
                    if (idCol != -1) {
                        systemMaxId = cursor.getInt(idCol)
                    }
                }
            }
            
            val localCount = dao.getCallLogCount()
            val expectedLocalCount = minOf(systemCount, 200)
            
            val lastSyncedMaxId = prefs.getInt("last_synced_call_log_max_id", -1)
            val lastSyncedCount = prefs.getInt("last_synced_call_log_count", -1)
            
            if (localCount > 0 && localCount == expectedLocalCount && systemMaxId == lastSyncedMaxId && systemCount == lastSyncedCount) {
                // No new logs; skip heavy sync
                return@withContext
            }
            
            val systemLogs = fetchSystemCallLogs()
            dao.clearCallLogs()
            dao.insertCallLogs(systemLogs)
            
            prefs.edit()
                .putInt("last_synced_call_log_max_id", systemMaxId)
                .putInt("last_synced_call_log_count", systemCount)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val systemLogs = fetchSystemCallLogs()
                dao.insertCallLogs(systemLogs)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    // --- Actions ---

    suspend fun addContact(name: String, number: String, label: String, email: String = "") {
        val ops = arrayListOf<ContentProviderOperation>()
        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null).withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build())
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE).withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name).build())
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE).withValue(Phone.NUMBER, number).withValue(Phone.TYPE, Phone.TYPE_MOBILE).build())
        if (email.isNotEmpty()) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                .build())
        }
        try { context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops) } catch (e: Exception) { e.printStackTrace() }
        syncContacts() // Update local cache
    }

    suspend fun deleteContact(number: String) {
        try {
            val contactId = getContactIdFromNumber(number)
            if (contactId != null) {
                context.contentResolver.delete(
                    ContactsContract.RawContacts.CONTENT_URI,
                    "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                    arrayOf(contactId)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        dao.getContactByNumber(number)?.let { dao.deleteContact(it) }
        syncContacts()
    }

    suspend fun deleteCallLog(id: Int) {
        dao.deleteCallLog(id)
        try {
            context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                "${CallLog.Calls._ID} = ?",
                arrayOf(id.toString())
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getCallHistoryByNumber(number: String): List<CallRecord> {
        return dao.getCallHistoryByNumber(number)
    }

    suspend fun toggleFavorite(number: String, isFavorite: Boolean) {
        try {
            val contactId = getContactIdFromNumber(number)
            if (contactId != null) {
                val values = ContentValues().apply { put(ContactsContract.Contacts.STARRED, if (isFavorite) 1 else 0) }
                context.contentResolver.update(
                    ContactsContract.Contacts.CONTENT_URI,
                    values,
                    "${ContactsContract.Contacts._ID} = ?",
                    arrayOf(contactId)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        dao.getContactByNumber(number)?.let { dao.updateContact(it.copy(favorite = isFavorite)) }
    }

    private fun getContactIdFromNumber(number: String): String? {
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
