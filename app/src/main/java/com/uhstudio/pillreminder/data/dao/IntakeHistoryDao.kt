package com.uhstudio.pillreminder.data.dao

import androidx.room.*
import com.uhstudio.pillreminder.data.model.IntakeHistory
import com.uhstudio.pillreminder.data.model.IntakeHistoryWithPill
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 복용 기록에 대한 데이터베이스 접근 인터페이스
 */
@Dao
interface IntakeHistoryDao {
    @Query("SELECT * FROM intake_history ORDER BY intakeTime DESC")
    suspend fun getAllHistoryOnce(): List<IntakeHistory>

    @Query("SELECT * FROM intake_history WHERE pillId = :pillId ORDER BY intakeTime DESC")
    fun getHistoryForPill(pillId: String): Flow<List<IntakeHistory>>

    @Query("""
        SELECT * FROM intake_history
        WHERE intakeTime >= :startDate AND intakeTime <= :endDate
        ORDER BY intakeTime DESC
    """)
    fun getHistoryBetweenDates(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<IntakeHistory>>

    @Query("""
        SELECT * FROM intake_history
        WHERE intakeTime >= :startOfDay AND intakeTime < :endOfDay
        ORDER BY intakeTime DESC
    """)
    fun getHistoryForDate(startOfDay: LocalDateTime, endOfDay: LocalDateTime): Flow<List<IntakeHistory>>

    @Transaction
    @Query("""
        SELECT intake_history.* FROM intake_history
        INNER JOIN pills ON intake_history.pillId = pills.id
        WHERE intake_history.intakeTime >= :startOfDay
        AND intake_history.intakeTime < :endOfDay
        ORDER BY intake_history.intakeTime DESC
    """)
    fun getHistoryWithPillForDate(startOfDay: LocalDateTime, endOfDay: LocalDateTime): Flow<List<IntakeHistoryWithPill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: IntakeHistory)

    @Update
    suspend fun updateHistory(history: IntakeHistory)

    @Query("SELECT * FROM intake_history WHERE id = :historyId")
    suspend fun getHistoryById(historyId: String): IntakeHistory?

    @Query("""
        SELECT COUNT(*) FROM intake_history
        WHERE pillId = :pillId
        AND intakeTime >= :startOfDay AND intakeTime < :endOfDay
        AND status = 'TAKEN'
    """)
    suspend fun getIntakeCountForDate(pillId: String, startOfDay: LocalDateTime, endOfDay: LocalDateTime): Int

    @Query("""
        SELECT COUNT(*) FROM intake_history
        WHERE alarmId = :alarmId
        AND intakeTime >= :startOfDay AND intakeTime < :endOfDay
        AND status = 'TAKEN'
    """)
    suspend fun getIntakeCountForAlarm(alarmId: String, startOfDay: LocalDateTime, endOfDay: LocalDateTime): Int

    @Transaction
    @Query("""
        SELECT DISTINCT date(intakeTime) as date
        FROM intake_history
        WHERE status = 'TAKEN'
        AND date(intakeTime) >= date(:startDate)
        AND date(intakeTime) <= date(:endDate)
    """)
    fun getIntakeDates(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<LocalDateTime>>
} 