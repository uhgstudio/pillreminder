package com.example.pillreminder.data.dao

import androidx.room.*
import com.example.pillreminder.data.model.IntakeHistory
import com.example.pillreminder.data.model.IntakeHistoryWithPill
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
        WHERE date(intakeTime) >= date(:startDate)
        AND date(intakeTime) <= date(:endDate)
        ORDER BY intakeTime DESC
    """)
    fun getHistoryBetweenDates(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<IntakeHistory>>

    @Query("""
        SELECT * FROM intake_history
        WHERE date(intakeTime) = date(:date)
        ORDER BY intakeTime DESC
    """)
    fun getHistoryForDate(date: LocalDateTime): Flow<List<IntakeHistory>>

    @Transaction
    @Query("""
        SELECT * FROM intake_history
        WHERE date(intakeTime) = date(:date)
        ORDER BY intakeTime DESC
    """)
    fun getHistoryWithPillForDate(date: LocalDateTime): Flow<List<IntakeHistoryWithPill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: IntakeHistory)

    @Query("""
        SELECT COUNT(*) FROM intake_history 
        WHERE pillId = :pillId 
        AND date(intakeTime) = date(:date)
        AND status = 'TAKEN'
    """)
    suspend fun getIntakeCountForDate(pillId: String, date: LocalDateTime): Int

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