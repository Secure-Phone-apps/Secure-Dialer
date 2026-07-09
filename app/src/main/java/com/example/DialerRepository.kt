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
        context.contentResolver.query(Phone.CONTENT_URI, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER, Phone.STARRED), null, null, "${Phone.DISPLAY_NAME} ASC")?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndex(Phone.NUMBER)
            val favIdx = cursor.getColumnIndex(Phone.STARRED)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx) ?: "Unknown"
                val num = cursor.getString(numIdx) ?: ""
                val fav = cursor.getInt(favIdx) == 1
                val pair = colors[Math.abs(name.hashCode()) % colors.size]
                contacts.add(Contact(num, name, "Mobile", fav, name.take(1), pair.first.value.toLong(), pair.second.value.toLong(), nameToT9(name)))
            }
        }
        return contacts.distinctBy { it.number }
    }

    private fun fetchSystemCallLogs(): List<CallRecord> {
        val logs = mutableListOf<CallRecord>()
        val colors = listOf(AvatarBlue to AvatarBlueText, AvatarOrange to AvatarOrangeText, AvatarGreen to AvatarGreenText)
        val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        context.contentResolver.query(CallLog.Calls.CONTENT_URI, arrayOf(CallLog.Calls._ID, CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION), null, null, "${CallLog.Calls.DATE} DESC")?.use { cursor ->
            while (cursor.moveToNext()) {
                val num = cursor.getString(2) ?: ""
                val name = cursor.getString(1) ?: num
                val type = when (cursor.getInt(3)) {
                    CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                    CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                    else -> CallType.INCOMING
                }
                val pair = colors[Math.abs(name.hashCode()) % colors.size]
                logs.add(CallRecord(cursor.getInt(0), name, num, "Mobile", sdf.format(Date(cursor.getLong(4))), type, name.take(1), pair.first.value.toLong(), pair.second.value.toLong(), cursor.getLong(5), false))
            }
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

    suspend fun toggleFavorite(number: String, isFavorite: Boolean) {
        val values = ContentValues().apply { put(ContactsContract.Contacts.STARRED, if (isFavorite) 1 else 0) }
        context.contentResolver.update(ContactsContract.Contacts.CONTENT_URI, values, "${Phone.NUMBER} = ?", arrayOf(number))
        dao.getContactByNumber(number)?.let { dao.updateContact(it.copy(favorite = isFavorite)) }
    }
}

// Keep these for simple lookups if needed, but repository should be preferred
fun getContactNameFromNumber(context: Context, number: String): String? {
    val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
    context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getString(0)
    }
    return null
}
