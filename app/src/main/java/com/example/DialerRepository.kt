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

class DialerRepository(private val context: Context) {
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

    fun getCallHistoryPaged(): Flow<PagingData<CallRecord>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = { dao.getCallHistoryPaged() }
        ).flow
    }

    // --- Sync Logic ---

    fun startObservingChanges(onChanged: () -> Unit) {
        try {
            val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    onChanged()
                }
            }
            context.contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
            context.contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)
        } catch (e: SecurityException) {
            e.printStackTrace()
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
        try {
            context.contentResolver.query(Phone.CONTENT_URI, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER, Phone.STARRED), null, null, "${Phone.DISPLAY_NAME} ASC")?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(Phone.NUMBER)
                val favIdx = cursor.getColumnIndex(Phone.STARRED)
                while (cursor.moveToNext()) {
                    val rawName = cursor.getString(nameIdx)
                    val num = cursor.getString(numIdx) ?: ""
                    val name = if (rawName.isNullOrBlank()) {
                        if (num.isBlank()) "Unknown" else num
                    } else {
                        rawName
                    }
                    val fav = cursor.getInt(favIdx) == 1
                    val pair = colors[Math.abs(name.hashCode()) % colors.size]
                    contacts.add(Contact(num, name, "Mobile", fav, name.take(1), pair.first.value.toLong(), pair.second.value.toLong(), nameToT9(name)))
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
                while (cursor.moveToNext()) {
                    val num = cursor.getString(2) ?: ""
                    val cachedName = cursor.getString(1)
                    val name = if (cachedName.isNullOrBlank()) {
                        if (num.isBlank()) "Unknown" else num
                    } else {
                        cachedName
                    }
                    val type = when (cursor.getInt(3)) {
                        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                        else -> CallType.INCOMING
                    }
                    val pair = colors[Math.abs(name.hashCode()) % colors.size]
                    logs.add(CallRecord(cursor.getInt(0), name, num, "Mobile", sdf.format(Date(cursor.getLong(4))), type, name.take(1), pair.first.value.toLong(), pair.second.value.toLong(), cursor.getLong(5), false))
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
                'A', 'B', 'C' -> '2'
                'D', 'E', 'F' -> '3'
                'G', 'H', 'I' -> '4'
                'J', 'K', 'L' -> '5'
                'M', 'N', 'O' -> '6'
                'P', 'Q', 'R', 'S' -> '7'
                'T', 'U', 'V' -> '8'
                'W', 'X', 'Y', 'Z' -> '9'
                else -> char
            }
        }.joinToString("")
    }

    // --- Actions ---

    suspend fun addContact(name: String, number: String, label: String) {
        val ops = arrayListOf<ContentProviderOperation>()
        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null).withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build())
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE).withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name).build())
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE).withValue(Phone.NUMBER, number).withValue(Phone.TYPE, Phone.TYPE_MOBILE).build())
        try { context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops) } catch (e: Exception) { e.printStackTrace() }
        syncContacts() // Update local cache
    }

    suspend fun deleteContact(number: String) {
        context.contentResolver.delete(ContactsContract.RawContacts.CONTENT_URI, "${Phone.NUMBER} = ?", arrayOf(number))
        dao.getContactByNumber(number)?.let { dao.deleteContact(it) }
    }

    suspend fun deleteCallLog(id: Int) {
        dao.deleteCallLog(id)
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
}

// Keep these for simple lookups if needed, but repository should be preferred
fun getContactNameFromNumber(context: Context, number: String): String? {
    val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
    context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getString(0)
    }
    return null
}
