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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DialerRepository(rawContext: Context) {
    val context: Context = rawContext.applicationContext
    val db = AppDatabase.getDatabase(context)
    val dao = db.dialerDao()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repositoryScope.launch {
            try {
                val initialContacts = dao.getAllContactsList()
                val initialSettings = dao.getAllSettingsList()
                ContactCache.init(initialContacts, initialSettings)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                dao.getAllContactsFlow().collect { contacts ->
                    val settings = dao.getAllSettingsList()
                    ContactCache.init(contacts, settings)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Paging ---

    fun getContactsPaged(query: String, accountName: String = ""): Flow<PagingData<Contact>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = {
                if (query.isEmpty()) dao.getContactsPaged(accountName)
                else dao.searchContacts("%$query%", accountName)
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

    suspend fun syncContacts(force: Boolean = false) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
            var systemContactsCount = 0
            var maxTimestamp = 0L
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP),
                null, null, "${ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP} DESC"
            )?.use { cursor ->
                systemContactsCount = cursor.count
                val tsCol = cursor.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)
                if (tsCol != -1 && cursor.moveToFirst()) {
                    maxTimestamp = cursor.getLong(tsCol)
                }
            }
            
            val localCount = dao.getContactsCount()
            val lastSyncedCount = prefs.getInt("last_synced_contacts_count", -1)
            val lastSyncedTimestamp = prefs.getLong("last_synced_contacts_timestamp", -1L)
            
            if (!force && localCount > 0 && systemContactsCount == lastSyncedCount && maxTimestamp == lastSyncedTimestamp) {
                // No changes in system contacts; skip heavy sync
                return@withContext
            }
            
            val systemContacts = fetchSystemContacts()
            
            // Atomically clear old local contacts cache and insert fresh system contacts snapshot
            dao.clearContacts()
            if (systemContacts.isNotEmpty()) {
                dao.insertContacts(systemContacts)
            }
            
            prefs.edit()
                .putInt("last_synced_contacts_count", systemContactsCount)
                .putLong("last_synced_contacts_timestamp", maxTimestamp)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val systemContacts = fetchSystemContacts()
                if (systemContacts.isNotEmpty()) {
                    dao.clearContacts()
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
            val expectedLocalCount = systemCount
            
            val lastSyncedMaxId = prefs.getInt("last_synced_call_log_max_id", -1)
            val lastSyncedCount = prefs.getInt("last_synced_call_log_count", -1)
            val currentLang = Locale.getDefault().language
            val lastSyncedLang = prefs.getString("last_synced_locale_lang", "")
            val localeChanged = currentLang != lastSyncedLang
            
            if (localCount > 0 && localCount == expectedLocalCount && systemMaxId == lastSyncedMaxId && systemCount == lastSyncedCount && !localeChanged) {
                // No new logs and no locale change; skip heavy sync
                return@withContext
            }
            
            val systemLogs = fetchSystemCallLogs()
            if (systemLogs.isNotEmpty()) {
                dao.clearCallLogs()
                dao.insertCallLogs(systemLogs)
            } else if (systemCount == 0 && lastSyncedCount > 0) {
                // System call logs were explicitly deleted
                dao.clearCallLogs()
            }
            
            prefs.edit()
                .putInt("last_synced_call_log_max_id", systemMaxId)
                .putInt("last_synced_call_log_count", systemCount)
                .putString("last_synced_locale_lang", currentLang)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val systemLogs = fetchSystemCallLogs()
                if (systemLogs.isNotEmpty()) {
                    dao.clearCallLogs()
                    dao.insertCallLogs(systemLogs)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    // --- Actions ---

    suspend fun addContact(
        name: String, 
        number: String, 
        label: String, 
        email: String = "",
        accountName: String = "",
        accountType: String = ""
    ) {
        val numbers = if (number.isNotBlank()) listOf(LabeledNumber(number, label.ifBlank { "Mobile" }, true)) else emptyList()
        val emails = if (email.isNotBlank()) listOf(LabeledEmail(email, "Home")) else emptyList()
        addContactWithDetails(
            name = name,
            numbers = numbers,
            emails = emails,
            addresses = emptyList(),
            accountName = accountName,
            accountType = accountType
        )
    }

    suspend fun addContactWithDetails(
        name: String,
        numbers: List<LabeledNumber>,
        emails: List<LabeledEmail> = emptyList(),
        addresses: List<LabeledAddress> = emptyList(),
        accountName: String = "",
        accountType: String = ""
    ) {
        val ops = arrayListOf<ContentProviderOperation>()
        val rawInsert = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
        if (accountName.isNotBlank() && accountType.isNotBlank() && accountName != "Phone") {
            rawInsert.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
            rawInsert.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
        } else {
            rawInsert.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            rawInsert.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
        }
        ops.add(rawInsert.build())
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
            .build())
        
        for (numItem in numbers) {
            if (numItem.number.isBlank()) continue
            val phoneType = when (numItem.label.lowercase()) {
                "work" -> Phone.TYPE_WORK
                "home" -> Phone.TYPE_HOME
                "other" -> Phone.TYPE_OTHER
                "main" -> Phone.TYPE_MAIN
                "mobile" -> Phone.TYPE_MOBILE
                else -> Phone.TYPE_CUSTOM
            }
            val builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, numItem.number)
                .withValue(Phone.TYPE, phoneType)
            if (phoneType == Phone.TYPE_CUSTOM) {
                builder.withValue(Phone.LABEL, numItem.label)
            }
            if (numItem.isPrimary) {
                builder.withValue(Phone.IS_PRIMARY, 1)
            }
            ops.add(builder.build())
        }

        for (emailItem in emails) {
            if (emailItem.email.isBlank()) continue
            val emailType = when (emailItem.label.lowercase()) {
                "work" -> ContactsContract.CommonDataKinds.Email.TYPE_WORK
                "other" -> ContactsContract.CommonDataKinds.Email.TYPE_OTHER
                else -> ContactsContract.CommonDataKinds.Email.TYPE_HOME
            }
            val builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, emailItem.email)
                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, emailType)
            ops.add(builder.build())
        }

        for (addrItem in addresses) {
            if (addrItem.address.isBlank()) continue
            val addrType = when (addrItem.label.lowercase()) {
                "work" -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK
                "other" -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER
                else -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME
            }
            val builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, addrItem.address)
                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, addrType)
            ops.add(builder.build())
        }

        try { context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops) } catch (e: Exception) { e.printStackTrace() }
        syncContacts(true) // Update local cache
    }

    suspend fun deleteContact(contact: Contact) {
        try {
            if (contact.rawContactId > 0) {
                context.contentResolver.delete(
                    ContactsContract.RawContacts.CONTENT_URI,
                    "${ContactsContract.RawContacts._ID} = ?",
                    arrayOf(contact.rawContactId.toString())
                )
            } else {
                val contactId = getContactIdFromNumber(contact.number)
                if (contactId != null) {
                    context.contentResolver.delete(
                        ContactsContract.RawContacts.CONTENT_URI,
                        "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                        arrayOf(contactId)
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        dao.deleteContact(contact)
        syncContacts()
    }

    suspend fun deleteContact(number: String) {
        val contact = dao.getContactByNumber(number)
        if (contact != null) {
            deleteContact(contact)
        } else {
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
            syncContacts()
        }
    }

    suspend fun deleteCallLog(id: Int) {
        dao.deleteCallLog(id)
        try {
            // Delete from system CallLog so it vanishes from other dialers too
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

    private fun getContactLookupUriFromNumber(number: String): Uri? {
        if (number.isBlank()) return null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.LOOKUP_KEY, ContactsContract.PhoneLookup._ID),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val lookupKey = cursor.getString(0)
                    val id = cursor.getLong(1)
                    ContactsContract.Contacts.getLookupUri(id, lookupKey)
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getContactIdFromNumber(number: String): String? {
        if (number.isBlank()) return null
        return try {
            // 1. Try PhoneLookup (Android's standard phone matching lookup)
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val idFromLookup = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            if (idFromLookup != null) return idFromLookup

            // 2. Try CommonDataKinds.Phone querying directly for exact match
            val phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val cleanedNumber = number.filter { it.isDigit() }
            val selection = "${Phone.NUMBER} = ? OR ${Phone.NORMALIZED_NUMBER} = ? OR REPLACE(REPLACE(REPLACE(REPLACE(${Phone.NUMBER}, ' ', ''), '-', ''), '(', ''), ')', '') = ?"
            val selectionArgs = arrayOf(number, number, cleanedNumber)
            val idFromPhoneQuery = context.contentResolver.query(
                phoneUri,
                arrayOf(Phone.CONTACT_ID),
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            if (idFromPhoneQuery != null) return idFromPhoneQuery

            // 3. Fallback: query with selection LIKE in CommonDataKinds.Phone
            if (cleanedNumber.length >= 7) {
                val last7Digits = cleanedNumber.takeLast(7)
                context.contentResolver.query(
                    phoneUri,
                    arrayOf(Phone.CONTACT_ID),
                    "${Phone.NUMBER} LIKE ?",
                    arrayOf("%$last7Digits"),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun insertManualCallRecord(name: String, number: String, type: CallType, durationSeconds: Long, simSlot: Int = 1) = withContext(Dispatchers.IO) {
        val timestampMs = System.currentTimeMillis()
        com.example.util.SimCallTracker.recordOutgoingCall(context, number, simSlot)
        try {
            val systemType = when (type) {
                CallType.MISSED -> CallLog.Calls.MISSED_TYPE
                CallType.OUTGOING -> CallLog.Calls.OUTGOING_TYPE
                CallType.INCOMING -> CallLog.Calls.INCOMING_TYPE
            }
            val values = ContentValues().apply {
                put(CallLog.Calls.NUMBER, number)
                put(CallLog.Calls.CACHED_NAME, name)
                put(CallLog.Calls.TYPE, systemType)
                put(CallLog.Calls.DATE, timestampMs)
                put(CallLog.Calls.DURATION, durationSeconds)
                put(CallLog.Calls.IS_READ, 1)
                put(CallLog.Calls.PHONE_ACCOUNT_ID, if (simSlot == 2) "sim_2" else "sim_1")
            }
            context.contentResolver.insert(CallLog.Calls.CONTENT_URI, values)
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            val colors = listOf(AvatarBlue to AvatarBlueText, AvatarOrange to AvatarOrangeText, AvatarGreen to AvatarGreenText)
            val pair = colors[Math.abs(name.hashCode()) % colors.size]
            val record = CallRecord(
                name = name,
                number = number,
                label = "Mobile",
                timestamp = sdf.format(Date(timestampMs)),
                type = type,
                avatarText = getInitials(name),
                avatarBgValue = pair.first.value.toLong(),
                avatarTextColorValue = pair.second.value.toLong(),
                duration = durationSeconds,
                hasVoicemail = false,
                timestampMs = timestampMs,
                simSlot = simSlot
            )
            dao.insertCallLogs(listOf(record))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Call Notes ---
    suspend fun updateCallRecordingNote(id: Int, note: String) = dao.updateCallRecordingNote(id, note)
    suspend fun getCallNote(number: String): CallNote? = dao.getLatestCallNote(number)
    fun getCallNotesForNumberFlow(number: String): Flow<List<CallNote>> = dao.getCallNotesForNumberFlow(number)
    fun getAllCallNotes(): Flow<List<CallNote>> = dao.getAllCallNotesFlow()
    suspend fun saveCallNote(callNote: CallNote) = dao.insertCallNote(callNote)
    suspend fun deleteCallNoteById(id: Long) = dao.deleteCallNoteById(id)
    suspend fun deleteCallNotesForNumber(number: String) = dao.deleteCallNotesForNumber(number)
}
