package com.example.data

import android.content.Context
import android.util.Base64
import com.example.model.*
import com.example.DialerRepository
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object BackupRestoreManager {

    suspend fun exportBackup(context: Context, password: String = ""): String {
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
        return if (password.isNotBlank()) {
            encryptData(jsonString, password)
        } else {
            jsonString
        }
    }

    suspend fun importBackup(context: Context, rawData: String, password: String = ""): Boolean {
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

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun exportContactsToVcf(context: Context): String {
        try {
            val db = AppDatabase.getDatabase(context)
            val dao = db.dialerDao()
            val contacts = dao.getAllContactsList()
            val sb = StringBuilder()
            contacts.forEach { contact ->
                sb.append("BEGIN:VCARD\r\n")
                sb.append("VERSION:3.0\r\n")
                sb.append("FN:${contact.name}\r\n")
                sb.append("TEL;TYPE=CELL:${contact.number}\r\n")
                if (contact.email.isNotEmpty()) {
                    sb.append("EMAIL;TYPE=HOME:${contact.email}\r\n")
                }
                sb.append("END:VCARD\r\n")
            }
            return sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    suspend fun importContactsFromVcf(context: Context, vcfContent: String): Boolean {
        try {
            val lines = vcfContent.split(Regex("\\r?\\n"))
            var currentName = ""
            var currentNum = ""
            var currentEmail = ""
            val db = AppDatabase.getDatabase(context)
            val dao = db.dialerDao()
            val repo = DialerRepository(context)
            var count = 0
            
            lines.forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) -> {
                        currentName = ""
                        currentNum = ""
                        currentEmail = ""
                    }
                    trimmed.startsWith("FN:", ignoreCase = true) -> {
                        currentName = trimmed.substring(3).trim()
                    }
                    trimmed.startsWith("TEL;", ignoreCase = true) -> {
                        val index = trimmed.indexOf(":")
                        if (index != -1) {
                            currentNum = trimmed.substring(index + 1).trim()
                        }
                    }
                    trimmed.startsWith("TEL:", ignoreCase = true) -> {
                        currentNum = trimmed.substring(4).trim()
                    }
                    trimmed.startsWith("EMAIL;", ignoreCase = true) -> {
                        val index = trimmed.indexOf(":")
                        if (index != -1) {
                            currentEmail = trimmed.substring(index + 1).trim()
                        }
                    }
                    trimmed.startsWith("EMAIL:", ignoreCase = true) -> {
                        currentEmail = trimmed.substring(6).trim()
                    }
                    trimmed.startsWith("END:VCARD", ignoreCase = true) -> {
                        if (currentNum.isNotBlank()) {
                            if (currentName.isBlank()) currentName = currentNum
                            repo.addContact(currentName, currentNum, "Mobile", currentEmail)
                            count++
                        }
                    }
                }
            }
            return count > 0
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun deriveKey(password: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(bytes, "AES")
    }

    private fun encryptData(plainText: String, password: String): String {
        val key = deriveKey(password)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16) { 0 }
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun decryptData(cipherText: String, password: String): String {
        val key = deriveKey(password)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16) { 0 }
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        val decoded = Base64.decode(cipherText, Base64.DEFAULT)
        val decrypted = cipher.doFinal(decoded)
        return String(decrypted, Charsets.UTF_8)
    }
}
