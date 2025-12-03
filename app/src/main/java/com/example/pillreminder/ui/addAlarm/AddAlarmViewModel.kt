package com.example.pillreminder.ui.addAlarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillreminder.data.database.PillReminderDatabase
import com.example.pillreminder.data.model.PillAlarm
import com.example.pillreminder.util.AlarmManagerUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.UUID

class AddAlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val alarmDao = database.pillAlarmDao()
    private val alarmManager = AlarmManagerUtil(application)

    suspend fun getAlarm(alarmId: String): PillAlarm? {
        return alarmDao.getAlarmById(alarmId)
    }

    fun saveAlarm(
        pillId: String,
        hour: Int,
        minute: Int,
        repeatDays: Set<DayOfWeek>,
        alarmId: String? = null,
        onComplete: () -> Unit
    ) {
        val alarm = PillAlarm(
            id = alarmId ?: UUID.randomUUID().toString(),
            pillId = pillId,
            hour = hour,
            minute = minute,
            repeatDays = repeatDays,
            enabled = true
        )

        viewModelScope.launch {
            // 기존 알람이 있으면 먼저 취소
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