package com.example.pillreminder.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.pillreminder.data.model.PillAlarm
import com.example.pillreminder.receiver.AlarmReceiver
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * 알람 매니저 유틸리티 클래스
 */
class AlarmManagerUtil(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 알람을 설정합니다.
     * @param pillAlarm 설정할 알람 정보
     */
    fun scheduleAlarm(pillAlarm: PillAlarm) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, pillAlarm.hour)
            set(Calendar.MINUTE, pillAlarm.minute)
            set(Calendar.SECOND, 0)
        }

        // 현재 시간보다 이전이면 다음 날로 설정
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", pillAlarm.id)
            putExtra("PILL_ID", pillAlarm.pillId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            pillAlarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 매일 반복되는 알람 설정
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    /**
     * 알람을 취소합니다.
     * @param pillAlarm 취소할 알람 정보
     */
    fun cancelAlarm(pillAlarm: PillAlarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            pillAlarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
} 