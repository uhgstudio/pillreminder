package com.example.pillreminder.ui.pillDetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillreminder.data.database.PillReminderDatabase
import com.example.pillreminder.data.model.Pill
import com.example.pillreminder.data.model.PillAlarm
import com.example.pillreminder.util.AlarmManagerUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PillDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val pillDao = database.pillDao()
    private val alarmDao = database.pillAlarmDao()
    private val alarmManagerUtil = AlarmManagerUtil(application)

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
            // 관련된 모든 알람들을 먼저 시스템에서 취소
            val alarms = alarmDao.getAlarmsForPill(pill.id)
            alarms.collect { alarmList ->
                alarmList.forEach { alarm ->
                    alarmManagerUtil.cancelAlarm(alarm)
                }
            }
            
            pillDao.deletePill(pill)
            onComplete()
        }
    }

    fun deleteAlarm(alarm: PillAlarm) {
        viewModelScope.launch {
            // 시스템 알람 취소
            alarmManagerUtil.cancelAlarm(alarm)
            // 데이터베이스에서 삭제
            alarmDao.deleteAlarm(alarm)
        }
    }
} 