package com.uhstudio.pillreminder.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uhstudio.pillreminder.ads.AdManager
import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import com.uhstudio.pillreminder.data.model.IntakeStatus
import com.uhstudio.pillreminder.data.model.Pill
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.util.ScheduleCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/**
 * 오늘의 알람 정보
 *
 * @property alarm 알람 설정
 * @property pill 약 정보
 * @property time 알람 시간
 * @property isTaken 복용 완료 여부
 */
data class TodayAlarm(
    val alarm: PillAlarm,
    val pill: Pill,
    val time: LocalDateTime,
    val isTaken: Boolean
)

/**
 * 오늘의 복용 통계
 *
 * @property totalCount 총 알람 개수
 * @property takenCount 복용 완료 개수
 * @property skippedCount 건너뜀 개수
 * @property adherenceRate 복용률 (0-100%)
 */
data class IntakeStats(
    val totalCount: Int = 0,
    val takenCount: Int = 0,
    val skippedCount: Int = 0,
    val adherenceRate: Float = 0f
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val pillDao = database.pillDao()
    private val alarmDao = database.pillAlarmDao()
    private val historyDao = database.intakeHistoryDao()
    private val adManager = AdManager.getInstance(application)

    // 광고 표시 여부 상태
    private val _shouldShowAd = MutableStateFlow(false)
    val shouldShowAd: StateFlow<Boolean> = _shouldShowAd.asStateFlow()

    fun onAdShown() {
        _shouldShowAd.value = false
    }

    val pills: Flow<List<Pill>> = pillDao.getAllPills()
    
    // 재고 부족 약 목록
    val lowStockPills: Flow<List<Pill>> = pillDao.getLowStockPills()

    private val _todayAlarms = MutableStateFlow<List<TodayAlarm>>(emptyList())
    val todayAlarms: StateFlow<List<TodayAlarm>> = _todayAlarms.asStateFlow()

    private val _todayStats = MutableStateFlow(IntakeStats())
    val todayStats: StateFlow<IntakeStats> = _todayStats.asStateFlow()

    private val _missedAlarmsCount = MutableStateFlow(0)
    val missedAlarmsCount: StateFlow<Int> = _missedAlarmsCount.asStateFlow()

    init {
        loadTodayAlarms()
        // todayAlarms가 로드된 후에 stats 계산
        viewModelScope.launch {
            _todayAlarms.collectLatest {
                updateTodayStats()
            }
        }
    }

    private fun loadTodayAlarms() {
        viewModelScope.launch {
            try {
                val allAlarms = alarmDao.getEnabledAlarmsOnce()
                val allPills = pillDao.getAllPillsOnce()
                val today = LocalDate.now()
                val now = LocalDateTime.now()

                // 오늘의 모든 알람 계산
                val todayAlarmsList = mutableListOf<TodayAlarm>()

                allAlarms.forEach { alarm ->
                    val nextTime = ScheduleCalculator.calculateNextAlarmTime(alarm, now.minusDays(1))
                    if (nextTime != null && nextTime.toLocalDate() == today) {
                        val pill = allPills.find { it.id == alarm.pillId }
                        if (pill != null) {
                            // 이 알람에 대한 복용 여부 확인 (alarmId 기준)
                            val startOfDay = today.atStartOfDay()
                            val endOfDay = today.plusDays(1).atStartOfDay()
                            val isTaken = historyDao.getIntakeCountForAlarm(alarm.id, startOfDay, endOfDay) > 0

                            todayAlarmsList.add(
                                TodayAlarm(
                                    alarm = alarm,
                                    pill = pill,
                                    time = nextTime,
                                    isTaken = isTaken
                                )
                            )
                        }
                    }
                }

                // 시간순 정렬
                _todayAlarms.value = todayAlarmsList.sortedBy { it.time }

                // 미복용 알람 카운트 (현재 시간 지난 것만)
                _missedAlarmsCount.value = todayAlarmsList.count {
                    !it.isTaken && it.time.isBefore(now)
                }
            } catch (e: Exception) {
                // 에러 처리
                _todayAlarms.value = emptyList()
            }
        }
    }

    private suspend fun updateTodayStats() {
        try {
            val today = LocalDateTime.now()
            val startOfDay = today.toLocalDate().atStartOfDay()
            val endOfDay = today.toLocalDate().plusDays(1).atStartOfDay()

            // one-shot 쿼리로 오늘의 복용 기록 조회
            val historyList = historyDao.getHistoryForDateOnce(startOfDay, endOfDay)
            val totalAlarms = _todayAlarms.value.size

            // 알람별로 중복 제거: 같은 alarmId는 한 번만 카운트
            val uniqueTaken = historyList
                .filter { it.status == IntakeStatus.TAKEN }
                .distinctBy { it.alarmId }
                .size

            val uniqueSkipped = historyList
                .filter { it.status == IntakeStatus.SKIPPED }
                .distinctBy { it.alarmId }
                .size

            val adherenceRate = if (totalAlarms > 0) {
                (uniqueTaken.toFloat() / totalAlarms.toFloat()) * 100f
            } else {
                0f
            }

            _todayStats.value = IntakeStats(
                totalCount = totalAlarms,
                takenCount = uniqueTaken,
                skippedCount = uniqueSkipped,
                adherenceRate = adherenceRate
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to update today stats")
            _todayStats.value = IntakeStats()
        }
    }

    fun deletePill(pill: Pill, onShowAd: () -> Unit = {}) {
        viewModelScope.launch {
            pillDao.deletePill(pill)

            // 삭제 완료 후 광고 표시 체크
            val shouldShow = adManager.incrementAndCheckAlarmRegistration()
            if (shouldShow) {
                adManager.loadInterstitialAd {
                    onShowAd()
                }
            }
        }
    }

    fun refreshData() {
        loadTodayAlarms()
        // updateTodayStats()는 _todayAlarms collectLatest에 의해 자동 트리거됨
    }
} 