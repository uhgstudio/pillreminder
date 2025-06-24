package com.example.pillreminder.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.pillreminder.data.model.PillAlarm
import com.example.pillreminder.receiver.AlarmReceiver
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * 알람 매니저 유틸리티 클래스
 */
class AlarmManagerUtil(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val TAG = "AlarmManagerUtil"

    /**
     * 알람을 설정합니다.
     * @param pillAlarm 설정할 알람 정보
     */
    fun scheduleAlarm(pillAlarm: PillAlarm) {
        Log.d(TAG, "알람 설정 시작: ${pillAlarm.id}, 시간: ${pillAlarm.hour}:${pillAlarm.minute}, 요일: ${pillAlarm.repeatDays}")
        
        // Android 12+ 에서 정확한 알람 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "정확한 알람 권한이 없습니다. 설정에서 권한을 허용해주세요.")
                return
            }
        }

        // 각 요일별로 알람 설정
        pillAlarm.repeatDays.forEach { dayOfWeek ->
            scheduleAlarmForDay(pillAlarm, dayOfWeek)
        }
        
        Log.d(TAG, "모든 알람 설정 완료: ${pillAlarm.id}")
    }

    private fun scheduleAlarmForDay(pillAlarm: PillAlarm, dayOfWeek: DayOfWeek) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, pillAlarm.hour)
            set(Calendar.MINUTE, pillAlarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // 해당 요일로 설정
            val targetDayOfWeek = when(dayOfWeek) {
                DayOfWeek.SUNDAY -> Calendar.SUNDAY
                DayOfWeek.MONDAY -> Calendar.MONDAY
                DayOfWeek.TUESDAY -> Calendar.TUESDAY
                DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
                DayOfWeek.THURSDAY -> Calendar.THURSDAY
                DayOfWeek.FRIDAY -> Calendar.FRIDAY
                DayOfWeek.SATURDAY -> Calendar.SATURDAY
            }
            
            // 다음 해당 요일로 설정
            while (get(Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
            
            // 현재 시간보다 이전이면 다음 주로 설정
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, pillAlarm.id)
            putExtra(AlarmReceiver.EXTRA_PILL_ID, pillAlarm.pillId)
        }

        val requestCode = "${pillAlarm.id}_${dayOfWeek.value}".hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // 정확한 알람 설정 (Android 6.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            val alarmTime = Calendar.getInstance().apply { timeInMillis = calendar.timeInMillis }
            Log.d(TAG, "알람 설정 완료: ${pillAlarm.id}, 요일: $dayOfWeek, 예정 시간: ${alarmTime.time}")
            
        } catch (e: Exception) {
            Log.e(TAG, "알람 설정 실패: ${pillAlarm.id}, 요일: $dayOfWeek", e)
        }
    }

    /**
     * 알람을 취소합니다.
     * @param pillAlarm 취소할 알람 정보
     */
    fun cancelAlarm(pillAlarm: PillAlarm) {
        Log.d(TAG, "알람 취소 시작: ${pillAlarm.id}")
        
        pillAlarm.repeatDays.forEach { dayOfWeek ->
            val intent = Intent(context, AlarmReceiver::class.java)
            val requestCode = "${pillAlarm.id}_${dayOfWeek.value}".hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "알람 취소 완료: ${pillAlarm.id}, 요일: $dayOfWeek")
        }
        
        Log.d(TAG, "모든 알람 취소 완료: ${pillAlarm.id}")
    }
} 