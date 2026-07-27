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
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.dialerDao()

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

    private suspend fun fetchSystemContacts(): List<Contact> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()
        val colors = listOf(AvatarBlue to AvatarBlueText, AvatarOrange to AvatarOrangeText, AvatarGreen to AvatarGreenText)
        
        val emailMap = mutableMapOf<String, String>()
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.CONTACT_ID, ContactsContract.CommonDataKinds.Email.ADDRESS),
                null, null, null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                val addrIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                while (cursor.moveToNext()) {
                    if (idIdx != -1 && addrIdx != -1) {
                        val cid = cursor.getString(idIdx) ?: ""
                        val email = cursor.getString(addrIdx) ?: ""
                        if (cid.isNotEmpty() && email.isNotEmpty()) {
                            emailMap[cid] = email
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            context.contentResolver.query(
                Phone.CONTENT_URI,
                arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER, Phone.STARRED, Phone.PHOTO_THUMBNAIL_URI, Phone.CONTACT_ID),
                null, null, "${Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(Phone.NUMBER)
                val favIdx = cursor.getColumnIndex(Phone.STARRED)
                val photoIdx = cursor.getColumnIndex(Phone.PHOTO_THUMBNAIL_URI)
                val cidIdx = cursor.getColumnIndex(Phone.CONTACT_ID)
                while (cursor.moveToNext()) {
                    val num = cursor.getString(numIdx) ?: ""
                    val rawName = cursor.getString(nameIdx)
                    val name = if (rawName.isNullOrBlank()) (if (num.isBlank()) "Unknown" else num) else rawName
                    val fav = cursor.getInt(favIdx) == 1
                    val photoUri = if (photoIdx != -1) cursor.getString(photoIdx) ?: "" else ""
                    val contactId = if (cidIdx != -1) cursor.getString(cidIdx) ?: "" else ""
                    val email = emailMap[contactId] ?: ""
                    val pair = colors[Math.abs(name.hashCode()) % colors.size]
                    contacts.add(Contact(num, name, "Mobile", fav, name.take(1), pair.first.value.toLong(), pair.second.value.toLong(), nameToT9(name), email, photoUri))
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        contacts.distinctBy { it.number }
    }

    private suspend fun fetchSystemCallLogs(): List<CallRecord> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val logs = mutableListOf<CallRecord>()
        val colors = listOf(AvatarBlue to AvatarBlueText, AvatarOrange to AvatarOrangeText, AvatarGreen to AvatarGreenText)
        val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        
        val allContacts = try {
            dao.getAllContactsList()
        } catch (e: Exception) {
            emptyList<Contact>()
        }

        val allSettings = try {
            dao.getAllSettingsList()
        } catch (e: Exception) {
            emptyList<AppSetting>()
        }

        val cnapPrefix = "cnap_"
        val cnapMap = allSettings.filter { it.key.startsWith(cnapPrefix) }
            .associate { it.key.substring(cnapPrefix.length) to it.value }

        fun numbersMatch(n1: String, n2: String): Boolean {
            val c1 = n1.filter { it.isDigit() }
            val c2 = n2.filter { it.isDigit() }
            if (c1.isEmpty() || c2.isEmpty()) return false
            if (c1 == c2) return true
            val minLen = minOf(c1.length, c2.length)
            if (minLen >= 7) {
                val matchLen = if (minLen >= 10) 10 else if (minLen >= 8) 8 else 7
                return c1.takeLast(matchLen) == c2.takeLast(matchLen)
            }
            return false
        }

        try {
            val queryUri = CallLog.Calls.CONTENT_URI.buildUpon()
                .appendQueryParameter("limit", "200")
                .build()
            context.contentResolver.query(
                queryUri,
                arrayOf(CallLog.Calls._ID, CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION),
                null, null, "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

                while (cursor.moveToNext()) {
                    val num = if (numIdx != -1) cursor.getString(numIdx) ?: "" else ""
                    val cachedName = if (nameIdx != -1) cursor.getString(nameIdx) else null
                    
                    val matchingContact = if (num.isNotEmpty()) {
                        allContacts.firstOrNull { numbersMatch(it.number, num) }
                    } else null
                    
                    val matchingCnapName = if (num.isNotEmpty()) {
                        cnapMap.entries.firstOrNull { numbersMatch(it.key, num) }?.value
                    } else null

                    val name = when {
                        matchingContact != null -> matchingContact.name
                        !cachedName.isNullOrBlank() -> cachedName
                        !matchingCnapName.isNullOrBlank() -> matchingCnapName
                        num.isBlank() -> "Unknown"
                        else -> num
                    }
                    
                    val typeVal = if (typeIdx != -1) cursor.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                    val type = when (typeVal) {
                        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                        else -> CallType.INCOMING
                    }
                    val idVal = if (idIdx != -1) cursor.getInt(idIdx) else 0
                    val dateVal = if (dateIdx != -1) cursor.getLong(dateIdx) else 0L
                    val durVal = if (durIdx != -1) cursor.getLong(durIdx) else 0L

                    val pair = colors[Math.abs(name.hashCode()) % colors.size]
                    val photoUriVal = matchingContact?.photoUri ?: ""
                    logs.add(CallRecord(idVal, name, num, "Mobile", sdf.format(Date(dateVal)), type, name.take(1), pair.first.value.toLong(), pair.second.value.toLong(), durVal, false, photoUriVal))
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        logs
    }

    // --- T9 Helper ---

    private fun nameToT9(name: String): String {
        return name.uppercase().map { char ->
            when (char) {
                in 'A'..'C' -> '2'
                in 'D'..'F' -> '3'
                in 'G'..'I' -> '4'
                in 'J'..'L' -> '5'
                in 'M'..'O' -> '6'
                in 'P'..'S' -> '7'
                in 'T'..'V' -> '8'
                in 'W'..'Z' -> '9'
                else -> char
            }
        }.joinToString("")
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
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup._ID)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    if (idIndex >= 0) {
                        return cursor.getString(idIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // --- Settings Persistence ---

    fun getBlockedNumbers(): Flow<List<BlockedNumber>> = dao.getBlockedNumbersFlow()
    suspend fun addBlockedNumber(number: String) = dao.insertBlockedNumber(BlockedNumber(number))
    suspend fun removeBlockedNumber(number: String) = dao.deleteBlockedNumber(BlockedNumber(number))
    suspend fun isBlocked(number: String): Boolean = CallBlockerService.isNumberBlocked(dao, number)

    // --- Backup & Restore ---
    suspend fun exportBackup(password: String = ""): String = com.example.data.BackupRestoreManager.exportBackup(context, password)
    suspend fun importBackup(rawData: String, password: String = ""): Boolean = com.example.data.BackupRestoreManager.importBackup(context, rawData, password)

    // --- Watchdog & Health ---
    suspend fun getServiceHealth(): ServiceHealth = CallBlockerService.getServiceHealthStatus(context)

    fun getSpeedDial(): Flow<List<SpeedDial>> = dao.getSpeedDialFlow()
    suspend fun saveSpeedDial(key: Int, number: String, name: String) = dao.insertSpeedDial(SpeedDial(key, number, name))
    suspend fun deleteSpeedDial(key: Int) = dao.deleteSpeedDial(key)

    fun getQuickResponses(): Flow<List<QuickResponse>> = dao.getQuickResponsesFlow()
    suspend fun addQuickResponse(message: String) = dao.insertQuickResponse(QuickResponse(message = message))
    suspend fun deleteQuickResponse(response: QuickResponse) = dao.deleteQuickResponse(response)

    suspend fun getVoicemailNumber(): String = dao.getSetting("voicemail_number") ?: ""
    suspend fun saveVoicemailNumber(number: String) = dao.insertSetting(AppSetting("voicemail_number", number))

    suspend fun getPreferredSim(): String = dao.getSetting("preferred_sim") ?: "Ask"
    suspend fun savePreferredSim(sim: String) = dao.insertSetting(AppSetting("preferred_sim", sim))

    // --- Call Notes ---
    suspend fun getCallNote(number: String): CallNote? = dao.getCallNote(number)
    fun getAllCallNotes(): Flow<List<CallNote>> = dao.getAllCallNotesFlow()
    suspend fun saveCallNote(number: String, note: String) = dao.insertCallNote(CallNote(number, note))
    suspend fun deleteCallNote(number: String) = dao.deleteCallNote(number)

    // --- Call Recordings ---
    fun getAllCallRecordings(): Flow<List<CallRecording>> = dao.getAllCallRecordingsFlow()
    suspend fun saveCallRecording(recording: CallRecording) = dao.insertCallRecording(recording)
    suspend fun deleteCallRecording(id: Int) = dao.deleteCallRecording(id)
}

// Keep these for simple lookups if needed, but repository should be preferred
fun getContactNameFromNumber(context: Context, number: String): String? {
    val attributionContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        context.createAttributionContext("default")
    } else {
        context
    }
    try {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        attributionContext.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    // Fallback: Query our local database contacts using robust matching
    try {
        val db = com.example.data.AppDatabase.getDatabase(context)
        val dao = db.dialerDao()
        val cleanInput = number.filter { it.isDigit() }
        if (cleanInput.isNotEmpty()) {
            val contacts = kotlinx.coroutines.runBlocking { dao.getAllContactsList() }
            val match = contacts.firstOrNull { contact ->
                val cleanContact = contact.number.filter { it.isDigit() }
                if (cleanContact.isEmpty()) false
                else if (cleanInput == cleanContact) true
                else {
                    val minLen = minOf(cleanInput.length, cleanContact.length)
                    if (minLen >= 7) {
                        val matchLen = if (minLen >= 10) 10 else if (minLen >= 8) 8 else 7
                        cleanInput.takeLast(matchLen) == cleanContact.takeLast(matchLen)
                    } else false
                }
            }
            if (match != null) return match.name
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}
