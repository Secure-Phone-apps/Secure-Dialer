package com.example.data

import android.content.Context
import androidx.room.*
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.Contact

class Converters {
    @TypeConverter
    fun fromCallType(value: CallType): String = value.name

    @TypeConverter
    fun toCallType(value: String): CallType = CallType.valueOf(value)
}

@Database(entities = [Contact::class, CallRecord::class], version = 1, exportSchema = false)
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
