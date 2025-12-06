package com.uhstudio.pillreminder.ui.alarm

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import com.uhstudio.pillreminder.data.model.IntakeHistory
import com.uhstudio.pillreminder.data.model.IntakeStatus
import com.uhstudio.pillreminder.ui.theme.PillReminderTheme
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

/**
 * 전체 화면 알람 액티비티
 * 화면이 꺼져있어도 알람을 표시하고 소리와 진동으로 알림
 */
class AlarmActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private lateinit var database: PillReminderDatabase

    companion object {
        const val EXTRA_PILL_ID = "pill_id"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_PILL_NAME = "pill_name"
        const val EXTRA_ALARM_SOUND_URI = "alarm_sound_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 화면이 꺼져있어도 켜지도록 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // 잠금 화면 위에 표시
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        database = PillReminderDatabase.getDatabase(applicationContext)

        val pillId = intent.getStringExtra(EXTRA_PILL_ID) ?: ""
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: ""
        val pillName = intent.getStringExtra(EXTRA_PILL_NAME) ?: "약"
        val alarmSoundUri = intent.getStringExtra(EXTRA_ALARM_SOUND_URI)

        // Notification 취소
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId.hashCode())

        // 알람 소리와 진동 시작
        startAlarmSound(alarmSoundUri)
        startVibration()

        setContent {
            PillReminderTheme {
                AlarmScreen(
                    pillName = pillName,
                    onTakePill = {
                        lifecycleScope.launch {
                            saveIntakeHistory(pillId, alarmId, IntakeStatus.TAKEN)
                            stopAlarmAndFinish()
                        }
                    },
                    onSkipPill = {
                        lifecycleScope.launch {
                            saveIntakeHistory(pillId, alarmId, IntakeStatus.SKIPPED)
                            stopAlarmAndFinish()
                        }
                    },
                    onSnooze = {
                        // TODO: 스누즈 기능 구현
                        stopAlarmAndFinish()
                    }
                )
            }
        }
    }

    private fun startAlarmSound(customSoundUri: String?) {
        try {
            val alarmUri = if (customSoundUri != null) {
                android.net.Uri.parse(customSoundUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 1000, 1000) // 대기, 진동, 대기를 반복

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(pattern, 0)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private suspend fun saveIntakeHistory(pillId: String, alarmId: String, status: IntakeStatus) {
        val history = IntakeHistory(
            id = UUID.randomUUID().toString(),
            pillId = pillId,
            alarmId = alarmId,
            intakeTime = LocalDateTime.now(),
            status = status
        )
        database.intakeHistoryDao().insertHistory(history)
    }

    private fun stopAlarmAndFinish() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        vibrator?.cancel()
        vibrator = null

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmAndFinish()
    }
}

@Composable
fun AlarmScreen(
    pillName: String,
    onTakePill: () -> Unit,
    onSkipPill: () -> Unit,
    onSnooze: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "약 복용 시간",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = pillName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onTakePill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "복용 완료",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "5분 후 다시 알림",
                        fontSize = 16.sp
                    )
                }

                TextButton(
                    onClick = onSkipPill,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "건너뛰기",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
