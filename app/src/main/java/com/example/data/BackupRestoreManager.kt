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

package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.DialerRepository
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupRestoreManager {

    suspend fun writeTextToUri(context: Context, uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os, Charsets.UTF_8).use { it.write(content); it.flush() }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun readTextFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun exportBlockedNumbers(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val list = AppDatabase.getDatabase(context).dialerDao().getBlockedNumbersList()
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            buildString {
                append("# Dialer Blocked Numbers Export\n# Exported: $timestamp\n# Format: One phone number per line\n\n")
                list.forEach { if (it.number.isNotBlank()) append(it.number.trim()).append("\n") }
            }
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun importBlockedNumbers(context: Context, rawData: String): Int = withContext(Dispatchers.IO) {
        try {
            val dao = AppDatabase.getDatabase(context).dialerDao()
            var count = 0
            val trimmed = rawData.trim()

            if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
                try {
                    val array = if (trimmed.startsWith("[")) {
                        JSONArray(trimmed)
                    } else {
                        JSONObject(trimmed).optJSONArray("blocked_numbers") ?: JSONArray()
                    }
                    for (i in 0 until array.length()) {
                        val num = array.optString(i, "").trim()
                        if (num.isNotBlank()) {
                            dao.insertBlockedNumber(BlockedNumber(number = num))
                            count++
                        }
                    }
                } catch (_: Exception) {}
            }

            if (count == 0) {
                trimmed.lineSequence()
                    .map { it.trim().replace("\"", "") }
                    .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("//") }
                    .map { if (it.contains(",")) it.split(",")[0].trim() else it }
                    .filter { it.length >= 2 }
                    .forEach {
                        dao.insertBlockedNumber(BlockedNumber(number = it))
                        count++
                    }
            }
            count
        } catch (_: Exception) {
            0
        }
    }

    data class VcfImportResult(
        val importedCount: Int,
        val skippedCount: Int,
        val savedToPrivateVault: Boolean
    )

    internal data class ParsedVcfContact(
        val name: String,
        val numbers: List<LabeledNumber>,
        val emails: List<LabeledEmail>,
        val addresses: List<LabeledAddress>,
        val note: String,
        val organization: String,
        val title: String,
        val birthday: String,
        val website: String,
        val nickname: String
    )

    suspend fun exportBackup(context: Context, password: String = ""): String = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(context).dialerDao()
        val converters = Converters()

        val json = JSONObject().apply {
            put("version", 3)
            put("timestamp", System.currentTimeMillis())

            put("blocked_numbers", JSONArray().apply {
                dao.getBlockedNumbersList().forEach { put(it.number) }
            })

            put("speed_dial", JSONArray().apply {
                dao.getSpeedDialList().forEach {
                    put(JSONObject().apply {
                        put("key", it.key)
                        put("number", it.number)
                        put("name", it.name)
                    })
                }
            })

            put("quick_responses", JSONArray().apply {
                dao.getQuickResponsesList().forEach { put(it.message) }
            })

            put("app_settings", JSONArray().apply {
                dao.getAllSettingsList().forEach {
                    put(JSONObject().apply {
                        put("key", it.key)
                        put("value", it.value)
                    })
                }
            })

            put("call_notes", JSONArray().apply {
                dao.getAllCallNotesList().forEach {
                    put(JSONObject().apply {
                        put("number", it.number)
                        put("note", it.note)
                        put("lastUpdated", it.lastUpdated)
                    })
                }
            })

            put("spam_numbers", JSONArray().apply {
                dao.getAllSpamNumbersList().forEach { put(it.number) }
            })

            put("call_reminders", JSONArray().apply {
                dao.getAllRemindersList().forEach {
                    put(JSONObject().apply {
                        put("number", it.number)
                        put("contactName", it.contactName)
                        put("reminderTime", it.reminderTime)
                        put("note", it.note)
                    })
                }
            })

            put("private_contacts", JSONArray().apply {
                dao.getAllPrivateContactsList().forEach { pc ->
                    put(JSONObject().apply {
                        put("name", pc.name)
                        put("number", pc.number)
                        put("label", pc.label)
                        put("favorite", pc.favorite)
                        put("email", pc.email)
                        put("photoUri", pc.photoUri)
                        put("note", pc.note)
                        put("numbersJson", converters.fromLabeledNumberList(pc.numbers))
                        put("emailsJson", converters.fromLabeledEmailList(pc.emails))
                        put("addressesJson", converters.fromLabeledAddressList(pc.addresses))
                    })
                }
            })
        }

        val jsonString = json.toString(2)
        if (password.isNotBlank()) encryptData(jsonString, password) else jsonString
    }

    suspend fun importBackup(context: Context, rawData: String, password: String = ""): Boolean = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(context).dialerDao()
        val converters = Converters()
        try {
            val jsonString = if (password.isNotBlank()) {
                decryptData(rawData, password)
            } else {
                if (rawData.trim().startsWith("{")) rawData else decryptData(rawData, password)
            }

            val json = JSONObject(jsonString)

            json.optJSONArray("blocked_numbers")?.let { arr ->
                for (i in 0 until arr.length()) dao.insertBlockedNumber(BlockedNumber(arr.getString(i)))
            }

            json.optJSONArray("speed_dial")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    dao.insertSpeedDial(SpeedDial(obj.getInt("key"), obj.getString("number"), obj.getString("name")))
                }
            }

            json.optJSONArray("quick_responses")?.let { arr ->
                for (i in 0 until arr.length()) dao.insertQuickResponse(QuickResponse(message = arr.getString(i)))
            }

            json.optJSONArray("app_settings")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    dao.insertSetting(AppSetting(obj.getString("key"), obj.getString("value")))
                }
            }

            json.optJSONArray("call_notes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    dao.insertCallNote(
                        CallNote(
                            number = obj.getString("number"),
                            note = obj.getString("note"),
                            lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
                        )
                    )
                }
            }

            json.optJSONArray("spam_numbers")?.let { arr ->
                val spamList = mutableListOf<SpamNumber>()
                for (i in 0 until arr.length()) spamList.add(SpamNumber(arr.getString(i)))
                dao.insertSpamNumbers(spamList)
            }

            json.optJSONArray("call_reminders")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    dao.insertReminder(
                        CallReminder(
                            number = obj.getString("number"),
                            name = obj.optString("contactName", ""),
                            reminderTime = obj.getLong("reminderTime"),
                            note = obj.optString("note", "")
                        )
                    )
                }
            }

            json.optJSONArray("private_contacts")?.let { arr ->
                val existing = dao.getAllPrivateContactsList()
                val existingKeys = existing.map { "${it.name.lowercase(Locale.ROOT)}|${it.number.filter { c -> c.isDigit() }}" }.toMutableSet()
                val colors = listOf(
                    com.example.ui.theme.AvatarBlue to com.example.ui.theme.AvatarBlueText,
                    com.example.ui.theme.AvatarOrange to com.example.ui.theme.AvatarOrangeText,
                    com.example.ui.theme.AvatarGreen to com.example.ui.theme.AvatarGreenText
                )
                val toInsert = mutableListOf<PrivateContact>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("name", "")
                    val number = obj.optString("number", "")
                    val key = "${name.lowercase(Locale.ROOT)}|${number.filter { c -> c.isDigit() }}"
                    if (key in existingKeys) continue
                    existingKeys.add(key)

                    val numbers = converters.toLabeledNumberList(obj.optString("numbersJson", ""))
                    val emails = converters.toLabeledEmailList(obj.optString("emailsJson", ""))
                    val addresses = converters.toLabeledAddressList(obj.optString("addressesJson", ""))
                    val pair = colors[Math.abs(name.hashCode()) % colors.size]
                    toInsert.add(
                        PrivateContact(
                            number = number,
                            name = name.ifBlank { number.ifBlank { "Unknown" } },
                            label = obj.optString("label", "Mobile"),
                            favorite = obj.optBoolean("favorite", false),
                            avatarText = getInitials(name.ifBlank { number }),
                            avatarBgValue = pair.first.value.toLong(),
                            avatarTextColorValue = pair.second.value.toLong(),
                            t9Mapping = com.example.nameToT9(name.ifBlank { number }),
                            email = obj.optString("email", ""),
                            photoUri = obj.optString("photoUri", ""),
                            numbers = if (numbers.isNotEmpty()) numbers else if (number.isNotBlank()) listOf(LabeledNumber(number, obj.optString("label", "Mobile"), true)) else emptyList(),
                            emails = emails,
                            addresses = addresses,
                            note = obj.optString("note", "")
                        )
                    )
                }
                if (toInsert.isNotEmpty()) {
                    dao.insertPrivateContacts(toInsert)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun exportContactsToVcf(
        context: Context,
        contactsOverride: List<Contact>? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val contacts = contactsOverride ?: AppDatabase.getDatabase(context).dialerDao().getAllContactsList()
            buildString {
                contacts.forEach { contact ->
                    append("BEGIN:VCARD\r\nVERSION:3.0\r\n")
                    append("FN:").append(escapeVcfValue(contact.name)).append("\r\n")
                    contact.getAllNumbers().forEach { num ->
                        val type = when (num.label.lowercase(Locale.ROOT)) {
                            "work" -> "WORK"
                            "home" -> "HOME"
                            "main" -> "PREF"
                            "mobile" -> "CELL"
                            else -> "VOICE"
                        }
                        append("TEL;TYPE=").append(type).append(":").append(num.number).append("\r\n")
                    }
                    contact.getAllEmails().forEach { eml ->
                        val type = when (eml.label.lowercase(Locale.ROOT)) {
                            "work" -> "WORK"
                            "other" -> "OTHER"
                            else -> "HOME"
                        }
                        append("EMAIL;TYPE=").append(type).append(":").append(escapeVcfValue(eml.email)).append("\r\n")
                    }
                    contact.getAllAddresses().forEach { adr ->
                        val type = when (adr.label.lowercase(Locale.ROOT)) {
                            "work" -> "WORK"
                            "other" -> "OTHER"
                            else -> "HOME"
                        }
                        append("ADR;TYPE=").append(type).append(":;;").append(escapeVcfValue(adr.address.replace(";", " "))).append(";;;;\r\n")
                    }
                    if (contact.note.isNotBlank()) {
                        append("NOTE:").append(escapeVcfValue(contact.note)).append("\r\n")
                    }
                    if (contact.isPrivateVault) {
                        append("X-SECUREDIALER-STORAGE:PRIVATE_VAULT\r\n")
                    }
                    append("END:VCARD\r\n")
                }
            }
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun importContactsFromVcf(
        context: Context,
        vcfContent: String,
        saveToPrivateVault: Boolean = false,
        accountName: String = "",
        accountType: String = ""
    ): Boolean {
        val result = importContactsFromVcfDetailed(
            context = context,
            vcfContent = vcfContent,
            saveToPrivateVault = saveToPrivateVault,
            accountName = accountName,
            accountType = accountType
        )
        return result.importedCount > 0
    }

    suspend fun importContactsFromVcfDetailed(
        context: Context,
        vcfContent: String,
        saveToPrivateVault: Boolean = false,
        accountName: String = "",
        accountType: String = ""
    ): VcfImportResult = withContext(Dispatchers.IO) {
        try {
            val parsed = parseVcfRecords(vcfContent)
            if (parsed.isEmpty()) {
                return@withContext VcfImportResult(0, 0, saveToPrivateVault)
            }
            val repo = DialerRepository(context)
            var imported = 0
            var skipped = 0
            for (record in parsed) {
                try {
                    repo.addContactWithDetails(
                        name = record.name,
                        numbers = record.numbers,
                        emails = record.emails,
                        addresses = record.addresses,
                        accountName = accountName,
                        accountType = accountType,
                        note = record.note,
                        organization = record.organization,
                        title = record.title,
                        birthday = record.birthday,
                        website = record.website,
                        nickname = record.nickname,
                        saveToPrivateVault = saveToPrivateVault,
                        syncAfter = false
                    )
                    imported++
                } catch (_: Exception) {
                    skipped++
                }
            }
            if (imported > 0 && !saveToPrivateVault) {
                repo.syncContacts(true)
            }
            VcfImportResult(imported, skipped, saveToPrivateVault)
        } catch (_: Exception) {
            VcfImportResult(0, 0, saveToPrivateVault)
        }
    }

    internal fun parseVcfRecords(vcfContent: String): List<ParsedVcfContact> {
        val sanitized = vcfContent.removePrefix("\uFEFF")
        val rawLines = sanitized.split(Regex("\\r?\\n"))
        val logicalLines = mutableListOf<String>()

        for (rawLine in rawLines) {
            val prev = logicalLines.lastOrNull()
            if (prev != null && isInsideQuotedPrintableLine(prev) && prev.endsWith("=")) {
                val withoutSoftBreak = prev.dropLast(1)
                val nextPart = if (rawLine.startsWith(" ") || rawLine.startsWith("\t")) rawLine.drop(1) else rawLine
                logicalLines[logicalLines.lastIndex] = withoutSoftBreak + nextPart
            } else if (prev != null && (rawLine.startsWith(" ") || rawLine.startsWith("\t"))) {
                logicalLines[logicalLines.lastIndex] = prev + rawLine.drop(1)
            } else {
                logicalLines.add(rawLine)
            }
        }

        val results = mutableListOf<ParsedVcfContact>()
        var inCard = false
        var currentName = ""
        val currentNumbers = mutableListOf<LabeledNumber>()
        val currentEmails = mutableListOf<LabeledEmail>()
        val currentAddresses = mutableListOf<LabeledAddress>()
        val currentNotes = mutableListOf<String>()
        var currentOrg = ""
        var currentTitle = ""
        var currentBirthday = ""
        var currentUrl = ""
        val currentUrls = mutableListOf<String>()
        var currentNickname = ""
        val extraFields = mutableListOf<String>()
        val groupToPhoneIndex = mutableMapOf<String, Int>()
        val groupToEmailIndex = mutableMapOf<String, Int>()
        val groupToAddressIndex = mutableMapOf<String, Int>()

        fun resetCard() {
            currentName = ""
            currentNumbers.clear()
            currentEmails.clear()
            currentAddresses.clear()
            currentNotes.clear()
            currentOrg = ""
            currentTitle = ""
            currentBirthday = ""
            currentUrl = ""
            currentUrls.clear()
            currentNickname = ""
            extraFields.clear()
            groupToPhoneIndex.clear()
            groupToEmailIndex.clear()
            groupToAddressIndex.clear()
        }

        for (line in logicalLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.equals("BEGIN:VCARD", ignoreCase = true)) {
                inCard = true
                resetCard()
                continue
            }
            if (trimmed.equals("END:VCARD", ignoreCase = true)) {
                if (inCard) {
                    val fallbackName = currentNumbers.firstOrNull()?.number
                        ?: currentEmails.firstOrNull()?.email
                        ?: currentOrg
                        ?: currentNickname
                        ?: ""
                    val resolvedName = currentName.ifBlank { fallbackName }
                    val hasAnyData = resolvedName.isNotBlank() ||
                            currentNumbers.isNotEmpty() ||
                            currentEmails.isNotEmpty() ||
                            currentAddresses.isNotEmpty() ||
                            currentNotes.isNotEmpty() ||
                            currentOrg.isNotBlank() ||
                            currentTitle.isNotBlank() ||
                            currentBirthday.isNotBlank() ||
                            currentUrls.isNotEmpty() ||
                            currentNickname.isNotBlank() ||
                            extraFields.isNotEmpty()
                    if (hasAnyData) {
                        val combinedNote = buildString {
                            if (currentNotes.isNotEmpty()) append(currentNotes.joinToString("\n"))
                            val structuredExtras = buildList {
                                if (currentOrg.isNotBlank()) add("Company: $currentOrg")
                                if (currentTitle.isNotBlank()) add("Title: $currentTitle")
                                if (currentBirthday.isNotBlank()) add("Birthday: $currentBirthday")
                                if (currentUrls.isNotEmpty()) add("Website: ${currentUrls.joinToString(", ")}")
                                if (currentNickname.isNotBlank()) add("Nickname: $currentNickname")
                                addAll(extraFields)
                            }
                            if (structuredExtras.isNotEmpty()) {
                                if (isNotEmpty()) append("\n")
                                append(structuredExtras.joinToString("\n"))
                            }
                        }
                        results.add(
                            ParsedVcfContact(
                                name = resolvedName.ifBlank { "Unknown" },
                                numbers = currentNumbers.toList(),
                                emails = currentEmails.toList(),
                                addresses = currentAddresses.toList(),
                                note = combinedNote.trim(),
                                organization = currentOrg.trim(),
                                title = currentTitle.trim(),
                                birthday = currentBirthday.trim(),
                                website = currentUrls.joinToString(", "),
                                nickname = currentNickname.trim()
                            )
                        )
                    }
                }
                inCard = false
                continue
            }
            if (!inCard) continue

            val colonIndex = findUnquotedColonIndex(trimmed)
            if (colonIndex == -1) continue

            val header = trimmed.substring(0, colonIndex)
            val rawValue = trimmed.substring(colonIndex + 1)
            val firstSegment = header.substringBefore(";")
            val groupPrefix = if (firstSegment.contains(".")) {
                firstSegment.substringBeforeLast(".").trim().lowercase(Locale.ROOT)
            } else ""
            val propName = firstSegment.substringAfterLast(".").trim().uppercase(Locale.ROOT)
            val tagUpper = header.uppercase(Locale.ROOT)

            when (propName) {
                "FN" -> {
                    val decoded = decodeVcfField(header, rawValue).trim()
                    if (decoded.isNotEmpty()) currentName = decoded
                }
                "N" -> {
                    if (currentName.isBlank()) {
                        val rawParts = splitVcfStructuredValue(decodeVcfFieldWithoutUnescape(header, rawValue))
                        // RFC 2426 N format: Family;Given;Additional;Prefix;Suffix
                        val family = unescapeVcfValue(rawParts.getOrNull(0) ?: "")
                        val given = unescapeVcfValue(rawParts.getOrNull(1) ?: "")
                        val middle = unescapeVcfValue(rawParts.getOrNull(2) ?: "")
                        val prefix = unescapeVcfValue(rawParts.getOrNull(3) ?: "")
                        val suffix = unescapeVcfValue(rawParts.getOrNull(4) ?: "")
                        val assembled = listOf(prefix, given, middle, family, suffix)
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .joinToString(" ")
                        if (assembled.isNotBlank()) currentName = assembled
                    }
                }
                "TEL" -> {
                    var numVal = decodeVcfField(header, rawValue).trim()
                    if (numVal.startsWith("tel:", ignoreCase = true)) {
                        numVal = numVal.substring(4).trim()
                    }
                    val label = when {
                        tagUpper.contains("FAX") -> "Other"
                        tagUpper.contains("WORK") -> "Work"
                        tagUpper.contains("HOME") -> "Home"
                        tagUpper.contains("MAIN") -> "Main"
                        tagUpper.contains("OTHER") -> "Other"
                        tagUpper.contains("CELL") || tagUpper.contains("MOBILE") || tagUpper.contains("IPHONE") -> "Mobile"
                        else -> "Mobile"
                    }
                    val isPref = tagUpper.contains("PREF") || currentNumbers.isEmpty()
                    if (numVal.isNotBlank()) {
                        val insertIdx = currentNumbers.size
                        currentNumbers.add(LabeledNumber(number = numVal, label = label, isPrimary = isPref))
                        if (groupPrefix.isNotEmpty()) {
                            groupToPhoneIndex[groupPrefix] = insertIdx
                        }
                    }
                }
                "EMAIL" -> {
                    var emailVal = decodeVcfField(header, rawValue).trim()
                    if (emailVal.startsWith("mailto:", ignoreCase = true)) {
                        emailVal = emailVal.substring(7).trim()
                    }
                    val label = when {
                        tagUpper.contains("WORK") -> "Work"
                        tagUpper.contains("OTHER") -> "Other"
                        else -> "Home"
                    }
                    if (emailVal.isNotBlank()) {
                        val insertIdx = currentEmails.size
                        currentEmails.add(LabeledEmail(email = emailVal, label = label))
                        if (groupPrefix.isNotEmpty()) {
                            groupToEmailIndex[groupPrefix] = insertIdx
                        }
                    }
                }
                "ADR" -> {
                    val rawParts = splitVcfStructuredValue(decodeVcfFieldWithoutUnescape(header, rawValue))
                    val cleaned = rawParts
                        .map { unescapeVcfValue(it).trim() }
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                    val label = when {
                        tagUpper.contains("WORK") -> "Work"
                        tagUpper.contains("OTHER") -> "Other"
                        else -> "Home"
                    }
                    if (cleaned.isNotBlank()) {
                        val insertIdx = currentAddresses.size
                        currentAddresses.add(LabeledAddress(address = cleaned, label = label))
                        if (groupPrefix.isNotEmpty()) {
                            groupToAddressIndex[groupPrefix] = insertIdx
                        }
                    }
                }
                "X-ABLABEL" -> {
                    if (groupPrefix.isNotEmpty()) {
                        val customLabel = decodeVcfField(header, rawValue)
                            .removePrefix("_\$!<")
                            .removeSuffix(">!\$_")
                            .trim()
                        if (customLabel.isNotBlank()) {
                            groupToPhoneIndex[groupPrefix]?.let { idx ->
                                currentNumbers[idx] = currentNumbers[idx].copy(label = customLabel)
                            }
                            groupToEmailIndex[groupPrefix]?.let { idx ->
                                currentEmails[idx] = currentEmails[idx].copy(label = customLabel)
                            }
                            groupToAddressIndex[groupPrefix]?.let { idx ->
                                currentAddresses[idx] = currentAddresses[idx].copy(label = customLabel)
                            }
                        }
                    }
                }
                "NOTE" -> {
                    val noteVal = decodeVcfField(header, rawValue).trim()
                    if (noteVal.isNotBlank()) currentNotes.add(noteVal)
                }
                "ORG" -> {
                    val parts = splitVcfStructuredValue(decodeVcfFieldWithoutUnescape(header, rawValue))
                        .map { unescapeVcfValue(it).trim() }
                        .filter { it.isNotBlank() }
                    val orgVal = parts.firstOrNull() ?: ""
                    val deptVal = parts.drop(1).joinToString(" - ")
                    if (orgVal.isNotBlank()) currentOrg = orgVal
                    if (deptVal.isNotBlank()) extraFields.add("Department: $deptVal")
                }
                "TITLE" -> {
                    val titleVal = decodeVcfField(header, rawValue).trim()
                    if (titleVal.isNotBlank()) currentTitle = titleVal
                }
                "ROLE" -> {
                    val roleVal = decodeVcfField(header, rawValue).trim()
                    if (roleVal.isNotBlank()) {
                        if (currentTitle.isBlank()) currentTitle = roleVal else extraFields.add("Role: $roleVal")
                    }
                }
                "BDAY" -> {
                    val bdayVal = decodeVcfField(header, rawValue).trim()
                    if (bdayVal.isNotBlank()) currentBirthday = bdayVal
                }
                "URL" -> {
                    val urlVal = decodeVcfField(header, rawValue).trim()
                    if (urlVal.isNotBlank()) {
                        currentUrl = urlVal
                        currentUrls.add(urlVal)
                    }
                }
                "NICKNAME", "X-NICKNAME" -> {
                    val nickVal = decodeVcfField(header, rawValue).trim()
                    if (nickVal.isNotBlank()) currentNickname = nickVal
                }
                "IMPP" -> {
                    val imVal = decodeVcfField(header, rawValue).trim()
                    if (imVal.isNotBlank()) extraFields.add("IM: $imVal")
                }
                "CATEGORIES" -> {
                    val catVal = decodeVcfField(header, rawValue).trim()
                    if (catVal.isNotBlank()) extraFields.add("Categories: $catVal")
                }
            }
        }
        return results
    }

    private fun isInsideQuotedPrintableLine(line: String): Boolean {
        val colonIdx = findUnquotedColonIndex(line)
        return if (colonIdx > 0) {
            line.substring(0, colonIdx).contains("QUOTED-PRINTABLE", ignoreCase = true)
        } else {
            false
        }
    }

    private fun findUnquotedColonIndex(line: String): Int {
        var inQuotes = false
        for (i in line.indices) {
            val c = line[i]
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (c == ':' && !inQuotes) {
                return i
            }
        }
        return -1
    }

    private fun splitVcfStructuredValue(value: String): List<String> {
        val parts = mutableListOf<String>()
        val sb = StringBuilder()
        var escaped = false
        for (c in value) {
            if (escaped) {
                sb.append('\\').append(c)
                escaped = false
            } else if (c == '\\') {
                escaped = true
            } else if (c == ';') {
                parts.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
        }
        if (escaped) sb.append('\\')
        parts.add(sb.toString())
        return parts
    }

    private fun decodeVcfFieldWithoutUnescape(header: String, rawValue: String): String {
        if (header.contains("QUOTED-PRINTABLE", ignoreCase = true)) {
            val charsetName = extractCharsetFromHeader(header)
            return decodeQuotedPrintable(rawValue, charsetName)
        }
        return rawValue
    }

    private fun decodeVcfField(tag: String, rawValue: String): String {
        val value = decodeVcfFieldWithoutUnescape(tag, rawValue)
        return unescapeVcfValue(value)
    }

    private fun extractCharsetFromHeader(header: String): String {
        val match = Regex("CHARSET=([^;:]+)", RegexOption.IGNORE_CASE).find(header)
        return match?.groupValues?.getOrNull(1)?.trim()?.removeSurrounding("\"") ?: "UTF-8"
    }

    private fun decodeQuotedPrintable(input: String, charsetName: String = "UTF-8"): String {
        return try {
            val bytes = ByteArrayOutputStream()
            var i = 0
            while (i < input.length) {
                val c = input[i]
                if (c == '=' && i + 2 < input.length) {
                    val hex = input.substring(i + 1, i + 3)
                    val b = hex.toIntOrNull(16)
                    if (b != null) {
                        bytes.write(b)
                        i += 3
                        continue
                    }
                }
                bytes.write(c.code and 0xFF)
                i++
            }
            try {
                bytes.toString(charsetName)
            } catch (_: Exception) {
                bytes.toString("UTF-8")
            }
        } catch (_: Exception) {
            input
        }
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val chars = password.toCharArray()
        val spec = PBEKeySpec(chars, salt, iterations, 256)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
            chars.fill('\u0000') // Zeroize password in memory immediately
        }
    }

    private fun encryptData(plainText: String, password: String): String {
        val random = SecureRandom()
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val iv = ByteArray(12).also { random.nextBytes(it) }

        val key = deriveKey(password, salt, 250000)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))

        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(salt.size + iv.size + encrypted.size)

        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(encrypted, 0, combined, salt.size + iv.size, encrypted.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptData(cipherText: String, password: String): String {
        val combined = Base64.decode(cipherText, Base64.DEFAULT)
        if (combined.size < 28) throw IllegalArgumentException("Corrupt backup payload")

        val salt = ByteArray(16)
        val iv = ByteArray(12)
        val encrypted = ByteArray(combined.size - 28)

        System.arraycopy(combined, 0, salt, 0, 16)
        System.arraycopy(combined, 16, iv, 0, 12)
        System.arraycopy(combined, 28, encrypted, 0, encrypted.size)

        return try {
            val key = deriveKey(password, salt, 250000)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            val legacyKey = deriveKey(password, salt, 10000)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, legacyKey, GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }
    }

    private fun escapeVcfValue(value: String): String {
        return buildString(value.length) {
            for (c in value) {
                when (c) {
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> {}
                    ',' -> append("\\,")
                    ';' -> append("\\;")
                    else -> append(c)
                }
            }
        }
    }

    private fun unescapeVcfValue(value: String): String {
        val trimmed = value.trim()
        if (!trimmed.contains('\\')) return trimmed
        val sb = StringBuilder(trimmed.length)
        var i = 0
        while (i < trimmed.length) {
            val c = trimmed[i]
            if (c == '\\' && i + 1 < trimmed.length) {
                when (val next = trimmed[i + 1]) {
                    'n', 'N' -> sb.append('\n')
                    ',' -> sb.append(',')
                    ';' -> sb.append(';')
                    ':' -> sb.append(':')
                    '\\' -> sb.append('\\')
                    else -> sb.append(next)
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}