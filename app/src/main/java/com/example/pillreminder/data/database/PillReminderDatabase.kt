package com.example.pillreminder.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.pillreminder.data.converter.Converters
import com.example.pillreminder.data.dao.IntakeHistoryDao
import com.example.pillreminder.data.dao.PillAlarmDao
import com.example.pillreminder.data.dao.PillDao
import com.example.pillreminder.data.model.IntakeHistory
import com.example.pillreminder.data.model.Pill
import com.example.pillreminder.data.model.PillAlarm

/**
 * 앱의 메인 데이터베이스
 */
@Database(
    entities = [
        Pill::class,
        PillAlarm::class,
        IntakeHistory::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PillReminderDatabase : RoomDatabase() {
    abstract fun pillDao(): PillDao
    abstract fun pillAlarmDao(): PillAlarmDao
    abstract fun intakeHistoryDao(): IntakeHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: PillReminderDatabase? = null

        fun getDatabase(context: Context): PillReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PillReminderDatabase::class.java,
                    "pill_reminder_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 