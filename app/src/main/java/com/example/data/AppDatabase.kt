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
import android.os.Build
import android.os.SystemClock
import android.os.UserManager
import androidx.annotation.VisibleForTesting
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

sealed interface DatabaseIntegrityStatus {
    data object Healthy : DatabaseIntegrityStatus
    data class TransientKeystoreError(val reason: String) : DatabaseIntegrityStatus
    data class NativeCryptoLoadError(val reason: String) : DatabaseIntegrityStatus
    data class CorruptedQuarantined(val backupFileName: String) : DatabaseIntegrityStatus
    data class RobolectricTestFallback(val reason: String) : DatabaseIntegrityStatus
}

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
        PrivateContact::class,
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
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dialerDao(): DialerDao

    companion object {
        const val DATABASE_NAME = "dialer_database"

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `private_contacts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `number` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `favorite` INTEGER NOT NULL,
                        `avatarText` TEXT NOT NULL,
                        `avatarBgValue` INTEGER NOT NULL,
                        `avatarTextColorValue` INTEGER NOT NULL,
                        `t9Mapping` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `photoUri` TEXT NOT NULL,
                        `accountName` TEXT NOT NULL,
                        `accountType` TEXT NOT NULL,
                        `numbers` TEXT NOT NULL,
                        `emails` TEXT NOT NULL,
                        `addresses` TEXT NOT NULL,
                        `note` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_private_contacts_number` ON `private_contacts` (`number`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_private_contacts_name` ON `private_contacts` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_private_contacts_t9Mapping` ON `private_contacts` (`t9Mapping`)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `call_history` ADD COLUMN `numberPresentation` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `call_history` ADD COLUMN `blockReason` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `call_history` ADD COLUMN `callScreeningAppName` TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Volatile
        private var lastRecoveryAttemptAtMs: Long = 0L
        private const val AUTO_RECOVERY_COOLDOWN_MS = 1_500L

        private val _integrityStatus = MutableStateFlow<DatabaseIntegrityStatus>(DatabaseIntegrityStatus.Healthy)
        val integrityStatus: StateFlow<DatabaseIntegrityStatus> = _integrityStatus.asStateFlow()

        fun dismissIntegrityWarning() {
            if (_integrityStatus.value is DatabaseIntegrityStatus.CorruptedQuarantined) {
                _integrityStatus.value = DatabaseIntegrityStatus.Healthy
            }
        }

        internal fun isDeviceUserUnlocked(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return true
            return try {
                val userManager = context.getSystemService(UserManager::class.java)
                userManager?.isUserUnlocked ?: true
            } catch (_: Throwable) {
                true
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            val current = INSTANCE
            if (current != null) {
                if (_integrityStatus.value is DatabaseIntegrityStatus.TransientKeystoreError &&
                    isDeviceUserUnlocked(context)
                ) {
                    retryOpenOnTransientRecoveryIfUnlocked(context)
                    return INSTANCE ?: current
                }
                return current
            }
            return synchronized(this) {
                val lockedCurrent = INSTANCE
                if (lockedCurrent != null) {
                    if (_integrityStatus.value is DatabaseIntegrityStatus.TransientKeystoreError &&
                        isDeviceUserUnlocked(context)
                    ) {
                        retryOpenOnTransientRecoveryIfUnlocked(context)
                    }
                    INSTANCE ?: lockedCurrent
                } else {
                    buildDatabase(context.applicationContext).also { INSTANCE = it }
                }
            }
        }

        /**
         * Attempts to reopen the encrypted on-disk database only when currently in a recoverable
         * error state and the device has completed credential unlock (Direct Boot safe).
         */
        fun retryOpenOnTransientRecoveryIfUnlocked(
            context: Context,
            force: Boolean = false
        ): Boolean {
            val currentStatus = _integrityStatus.value
            if (currentStatus !is DatabaseIntegrityStatus.TransientKeystoreError &&
                currentStatus !is DatabaseIntegrityStatus.NativeCryptoLoadError
            ) {
                return true
            }
            if (!isDeviceUserUnlocked(context)) {
                return false
            }
            return retryOpenOnTransientRecovery(context, force = force)
        }

        /**
         * Attempts to reopen the encrypted on-disk database after a TransientKeystoreError
         * (e.g. device unlocked after Direct Boot or Keystore daemon recovered) or NativeCryptoLoadError.
         */
        @Synchronized
        @JvmOverloads
        fun retryOpenOnTransientRecovery(context: Context, force: Boolean = false): Boolean {
            val currentStatus = _integrityStatus.value
            if (currentStatus !is DatabaseIntegrityStatus.TransientKeystoreError &&
                currentStatus !is DatabaseIntegrityStatus.NativeCryptoLoadError
            ) {
                return true
            }
            val now = SystemClock.elapsedRealtime()
            if (!force && lastRecoveryAttemptAtMs != 0L && (now - lastRecoveryAttemptAtMs) < AUTO_RECOVERY_COOLDOWN_MS) {
                return false
            }
            lastRecoveryAttemptAtMs = now

            val prevInstance = INSTANCE
            val reopened = buildDatabase(context.applicationContext)
            val newStatus = _integrityStatus.value
            return if (newStatus == DatabaseIntegrityStatus.Healthy ||
                newStatus is DatabaseIntegrityStatus.CorruptedQuarantined ||
                newStatus is DatabaseIntegrityStatus.RobolectricTestFallback
            ) {
                INSTANCE = reopened
                if (prevInstance != null && prevInstance !== reopened) {
                    try { prevInstance.close() } catch (_: Throwable) {}
                }
                true
            } else {
                // Keep existing fallback instance intact rather than leaking repeated in-memory DBs
                if (prevInstance != null) {
                    if (reopened !== prevInstance) {
                        try { reopened.close() } catch (_: Throwable) {}
                    }
                    INSTANCE = prevInstance
                } else {
                    INSTANCE = reopened
                }
                false
            }
        }

        @Volatile
        private var warmUpStarted = false
        private val warmUpLock = Any()
        @Volatile
        private var warmUpCompleted: kotlinx.coroutines.CompletableDeferred<Unit> =
            kotlinx.coroutines.CompletableDeferred<Unit>().apply { complete(Unit) }

        suspend fun awaitWarmUp() {
            try {
                warmUpCompleted.await()
            } catch (_: Throwable) {}
        }

        fun warmUpAsync(context: Context) {
            val deferredToRun: kotlinx.coroutines.CompletableDeferred<Unit>
            synchronized(warmUpLock) {
                if (warmUpStarted || INSTANCE != null) return
                warmUpStarted = true
                deferredToRun = kotlinx.coroutines.CompletableDeferred()
                warmUpCompleted = deferredToRun
            }
            val appContext = context.applicationContext
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    if (!isRobolectricEnvironment()) {
                        try {
                            System.loadLibrary("sqlcipher")
                        } catch (_: UnsatisfiedLinkError) {
                            // buildDatabase will report NativeCryptoLoadError if missing
                        }
                    }
                    val db = getDatabase(appContext)
                    db.dialerDao().getContactsCount()
                } catch (_: Throwable) {
                } finally {
                    deferredToRun.complete(Unit)
                }
            }
        }

        internal fun isRobolectricEnvironment(): Boolean {
            return Build.FINGERPRINT.equals("robolectric", ignoreCase = true) ||
                Build.HARDWARE.equals("robolectric", ignoreCase = true)
        }

        @VisibleForTesting
        internal fun resetForTesting(status: DatabaseIntegrityStatus = DatabaseIntegrityStatus.Healthy) {
            synchronized(this) {
                lastRecoveryAttemptAtMs = 0L
                val prev = INSTANCE
                INSTANCE = null
                try { prev?.close() } catch (_: Throwable) {}
                _integrityStatus.value = status
            }
            synchronized(warmUpLock) {
                warmUpStarted = false
                warmUpCompleted = kotlinx.coroutines.CompletableDeferred<Unit>().apply { complete(Unit) }
            }
        }

        private fun buildDatabase(appContext: Context): AppDatabase {
            if (isRobolectricEnvironment()) {
                _integrityStatus.value = DatabaseIntegrityStatus.RobolectricTestFallback("Robolectric JVM test runtime")
                return Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
            }

            try {
                System.loadLibrary("sqlcipher")
            } catch (e: UnsatisfiedLinkError) {
                _integrityStatus.value = DatabaseIntegrityStatus.NativeCryptoLoadError(
                    e.message ?: "Native SQLCipher library failed to load"
                )
                return Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java)
                    .build()
            }

            return try {
                val dbKey = DatabaseKeyManager.getDatabaseKey(appContext)
                ensureDatabaseIntegrity(appContext, dbKey)
                val factory = SupportOpenHelperFactory(dbKey.copyOf())

                Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11)
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
            } catch (e: TransientKeystoreException) {
                // Keep wrapped key & on-disk encrypted DB intact; expose recovery state
                _integrityStatus.value = DatabaseIntegrityStatus.TransientKeystoreError(
                    e.message ?: "Android Keystore temporarily unavailable"
                )
                Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java)
                    .build()
            } catch (e: Throwable) {
                // Emergency crash shield for telecom services while preserving existing files on disk
                _integrityStatus.value = DatabaseIntegrityStatus.TransientKeystoreError(
                    e.message ?: "Encrypted database open interrupted"
                )
                Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java)
                    .build()
            }
        }

        internal fun ensureDatabaseIntegrity(appContext: Context, dbKey: ByteArray) {
            val dbFile = appContext.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists() || dbFile.length() == 0L) {
                _integrityStatus.value = DatabaseIntegrityStatus.Healthy
                return
            }

            // 1. Verify if database opens cleanly with the current key via native SQLCipher open
            if (canOpenDatabase(dbFile, dbKey)) {
                _integrityStatus.value = DatabaseIntegrityStatus.Healthy
                return
            }

            // 2. If it cannot be opened with the key, check if it's an unencrypted plaintext SQLite database
            if (isPlaintextDatabase(dbFile)) {
                val migrated = migratePlaintextDatabase(dbFile, dbKey)
                if (migrated && canOpenDatabase(dbFile, dbKey)) {
                    _integrityStatus.value = DatabaseIntegrityStatus.Healthy
                    return
                }
            }

            // 3. Database is unreadable with current key: quarantine rather than permanently deleting
            val backupName = quarantineDatabaseFiles(dbFile)
            _integrityStatus.value = DatabaseIntegrityStatus.CorruptedQuarantined(backupName)
        }

        internal fun quarantineDatabaseFiles(dbFile: File): String {
            val timestamp = System.currentTimeMillis()
            val parent = dbFile.parentFile
            val backupName = "${dbFile.name}.corrupt.$timestamp.bak"
            if (parent != null) {
                // Prune older quarantine backups beyond 2 copies to prevent storage bloat
                parent.listFiles { f ->
                    f.name.startsWith("${dbFile.name}.corrupt.") && f.name.endsWith(".bak")
                }?.sortedByDescending { it.lastModified() }?.drop(1)?.forEach { old ->
                    try { old.delete() } catch (_: Throwable) {}
                }

                val backupFile = File(parent, backupName)
                if (dbFile.exists()) {
                    try {
                        if (!dbFile.renameTo(backupFile)) {
                            dbFile.copyTo(backupFile, overwrite = true)
                            dbFile.delete()
                        }
                    } catch (_: Throwable) {}
                }
                listOf("-wal", "-shm", "-journal").forEach { suffix ->
                    val sidecar = File("${dbFile.absolutePath}$suffix")
                    if (sidecar.exists()) {
                        try { sidecar.renameTo(File(parent, "$backupName$suffix")) } catch (_: Throwable) {}
                    }
                }
            } else {
                deleteDatabaseFiles(dbFile)
            }
            return backupName
        }

        private fun canOpenDatabase(dbFile: File, dbKey: ByteArray): Boolean {
            if (!dbFile.exists() || dbFile.length() == 0L) return true
            var testDb: SQLiteDatabase? = null
            var cursor: android.database.Cursor? = null
            return try {
                testDb = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    dbKey,
                    null,
                    SQLiteDatabase.OPEN_READWRITE,
                    null,
                    null
                )
                cursor = testDb?.rawQuery("SELECT count(*) FROM sqlite_master;", null)
                cursor != null && cursor.moveToFirst()
            } catch (_: Throwable) {
                false
            } finally {
                try { cursor?.close() } catch (_: Throwable) {}
                try { testDb?.close() } catch (_: Throwable) {}
            }
        }

        internal fun isPlaintextDatabase(dbFile: File): Boolean {
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
                    byteArrayOf(),
                    null,
                    SQLiteDatabase.OPEN_READWRITE,
                    null,
                    null
                )
                val keyHex = dbKey.joinToString("") { "%02x".format(it) }
                val escapedPath = tempEncryptedFile.absolutePath.replace("'", "''")
                plaintextDb.execSQL("ATTACH DATABASE '$escapedPath' AS encrypted KEY \"x'$keyHex'\";")
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
                android.database.sqlite.SQLiteDatabase.deleteDatabase(dbFile)
            } catch (_: Throwable) {}
            try {
                dbFile.delete()
                File("${dbFile.absolutePath}-wal").delete()
                File("${dbFile.absolutePath}-shm").delete()
                File("${dbFile.absolutePath}-journal").delete()
            } catch (_: Throwable) {}
        }
    }
}