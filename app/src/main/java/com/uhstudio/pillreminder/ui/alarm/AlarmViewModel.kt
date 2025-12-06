package com.uhstudio.pillreminder.ui.alarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.util.AlarmManagerUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.UUID

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val alarmDao = database.pillAlarmDao()
    private val alarmManagerUtil = AlarmManagerUtil(application)

    fun getAlarmsForPill(pillId: String): Flow<List<PillAlarm>> {
        return alarmDao.getAlarmsForPill(pillId)
    }

    fun addAlarm(pillId: String, hour: Int, minute: Int, repeatDays: Set<DayOfWeek>) {
        val alarm = PillAlarm(
            id = UUID.randomUUID().toString(),
            pillId = pillId,
            hour = hour,
            minute = minute,
            repeatDays = repeatDays,
            enabled = true
        )
        viewModelScope.launch {
            alarmDao.insertAlarm(alarm)
            alarmManagerUtil.scheduleAlarm(alarm)
        }
    }

    fun deleteAlarm(alarm: PillAlarm) {
        viewModelScope.launch {
            alarmManagerUtil.cancelAlarm(alarm)
            alarmDao.deleteAlarm(alarm)
        }
    }

    fun updateAlarmEnabled(alarmId: String, enabled: Boolean) {
        viewModelScope.launch {
            alarmDao.updateAlarmEnabled(alarmId, enabled)
            val alarm = alarmDao.getAlarmById(alarmId)
            alarm?.let {
                if (enabled) {
                    alarmManagerUtil.scheduleAlarm(it)
                } else {
                    alarmManagerUtil.cancelAlarm(it)
                }
            }
        }
    }
} 