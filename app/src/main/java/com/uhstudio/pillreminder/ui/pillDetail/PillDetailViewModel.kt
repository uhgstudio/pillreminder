package com.uhstudio.pillreminder.ui.pillDetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import com.uhstudio.pillreminder.data.model.IntakeHistory
import com.uhstudio.pillreminder.data.model.IntakeStatus
import com.uhstudio.pillreminder.data.model.Pill
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.data.model.ScheduleType
import com.uhstudio.pillreminder.data.model.ScheduleConfig
import com.uhstudio.pillreminder.util.AlarmManagerUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.util.UUID
import java.time.LocalDate

class PillDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val pillDao = database.pillDao()
    private val alarmDao = database.pillAlarmDao()
    private val historyDao = database.intakeHistoryDao()

    private val _pill = MutableStateFlow<Pill?>(null)
    val pill: StateFlow<Pill?> = _pill

    fun getAlarms(pillId: String): Flow<List<PillAlarm>> {
        return alarmDao.getAlarmsForPill(pillId)
    }

    /**
     * 특정 약의 복용 기록 가져오기
     */
    fun getIntakeHistory(pillId: String): Flow<List<IntakeHistory>> {
        return historyDao.getHistoryForPill(pillId)
    }

    /**
     * 복용 상태 업데이트
     */
    fun updateIntakeStatus(historyId: String, status: IntakeStatus) {
        viewModelScope.launch {
            val history = historyDao.getHistoryById(historyId)
            history?.let {
                historyDao.updateHistory(it.copy(status = status))
            }
        }
    }

    fun loadPill(pillId: String) {
        viewModelScope.launch {
            _pill.value = pillDao.getPillById(pillId)
        }
    }

    fun deletePill(pill: Pill, onComplete: () -> Unit) {
        viewModelScope.launch {
            // 연관된 알람들 취소
            val alarms = alarmDao.getAlarmsForPillOnce(pill.id)
            val alarmManagerUtil = AlarmManagerUtil(getApplication())
            alarms.forEach { alarm ->
                alarmManagerUtil.cancelAlarm(alarm)
            }

            // 약 삭제 (CASCADE로 알람도 DB에서 삭제됨)
            pillDao.deletePill(pill)
            onComplete()
        }
    }

    fun deleteAlarm(alarm: PillAlarm) {
        viewModelScope.launch {
            val alarmManagerUtil = AlarmManagerUtil(getApplication())
            alarmManagerUtil.cancelAlarm(alarm)
            alarmDao.deleteAlarm(alarm)
        }
    }

    fun toggleAlarm(alarm: PillAlarm, enabled: Boolean) {
        viewModelScope.launch {
            // Update database
            alarmDao.updateAlarmEnabled(alarm.id, enabled)
            
            // Update the alarm to be scheduled/canceled
            val updatedAlarm = alarm.copy(enabled = enabled)
            val alarmManagerUtil = AlarmManagerUtil(getApplication())
            if (enabled) {
                alarmManagerUtil.scheduleAlarm(updatedAlarm)
            } else {
                alarmManagerUtil.cancelAlarm(updatedAlarm)
            }
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun saveAlarm(
        pillId: String,
        hour: Int,
        minute: Int,
        scheduleType: ScheduleType,
        scheduleConfig: ScheduleConfig,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        alarmSoundUri: String? = null,
        alarmId: String? = null,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val scheduleConfigJson = json.encodeToString(scheduleConfig)
            val now = LocalDateTime.now()
            
            val alarm = PillAlarm(
                id = alarmId ?: UUID.randomUUID().toString(),
                pillId = pillId,
                hour = hour,
                minute = minute,
                scheduleType = scheduleType,
                scheduleConfig = scheduleConfigJson,
                startDate = startDate,
                endDate = endDate,
                repeatDays = emptySet(),
                enabled = true,
                alarmSoundUri = alarmSoundUri,
                createdAt = now,
                updatedAt = now
            )

            val alarmManager = AlarmManagerUtil(getApplication())
            
            if (alarmId != null) {
                alarmDao.getAlarmById(alarmId)?.let { oldAlarm ->
                    alarmManager.cancelAlarm(oldAlarm)
                }
            }

            alarmDao.insertAlarm(alarm)
            alarmManager.scheduleAlarm(alarm)
            onComplete()
        }
    }
}