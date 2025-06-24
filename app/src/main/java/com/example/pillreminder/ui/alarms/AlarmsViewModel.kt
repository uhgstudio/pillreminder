package com.example.pillreminder.ui.alarms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.pillreminder.data.database.PillReminderDatabase
import com.example.pillreminder.data.model.PillAlarm
import kotlinx.coroutines.flow.Flow

class AlarmsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val alarmDao = database.pillAlarmDao()

    val allAlarms: Flow<List<PillAlarm>> = alarmDao.getAllAlarms()

    fun getEnabledAlarms(): Flow<List<PillAlarm>> {
        return alarmDao.getEnabledAlarms()
    }
} 