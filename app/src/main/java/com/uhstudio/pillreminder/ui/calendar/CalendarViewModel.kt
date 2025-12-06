package com.uhstudio.pillreminder.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import com.uhstudio.pillreminder.data.model.IntakeHistoryWithPill
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalTime

/**
 * 캘린더 화면을 위한 ViewModel
 */
class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val intakeHistoryDao = database.intakeHistoryDao()
    private val pillDao = database.pillDao()

    /**
     * 특정 날짜의 복용 기록을 약 정보와 함께 가져옴
     */
    fun getIntakeHistoryForDate(date: LocalDate): Flow<List<IntakeHistoryWithPill>> {
        val startOfDay = date.atStartOfDay()
        return intakeHistoryDao.getHistoryWithPillForDate(startOfDay)
    }

    /**
     * 특정 기간의 복용 날짜들을 가져옴
     */
    fun getIntakeDatesInRange(startDate: LocalDate, endDate: LocalDate): Flow<Set<LocalDate>> {
        return intakeHistoryDao.getIntakeDates(
            startDate.atStartOfDay(),
            endDate.atTime(LocalTime.MAX)
        ).map { dates ->
            dates.map { it.toLocalDate() }.toSet()
        }
    }

    /**
     * 특정 날짜의 약별 복용 현황을 가져옴
     */
    fun getPillIntakeStatusForDate(date: LocalDate): Flow<Map<String, IntakeStatus>> {
        return combine(
            pillDao.getAllPills(),
            getIntakeHistoryForDate(date)
        ) { pills, history ->
            pills.associate { pill ->
                val pillHistory = history.filter { it.history.pillId == pill.id }
                val status = when {
                    pillHistory.any { it.history.status == com.uhstudio.pillreminder.data.model.IntakeStatus.TAKEN } ->
                        IntakeStatus.TAKEN
                    pillHistory.any { it.history.status == com.uhstudio.pillreminder.data.model.IntakeStatus.SKIPPED } ->
                        IntakeStatus.SKIPPED
                    else -> IntakeStatus.MISSED
                }
                pill.id to status
            }
        }
    }

    /**
     * 특정 월의 통계를 가져옴
     */
    fun getMonthlyStats(yearMonth: java.time.YearMonth): Flow<MonthlyStats> {
        val startDate = yearMonth.atDay(1).atStartOfDay()
        val endDate = yearMonth.atEndOfMonth().atTime(LocalTime.MAX)

        return intakeHistoryDao.getHistoryBetweenDates(startDate, endDate).map { histories ->
            val takenCount = histories.count { it.status == com.uhstudio.pillreminder.data.model.IntakeStatus.TAKEN }
            val skippedCount = histories.count { it.status == com.uhstudio.pillreminder.data.model.IntakeStatus.SKIPPED }
            val totalCount = histories.size

            val adherenceRate = if (totalCount > 0) {
                (takenCount.toFloat() / totalCount.toFloat()) * 100f
            } else {
                0f
            }

            MonthlyStats(
                totalCount = totalCount,
                takenCount = takenCount,
                skippedCount = skippedCount,
                adherenceRate = adherenceRate
            )
        }
    }

    /**
     * 특정 기간의 날짜별 상태 맵을 가져옴
     */
    fun getDateStatusMap(startDate: LocalDate, endDate: LocalDate): Flow<Map<LocalDate, DateStatus>> {
        return intakeHistoryDao.getHistoryBetweenDates(
            startDate.atStartOfDay(),
            endDate.atTime(LocalTime.MAX)
        ).map { histories ->
            histories
                .groupBy { it.intakeTime.toLocalDate() }
                .mapValues { (_, dayHistories) ->
                    when {
                        dayHistories.any { it.status == com.uhstudio.pillreminder.data.model.IntakeStatus.TAKEN } ->
                            DateStatus.TAKEN
                        dayHistories.any { it.status == com.uhstudio.pillreminder.data.model.IntakeStatus.SKIPPED } ->
                            DateStatus.SKIPPED
                        else -> DateStatus.NONE
                    }
                }
        }
    }
}

/**
 * 월간 복용 통계
 *
 * @property totalCount 총 복용 기록 개수
 * @property takenCount 복용 완료 개수
 * @property skippedCount 건너뜀 개수
 * @property adherenceRate 복용률 (0-100%)
 */
data class MonthlyStats(
    val totalCount: Int = 0,
    val takenCount: Int = 0,
    val skippedCount: Int = 0,
    val adherenceRate: Float = 0f
)

/**
 * 캘린더 날짜별 상태
 *
 * 복용 기록이 있는 날짜의 상태를 나타냅니다.
 */
enum class DateStatus {
    /** 복용 완료 */
    TAKEN,
    /** 건너뜀 */
    SKIPPED,
    /** 기록 없음 */
    NONE
}

/**
 * 캘린더에서 사용할 복용 상태
 */
enum class IntakeStatus {
    TAKEN,      // 복용함
    SKIPPED,    // 건너뜀
    MISSED      // 놓침
}