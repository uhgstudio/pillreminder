package com.uhstudio.pillreminder.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.uhstudio.pillreminder.R
import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import com.uhstudio.pillreminder.data.model.IntakeHistory
import com.uhstudio.pillreminder.data.model.IntakeStatus
import com.uhstudio.pillreminder.util.AlarmManagerUtil
import com.uhstudio.pillreminder.util.RequestCodeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

/**
 * 알람을 받아 처리하는 BroadcastReceiver
 */
class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID = "pill_reminder_channel"
        const val ACTION_TAKE_PILL = "com.uhstudio.pillreminder.TAKE_PILL"
        const val ACTION_SKIP_PILL = "com.uhstudio.pillreminder.SKIP_PILL"
        const val ACTION_SNOOZE = "com.uhstudio.pillreminder.SNOOZE"
        const val EXTRA_ALARM_ID = "ALARM_ID"
        const val EXTRA_PILL_ID = "PILL_ID"
        private const val SNOOZE_MINUTES = 10L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Timber.d("AlarmReceiver.onReceive: action=${intent.action}")
                when (intent.action) {
                    ACTION_TAKE_PILL -> handleIntake(context, intent, IntakeStatus.TAKEN)
                    ACTION_SKIP_PILL -> handleIntake(context, intent, IntakeStatus.SKIPPED)
                    ACTION_SNOOZE -> handleSnooze(context, intent)
                    else -> showNotification(context, intent)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in AlarmReceiver.onReceive: action=${intent.action}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleIntake(context: Context, intent: Intent, status: IntakeStatus) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        val pillId = intent.getStringExtra(EXTRA_PILL_ID)

        if (alarmId == null || pillId == null) {
            Timber.e("handleIntake: Missing alarmId or pillId - alarmId=$alarmId, pillId=$pillId")
            return
        }

        try {
            val database = PillReminderDatabase.getDatabase(context)
            val history = IntakeHistory(
                id = UUID.randomUUID().toString(),
                pillId = pillId,
                alarmId = alarmId,
                intakeTime = LocalDateTime.now(),
                status = status
            )
            database.intakeHistoryDao().insertHistory(history)
            Timber.d("Intake history saved: pillId=$pillId, status=$status")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save intake history: pillId=$pillId, status=$status")
            // 데이터베이스 저장 실패해도 알림은 제거
        }

        try {
            // 알림 제거
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null) {
                notificationManager.cancel(RequestCodeUtil.generateRequestCode(alarmId))
                Timber.d("Notification cancelled: alarmId=$alarmId")
            } else {
                Timber.e("NotificationManager is null, cannot cancel notification")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel notification: alarmId=$alarmId")
        }
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        val pillId = intent.getStringExtra(EXTRA_PILL_ID)

        if (alarmId == null || pillId == null) {
            Timber.e("handleSnooze: Missing alarmId or pillId - alarmId=$alarmId, pillId=$pillId")
            return
        }

        try {
            // 알림 제거
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(RequestCodeUtil.generateRequestCode(alarmId))
                ?: Timber.e("NotificationManager is null in handleSnooze")

            // 10분 후에 다시 알람 스케줄
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager == null) {
                Timber.e("AlarmManager is null, cannot schedule snooze")
                return
            }

            val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_PILL_ID, pillId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                RequestCodeUtil.generateRequestCodeWithPrefix("snooze", alarmId),
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
            Timber.d("Snooze scheduled: alarmId=$alarmId, snoozeTime=${SNOOZE_MINUTES}min")
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle snooze: alarmId=$alarmId")
        }
    }

    private suspend fun showNotification(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        val pillId = intent.getStringExtra(EXTRA_PILL_ID)

        if (alarmId == null || pillId == null) {
            Timber.e("showNotification: Missing alarmId or pillId - alarmId=$alarmId, pillId=$pillId")
            return
        }

        try {
            val database = PillReminderDatabase.getDatabase(context)
            val pill = database.pillDao().getPillById(pillId)

            if (pill == null) {
                Timber.e("showNotification: Pill not found - pillId=$pillId")
                return
            }

            Timber.d("showNotification: Showing notification for pill=${pill.name}")

            pill.let {
            // 다음 알람 스케줄링
            val alarm = database.pillAlarmDao().getAlarmById(alarmId)
            alarm?.let { pillAlarm ->
                val alarmManagerUtil = AlarmManagerUtil(context)
                alarmManagerUtil.scheduleAlarm(pillAlarm)
            }

            // 전체 화면 알람 Activity Intent
            val alarmActivityIntent = Intent(context, com.uhstudio.pillreminder.ui.alarm.AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_USER_ACTION
                putExtra(com.uhstudio.pillreminder.ui.alarm.AlarmActivity.EXTRA_PILL_ID, pillId)
                putExtra(com.uhstudio.pillreminder.ui.alarm.AlarmActivity.EXTRA_ALARM_ID, alarmId)
                putExtra(com.uhstudio.pillreminder.ui.alarm.AlarmActivity.EXTRA_PILL_NAME, it.name)
                alarm?.alarmSoundUri?.let { uri ->
                    putExtra(com.uhstudio.pillreminder.ui.alarm.AlarmActivity.EXTRA_ALARM_SOUND_URI, uri)
                }
            }

            // Full Screen Intent용 PendingIntent
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                RequestCodeUtil.generateRequestCode(alarmId),
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
                RequestCodeUtil.generateRequestCodeWithPrefix("take", alarmId),
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
                RequestCodeUtil.generateRequestCodeWithPrefix("skip", alarmId),
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
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null) {
                notificationManager.notify(RequestCodeUtil.generateRequestCode(alarmId), notification)
                Timber.d("Notification shown successfully: pillId=$pillId, alarmId=$alarmId")
            } else {
                Timber.e("NotificationManager is null, cannot show notification")
            }
        }
        } catch (e: Exception) {
            Timber.e(e, "Failed to show notification: pillId=$pillId, alarmId=$alarmId")
        }
    }
} 