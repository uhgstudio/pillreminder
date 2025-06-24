package com.example.pillreminder.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.pillreminder.MainActivity
import com.example.pillreminder.R
import com.example.pillreminder.data.database.PillReminderDatabase
import com.example.pillreminder.data.model.IntakeHistory
import com.example.pillreminder.data.model.IntakeStatus
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
        private const val TAG = "AlarmReceiver"
        const val ACTION_TAKE_PILL = "com.example.pillreminder.TAKE_PILL"
        const val ACTION_SKIP_PILL = "com.example.pillreminder.SKIP_PILL"
        const val EXTRA_ALARM_ID = "ALARM_ID"
        const val EXTRA_PILL_ID = "PILL_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "알람 수신: ${intent.action}")
        
        when (intent.action) {
            ACTION_TAKE_PILL -> {
                Log.d(TAG, "복용 완료 액션 처리")
                handleIntake(context, intent, IntakeStatus.TAKEN)
            }
            ACTION_SKIP_PILL -> {
                Log.d(TAG, "건너뛰기 액션 처리")
                handleIntake(context, intent, IntakeStatus.SKIPPED)
            }
            else -> {
                Log.d(TAG, "알람 알림 표시")
                showNotification(context, intent)
            }
        }
    }

    private fun handleIntake(context: Context, intent: Intent, status: IntakeStatus) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: return
        val pillId = intent.getStringExtra(EXTRA_PILL_ID) ?: return

        Log.d(TAG, "복용 기록 저장: alarmId=$alarmId, pillId=$pillId, status=$status")

        CoroutineScope(Dispatchers.IO).launch {
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
            
            Log.d(TAG, "복용 기록 저장 완료 및 알림 제거")
        }
    }

    private fun showNotification(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: return
        val pillId = intent.getStringExtra(EXTRA_PILL_ID) ?: return

        Log.d(TAG, "알림 생성 시작: alarmId=$alarmId, pillId=$pillId")

        CoroutineScope(Dispatchers.IO).launch {
            val database = PillReminderDatabase.getDatabase(context)
            val pill = database.pillDao().getPillById(pillId)
            
            pill?.let {
                Log.d(TAG, "약 정보 조회 성공: ${it.name}")
                
                val notificationManager = 
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
                // 알림 채널 생성 (Android 8.0 이상)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.alarm_channel_name),
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = context.getString(R.string.alarm_channel_description)
                    }
                    notificationManager.createNotificationChannel(channel)
                    Log.d(TAG, "알림 채널 생성 완료")
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

                // 앱 실행 인텐트
                val contentIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    context,
                    "content_$alarmId".hashCode(),
                    contentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 알림 생성
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.alarm_notification_title))
                    .setContentText(context.getString(R.string.alarm_notification_text, it.name))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(contentPendingIntent)
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

                notificationManager.notify(alarmId.hashCode(), notification)
                Log.d(TAG, "알림 표시 완료: ${it.name}")
                
            } ?: run {
                Log.e(TAG, "약 정보를 찾을 수 없습니다: pillId=$pillId")
            }
        }
    }
} 