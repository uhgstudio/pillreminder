package com.uhstudio.pillreminder.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.uhstudio.pillreminder.data.converter.Converters
import com.uhstudio.pillreminder.data.dao.AppSettingsDao
import com.uhstudio.pillreminder.data.dao.IntakeHistoryDao
import com.uhstudio.pillreminder.data.dao.PillAlarmDao
import com.uhstudio.pillreminder.data.dao.PillDao
import com.uhstudio.pillreminder.data.model.AppSettings
import com.uhstudio.pillreminder.data.model.IntakeHistory
import com.uhstudio.pillreminder.data.model.Pill
import com.uhstudio.pillreminder.data.model.PillAlarm

/**
 * 앱의 메인 데이터베이스
 */
@Database(
    entities = [
        Pill::class,
        PillAlarm::class,
        IntakeHistory::class,
        AppSettings::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PillReminderDatabase : RoomDatabase() {
    abstract fun pillDao(): PillDao
    abstract fun pillAlarmDao(): PillAlarmDao
    abstract fun intakeHistoryDao(): IntakeHistoryDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: PillReminderDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // PillAlarm 테이블에 alarmSoundUri 컬럼 추가
                db.execSQL("ALTER TABLE pill_alarms ADD COLUMN alarmSoundUri TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // AppSettings 테이블 생성
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_settings (
                        id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                        isPremiumUser INTEGER NOT NULL DEFAULT 0,
                        purchaseToken TEXT,
                        purchaseTime INTEGER,
                        adOnScreenVisitEnabled INTEGER NOT NULL DEFAULT 1,
                        adOnScreenVisitThreshold INTEGER NOT NULL DEFAULT 3,
                        adOnScreenVisitCounter INTEGER NOT NULL DEFAULT 0,
                        adOnAlarmCountEnabled INTEGER NOT NULL DEFAULT 1,
                        adOnAlarmCountThreshold INTEGER NOT NULL DEFAULT 5,
                        adOnTimeBased INTEGER NOT NULL DEFAULT 1,
                        lastAdShownTime INTEGER,
                        adTimeIntervalHours INTEGER NOT NULL DEFAULT 24,
                        totalScreenVisits INTEGER NOT NULL DEFAULT 0,
                        totalAlarmRegistrations INTEGER NOT NULL DEFAULT 0,
                        totalAppLaunches INTEGER NOT NULL DEFAULT 0,
                        adOnAppLaunchEnabled INTEGER NOT NULL DEFAULT 1,
                        adOnAppLaunchThreshold INTEGER NOT NULL DEFAULT 4
                    )
                """)

                // 기본 설정 레코드 삽입
                db.execSQL("INSERT INTO app_settings (id) VALUES (1)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // pill_alarms 테이블에 인덱스 추가
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pill_alarms_pillId ON pill_alarms(pillId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pill_alarms_enabled ON pill_alarms(enabled)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pill_alarms_hour_minute ON pill_alarms(hour, minute)")

                // intake_history 테이블에 인덱스 추가
                db.execSQL("CREATE INDEX IF NOT EXISTS index_intake_history_pillId ON intake_history(pillId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_intake_history_alarmId ON intake_history(alarmId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_intake_history_intakeTime ON intake_history(intakeTime)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. 새 컬럼 추가
                db.execSQL("ALTER TABLE pill_alarms ADD COLUMN scheduleType TEXT NOT NULL DEFAULT 'WEEKLY'")
                db.execSQL("ALTER TABLE pill_alarms ADD COLUMN scheduleConfig TEXT")
                db.execSQL("ALTER TABLE pill_alarms ADD COLUMN startDate TEXT")
                db.execSQL("ALTER TABLE pill_alarms ADD COLUMN endDate TEXT")
                db.execSQL("ALTER TABLE pill_alarms ADD COLUMN createdAt TEXT NOT NULL DEFAULT '2024-01-01T00:00:00'")
                db.execSQL("ALTER TABLE pill_alarms ADD COLUMN updatedAt TEXT NOT NULL DEFAULT '2024-01-01T00:00:00'")

                // 2. 새 인덱스 추가
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pill_alarms_scheduleType ON pill_alarms(scheduleType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pill_alarms_startDate ON pill_alarms(startDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pill_alarms_endDate ON pill_alarms(endDate)")

                // 3. 기존 repeatDays를 scheduleConfig로 변환하는 로직은 앱에서 처리
                //    (SQLite에서 복잡한 JSON 변환은 어려우므로, 앱 시작 시 Repository에서 처리)
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 제거된 스케줄 타입들을 새로운 타입으로 변환
                // INTERVAL_HOURS → DAILY
                db.execSQL("UPDATE pill_alarms SET scheduleType = 'DAILY' WHERE scheduleType = 'INTERVAL_HOURS'")

                // WEEKDAY_ONLY → WEEKLY (평일: 월~금)
                db.execSQL("""
                    UPDATE pill_alarms
                    SET scheduleType = 'WEEKLY',
                        scheduleConfig = '{"type":"com.uhstudio.pillreminder.data.model.ScheduleConfig.Weekly","days":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY"]}'
                    WHERE scheduleType = 'WEEKDAY_ONLY'
                """)

                // WEEKEND_ONLY → WEEKLY (주말: 토~일)
                db.execSQL("""
                    UPDATE pill_alarms
                    SET scheduleType = 'WEEKLY',
                        scheduleConfig = '{"type":"com.uhstudio.pillreminder.data.model.ScheduleConfig.Weekly","days":["SATURDAY","SUNDAY"]}'
                    WHERE scheduleType = 'WEEKEND_ONLY'
                """)

                // MONTHLY → DAILY (매월 특정일은 매일로 변환)
                db.execSQL("UPDATE pill_alarms SET scheduleType = 'DAILY' WHERE scheduleType = 'MONTHLY'")
            }
        }

        fun getDatabase(context: Context): PillReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PillReminderDatabase::class.java,
                    "pill_reminder_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 