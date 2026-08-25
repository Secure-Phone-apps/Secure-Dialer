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
import com.example.model.*
import com.example.DialerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupRestoreManager {

    suspend fun writeTextToUri(context: Context, uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun readTextFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun exportBlockedNumbers(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val dao = db.dialerDao()
            val list = dao.getBlockedNumbersList()
            val sb = StringBuilder()
            sb.append("# Dialer Blocked Numbers Export\n")
            sb.append("# Exported: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
            sb.append("# Format: One phone number per line\n\n")
            list.forEach { item ->
                if (item.number.isNotBlank()) {
                    sb.append(item.number.trim()).append("\n")
                }
            }
            sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    suspend fun importBlockedNumbers(context: Context, rawData: String): Int = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val dao = db.dialerDao()
            var count = 0
            val trimmed = rawData.trim()

            if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
                // Try parsing JSON format
                try {
                    if (trimmed.startsWith("[")) {
                        val array = JSONArray(trimmed)
                        for (i in 0 until array.length()) {
                            val num = array.optString(i, "").trim()
                            if (num.isNotBlank()) {
                                dao.insertBlockedNumber(BlockedNumber(number = num))
                                count++
                            }
                        }
                    } else {
                        val obj = JSONObject(trimmed)
                        if (obj.has("blocked_numbers")) {
                            val array = obj.getJSONArray("blocked_numbers")
                            for (i in 0 until array.length()) {
                                val num = array.optString(i, "").trim()
                                if (num.isNotBlank()) {
                                    dao.insertBlockedNumber(BlockedNumber(number = num))
                                    count++
                                }
                            }
                        }
                    }
                } catch (je: Exception) {
                    je.printStackTrace()
                }
            }

            if (count == 0) {
                // Parse text / CSV lines
                val lines = rawData.split(Regex("\\r?\\n"))
                for (line in lines) {
                    val cleanLine = line.trim()
                    if (cleanLine.isBlank() || cleanLine.startsWith("#") || cleanLine.startsWith("//")) {
                        continue
                    }
                    // Handle potential CSV with comma
                    val numberPart = if (cleanLine.contains(",")) {
                        cleanLine.split(",")[0].trim().replace("\"", "")
                    } else {
                        cleanLine.replace("\"", "")
                    }
                    if (numberPart.isNotBlank() && numberPart.length >= 2) {
                        dao.insertBlockedNumber(BlockedNumber(number = numberPart))
                        count++
                    }
                }
            }
            count
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    suspend fun exportBackup(context: Context, password: String = ""): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val dao = db.dialerDao()

        val json = JSONObject().apply {
            put("version", 1)
            put("timestamp", System.currentTimeMillis())

            // Blocked Numbers
            val blockedArray = JSONArray()
            dao.getBlockedNumbersList().forEach { blockedArray.put(it.number) }
            put("blocked_numbers", blockedArray)

            // Speed Dial
            val speedArray = JSONArray()
            dao.getSpeedDialList().forEach {
                speedArray.put(JSONObject().apply {
                    put("key", it.key)
                    put("number", it.number)
                    put("name", it.name)
                })
            }
            put("speed_dial", speedArray)

            // Quick Responses
            val quickArray = JSONArray()
            dao.getQuickResponsesList().forEach { quickArray.put(it.message) }
            put("quick_responses", quickArray)

            // App Settings
            val settingsArray = JSONArray()
            dao.getAllSettingsList().forEach {
                settingsArray.put(JSONObject().apply {
                    put("key", it.key)
                    put("value", it.value)
                })
            }
            put("app_settings", settingsArray)

            // Call Notes
            val notesArray = JSONArray()
            dao.getAllCallNotesList().forEach {
                notesArray.put(JSONObject().apply {
                    put("number", it.number)
                    put("note", it.note)
                    put("lastUpdated", it.lastUpdated)
                })
            }
            put("call_notes", notesArray)
        }

        val jsonString = json.toString(2)
        if (password.isNotBlank()) {
            encryptData(jsonString, password)
        } else {
            jsonString
        }
    }

    suspend fun importBackup(context: Context, rawData: String, password: String = ""): Boolean = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val dao = db.dialerDao()

        try {
            val jsonString = if (password.isNotBlank()) {
                decryptData(rawData, password)
            } else {
                if (rawData.trim().startsWith("{")) rawData else decryptData(rawData, password)
            }

            val json = JSONObject(jsonString)

            // Restore Blocked Numbers
            if (json.has("blocked_numbers")) {
                val array = json.getJSONArray("blocked_numbers")
                for (i in 0 until array.length()) {
                    dao.insertBlockedNumber(BlockedNumber(array.getString(i)))
                }
            }

            // Restore Speed Dial
            if (json.has("speed_dial")) {
                val array = json.getJSONArray("speed_dial")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    dao.insertSpeedDial(SpeedDial(obj.getInt("key"), obj.getString("number"), obj.getString("name")))
                }
            }

            // Restore Quick Responses
            if (json.has("quick_responses")) {
                val array = json.getJSONArray("quick_responses")
                for (i in 0 until array.length()) {
                    dao.insertQuickResponse(QuickResponse(message = array.getString(i)))
                }
            }

            // Restore App Settings
            if (json.has("app_settings")) {
                val array = json.getJSONArray("app_settings")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    dao.insertSetting(AppSetting(obj.getString("key"), obj.getString("value")))
                }
            }

            // Restore Call Notes
            if (json.has("call_notes")) {
                val array = json.getJSONArray("call_notes")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    dao.insertCallNote(
                        CallNote(
                            number = obj.getString("number"),
                            note = obj.getString("note"),
                            lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
                        )
                    )
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportContactsToVcf(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val dao = db.dialerDao()
            val contacts = dao.getAllContactsList()
            val sb = StringBuilder()
            contacts.forEach { contact ->
                sb.append("BEGIN:VCARD\r\n")
                sb.append("VERSION:3.0\r\n")
                sb.append("FN:${contact.name}\r\n")
                val allNumbers = contact.getAllNumbers()
                for (num in allNumbers) {
                    val type = when (num.label.lowercase()) {
                        "work" -> "WORK"
                        "home" -> "HOME"
                        "mobile" -> "CELL"
                        else -> "VOICE"
                    }
                    sb.append("TEL;TYPE=$type:${num.number}\r\n")
                }
                val allEmails = contact.getAllEmails()
                for (eml in allEmails) {
                    val type = when (eml.label.lowercase()) {
                        "work" -> "WORK"
                        else -> "HOME"
                    }
                    sb.append("EMAIL;TYPE=$type:${eml.email}\r\n")
                }
                for (adr in contact.getAllAddresses()) {
                    val type = when (adr.label.lowercase()) {
                        "work" -> "WORK"
                        else -> "HOME"
                    }
                    sb.append("ADR;TYPE=$type:;;${adr.address.replace(";", " ")};;;;\r\n")
                }
                sb.append("END:VCARD\r\n")
            }
            sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    suspend fun importContactsFromVcf(context: Context, vcfContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val lines = vcfContent.split(Regex("\\r?\\n"))
            var currentName = ""
            val currentNumbers = mutableListOf<LabeledNumber>()
            val currentEmails = mutableListOf<LabeledEmail>()
            val currentAddresses = mutableListOf<LabeledAddress>()
            val repo = DialerRepository(context)
            var count = 0
            
            lines.forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) -> {
                        currentName = ""
                        currentNumbers.clear()
                        currentEmails.clear()
                        currentAddresses.clear()
                    }
                    trimmed.startsWith("FN;", ignoreCase = true) -> {
                        val index = trimmed.indexOf(":")
                        if (index != -1) {
                            currentName = cleanVcfValue(trimmed.substring(index + 1))
                        }
                    }
                    trimmed.startsWith("FN:", ignoreCase = true) -> {
                        currentName = cleanVcfValue(trimmed.substring(3))
                    }
                    trimmed.startsWith("N;", ignoreCase = true) || trimmed.startsWith("N:", ignoreCase = true) -> {
                        if (currentName.isBlank()) {
                            val index = trimmed.indexOf(":")
                            if (index != -1) {
                                val parts = trimmed.substring(index + 1).split(";").map { cleanVcfValue(it) }
                                val formatted = parts.filter { it.isNotBlank() }.reversed().joinToString(" ")
                                if (formatted.isNotBlank()) currentName = formatted
                            }
                        }
                    }
                    trimmed.startsWith("TEL", ignoreCase = true) -> {
                        val index = trimmed.indexOf(":")
                        if (index != -1) {
                            val rawVal = cleanVcfValue(trimmed.substring(index + 1))
                            val tag = trimmed.substring(0, index).uppercase()
                            val label = when {
                                tag.contains("WORK") -> "Work"
                                tag.contains("HOME") -> "Home"
                                tag.contains("CELL") || tag.contains("MOBILE") -> "Mobile"
                                else -> "Mobile"
                            }
                            if (rawVal.isNotBlank()) {
                                val isFirst = currentNumbers.isEmpty()
                                currentNumbers.add(LabeledNumber(number = rawVal, label = label, isPrimary = isFirst))
                            }
                        }
                    }
                    trimmed.startsWith("EMAIL", ignoreCase = true) -> {
                        val index = trimmed.indexOf(":")
                        if (index != -1) {
                            val rawVal = cleanVcfValue(trimmed.substring(index + 1))
                            val tag = trimmed.substring(0, index).uppercase()
                            val label = if (tag.contains("WORK")) "Work" else "Home"
                            if (rawVal.isNotBlank()) {
                                currentEmails.add(LabeledEmail(email = rawVal, label = label))
                            }
                        }
                    }
                    trimmed.startsWith("ADR", ignoreCase = true) -> {
                        val index = trimmed.indexOf(":")
                        if (index != -1) {
                            val rawVal = cleanVcfValue(trimmed.substring(index + 1))
                            val cleanedAddr = rawVal.split(";").filter { it.isNotBlank() }.joinToString(", ")
                            val tag = trimmed.substring(0, index).uppercase()
                            val label = if (tag.contains("WORK")) "Work" else "Home"
                            if (cleanedAddr.isNotBlank()) {
                                currentAddresses.add(LabeledAddress(address = cleanedAddr, label = label))
                            }
                        }
                    }
                    trimmed.startsWith("END:VCARD", ignoreCase = true) -> {
                        if (currentNumbers.isNotEmpty()) {
                            if (currentName.isBlank()) currentName = currentNumbers.first().number
                            repo.addContactWithDetails(
                                name = currentName,
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
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val iterations = 10000
        val keyLength = 256
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun encryptData(plainText: String, password: String): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)

        val iv = ByteArray(12)
        random.nextBytes(iv)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(salt.size + iv.size + encryptedBytes.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, salt.size + iv.size, encryptedBytes.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptData(cipherText: String, password: String): String {
        val combined = Base64.decode(cipherText, Base64.DEFAULT)
        if (combined.size < 28) {
            throw IllegalArgumentException("Invalid encrypted payload size")
        }

        val salt = ByteArray(16)
        val iv = ByteArray(12)
        val encryptedBytes = ByteArray(combined.size - 28)

        System.arraycopy(combined, 0, salt, 0, 16)
        System.arraycopy(combined, 16, iv, 0, 12)
        System.arraycopy(combined, 28, encryptedBytes, 0, encryptedBytes.size)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun cleanVcfValue(value: String): String {
        return value.trim()
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\n", "\n")
            .replace("\\\\", "\\")
    }
}
