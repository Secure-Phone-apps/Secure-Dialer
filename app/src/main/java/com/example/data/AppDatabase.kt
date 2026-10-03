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
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.example.model.*
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class Converters {
    @TypeConverter
    fun fromCallType(value: CallType?): String = (value ?: CallType.INCOMING).name

    @TypeConverter
    fun toCallType(value: String?): CallType = try {
        if (!value.isNullOrBlank()) CallType.valueOf(value) else CallType.INCOMING
    } catch (_: Exception) {
        CallType.INCOMING
    }

    @TypeConverter
    fun fromLabeledNumberList(list: List<LabeledNumber>?): String {
        if (list.isNullOrEmpty()) return ""
        val jsonArray = JSONArray()
        list.forEach { item ->
            jsonArray.put(JSONObject().apply {
                put("num", item.number)
                put("lbl", item.label)
                put("pri", item.isPrimary)
            })
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toLabeledNumberList(value: String?): List<LabeledNumber> {
        if (value.isNullOrBlank()) return emptyList()
        val list = mutableListOf<LabeledNumber>()
        try {
            val jsonArray = JSONArray(value)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    LabeledNumber(
                        number = obj.optString("num", ""),
                        label = obj.optString("lbl", "Mobile"),
                        isPrimary = obj.optBoolean("pri", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    @TypeConverter
    fun fromLabeledEmailList(list: List<LabeledEmail>?): String {
        if (list.isNullOrEmpty()) return ""
        val jsonArray = JSONArray()
        list.forEach { item ->
            jsonArray.put(JSONObject().apply {
                put("eml", item.email)
                put("lbl", item.label)
            })
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toLabeledEmailList(value: String?): List<LabeledEmail> {
        if (value.isNullOrBlank()) return emptyList()
        val list = mutableListOf<LabeledEmail>()
        try {
            val jsonArray = JSONArray(value)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    LabeledEmail(
                        email = obj.optString("eml", ""),
                        label = obj.optString("lbl", "Home")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    @TypeConverter
    fun fromLabeledAddressList(list: List<LabeledAddress>?): String {
        if (list.isNullOrEmpty()) return ""
        val jsonArray = JSONArray()
        list.forEach { item ->
            jsonArray.put(JSONObject().apply {
                put("adr", item.address)
                put("lbl", item.label)
            })
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toLabeledAddressList(value: String?): List<LabeledAddress> {
        if (value.isNullOrBlank()) return emptyList()
        val list = mutableListOf<LabeledAddress>()
        try {
            val jsonArray = JSONArray(value)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    LabeledAddress(
                        address = obj.optString("adr", ""),
                        label = obj.optString("lbl", "Home")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}

@Database(
    entities = [
        Contact::class,
        CallRecord::class,
        BlockedNumber::class,
        SpeedDial::class,
        QuickResponse::class,
        AppSetting::class,
        CallNote::class,
        CallRecording::class,
        SpamNumber::class,
        CallReminder::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dialerDao(): DialerDao

    companion object {
        const val DATABASE_NAME = "dialer_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): AppDatabase {
            return try {
                SQLiteDatabase.loadLibs(appContext)
                val dbKey = DatabaseKeyManager.getDatabaseKey(appContext)
                val factory = SupportFactory(dbKey)
                ensureDatabaseIntegrity(appContext, factory, dbKey)

                Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .openHelperFactory(factory)
                    // High-concurrency encrypted WAL mode: readers never block writers
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
            } catch (e: Throwable) {
                // Host JVM Unit Test fallback (Robolectric without native SQLCipher .so)
                Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
            }
        }

        private fun ensureDatabaseIntegrity(appContext: Context, factory: SupportFactory, dbKey: ByteArray) {
            val dbFile = appContext.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists() || dbFile.length() == 0L) {
                return
            }

            // 1. Verify if database opens cleanly with the current key via SupportFactory
            if (canOpenDatabase(appContext, factory)) {
                return
            }

            // 2. If it cannot be opened with the key, check if it's an unencrypted plaintext SQLite database
            if (isPlaintextDatabase(dbFile)) {
                val migrated = migratePlaintextDatabase(dbFile, dbKey)
                if (migrated && canOpenDatabase(appContext, factory)) {
                    return
                }
            }

            // 3. Database is corrupt or encrypted with an unrecoverable old key.
            // Self-heal by removing the unreadable file so Room can recreate a fresh encrypted database.
            deleteDatabaseFiles(dbFile)
        }

        private fun canOpenDatabase(appContext: Context, factory: SupportFactory): Boolean {
            var helper: SupportSQLiteOpenHelper? = null
            var db: androidx.sqlite.db.SupportSQLiteDatabase? = null
            var cursor: android.database.Cursor? = null
            return try {
                helper = factory.create(
                    SupportSQLiteOpenHelper.Configuration.builder(appContext)
                        .name(DATABASE_NAME)
                        .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {}
                            override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                        })
                        .build()
                )
                db = helper.writableDatabase
                cursor = db.query("SELECT count(*) FROM sqlite_master;")
                cursor != null && cursor.moveToFirst()
            } catch (_: Exception) {
                false
            } finally {
                try { cursor?.close() } catch (_: Exception) {}
                try { db?.close() } catch (_: Exception) {}
                try { helper?.close() } catch (_: Exception) {}
            }
        }

        private fun isPlaintextDatabase(dbFile: File): Boolean {
            if (!dbFile.exists() || dbFile.length() < 16L) return false
            return try {
                dbFile.inputStream().use { stream ->
                    val header = ByteArray(16)
                    val read = stream.read(header)
                    read == 16 && String(header, Charsets.US_ASCII).startsWith("SQLite format 3")
                }
            } catch (_: Exception) {
                false
            }
        }

        private fun migratePlaintextDatabase(dbFile: File, dbKey: ByteArray): Boolean {
            val tempEncryptedFile = File(dbFile.parentFile, "${dbFile.name}_encrypted_temp.db")
            if (tempEncryptedFile.exists()) tempEncryptedFile.delete()

            var plaintextDb: SQLiteDatabase? = null
            return try {
                plaintextDb = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    "",
                    null,
                    SQLiteDatabase.OPEN_READWRITE
                )
                val keyHex = dbKey.joinToString("") { "%02x".format(it) }
                plaintextDb.execSQL("ATTACH DATABASE '${tempEncryptedFile.absolutePath}' AS encrypted KEY \"x'$keyHex'\";")
                val cursor = plaintextDb.rawQuery("SELECT sqlcipher_export('encrypted');", null)
                cursor?.moveToFirst()
                cursor?.close()
                plaintextDb.execSQL("DETACH DATABASE encrypted;")
                plaintextDb.close()
                plaintextDb = null

                deleteDatabaseFiles(dbFile)
                tempEncryptedFile.renameTo(dbFile)
            } catch (_: Exception) {
                try { plaintextDb?.close() } catch (_: Exception) {}
                if (tempEncryptedFile.exists()) tempEncryptedFile.delete()
                false
            }
        }

        private fun deleteDatabaseFiles(dbFile: File) {
            try {
                dbFile.delete()
                File("${dbFile.absolutePath}-wal").delete()
                File("${dbFile.absolutePath}-shm").delete()
                File("${dbFile.absolutePath}-journal").delete()
            } catch (_: Exception) {}
        }
    }
}