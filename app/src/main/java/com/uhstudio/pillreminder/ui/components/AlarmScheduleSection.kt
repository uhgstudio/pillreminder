package com.uhstudio.pillreminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uhstudio.pillreminder.R
import com.uhstudio.pillreminder.ui.theme.BlobPink
import com.uhstudio.pillreminder.ui.theme.StitchDeepBrown
import com.uhstudio.pillreminder.ui.theme.WarmPrimary
import java.time.DayOfWeek

/**
 * 재사용 가능한 알람 스케줄 설정 UI 컴포넌트
 * AddPillScreen과 AddAlarmScreen에서 동일하게 사용
 */
@Composable
fun AlarmScheduleSection(
    hour: Int,
    minute: Int,
    onTimeClick: () -> Unit,
    selectedDays: Set<DayOfWeek>,
    onDaysChange: (Set<DayOfWeek>) -> Unit,
    dosage: String? = null,
    onDosageClick: (() -> Unit)? = null,
    alarmSoundName: String? = null,
    onSoundClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // ... (rest of the code remains similar, but with null checks)

        // SCHEDULE 헤더
        Text(
            text = stringResource(R.string.label_schedule),
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = StitchDeepBrown.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 시간 선택 카드
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(20.dp, RoundedCornerShape(32.dp), ambientColor = Color.Black.copy(alpha = 0.05f)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(32.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFFF3E0), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Alarm, null, tint = WarmPrimary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = String.format(
                            "%02d:%02d %s",
                            if (hour > 12) hour - 12 else if (hour == 0) 12 else hour,
                            minute,
                            if (hour >= 12) stringResource(R.string.label_pm) else stringResource(R.string.label_am)
                        ),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = StitchDeepBrown
                        )
                    )
                    Text(
                        text = stringResource(R.string.alarm_every_day),
                        style = MaterialTheme.typography.labelSmall,
                        color = StitchDeepBrown.copy(alpha = 0.6f)
                    )
                }
                Button(
                    onClick = onTimeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        stringResource(R.string.btn_change),
                        color = WarmPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 요일 선택기
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val daysNames = listOf(
                stringResource(R.string.day_mon_short),
                stringResource(R.string.day_tue_short),
                stringResource(R.string.day_wed_short),
                stringResource(R.string.day_thu_short),
                stringResource(R.string.day_fri_short),
                stringResource(R.string.day_sat_short),
                stringResource(R.string.day_sun_short)
            )
            daysNames.forEachIndexed { index, day ->
                val dayOfWeek = DayOfWeek.of(if (index == 6) 7 else index + 1)
                val isSelected = selectedDays.contains(dayOfWeek)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(
                            if (isSelected) 10.dp else 0.dp,
                            RoundedCornerShape(16.dp),
                            ambientColor = WarmPrimary.copy(alpha = 0.3f)
                        )
                        .background(
                            if (isSelected) WarmPrimary else Color.White,
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else Color(0xFFF5F5F5),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            val newDays = if (isSelected) {
                                selectedDays - dayOfWeek
                            } else {
                                selectedDays + dayOfWeek
                            }
                            onDaysChange(newDays)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        color = if (isSelected) Color.White else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (dosage != null || alarmSoundName != null) {
            Spacer(modifier = Modifier.height(24.dp))

            // 빠른 설정 리스트
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(20.dp, RoundedCornerShape(32.dp), ambientColor = Color.Black.copy(alpha = 0.05f))
                    .background(Color.White, RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
            ) {
                if (dosage != null && onDosageClick != null) {
                    SettingItem(
                        icon = Icons.Default.Medication,
                        label = stringResource(R.string.pill_dosage_label),
                        value = dosage.ifBlank { "1" + stringResource(R.string.label_unit) },
                        color = BlobPink,
                        onClick = onDosageClick
                    )
                }
                
                if (dosage != null && alarmSoundName != null) {
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 20.dp))
                }
                
                if (alarmSoundName != null && onSoundClick != null) {
                    SettingItem(
                        icon = Icons.Default.NotificationsActive,
                        label = stringResource(R.string.gentle_reminder_label),
                        value = alarmSoundName,
                        color = BlobPink,
                        onClick = onSoundClick
                    )
                }
            }
        }
    }
}

/**
 * 설정 항목 컴포넌트
 */
@Composable
private fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = StitchDeepBrown,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = StitchDeepBrown.copy(alpha = 0.6f)
                )
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(20.dp)
        )
    }
}
