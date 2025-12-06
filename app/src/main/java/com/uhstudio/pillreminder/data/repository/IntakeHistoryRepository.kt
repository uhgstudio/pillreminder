package com.uhstudio.pillreminder.data.repository

import com.uhstudio.pillreminder.data.dao.IntakeHistoryDao
import com.uhstudio.pillreminder.data.model.IntakeHistory
import com.uhstudio.pillreminder.data.model.IntakeHistoryWithPill
import com.uhstudio.pillreminder.data.model.IntakeStatus
import com.uhstudio.pillreminder.util.Result
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.time.LocalDateTime

/**
 * 복용 기록 관리를 위한 Repository
 */
class IntakeHistoryRepository(
    private val intakeHistoryDao: IntakeHistoryDao
) {
    /**
     * 특정 약의 복용 기록을 Flow로 반환
     */
    fun getHistoryForPill(pillId: String): Flow<List<IntakeHistory>> {
        return intakeHistoryDao.getHistoryForPill(pillId)
    }

    /**
     * 기간별 복용 기록을 Flow로 반환
     */
    fun getHistoryBetweenDates(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<IntakeHistory>> {
        return intakeHistoryDao.getHistoryBetweenDates(startDate, endDate)
    }

    /**
     * 특정 날짜의 복용 기록을 Flow로 반환
     */
    fun getHistoryForDate(date: LocalDateTime): Flow<List<IntakeHistory>> {
        return intakeHistoryDao.getHistoryForDate(date)
    }

    /**
     * 특정 날짜의 복용 기록 (약 정보 포함)을 Flow로 반환
     */
    fun getHistoryWithPillForDate(date: LocalDateTime): Flow<List<IntakeHistoryWithPill>> {
        return intakeHistoryDao.getHistoryWithPillForDate(date)
    }

    /**
     * 기간별 복용 날짜 목록을 Flow로 반환
     */
    fun getIntakeDates(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<LocalDateTime>> {
        return intakeHistoryDao.getIntakeDates(startDate, endDate)
    }

    /**
     * 복용 기록 추가
     */
    suspend fun addIntakeHistory(
        pillId: String,
        alarmId: String,
        status: IntakeStatus,
        intakeTime: LocalDateTime = LocalDateTime.now()
    ): Result<Unit> {
        return try {
            val history = IntakeHistory(
                id = java.util.UUID.randomUUID().toString(),
                pillId = pillId,
                alarmId = alarmId,
                intakeTime = intakeTime,
                status = status
            )

            intakeHistoryDao.insertHistory(history)
            Timber.d("addIntakeHistory: history added - pillId=$pillId, status=$status")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add intake history: pillId=$pillId")
            Result.Error(e, "복용 기록을 저장하는데 실패했습니다.")
        }
    }

    /**
     * 복용 기록 직접 추가 (전체 IntakeHistory 객체)
     */
    suspend fun addIntakeHistoryDirect(history: IntakeHistory): Result<Unit> {
        return try {
            intakeHistoryDao.insertHistory(history)
            Timber.d("addIntakeHistoryDirect: history added - ${history.id}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add intake history: ${history.id}")
            Result.Error(e, "복용 기록을 저장하는데 실패했습니다.")
        }
    }

    /**
     * 특정 날짜의 복용 횟수 조회
     */
    suspend fun getIntakeCountForDate(
        pillId: String,
        date: LocalDateTime
    ): Result<Int> {
        return try {
            val count = intakeHistoryDao.getIntakeCountForDate(pillId, date)
            Timber.d("getIntakeCountForDate: pillId=$pillId, date=$date, count=$count")
            Result.Success(count)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get intake count: pillId=$pillId, date=$date")
            Result.Error(e, "복용 횟수를 조회하는데 실패했습니다.")
        }
    }

    /**
     * 모든 복용 기록 조회 (한 번만)
     */
    suspend fun getAllHistoryOnce(): Result<List<IntakeHistory>> {
        return try {
            val history = intakeHistoryDao.getAllHistoryOnce()
            Timber.d("getAllHistoryOnce: ${history.size} records found")
            Result.Success(history)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get all history")
            Result.Error(e, "복용 기록을 불러오는데 실패했습니다.")
        }
    }

    /**
     * 복용 통계 계산
     */
    suspend fun calculateIntakeStats(
        pillId: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Result<IntakeStats> {
        return try {
            // 기간 내 전체 복용 기록 조회
            val allHistory = intakeHistoryDao.getAllHistoryOnce()
            val filteredHistory = allHistory.filter { history ->
                history.pillId == pillId &&
                !history.intakeTime.isBefore(startDate) &&
                !history.intakeTime.isAfter(endDate)
            }

            val totalCount = filteredHistory.size
            val takenCount = filteredHistory.count { it.status == IntakeStatus.TAKEN }
            val skippedCount = filteredHistory.count { it.status == IntakeStatus.SKIPPED }
            val adherenceRate = if (totalCount > 0) {
                (takenCount.toDouble() / totalCount.toDouble() * 100).toInt()
            } else {
                0
            }

            val stats = IntakeStats(
                totalCount = totalCount,
                takenCount = takenCount,
                skippedCount = skippedCount,
                adherenceRate = adherenceRate
            )

            Timber.d("calculateIntakeStats: $stats")
            Result.Success(stats)
        } catch (e: Exception) {
            Timber.e(e, "Failed to calculate intake stats: pillId=$pillId")
            Result.Error(e, "복용 통계를 계산하는데 실패했습니다.")
        }
    }
}

/**
 * 복용 통계 데이터 클래스
 */
data class IntakeStats(
    val totalCount: Int,      // 전체 복용 기회
    val takenCount: Int,      // 복용한 횟수
    val skippedCount: Int,    // 건너뛴 횟수
    val adherenceRate: Int    // 복용률 (%)
)
