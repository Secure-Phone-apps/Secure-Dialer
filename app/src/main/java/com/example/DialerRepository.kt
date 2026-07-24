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

class DialerRepository(rawContext: Context) {
    val context: Context = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        rawContext.createAttributionContext("default")
    } else {
        rawContext
    }
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

    fun startObservingChanges(onChanged: () -> Unit) {
        if (contentObserver != null) return
        try {
            val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    onChanged()
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

    suspend fun syncContacts() {
        val systemContacts = fetchSystemContacts()
        dao.insertContacts(systemContacts)
    }

    suspend fun syncCallLogs() {
        val systemLogs = fetchSystemCallLogs()
        dao.insertCallLogs(systemLogs)
    }

    private fun fetchSystemContacts(): List<Contact> {
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
        }
        return contacts.distinctBy { it.number }
    }

    private fun fetchSystemCallLogs(): List<CallRecord> {
        val logs = mutableListOf<CallRecord>()
        val colors = listOf(AvatarBlue to AvatarBlueText, AvatarOrange to AvatarOrangeText, AvatarGreen to AvatarGreenText)
        val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        try {
            context.contentResolver.query(CallLog.Calls.CONTENT_URI, arrayOf(CallLog.Calls._ID, CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION), null, null, "${CallLog.Calls.DATE} DESC")?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

                while (cursor.moveToNext()) {
                    val num = if (numIdx != -1) cursor.getString(numIdx) ?: "" else ""
                    val cachedName = if (nameIdx != -1) cursor.getString(nameIdx) else null
                    val name = if (cachedName.isNullOrBlank()) {
                        if (num.isBlank()) "Unknown" else num
                    } else {
                        cachedName
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
                    logs.add(CallRecord(idVal, name, num, "Mobile", sdf.format(Date(dateVal)), type, name.take(1), pair.first.value.toLong(), pair.second.value.toLong(), durVal, false))
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        return logs
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
    suspend fun isBlocked(number: String): Boolean = dao.isBlocked(number)

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
    val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
    attributionContext.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getString(0)
    }
    return null
}
