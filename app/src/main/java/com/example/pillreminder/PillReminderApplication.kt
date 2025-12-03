package com.example.pillreminder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * 앱 전체의 Application 클래스
 * 알림 채널을 앱 시작 시 한 번만 초기화
 */
class PillReminderApplication : Application() {

    companion object {
        const val CHANNEL_ID = "pill_reminder_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * 알림 채널 생성 (Android 8.0 이상)
     * 앱 시작 시 한 번만 호출되며, 이후에는 채널 설정이 유지됨
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 알람 전용 오디오 속성 설정
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)  // 알람 용도로 설정
                .build()

            // 알람 소리 URI 가져오기 (시스템 기본 알람음)
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH  // 높은 중요도 (소리 + 헤드업 알림)
            ).apply {
                description = getString(R.string.alarm_channel_description)

                // 알람 소리 설정 (명시적으로 알람음 지정)
                setSound(alarmSound, audioAttributes)

                // 진동 설정
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)  // 진동 패턴

                // LED 설정
                enableLights(true)
                lightColor = android.graphics.Color.RED

                // 잠금화면 표시
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC

                // 배지 표시
                setShowBadge(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)

            // 채널이 이미 존재하는 경우 재생성하지 않음
            // 사용자가 채널 설정을 변경했을 수 있으므로 기존 설정 유지
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
