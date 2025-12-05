package com.example.pillreminder.ui.alarms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillreminder.data.database.PillReminderDatabase
import com.example.pillreminder.data.model.PillAlarm
import com.example.pillreminder.util.AlarmManagerUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AlarmsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val alarmDao = database.pillAlarmDao()
    private val pillDao = database.pillDao()
    private val alarmManagerUtil = AlarmManagerUtil(application)

    val allAlarms: Flow<List<PillAlarm>> = alarmDao.getAllAlarms()
    val allPills = pillDao.getAllPills()

    fun getEnabledAlarms(): Flow<List<PillAlarm>> {
        return alarmDao.getEnabledAlarms()
    }

    fun toggleAlarm(alarm: PillAlarm, enabled: Boolean) {
        viewModelScope.launch {
            // 데이터베이스 업데이트
            alarmDao.updateAlarmEnabled(alarm.id, enabled)

            // 알람 스케줄/취소
            if (enabled) {
                val updatedAlarm = alarm.copy(enabled = true)
                alarmManagerUtil.scheduleAlarm(updatedAlarm)
            } else {
                alarmManagerUtil.cancelAlarm(alarm)
            }
        }
    }

    fun deleteAlarm(alarm: PillAlarm) {
        viewModelScope.launch {
            alarmManagerUtil.cancelAlarm(alarm)
            alarmDao.deleteAlarm(alarm)
        }
    }
} 