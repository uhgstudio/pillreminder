package com.example.pillreminder.ui.pillDetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillreminder.data.database.PillReminderDatabase
import com.example.pillreminder.data.model.Pill
import com.example.pillreminder.data.model.PillAlarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PillDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val pillDao = database.pillDao()
    private val alarmDao = database.pillAlarmDao()

    private val _pill = MutableStateFlow<Pill?>(null)
    val pill: StateFlow<Pill?> = _pill

    fun getAlarms(pillId: String): Flow<List<PillAlarm>> {
        return alarmDao.getAlarmsForPill(pillId)
    }

    fun loadPill(pillId: String) {
        viewModelScope.launch {
            _pill.value = pillDao.getPillById(pillId)
        }
    }

    fun deletePill(pill: Pill, onComplete: () -> Unit) {
        viewModelScope.launch {
            pillDao.deletePill(pill)
            onComplete()
        }
    }
} 