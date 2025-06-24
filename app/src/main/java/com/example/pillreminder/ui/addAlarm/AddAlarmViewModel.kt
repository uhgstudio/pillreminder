package com.example.pillreminder.ui.addAlarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillreminder.data.database.PillReminderDatabase
import com.example.pillreminder.data.model.PillAlarm
import com.example.pillreminder.util.AlarmManagerUtil
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.UUID

class AddAlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val alarmDao = database.pillAlarmDao()
    private val alarmManager = AlarmManagerUtil(application)

    fun saveAlarm(
        pillId: String,
        hour: Int,
        minute: Int,
        repeatDays: Set<DayOfWeek>,
        onComplete: () -> Unit
    ) {
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
            alarmManager.scheduleAlarm(alarm)
            onComplete()
        }
    }
} 