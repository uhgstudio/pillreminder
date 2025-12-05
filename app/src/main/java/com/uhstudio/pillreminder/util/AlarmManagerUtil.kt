package com.example.pillreminder.util

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.example.pillreminder.data.model.PillAlarm
import com.example.pillreminder.receiver.AlarmReceiver
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.*

/**
 * 알람 매니저 유틸리티 클래스
 */
class AlarmManagerUtil(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 알람을 설정합니다.
     * @param pillAlarm 설정할 알람 정보
     * @return 알람 설정 성공 여부 (권한 없으면 false)
     */
    fun scheduleAlarm(pillAlarm: PillAlarm): Boolean {
        if (!pillAlarm.enabled || pillAlarm.repeatDays.isEmpty()) {
            return false
        }

        // Android 12+ (API 31)에서 정확한 알람 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // 정확한 알람 권한이 없으면 스케줄링 실패
                // 약 복용 알람은 정확한 시간이 중요하므로 대체 방법 사용하지 않음
                return false
            }
        }

        val nextAlarmTime = calculateNextAlarmTime(pillAlarm)
        val triggerTimeMillis = nextAlarmTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

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

        // 정확한 알람 설정 (Doze 모드에서도 작동)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }

        return true
    }

    /**
     * 다음 알람 시간을 계산합니다.
     */
    private fun calculateNextAlarmTime(pillAlarm: PillAlarm): LocalDateTime {
        val now = LocalDateTime.now()
        val todayAlarmTime = now
            .withHour(pillAlarm.hour)
            .withMinute(pillAlarm.minute)
            .withSecond(0)
            .withNano(0)

        // 오늘의 요일 확인
        val todayDayOfWeek = now.dayOfWeek

        // 오늘이 반복 요일에 포함되고 아직 알람 시간이 지나지 않았으면 오늘 알람
        if (todayDayOfWeek in pillAlarm.repeatDays && todayAlarmTime.isAfter(now)) {
            return todayAlarmTime
        }

        // 다음 반복 요일 찾기
        var nextDateTime = todayAlarmTime.plusDays(1)
        for (i in 1..7) {
            if (nextDateTime.dayOfWeek in pillAlarm.repeatDays) {
                return nextDateTime
            }
            nextDateTime = nextDateTime.plusDays(1)
        }

        // 기본값 (반복 요일이 없는 경우 - 실제로는 발생하지 않음)
        return todayAlarmTime.plusDays(1)
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

    /**
     * 알람 ID로 취소합니다.
     */
    fun cancelAlarmById(alarmId: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * 정확한 알람 권한이 있는지 확인합니다 (Android 12+).
     * @return 권한이 있으면 true, 없거나 필요 없으면 true (Android 12 미만)
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Android 12 미만에서는 권한 불필요
        }
    }

    /**
     * 정확한 알람 권한 설정 화면으로 이동합니다 (Android 12+).
     */
    fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * 배터리 최적화가 해제되어 있는지 확인합니다.
     * @return 배터리 최적화가 해제되어 있으면 true
     */
    fun isBatteryOptimizationDisabled(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * 배터리 최적화 해제 요청 화면으로 이동합니다.
     */
    fun requestDisableBatteryOptimization() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * 전체 화면 알림 권한이 있는지 확인합니다 (Android 12+).
     * @return 권한이 있으면 true
     */
    fun canUseFullScreenIntent(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ (API 34)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.canUseFullScreenIntent()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12-13 (API 31-33)은 USE_FULL_SCREEN_INTENT 권한이 자동으로 부여됨
            true
        } else {
            // Android 12 미만은 권한 불필요
            true
        }
    }

    /**
     * 전체 화면 알림 권한 설정 화면으로 이동합니다 (Android 14+).
     */
    fun requestFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
} 