package com.example.pillreminder.ui.alarm

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillreminder.data.database.PillReminderDatabase
import com.example.pillreminder.data.model.PillAlarm
import com.example.pillreminder.receiver.AlarmReceiver
import com.example.pillreminder.util.AlarmUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.UUID

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val alarmDao = database.pillAlarmDao()
    private val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun getAlarmsForPill(pillId: String): Flow<List<PillAlarm>> {
        return alarmDao.getAlarmsForPill(pillId)
    }

    fun addAlarm(pillId: String, hour: Int, minute: Int, repeatDays: Set<DayOfWeek>) {
        val alarm = PillAlarm(
            id = UUID.randomUUID().toString(),
            pillId = pillId,
            hour = hour,
            minute = minute,
            repeatDays = repeatDays
        )
        viewModelScope.launch {
            alarmDao.insertAlarm(alarm)
            scheduleAlarm(alarm)
        }
    }

    fun deleteAlarm(alarm: PillAlarm) {
        viewModelScope.launch {
            alarmDao.deleteAlarm(alarm)
            cancelAlarm(alarm)
        }
    }

    fun updateAlarmEnabled(alarmId: String, enabled: Boolean) {
        viewModelScope.launch {
            alarmDao.updateAlarmEnabled(alarmId, enabled)
            val alarm = alarmDao.getAlarmById(alarmId)
            alarm?.let {
                if (enabled) {
                    scheduleAlarm(it)
                } else {
                    cancelAlarm(it)
                }
            }
        }
    }

    private fun scheduleAlarm(alarm: PillAlarm) {
        val intent = Intent(getApplication(), AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("PILL_ID", alarm.pillId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 다음 알람 시간 계산
        val nextAlarmTime = AlarmUtil.calculateNextAlarmTime(
            hour = alarm.hour,
            minute = alarm.minute,
            repeatDays = alarm.repeatDays
        )
        
        // 알람 설정
        val triggerAtMillis = AlarmUtil.calculateMillisToNextAlarm(nextAlarmTime)
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(
                System.currentTimeMillis() + triggerAtMillis,
                pendingIntent
            ),
            pendingIntent
        )
    }

    private fun cancelAlarm(alarm: PillAlarm) {
        val intent = Intent(getApplication(), AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
} 