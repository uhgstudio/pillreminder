package com.uhstudio.pillreminder.util

import com.uhstudio.pillreminder.data.dao.PillAlarmDao
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.data.model.ScheduleConfig
import com.uhstudio.pillreminder.data.model.ScheduleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.time.LocalDateTime

/**
 * 레거시 알람(repeatDays 기반)을 새 스케줄 시스템으로 마이그레이션하는 유틸리티
 */
class LegacyAlarmMigrator(
    private val pillAlarmDao: PillAlarmDao
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 레거시 알람을 새 스케줄 시스템으로 마이그레이션
     * @return 마이그레이션된 알람 수
     */
    suspend fun migrateAllLegacyAlarms(): Int = withContext(Dispatchers.IO) {
        try {
            val allAlarms = pillAlarmDao.getAllAlarmsOnce()
            var migratedCount = 0

            Timber.d("Starting legacy alarm migration, total alarms: ${allAlarms.size}")

            allAlarms.forEach { alarm ->
                if (needsMigration(alarm)) {
                    try {
                        val migratedAlarm = migrateAlarm(alarm)
                        pillAlarmDao.updateAlarm(migratedAlarm)
                        migratedCount++
                        Timber.d("Migrated alarm: ${alarm.id}")
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to migrate alarm: ${alarm.id}")
                    }
                }
            }

            Timber.i("Legacy alarm migration completed: $migratedCount alarms migrated")
            migratedCount
        } catch (e: Exception) {
            Timber.e(e, "Legacy alarm migration failed")
            0
        }
    }

    /**
     * 알람이 마이그레이션이 필요한지 확인
     */
    private fun needsMigration(alarm: PillAlarm): Boolean {
        // scheduleConfig가 null이거나 비어있고, repeatDays가 있으면 마이그레이션 필요
        @Suppress("DEPRECATION")
        return (alarm.scheduleConfig == null || alarm.scheduleConfig.isBlank()) &&
                alarm.repeatDays.isNotEmpty()
    }

    /**
     * 레거시 알람을 새 스케줄 시스템으로 변환
     */
    private fun migrateAlarm(alarm: PillAlarm): PillAlarm {
        @Suppress("DEPRECATION")
        val repeatDays = alarm.repeatDays

        // repeatDays를 ScheduleConfig.Weekly로 변환
        val weeklyConfig = ScheduleConfig.Weekly.from(repeatDays)
        val scheduleConfigJson = json.encodeToString(weeklyConfig)

        Timber.d("Migrating alarm ${alarm.id}: days=${repeatDays.map { it.name }} -> config=$scheduleConfigJson")

        return alarm.copy(
            scheduleType = ScheduleType.WEEKLY,
            scheduleConfig = scheduleConfigJson,
            updatedAt = LocalDateTime.now()
            // repeatDays는 하위 호환성을 위해 유지
        )
    }

    /**
     * 특정 알람 하나만 마이그레이션 (테스트용)
     */
    suspend fun migrateSingleAlarm(alarmId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val alarm = pillAlarmDao.getAlarmById(alarmId)
            if (alarm != null && needsMigration(alarm)) {
                val migratedAlarm = migrateAlarm(alarm)
                pillAlarmDao.updateAlarm(migratedAlarm)
                Timber.d("Single alarm migrated: $alarmId")
                true
            } else {
                Timber.d("Alarm does not need migration: $alarmId")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to migrate single alarm: $alarmId")
            false
        }
    }

    /**
     * 마이그레이션 필요한 알람 수 확인
     */
    suspend fun countLegacyAlarms(): Int = withContext(Dispatchers.IO) {
        try {
            val allAlarms = pillAlarmDao.getAllAlarmsOnce()
            allAlarms.count { needsMigration(it) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to count legacy alarms")
            0
        }
    }
}
