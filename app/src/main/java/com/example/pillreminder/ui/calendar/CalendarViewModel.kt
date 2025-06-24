package com.example.pillreminder.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillreminder.data.database.PillReminderDatabase
import com.example.pillreminder.data.model.IntakeHistory
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 캘린더 화면을 위한 ViewModel
 */
class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val intakeHistoryDao = database.intakeHistoryDao()
    private val pillDao = database.pillDao()

    /**
     * 특정 날짜의 복용 기록을 가져옴
     */
    fun getIntakeHistoryForDate(date: LocalDate): Flow<List<IntakeHistory>> {
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.atTime(LocalTime.MAX)
        return intakeHistoryDao.getHistoryForDate(startOfDay)
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
                val pillHistory = history.filter { it.pillId == pill.id }
                val status = when {
                    pillHistory.any { it.status == com.example.pillreminder.data.model.IntakeStatus.TAKEN } ->
                        IntakeStatus.TAKEN
                    pillHistory.any { it.status == com.example.pillreminder.data.model.IntakeStatus.SKIPPED } ->
                        IntakeStatus.SKIPPED
                    else -> IntakeStatus.MISSED
                }
                pill.id to status
            }
        }
    }
}

/**
 * 캘린더에서 사용할 복용 상태
 */
enum class IntakeStatus {
    TAKEN,      // 복용함
    SKIPPED,    // 건너뜀
    MISSED      // 놓침
} 