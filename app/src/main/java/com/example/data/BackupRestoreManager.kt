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

    suspend fun exportBackup(context: Context, password: String = ""): String = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(context).dialerDao()

        val json = JSONObject().apply {
            put("version", 2)
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

            // COMPLETE RESTORATION: Included missing entities
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
        }

        val jsonString = json.toString(2)
        if (password.isNotBlank()) encryptData(jsonString, password) else jsonString
    }

    suspend fun importBackup(context: Context, rawData: String, password: String = ""): Boolean = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(context).dialerDao()
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
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun exportContactsToVcf(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val contacts = AppDatabase.getDatabase(context).dialerDao().getAllContactsList()
            buildString {
                contacts.forEach { contact ->
                    append("BEGIN:VCARD\r\nVERSION:3.0\r\n")
                    append("FN:").append(contact.name).append("\r\n")
                    contact.getAllNumbers().forEach { num ->
                        val type = when (num.label.lowercase(Locale.ROOT)) {
                            "work" -> "WORK"; "home" -> "HOME"; "mobile" -> "CELL"; else -> "VOICE"
                        }
                        append("TEL;TYPE=").append(type).append(":").append(num.number).append("\r\n")
                    }
                    contact.getAllEmails().forEach { eml ->
                        val type = if (eml.label.lowercase(Locale.ROOT) == "work") "WORK" else "HOME"
                        append("EMAIL;TYPE=").append(type).append(":").append(eml.email).append("\r\n")
                    }
                    contact.getAllAddresses().forEach { adr ->
                        val type = if (adr.label.lowercase(Locale.ROOT) == "work") "WORK" else "HOME"
                        append("ADR;TYPE=").append(type).append(":;;").append(adr.address.replace(";", " ")).append(";;;;\r\n")
                    }
                    append("END:VCARD\r\n")
                }
            }
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun importContactsFromVcf(context: Context, vcfContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // CRITICAL FIX: Unfold lines according to RFC 2426/6350 before parsing
            val unfolded = vcfContent.replace(Regex("\r?\n[ \t]"), "")
            val lines = unfolded.split(Regex("\\r?\\n"))

            var currentName = ""
            val currentNumbers = mutableListOf<LabeledNumber>()
            val currentEmails = mutableListOf<LabeledEmail>()
            val currentAddresses = mutableListOf<LabeledAddress>()
            val repo = DialerRepository(context)
            var count = 0

            for (line in lines) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) -> {
                        currentName = ""
                        currentNumbers.clear()
                        currentEmails.clear()
                        currentAddresses.clear()
                    }
                    trimmed.startsWith("FN", ignoreCase = true) -> {
                        val index = trimmed.indexOf(":")
                        if (index != -1) currentName = decodeVcfField(trimmed.substring(0, index), trimmed.substring(index + 1))
                    }
                    trimmed.startsWith("N", ignoreCase = true) && !trimmed.startsWith("NOTE", ignoreCase = true) -> {
                        if (currentName.isBlank()) {
                            val index = trimmed.indexOf(":")
                            if (index != -1) {
                                val decoded = decodeVcfField(trimmed.substring(0, index), trimmed.substring(index + 1))
                                currentName = decoded.split(";").filter { it.isNotBlank() }.reversed().joinToString(" ")
                            }
                        }
                    }
                    trimmed.startsWith("TEL", ignoreCase = true) -> {
                        val index = trimmed.indexOf(":")
                        if (index != -1) {
                            val rawVal = decodeVcfField(trimmed.substring(0, index), trimmed.substring(index + 1))
                            val tag = trimmed.substring(0, index).uppercase(Locale.ROOT)
                            val label = when {
                                tag.contains("WORK") -> "Work"
                                tag.contains("HOME") -> "Home"
                                else -> "Mobile"
                            }
                            if (rawVal.isNotBlank()) {
                                currentNumbers.add(LabeledNumber(number = rawVal, label = label, isPrimary = currentNumbers.isEmpty()))
                            }
                        }
                    }
                    trimmed.startsWith("EMAIL", ignoreCase = true) -> {
                        val index = trimmed.indexOf(":")
                        if (index != -1) {
                            val rawVal = decodeVcfField(trimmed.substring(0, index), trimmed.substring(index + 1))
                            val tag = trimmed.substring(0, index).uppercase(Locale.ROOT)
                            val label = if (tag.contains("WORK")) "Work" else "Home"
                            if (rawVal.isNotBlank()) currentEmails.add(LabeledEmail(email = rawVal, label = label))
                        }
                    }
                    trimmed.startsWith("ADR", ignoreCase = true) -> {
                        val index = trimmed.indexOf(":")
                        if (index != -1) {
                            val rawVal = decodeVcfField(trimmed.substring(0, index), trimmed.substring(index + 1))
                            val cleaned = rawVal.split(";").filter { it.isNotBlank() }.joinToString(", ")
                            val tag = trimmed.substring(0, index).uppercase(Locale.ROOT)
                            val label = if (tag.contains("WORK")) "Work" else "Home"
                            if (cleaned.isNotBlank()) currentAddresses.add(LabeledAddress(address = cleaned, label = label))
                        }
                    }
                    trimmed.startsWith("END:VCARD", ignoreCase = true) -> {
                        if (currentNumbers.isNotEmpty()) {
                            val nameToUse = currentName.ifBlank { currentNumbers.first().number }
                            repo.addContactWithDetails(
                                name = nameToUse,
                                numbers = currentNumbers.toList(),
                                emails = currentEmails.toList(),
                                addresses = currentAddresses.toList()
                            )
                            count++
                        }
                    }
                }
            }
            count > 0
        } catch (_: Exception) {
            false
        }
    }

    private fun decodeVcfField(tag: String, rawValue: String): String {
        var value = rawValue
        if (tag.contains("QUOTED-PRINTABLE", ignoreCase = true)) {
            value = decodeQuotedPrintable(value)
        }
        return cleanVcfValue(value)
    }

    private fun decodeQuotedPrintable(input: String): String {
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
                bytes.write(c.code)
                i++
            }
            bytes.toString("UTF-8")
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

    private fun cleanVcfValue(value: String): String = value.trim()
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\n", "\n")
        .replace("\\\\", "\\")
}