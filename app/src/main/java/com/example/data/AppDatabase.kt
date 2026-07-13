package com.example.data

import android.content.Context
import androidx.room.*
import com.example.model.*

class Converters {
    @TypeConverter
    fun fromCallType(value: CallType): String = value.name

    @TypeConverter
    fun toCallType(value: String): CallType = CallType.valueOf(value)
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
        CallRecording::class
    ],
    version = 3,
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
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dialer_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
