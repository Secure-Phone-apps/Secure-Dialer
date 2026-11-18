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

    fun getContactsPaged(query: String, accountName: String = "", isLastNameFirst: Boolean = false): Flow<PagingData<Contact>> {
        val sanitizedQuery = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = {
                if (query.isEmpty()) dao.getContactsPaged(accountName, isLastNameFirst)
                else dao.searchContacts("%$sanitizedQuery%", accountName, isLastNameFirst)
            }
        ).flow
    }

    fun getFavoriteContacts(isLastNameFirst: Boolean = false): Flow<List<Contact>> = dao.getFavoriteContacts(isLastNameFirst)

    fun getAllContactsFlow(isLastNameFirst: Boolean = false): Flow<List<Contact>> = dao.getAllContactsFlow(isLastNameFirst)

    fun getCallHistoryPaged(): Flow<PagingData<CallRecord>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = { dao.getCallHistoryPaged() }
        ).flow
    }

    fun getAllCallHistoryFlow(): Flow<List<CallRecord>> = dao.getAllCallHistoryFlow()

    // --- Sync Logic ---

    private var contentObserver: ContentObserver? = null
    private val debounceHandler = Handler(Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null

    fun startObservingChanges(onChanged: () -> Unit) {
        if (contentObserver != null) return
        try {
            val observer = object : ContentObserver(debounceHandler) {
                override fun onChange(selfChange: Boolean) {
                    debounceRunnable?.let { debounceHandler.removeCallbacks(it) }
                    val runnable = Runnable { onChanged() }
                    debounceRunnable = runnable
                    debounceHandler.postDelayed(runnable, 400L)
                }
            }
            contentObserver = observer
            context.contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
            context.contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)
        } catch (_: SecurityException) {}
    }

    fun stopObservingChanges() {
        debounceRunnable?.let { debounceHandler.removeCallbacks(it) }
        debounceRunnable = null
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

    suspend fun syncCallLogs(force: Boolean = false) = withContext(Dispatchers.IO) {
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

            if (!force && localCount > 0 && localCount == systemCount && systemMaxId == lastSyncedMaxId && systemCount == lastSyncedCount && !localeChanged) {
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
        accountType: String = "",
        note: String = "",
        saveToPrivateVault: Boolean = false
    ) {
        val numbers = if (number.isNotBlank()) listOf(LabeledNumber(number, label.ifBlank { "Mobile" }, true)) else emptyList()
        val emails = if (email.isNotBlank()) listOf(LabeledEmail(email, "Home")) else emptyList()
        addContactWithDetails(
            name = name,
            numbers = numbers,
            emails = emails,
            addresses = emptyList(),
            accountName = accountName,
            accountType = accountType,
            note = note,
            saveToPrivateVault = saveToPrivateVault || accountType == PRIVATE_VAULT_ACCOUNT_TYPE
        )
    }

    internal fun splitFullNameParts(fullName: String): Pair<String, String> {
        val trimmed = fullName.trim()
        if (trimmed.isEmpty()) return "" to ""
        if (trimmed.contains(",")) {
            val parts = trimmed.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size >= 2) {
                return parts.drop(1).joinToString(" ") to parts[0]
            }
        }
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
        return if (words.size <= 1) {
            trimmed to ""
        } else {
            words.dropLast(1).joinToString(" ") to words.last()
        }
    }

    private fun buildPrivateContactEntity(
        id: Long = 0L,
        name: String,
        numbers: List<LabeledNumber>,
        emails: List<LabeledEmail>,
        addresses: List<LabeledAddress>,
        note: String,
        favorite: Boolean = false,
        photoUri: String = ""
    ): PrivateContact {
        val primaryNumber = numbers.firstOrNull { it.isPrimary }?.number
            ?: numbers.firstOrNull()?.number
            ?: ""
        val primaryLabel = numbers.firstOrNull { it.isPrimary }?.label
            ?: numbers.firstOrNull()?.label
            ?: "Mobile"
        val cleanName = name.trim().ifBlank { primaryNumber.ifBlank { "Unknown" } }
        val colors = listOf(AvatarBlue to AvatarBlueText, AvatarOrange to AvatarOrangeText, AvatarGreen to AvatarGreenText)
        val pair = colors[Math.abs(cleanName.hashCode()) % colors.size]
        return PrivateContact(
            id = id,
            number = primaryNumber,
            name = cleanName,
            label = primaryLabel,
            favorite = favorite,
            avatarText = getInitials(cleanName),
            avatarBgValue = pair.first.value.toLong(),
            avatarTextColorValue = pair.second.value.toLong(),
            t9Mapping = nameToT9(cleanName),
            email = emails.firstOrNull()?.email ?: "",
            photoUri = photoUri,
            accountName = PRIVATE_VAULT_ACCOUNT_NAME,
            accountType = PRIVATE_VAULT_ACCOUNT_TYPE,
            numbers = numbers,
            emails = emails,
            addresses = addresses,
            note = note.trim()
        )
    }

    suspend fun addContactWithDetails(
        name: String,
        numbers: List<LabeledNumber>,
        emails: List<LabeledEmail> = emptyList(),
        addresses: List<LabeledAddress> = emptyList(),
        accountName: String = "",
        accountType: String = "",
        note: String = "",
        organization: String = "",
        title: String = "",
        birthday: String = "",
        website: String = "",
        nickname: String = "",
        saveToPrivateVault: Boolean = false,
        syncAfter: Boolean = true
    ) {
        val isPrivate = saveToPrivateVault ||
                accountType == PRIVATE_VAULT_ACCOUNT_TYPE ||
                accountName == PRIVATE_VAULT_ACCOUNT_NAME

        if (isPrivate) {
            val combinedNote = buildString {
                if (note.isNotBlank()) append(note.trim())
                if (organization.isNotBlank() && !note.contains(organization.trim())) {
                    if (isNotEmpty()) append("\n")
                    append("Company: ").append(organization.trim())
                }
                if (title.isNotBlank() && !note.contains(title.trim())) {
                    if (isNotEmpty()) append("\n")
                    append("Title: ").append(title.trim())
                }
                if (birthday.isNotBlank() && !note.contains(birthday.trim())) {
                    if (isNotEmpty()) append("\n")
                    append("Birthday: ").append(birthday.trim())
                }
                if (website.isNotBlank() && !note.contains(website.trim())) {
                    if (isNotEmpty()) append("\n")
                    append("Website: ").append(website.trim())
                }
                if (nickname.isNotBlank() && !note.contains(nickname.trim())) {
                    if (isNotEmpty()) append("\n")
                    append("Nickname: ").append(nickname.trim())
                }
            }
            val entity = buildPrivateContactEntity(
                name = name,
                numbers = numbers,
                emails = emails,
                addresses = addresses,
                note = combinedNote
            )
            dao.insertPrivateContact(entity)
            return
        }

        val (givenName, familyName) = splitFullNameParts(name)
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
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name.trim())
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, givenName)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, familyName)
                .build()
        )

        appendContactDetailInsertOps(
            ops = ops,
            rawContactId = null,
            backRefIndex = 0,
            numbers = numbers,
            emails = emails,
            addresses = addresses,
            note = note.takeIf { it.isNotBlank() },
            organization = organization.takeIf { it.isNotBlank() },
            title = title.takeIf { it.isNotBlank() },
            birthday = birthday.takeIf { it.isNotBlank() },
            website = website.takeIf { it.isNotBlank() },
            nickname = nickname.takeIf { it.isNotBlank() }
        )

        try { context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops) } catch (_: Exception) {}
        if (syncAfter) syncContacts(true)
    }

    /**
     * Atomically updates an existing contact in-place using its RAW_CONTACT_ID (for system contacts)
     * or directly in the SQLCipher Private Vault table (for private contacts).
     */
    suspend fun updateContactWithDetails(
        oldContact: Contact,
        name: String,
        numbers: List<LabeledNumber>,
        emails: List<LabeledEmail> = emptyList(),
        addresses: List<LabeledAddress> = emptyList(),
        accountName: String = "",
        accountType: String = "",
        note: String? = null
    ) = withContext(Dispatchers.IO) {
        val targetIsPrivate = accountType == PRIVATE_VAULT_ACCOUNT_TYPE || accountName == PRIVATE_VAULT_ACCOUNT_NAME
        if (oldContact.isPrivateVault && (targetIsPrivate || accountType.isBlank())) {
            val localId = if (oldContact.id < 0L) -oldContact.id else oldContact.id
            val updated = buildPrivateContactEntity(
                id = localId,
                name = name,
                numbers = numbers,
                emails = emails,
                addresses = addresses,
                note = note ?: oldContact.note,
                favorite = oldContact.favorite,
                photoUri = oldContact.photoUri
            )
            dao.insertPrivateContact(updated)
            return@withContext
        }

        if (!oldContact.isPrivateVault && targetIsPrivate) {
            moveContactsToPrivateVault(listOf(oldContact.copy(
                name = name,
                numbers = numbers,
                emails = emails,
                addresses = addresses,
                note = note ?: oldContact.note
            )))
            return@withContext
        }

        val rawId = resolveEditableRawContactId(oldContact)
        if (rawId == null || rawId <= 0L) {
            addContactWithDetails(
                name = name,
                numbers = numbers,
                emails = emails,
                addresses = addresses,
                accountName = accountName,
                accountType = accountType,
                note = note ?: oldContact.note,
                saveToPrivateVault = targetIsPrivate
            )
            if (oldContact.favorite) {
                val primaryNum = numbers.firstOrNull()?.number ?: oldContact.number
                if (primaryNum.isNotBlank()) toggleFavorite(primaryNum, true)
            }
            if (oldContact.isPrivateVault && !targetIsPrivate) {
                dao.deletePrivateContactById(-oldContact.id)
            }
            return@withContext
        }

        val (givenName, familyName) = splitFullNameParts(name)
        val ops = buildUpdateContactOps(
            rawContactId = rawId,
            existingNameDataId = findStructuredNameDataId(rawId),
            displayName = name.trim(),
            givenName = givenName,
            familyName = familyName,
            numbers = numbers,
            emails = emails,
            addresses = addresses,
            note = note
        )

        try {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        } catch (_: Exception) {}

        syncContacts(true)
    }

    internal fun buildUpdateContactOps(
        rawContactId: Long,
        existingNameDataId: Long?,
        displayName: String,
        givenName: String,
        familyName: String,
        numbers: List<LabeledNumber>,
        emails: List<LabeledEmail>,
        addresses: List<LabeledAddress>,
        note: String? = null
    ): ArrayList<ContentProviderOperation> {
        val ops = arrayListOf<ContentProviderOperation>()

        if (existingNameDataId != null && existingNameDataId > 0L) {
            ops.add(
                ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existingNameDataId.toString()))
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, givenName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, familyName)
                    .build()
            )
        } else {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, givenName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, familyName)
                    .build()
            )
        }

        // Only replace phones, emails, postal addresses (and Note only if explicitly edited); preserve all other metadata rows
        if (note != null) {
            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} IN (?, ?, ?, ?)",
                        arrayOf(
                            rawContactId.toString(),
                            Phone.CONTENT_ITEM_TYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
                            ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
                        )
                    )
                    .build()
            )
        } else {
            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} IN (?, ?, ?)",
                        arrayOf(
                            rawContactId.toString(),
                            Phone.CONTENT_ITEM_TYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE
                        )
                    )
                    .build()
            )
        }

        appendContactDetailInsertOps(
            ops = ops,
            rawContactId = rawContactId,
            backRefIndex = null,
            numbers = numbers,
            emails = emails,
            addresses = addresses,
            note = note?.trim()?.takeIf { it.isNotEmpty() }
        )

        return ops
    }

    private fun appendContactDetailInsertOps(
        ops: ArrayList<ContentProviderOperation>,
        rawContactId: Long?,
        backRefIndex: Int?,
        numbers: List<LabeledNumber>,
        emails: List<LabeledEmail>,
        addresses: List<LabeledAddress>,
        note: String? = null,
        organization: String? = null,
        title: String? = null,
        birthday: String? = null,
        website: String? = null,
        nickname: String? = null
    ) {
        fun baseDataInsert(): ContentProviderOperation.Builder {
            val builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            if (rawContactId != null) {
                builder.withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
            } else if (backRefIndex != null) {
                builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, backRefIndex)
            }
            return builder
        }

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
            val builder = baseDataInsert()
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
                baseDataInsert()
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
                baseDataInsert()
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, addrItem.address)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, addrType)
                    .build()
            )
        }

        if (!note.isNullOrBlank()) {
            ops.add(
                baseDataInsert()
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Note.NOTE, note.trim())
                    .build()
            )
        }

        if (!organization.isNullOrBlank() || !title.isNullOrBlank()) {
            val orgBuilder = baseDataInsert()
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
            if (!organization.isNullOrBlank()) {
                orgBuilder.withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, organization.trim())
            }
            if (!title.isNullOrBlank()) {
                orgBuilder.withValue(ContactsContract.CommonDataKinds.Organization.TITLE, title.trim())
            }
            ops.add(orgBuilder.build())
        }

        if (!website.isNullOrBlank()) {
            ops.add(
                baseDataInsert()
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Website.URL, website.trim())
                    .withValue(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE_OTHER)
                    .build()
            )
        }

        if (!birthday.isNullOrBlank()) {
            ops.add(
                baseDataInsert()
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, birthday.trim())
                    .withValue(ContactsContract.CommonDataKinds.Event.TYPE, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)
                    .build()
            )
        }

        if (!nickname.isNullOrBlank()) {
            ops.add(
                baseDataInsert()
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Nickname.NAME, nickname.trim())
                    .build()
            )
        }
    }

    private fun resolveEditableRawContactId(contact: Contact): Long? {
        try {
            if (contact.rawContactId > 0L) {
                context.contentResolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(ContactsContract.RawContacts._ID, ContactsContract.RawContacts.ACCOUNT_TYPE),
                    "${ContactsContract.RawContacts._ID} = ? AND ${ContactsContract.RawContacts.DELETED} = 0",
                    arrayOf(contact.rawContactId.toString()),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val accType = cursor.getString(1) ?: ""
                        if (!accType.contains("whatsapp", ignoreCase = true)) {
                            return cursor.getLong(0)
                        }
                    }
                }
            }

            val contactIdStr = if (contact.contactId > 0L) contact.contactId.toString() else getContactIdFromNumber(contact.number)
            if (!contactIdStr.isNullOrBlank()) {
                context.contentResolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(ContactsContract.RawContacts._ID, ContactsContract.RawContacts.ACCOUNT_TYPE),
                    "${ContactsContract.RawContacts.CONTACT_ID} = ? AND ${ContactsContract.RawContacts.DELETED} = 0",
                    arrayOf(contactIdStr),
                    null
                )?.use { cursor ->
                    var fallbackId: Long? = null
                    while (cursor.moveToNext()) {
                        val rawId = cursor.getLong(0)
                        val accType = cursor.getString(1) ?: ""
                        if (!accType.contains("whatsapp", ignoreCase = true)) {
                            return rawId
                        }
                        if (fallbackId == null) fallbackId = rawId
                    }
                    return fallbackId
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun findStructuredNameDataId(rawContactId: Long): Long? {
        return try {
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID),
                "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    internal fun buildBatchDeleteOperations(contacts: List<Contact>): ArrayList<ContentProviderOperation> {
        val ops = arrayListOf<ContentProviderOperation>()
        val seenRawIds = mutableSetOf<Long>()
        val seenContactIds = mutableSetOf<String>()

        for (contact in contacts) {
            if (contact.isPrivateVault) continue
            when {
                contact.rawContactId > 0L && seenRawIds.add(contact.rawContactId) -> {
                    ops.add(
                        ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                            .withSelection("${ContactsContract.RawContacts._ID} = ?", arrayOf(contact.rawContactId.toString()))
                            .build()
                    )
                }
                contact.contactId > 0L && seenContactIds.add(contact.contactId.toString()) -> {
                    ops.add(
                        ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                            .withSelection("${ContactsContract.RawContacts.CONTACT_ID} = ?", arrayOf(contact.contactId.toString()))
                            .build()
                    )
                }
                else -> {
                    getContactIdFromNumber(contact.number)?.let { resolvedId ->
                        if (seenContactIds.add(resolvedId)) {
                            ops.add(
                                ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                                    .withSelection("${ContactsContract.RawContacts.CONTACT_ID} = ?", arrayOf(resolvedId))
                                    .build()
                            )
                        }
                    }
                }
            }
        }
        return ops
    }

    suspend fun deleteContact(contact: Contact) {
        deleteContacts(listOf(contact))
    }

    suspend fun deleteContacts(contacts: List<Contact>): Int = withContext(Dispatchers.IO) {
        if (contacts.isEmpty()) return@withContext 0

        val privateIds = contacts.filter { it.isPrivateVault && it.id < 0L }.map { -it.id }.distinct()
        val systemContacts = contacts.filter { !it.isPrivateVault }

        if (privateIds.isNotEmpty()) {
            privateIds.chunked(500).forEach { chunk ->
                dao.deletePrivateContactsByIds(chunk)
            }
        }

        if (systemContacts.isNotEmpty()) {
            val ops = buildBatchDeleteOperations(systemContacts)
            // Chunk ContentProviderOperation batches under Android's 1MB Binder transaction limit
            ops.chunked(150).forEach { batch ->
                try {
                    context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ArrayList(batch))
                } catch (_: Exception) {
                    // Fallback per-op if OEM provider rejects one read-only account row in the chunk
                    for (singleOp in batch) {
                        try {
                            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, arrayListOf(singleOp))
                        } catch (_: Exception) {}
                    }
                }
            }

            val systemCacheIds = systemContacts.map { it.id }.filter { it > 0L }.distinct()
            if (systemCacheIds.isNotEmpty()) {
                systemCacheIds.chunked(500).forEach { chunk ->
                    dao.deleteContactsByIds(chunk)
                }
            }
            syncContacts(true)
        }

        contacts.size
    }

    suspend fun moveContactsToPrivateVault(contacts: List<Contact>): Int = withContext(Dispatchers.IO) {
        val candidates = contacts.filter { !it.isPrivateVault }
        if (candidates.isEmpty()) return@withContext 0

        val privateEntities = candidates.map { it.toPrivateContact(preserveLocalId = false) }
        val insertedIds = db.withTransaction {
            dao.insertPrivateContacts(privateEntities)
        }
        if (insertedIds.size != candidates.size) return@withContext 0

        // Remove originals from public system storage now that they are safely persisted in SQLCipher
        val ops = buildBatchDeleteOperations(candidates)
        ops.chunked(150).forEach { batch ->
            try {
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ArrayList(batch))
            } catch (_: Exception) {
                for (singleOp in batch) {
                    try {
                        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, arrayListOf(singleOp))
                    } catch (_: Exception) {}
                }
            }
        }
        val cacheIds = candidates.map { it.id }.filter { it > 0L }.distinct()
        if (cacheIds.isNotEmpty()) {
            cacheIds.chunked(500).forEach { chunk -> dao.deleteContactsByIds(chunk) }
        }
        syncContacts(true)
        candidates.size
    }

    suspend fun exportPrivateContactToSystem(
        contact: Contact,
        accountName: String = "",
        accountType: String = ""
    ) = withContext(Dispatchers.IO) {
        if (!contact.isPrivateVault) return@withContext
        addContactWithDetails(
            name = contact.name,
            numbers = contact.getAllNumbers(),
            emails = contact.getAllEmails(),
            addresses = contact.getAllAddresses(),
            accountName = accountName,
            accountType = accountType,
            note = contact.note,
            saveToPrivateVault = false,
            syncAfter = true
        )
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

    suspend fun toggleFavorite(contact: Contact, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        if (contact.isPrivateVault && contact.id < 0L) {
            dao.updatePrivateFavoriteById(-contact.id, isFavorite)
        } else {
            toggleFavorite(contact.number, isFavorite)
        }
    }

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
        dao.getContactByNumber(number)?.let { matched ->
            if (matched.isPrivateVault && matched.id < 0L) {
                dao.updatePrivateFavoriteById(-matched.id, isFavorite)
            } else {
                dao.updateContact(matched.copy(favorite = isFavorite))
            }
        }
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