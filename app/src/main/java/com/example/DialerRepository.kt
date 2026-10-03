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

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.model.*
import com.example.ui.theme.*
import com.example.util.SimCallTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DialerRepository(rawContext: Context) {
    val context: Context = rawContext.applicationContext
    val db: AppDatabase = AppDatabase.getDatabase(context)
    val dao = db.dialerDao()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repositoryScope.launch {
            // Room Flow immediately emits initial dataset; eliminates redundant dual-query startup lag
            try {
                dao.getAllContactsFlow().collect { contacts ->
                    val settings = dao.getAllSettingsList()
                    ContactCache.init(contacts, settings)
                }
            } catch (_: Exception) {}
        }
    }

    // --- Paging & Flows ---

    fun getContactsPaged(query: String, accountName: String = ""): Flow<PagingData<Contact>> {
        val sanitizedQuery = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = {
                if (query.isEmpty()) dao.getContactsPaged(accountName)
                else dao.searchContacts("%$sanitizedQuery%", accountName)
            }
        ).flow
    }

    fun getFavoriteContacts(): Flow<List<Contact>> = dao.getFavoriteContacts()

    fun getAllContactsFlow(): Flow<List<Contact>> = dao.getAllContactsFlow()

    fun getCallHistoryPaged(): Flow<PagingData<CallRecord>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = { dao.getCallHistoryPaged() }
        ).flow
    }

    fun getAllCallHistoryFlow(): Flow<List<CallRecord>> = dao.getAllCallHistoryFlow()

    // --- Sync Logic ---

    private var contentObserver: ContentObserver? = null
    private var lastSyncTimestamp = 0L

    fun startObservingChanges(onChanged: () -> Unit) {
        if (contentObserver != null) return
        try {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
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
        } catch (_: SecurityException) {}
    }

    fun stopObservingChanges() {
        contentObserver?.let { observer ->
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) {}
            contentObserver = null
        }
    }

    suspend fun syncContacts(force: Boolean = false) = withContext(Dispatchers.IO) {
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
                return@withContext
            }

            val systemContacts = fetchSystemContacts()

            // ATOMIC TRANSACTION: Eliminates UI screen flashing blank during sync
            db.withTransaction {
                dao.clearContacts()
                if (systemContacts.isNotEmpty()) {
                    dao.insertContacts(systemContacts)
                }
            }

            prefs.edit()
                .putInt("last_synced_contacts_count", systemContactsCount)
                .putLong("last_synced_contacts_timestamp", maxTimestamp)
                .apply()
        } catch (_: Exception) {
            try {
                val systemContacts = fetchSystemContacts()
                if (systemContacts.isNotEmpty()) {
                    db.withTransaction {
                        dao.clearContacts()
                        dao.insertContacts(systemContacts)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun syncCallLogs() = withContext(Dispatchers.IO) {
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
            val lastSyncedMaxId = prefs.getInt("last_synced_call_log_max_id", -1)
            val lastSyncedCount = prefs.getInt("last_synced_call_log_count", -1)
            val currentLang = Locale.getDefault().language
            val lastSyncedLang = prefs.getString("last_synced_locale_lang", "")
            val localeChanged = currentLang != lastSyncedLang

            if (localCount > 0 && localCount == systemCount && systemMaxId == lastSyncedMaxId && systemCount == lastSyncedCount && !localeChanged) {
                return@withContext
            }

            val systemLogs = fetchSystemCallLogs()

            // ATOMIC TRANSACTION: Eliminates call log UI flash
            db.withTransaction {
                if (systemLogs.isNotEmpty()) {
                    dao.clearCallLogs()
                    dao.insertCallLogs(systemLogs)
                } else if (systemCount == 0 && lastSyncedCount > 0) {
                    dao.clearCallLogs()
                }
            }

            prefs.edit()
                .putInt("last_synced_call_log_max_id", systemMaxId)
                .putInt("last_synced_call_log_count", systemCount)
                .putString("last_synced_locale_lang", currentLang)
                .apply()
        } catch (_: Exception) {
            try {
                val systemLogs = fetchSystemCallLogs()
                if (systemLogs.isNotEmpty()) {
                    db.withTransaction {
                        dao.clearCallLogs()
                        dao.insertCallLogs(systemLogs)
                    }
                }
            } catch (_: Exception) {}
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
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build()
        )

        for (numItem in numbers) {
            if (numItem.number.isBlank()) continue
            val phoneType = when (numItem.label.lowercase(Locale.ROOT)) {
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
            val emailType = when (emailItem.label.lowercase(Locale.ROOT)) {
                "work" -> ContactsContract.CommonDataKinds.Email.TYPE_WORK
                "other" -> ContactsContract.CommonDataKinds.Email.TYPE_OTHER
                else -> ContactsContract.CommonDataKinds.Email.TYPE_HOME
            }
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, emailItem.email)
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, emailType)
                    .build()
            )
        }

        for (addrItem in addresses) {
            if (addrItem.address.isBlank()) continue
            val addrType = when (addrItem.label.lowercase(Locale.ROOT)) {
                "work" -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK
                "other" -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER
                else -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME
            }
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, addrItem.address)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, addrType)
                    .build()
            )
        }

        try { context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops) } catch (_: Exception) {}
        syncContacts(true)
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
                getContactIdFromNumber(contact.number)?.let { id ->
                    context.contentResolver.delete(
                        ContactsContract.RawContacts.CONTENT_URI,
                        "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                        arrayOf(id)
                    )
                }
            }
        } catch (_: Exception) {}
        dao.deleteContact(contact)
        syncContacts()
    }

    suspend fun deleteContact(number: String) {
        val contact = dao.getContactByNumber(number)
        if (contact != null) {
            deleteContact(contact)
        } else {
            try {
                getContactIdFromNumber(number)?.let { id ->
                    context.contentResolver.delete(
                        ContactsContract.RawContacts.CONTENT_URI,
                        "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                        arrayOf(id)
                    )
                }
            } catch (_: Exception) {}
            syncContacts()
        }
    }

    suspend fun deleteCallLog(id: Int) {
        dao.deleteCallLog(id)
        try {
            context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                "${CallLog.Calls._ID} = ?",
                arrayOf(id.toString())
            )
        } catch (_: Exception) {}
    }

    suspend fun clearAllCallLogs() {
        dao.clearCallLogs()
        try {
            context.contentResolver.delete(CallLog.Calls.CONTENT_URI, null, null)
        } catch (_: Exception) {}
    }

    suspend fun getCallHistoryByNumber(number: String): List<CallRecord> = dao.getCallHistoryByNumber(number)

    suspend fun toggleFavorite(number: String, isFavorite: Boolean) {
        try {
            getContactIdFromNumber(number)?.let { contactId ->
                val values = ContentValues().apply { put(ContactsContract.Contacts.STARRED, if (isFavorite) 1 else 0) }
                context.contentResolver.update(
                    ContactsContract.Contacts.CONTENT_URI,
                    values,
                    "${ContactsContract.Contacts._ID} = ?",
                    arrayOf(contactId)
                )
            }
        } catch (_: Exception) {}
        dao.getContactByNumber(number)?.let { dao.updateContact(it.copy(favorite = isFavorite)) }
    }

    private fun getContactIdFromNumber(number: String): String? {
        if (number.isBlank()) return null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) return cursor.getString(0)
            }

            val phoneUri = Phone.CONTENT_URI
            val cleanedNumber = number.filter { it.isDigit() }
            val selection = "${Phone.NUMBER} = ? OR ${Phone.NORMALIZED_NUMBER} = ? OR REPLACE(REPLACE(REPLACE(REPLACE(${Phone.NUMBER}, ' ', ''), '-', ''), '(', ''), ')', '') = ?"
            val selectionArgs = arrayOf(number, number, cleanedNumber)
            context.contentResolver.query(
                phoneUri,
                arrayOf(Phone.CONTACT_ID),
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) return cursor.getString(0)
            }

            if (cleanedNumber.length >= 7) {
                context.contentResolver.query(
                    phoneUri,
                    arrayOf(Phone.CONTACT_ID),
                    "${Phone.NUMBER} LIKE ?",
                    arrayOf("%${cleanedNumber.takeLast(7)}"),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun insertManualCallRecord(
        name: String, 
        number: String, 
        type: CallType, 
        durationSeconds: Long, 
        simSlot: Int = 1
    ) = withContext(Dispatchers.IO) {
        val timestampMs = System.currentTimeMillis()
        SimCallTracker.recordOutgoingCall(context, number, simSlot)
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
            }
            context.contentResolver.insert(CallLog.Calls.CONTENT_URI, values)
        } catch (_: Exception) {}

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
        } catch (_: Exception) {}
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