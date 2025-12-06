package com.uhstudio.pillreminder.ui.addAlarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.util.AlarmManagerUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

class EditAlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val alarmDao = database.pillAlarmDao()
    private val alarmManagerUtil = AlarmManagerUtil(application)

    private val _alarm = MutableStateFlow<PillAlarm?>(null)
    val alarm: StateFlow<PillAlarm?> = _alarm

    private val _selectedTime = MutableStateFlow(LocalTime.now())
    val selectedTime: StateFlow<LocalTime> = _selectedTime

    private val _selectedDays = MutableStateFlow(setOf<Int>())
    val selectedDays: StateFlow<Set<Int>> = _selectedDays

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadAlarm(alarmId: String) {
        viewModelScope.launch {
            val alarm = alarmDao.getAlarmById(alarmId)
            alarm?.let {
                _alarm.value = it
                _selectedTime.value = LocalTime.of(it.hour, it.minute)
                _selectedDays.value = it.repeatDays.map { dayOfWeek ->
                    when (dayOfWeek) {
                        DayOfWeek.MONDAY -> 1
                        DayOfWeek.TUESDAY -> 2
                        DayOfWeek.WEDNESDAY -> 3
                        DayOfWeek.THURSDAY -> 4
                        DayOfWeek.FRIDAY -> 5
                        DayOfWeek.SATURDAY -> 6
                        DayOfWeek.SUNDAY -> 7
                    }
                }.toSet()
            }
        }
    }

    fun updateTime(time: LocalTime) {
        _selectedTime.value = time
    }

    fun toggleDay(day: Int) {
        val currentDays = _selectedDays.value.toMutableSet()
        if (currentDays.contains(day)) {
            currentDays.remove(day)
        } else {
            currentDays.add(day)
        }
        _selectedDays.value = currentDays
    }

    fun updateAlarm(onSuccess: () -> Unit) {
        val currentAlarm = _alarm.value ?: return
        
        if (_selectedDays.value.isEmpty()) {
            return
        }

        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                // 기존 알람 취소
                alarmManagerUtil.cancelAlarm(currentAlarm)
                
                val repeatDays = _selectedDays.value.map { day ->
                    when (day) {
                        1 -> DayOfWeek.MONDAY
                        2 -> DayOfWeek.TUESDAY
                        3 -> DayOfWeek.WEDNESDAY
                        4 -> DayOfWeek.THURSDAY
                        5 -> DayOfWeek.FRIDAY
                        6 -> DayOfWeek.SATURDAY
                        7 -> DayOfWeek.SUNDAY
                        else -> DayOfWeek.MONDAY
                    }
                }.toSet()
                
                val updatedAlarm = currentAlarm.copy(
                    hour = _selectedTime.value.hour,
                    minute = _selectedTime.value.minute,
                    repeatDays = repeatDays
                )
                
                // 데이터베이스 업데이트
                alarmDao.updateAlarm(updatedAlarm)
                
                // 새 알람 설정
                alarmManagerUtil.scheduleAlarm(updatedAlarm)
                
                onSuccess()
            } finally {
                _isLoading.value = false
            }
        }
    }
} 