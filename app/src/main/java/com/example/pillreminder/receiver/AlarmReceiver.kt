package com.example.pillreminder.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.pillreminder.MainActivity
import com.example.pillreminder.R
import com.example.pillreminder.data.database.PillReminderDatabase
import com.example.pillreminder.data.model.IntakeHistory
import com.example.pillreminder.data.model.IntakeStatus
import com.example.pillreminder.util.AlarmManagerUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

/**
 * 알람을 받아 처리하는 BroadcastReceiver
 */
class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID = "pill_reminder_channel"
        const val ACTION_TAKE_PILL = "com.example.pillreminder.TAKE_PILL"
        const val ACTION_SKIP_PILL = "com.example.pillreminder.SKIP_PILL"
        const val ACTION_SNOOZE = "com.example.pillreminder.SNOOZE"
        const val EXTRA_ALARM_ID = "ALARM_ID"
        const val EXTRA_PILL_ID = "PILL_ID"
        private const val SNOOZE_MINUTES = 10L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_TAKE_PILL -> handleIntake(context, intent, IntakeStatus.TAKEN)
                    ACTION_SKIP_PILL -> handleIntake(context, intent, IntakeStatus.SKIPPED)
                    ACTION_SNOOZE -> handleSnooze(context, intent)
                    else -> showNotification(context, intent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleIntake(context: Context, intent: Intent, status: IntakeStatus) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: return
        val pillId = intent.getStringExtra(EXTRA_PILL_ID) ?: return

        val database = PillReminderDatabase.getDatabase(context)
        val history = IntakeHistory(
            id = UUID.randomUUID().toString(),
            pillId = pillId,
            alarmId = alarmId,
            intakeTime = LocalDateTime.now(),
            status = status
        )
        database.intakeHistoryDao().insertHistory(history)

        // 알림 제거
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId.hashCode())
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: return
        val pillId = intent.getStringExtra(EXTRA_PILL_ID) ?: return

        // 알림 제거
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId.hashCode())

        // 10분 후에 다시 알람 스케줄

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_PILL_ID, pillId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "snooze_$alarmId".hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeTime = System.currentTimeMillis() + (SNOOZE_MINUTES * 60 * 1000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                snoozeTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                snoozeTime,
                pendingIntent
            )
        }
    }

    private suspend fun showNotification(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: return
        val pillId = intent.getStringExtra(EXTRA_PILL_ID) ?: return

        val database = PillReminderDatabase.getDatabase(context)
        val pill = database.pillDao().getPillById(pillId)

        pill?.let {
            // 다음 알람 스케줄링
            val alarm = database.pillAlarmDao().getAlarmById(alarmId)
            alarm?.let { pillAlarm ->
                val alarmManagerUtil = AlarmManagerUtil(context)
                alarmManagerUtil.scheduleAlarm(pillAlarm)
            }

            // 전체 화면 알람 Activity Intent
            val alarmActivityIntent = Intent(context, com.example.pillreminder.ui.alarm.AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_USER_ACTION
                putExtra(com.example.pillreminder.ui.alarm.AlarmActivity.EXTRA_PILL_ID, pillId)
                putExtra(com.example.pillreminder.ui.alarm.AlarmActivity.EXTRA_ALARM_ID, alarmId)
                putExtra(com.example.pillreminder.ui.alarm.AlarmActivity.EXTRA_PILL_NAME, it.name)
            }

            // Full Screen Intent용 PendingIntent
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                alarmId.hashCode(),
                alarmActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 화면 상태 확인
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                powerManager.isInteractive
            } else {
                @Suppress("DEPRECATION")
                powerManager.isScreenOn
            }

            // 화면이 켜져 있으면 직접 Activity 실행 (테스트용)
            if (isScreenOn) {
                context.startActivity(alarmActivityIntent)
            }

            // 복용 액션 인텐트
            val takeIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_TAKE_PILL
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_PILL_ID, pillId)
            }
            val takePendingIntent = PendingIntent.getBroadcast(
                context,
                "take_$alarmId".hashCode(),
                takeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 건너뛰기 액션 인텐트
            val skipIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_SKIP_PILL
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_PILL_ID, pillId)
            }
            val skipPendingIntent = PendingIntent.getBroadcast(
                context,
                "skip_$alarmId".hashCode(),
                skipIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 알림 생성 (Full Screen Intent 포함)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.alarm_notification_title))
                .setContentText(context.getString(R.string.alarm_notification_text, it.name))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true)  // 전체 화면 Intent
                .setContentIntent(fullScreenPendingIntent)
                .addAction(
                    R.drawable.ic_check,
                    context.getString(R.string.btn_take_pill),
                    takePendingIntent
                )
                .addAction(
                    R.drawable.ic_skip,
                    context.getString(R.string.btn_skip_pill),
                    skipPendingIntent
                )
                .build()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(alarmId.hashCode(), notification)
        }
    }
} 