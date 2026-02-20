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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.uhstudio.pillreminder.R
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
        val pillName = intent.getStringExtra(EXTRA_PILL_NAME) ?: getString(R.string.default_pill_name)
        val alarmSoundUri = intent.getStringExtra(EXTRA_ALARM_SOUND_URI)

        // Notification 취소
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            val requestCode = com.uhstudio.pillreminder.util.RequestCodeUtil.generateRequestCode(alarmId)
            notificationManager.cancel(requestCode)
            timber.log.Timber.d("Notification cancelled for alarmId=$alarmId, requestCode=$requestCode")
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to cancel notification for alarmId=$alarmId")
        }

        // 알람 소리와 진동 시작
        startAlarmSound(alarmSoundUri)
        startVibration()

        setContent {
            PillReminderTheme {
                var pillImageUri by remember { mutableStateOf<String?>(null) }

                // Pill 정보 가져오기 (이미지 포함)
                LaunchedEffect(pillId) {
                    if (pillId.isNotEmpty()) {
                        try {
                            val pill = database.pillDao().getPillById(pillId)
                            pillImageUri = pill?.imageUri
                        } catch (e: Exception) {
                            timber.log.Timber.e(e, "Failed to load pill image for pillId=$pillId")
                        }
                    }
                }

                AlarmScreen(
                    pillName = pillName,
                    pillImageUri = pillImageUri,
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
        
        // 복용 완료 시 수량 차감
        if (status == IntakeStatus.TAKEN) {
            database.pillDao().decrementQuantity(pillId)
            
            // 수량 확인 및 경고
            val pill = database.pillDao().getPillById(pillId)
            if (pill != null && pill.quantity <= 0) {
                // 메인 스레드에서 토스트 표시
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this@AlarmActivity,
                        "${pill.name} 재고가 소진되었습니다.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            } else if (pill != null && pill.quantity <= pill.lowStockThreshold) {
                // 재고 부족 경고
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this@AlarmActivity,
                        "${pill.name} 재고가 ${pill.quantity}개 남았습니다.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
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
    pillImageUri: String?,
    onTakePill: () -> Unit,
    onSkipPill: () -> Unit,
    onSnooze: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF9F2)), // Soft Cream
        contentAlignment = Alignment.Center
    ) {
        // Background Glow Blobs (from HTML/Stitch)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-40).dp, y = (-40).dp)
                .size(240.dp)
                .background(Color(0xFFE8F3EF), CircleShape) // Sage Glow
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .size(200.dp)
                .background(Color(0xFFFFF2F0), CircleShape) // Peach Glow
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Pill Badge / Image (Hand-drawn styled circle)
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .shadow(16.dp, CircleShape, spotColor = Color(0xFFFF9A8B).copy(alpha = 0.2f))
                    .background(Color.White, CircleShape)
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFFFFF9F2))
                ) {
                    if (pillImageUri != null) {
                        AsyncImage(
                            model = pillImageUri,
                            contentDescription = pillName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = Color(0xFFFF9A8B),
                            modifier = Modifier.size(64.dp).align(Alignment.Center)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "It's time for",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF6B7280))
                )
                Text(
                    text = pillName,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151),
                        fontSize = 36.sp
                    )
                )
                Text(
                    text = "8:00 am • Routine Dose",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF6B7280).copy(alpha = 0.6f))
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons (Stitch Style)
            Button(
                onClick = onTakePill,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .shadow(12.dp, RoundedCornerShape(36.dp), spotColor = Color(0xFFFF9A8B).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9A8B))
            ) {
                Text(
                    text = "Log Dose",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            }

            Button(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F3EF)),
                border = BorderStroke(1.dp, Color(0xFF4C7B71).copy(alpha = 0.1f))
            ) {
                Text(
                    text = "Snooze 10m",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF4C7B71))
                )
            }

            TextButton(
                onClick = onSkipPill,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Skip this dose",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF9CA3AF))
                )
            }
        }
    }
}
