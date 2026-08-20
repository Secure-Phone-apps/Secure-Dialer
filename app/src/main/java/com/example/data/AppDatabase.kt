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
import com.example.model.*
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

class Converters {
    @TypeConverter
    fun fromCallType(value: CallType?): String = (value ?: CallType.INCOMING).name

    @TypeConverter
    fun toCallType(value: String?): CallType {
        if (value.isNullOrBlank()) return CallType.INCOMING
        return try {
            CallType.valueOf(value)
        } catch (e: Exception) {
            CallType.INCOMING
        }
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
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dialerDao(): DialerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appCtx = context.applicationContext
                val db = buildDatabase(appCtx)
                try {
                    // Test connection to ensure key and schema integrity
                    val helper = db.openHelper.writableDatabase
                    helper.query("SELECT 1").close()
                } catch (_: Exception) {
                    try { db.close() } catch (_: Exception) {}
                    try { appCtx.deleteDatabase("dialer_database") } catch (_: Exception) {}
                    val freshDb = buildDatabase(appCtx)
                    try {
                        freshDb.openHelper.writableDatabase.query("SELECT 1").close()
                    } catch (_: Exception) {
                    }
                    INSTANCE = freshDb
                    return@synchronized freshDb
                }
                INSTANCE = db
                db
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            SQLiteDatabase.loadLibs(context)
            val dbKey = DatabaseKeyManager.getDatabaseKey(context)
            val factory = SupportFactory(dbKey)
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "dialer_database"
            )
                .openHelperFactory(factory)
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE) // Avoid persistent unencrypted WAL files on disk
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }
    }
}
