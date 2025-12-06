package com.uhstudio.pillreminder.data.dao

import androidx.room.*
import com.uhstudio.pillreminder.data.model.PillAlarm
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek

/**
 * 약 알람 정보에 대한 데이터베이스 접근 인터페이스
 */
@Dao
interface PillAlarmDao {
    @Query("SELECT * FROM pill_alarms")
    fun getAllAlarms(): Flow<List<PillAlarm>>

    @Query("SELECT * FROM pill_alarms")
    suspend fun getAllAlarmsOnce(): List<PillAlarm>

    @Query("SELECT * FROM pill_alarms WHERE pillId = :pillId")
    fun getAlarmsForPill(pillId: String): Flow<List<PillAlarm>>

    @Query("SELECT * FROM pill_alarms WHERE id = :alarmId")
    suspend fun getAlarmById(alarmId: String): PillAlarm?

    @Query("SELECT * FROM pill_alarms WHERE enabled = 1")
    fun getEnabledAlarms(): Flow<List<PillAlarm>>

    @Query("SELECT * FROM pill_alarms WHERE enabled = 1")
    suspend fun getEnabledAlarmsOnce(): List<PillAlarm>

    @Query("SELECT * FROM pill_alarms WHERE pillId = :pillId")
    suspend fun getAlarmsForPillOnce(pillId: String): List<PillAlarm>

    @Query("SELECT * FROM pill_alarms WHERE repeatDays LIKE '%' || :dayOfWeek || '%'")
    fun getAlarmsForDay(dayOfWeek: DayOfWeek): Flow<List<PillAlarm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: PillAlarm)

    @Update
    suspend fun updateAlarm(alarm: PillAlarm)

    @Delete
    suspend fun deleteAlarm(alarm: PillAlarm)

    @Query("UPDATE pill_alarms SET enabled = :enabled WHERE id = :alarmId")
    suspend fun updateAlarmEnabled(alarmId: String, enabled: Boolean)
} 