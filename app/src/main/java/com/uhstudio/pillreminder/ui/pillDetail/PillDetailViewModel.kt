package com.uhstudio.pillreminder.ui.pillDetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import com.uhstudio.pillreminder.data.model.Pill
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.util.AlarmManagerUtil
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
} 