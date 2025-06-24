package com.example.pillreminder.ui.alarms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillreminder.data.database.PillReminderDatabase
import com.example.pillreminder.data.model.PillAlarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AlarmsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val alarmDao = database.pillAlarmDao()
    private val pillDao = database.pillDao()

    val allAlarms: Flow<List<PillAlarm>> = alarmDao.getAllAlarms()
    val allPills = pillDao.getAllPills()

    fun getEnabledAlarms(): Flow<List<PillAlarm>> {
        return alarmDao.getEnabledAlarms()
    }

    fun toggleAlarm(alarmId: String, enabled: Boolean) {
        viewModelScope.launch {
            alarmDao.updateAlarmEnabled(alarmId, enabled)
        }
    }
} 