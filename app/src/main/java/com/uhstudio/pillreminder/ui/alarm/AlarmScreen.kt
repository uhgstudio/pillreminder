package com.uhstudio.pillreminder.ui.alarm

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.data.model.ScheduleConfig
import com.uhstudio.pillreminder.data.model.ScheduleType
import com.uhstudio.pillreminder.util.toKoreanShort
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*
import com.uhstudio.pillreminder.R
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    viewModel: AlarmViewModel,
    pillId: String,
    onNavigateUp: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(8) }
    var selectedMinute by remember { mutableStateOf(0) }
    val alarms by viewModel.getAlarmsForPill(pillId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_alarms)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTimePicker = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.btn_add_alarm)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(alarms) { alarm ->
                AlarmItem(
                    alarm = alarm,
                    onDeleteClick = { viewModel.deleteAlarm(alarm) },
                    onEnabledChange = { enabled ->
                        viewModel.updateAlarmEnabled(alarm.id, enabled)
                    }
                )
            }
        }

        if (showTimePicker) {
            TimePickerDialog(
                onDismiss = { showTimePicker = false },
                onConfirm = { hour, minute, repeatDays ->
                    viewModel.addAlarm(pillId, hour, minute, repeatDays)
                    showTimePicker = false
                },
                initialHour = selectedHour,
                initialMinute = selectedMinute
            )
        }
    }
}

@Composable
fun AlarmItem(
    alarm: PillAlarm,
    onDeleteClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    val scheduleDescription = remember(alarm) {
        getScheduleDescription(alarm)
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = scheduleDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onEnabledChange
                )
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.btn_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun getScheduleDescription(alarm: PillAlarm): String {
    timber.log.Timber.d("AlarmScreen getScheduleDescription: alarmId=${alarm.id}, scheduleType=${alarm.scheduleType}, scheduleConfig=${alarm.scheduleConfig}, repeatDays=${alarm.repeatDays}")

    return try {
        Log.d(  "AlarmScreen", "AlarmItem: scheduleType=${alarm.scheduleType}")
        when (alarm.scheduleType) {
            ScheduleType.DAILY -> {
                timber.log.Timber.d("AlarmScreen: DAILY type")
                "매일"
            }
            ScheduleType.WEEKLY -> {
                timber.log.Timber.d("AlarmScreen: WEEKLY type, trying to parse scheduleConfig")

                // 먼저 scheduleConfig 시도
                val days = if (alarm.scheduleConfig != null && alarm.scheduleConfig.isNotBlank()) {
                    try {
                        val config = Json { ignoreUnknownKeys = true }.decodeFromString<ScheduleConfig.Weekly>(alarm.scheduleConfig)
                        val parsedDays = config.toDayOfWeekSet()
                        timber.log.Timber.d("AlarmScreen: Parsed days from scheduleConfig: $parsedDays")
                        parsedDays
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "AlarmScreen: Failed to parse scheduleConfig, falling back to repeatDays")
                        @Suppress("DEPRECATION")
                        alarm.repeatDays
                    }
                } else {
                    timber.log.Timber.d("AlarmScreen: No scheduleConfig, using repeatDays: ${alarm.repeatDays}")
                    @Suppress("DEPRECATION")
                    alarm.repeatDays
                }

                timber.log.Timber.d("AlarmScreen: Final days for WEEKLY: $days")
                when {
                    days.isEmpty() -> {
                        timber.log.Timber.w("AlarmScreen: Days is empty!")
                        "반복 없음"
                    }
                    days.size == 7 -> "매일"
                    else -> days.sortedBy { it.value }.joinToString(" ") { it.toKoreanShort() }
                }
            }
            ScheduleType.INTERVAL_DAYS -> {
                timber.log.Timber.d("AlarmScreen: INTERVAL_DAYS type")
                if (alarm.scheduleConfig == null || alarm.scheduleConfig.isBlank()) {
                    return "N일 마다"
                }
                val config = Json { ignoreUnknownKeys = true }.decodeFromString<ScheduleConfig.IntervalDays>(alarm.scheduleConfig)
                "${config.intervalDays}일 마다"
            }
            ScheduleType.SPECIFIC_DATES -> {
                timber.log.Timber.d("AlarmScreen: SPECIFIC_DATES type")
                if (alarm.scheduleConfig == null || alarm.scheduleConfig.isBlank()) {
                    return "특정 날짜"
                }
                val config = Json { ignoreUnknownKeys = true }.decodeFromString<ScheduleConfig.SpecificDates>(alarm.scheduleConfig)
                val dates = config.getDatesAsLocalDateSet()
                if (dates.isEmpty()) {
                    "특정 날짜 (미설정)"
                } else {
                    "특정 날짜 (${dates.size}개)"
                }
            }
            ScheduleType.CUSTOM -> "커스텀"
        }
    } catch (e: Exception) {
        timber.log.Timber.e(e, "AlarmScreen: Exception in getScheduleDescription")
        // 파싱 실패 시 레거시 방식으로 fallback
        @Suppress("DEPRECATION")
        val days = alarm.repeatDays
        timber.log.Timber.d("AlarmScreen: Fallback to repeatDays: $days")
        when {
            days.isEmpty() -> "반복 없음"
            days.size == 7 -> "매일"
            else -> days.sortedBy { it.value }.joinToString(" ") { it.toKoreanShort() }
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int, repeatDays: Set<DayOfWeek>) -> Unit,
    initialHour: Int,
    initialMinute: Int
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    var selectedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_set_alarm_time)) },
        text = {
            Column {
                TimePicker(
                    hour = selectedHour,
                    minute = selectedMinute,
                    onTimeChange = { hour, minute ->
                        selectedHour = hour
                        selectedMinute = minute
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                DaySelector(
                    selectedDays = selectedDays,
                    onDaysChange = { selectedDays = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedHour, selectedMinute, selectedDays)
                }
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun TimePicker(
    hour: Int,
    minute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit
) {
    // 시간 선택 UI 구현
    // Material3 TimePicker가 아직 실험적 단계이므로 임시로 간단한 선택기 사용
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NumberPicker(
            value = hour,
            onValueChange = { onTimeChange(it, minute) },
            range = 0..23
        )
        Text(":")
        NumberPicker(
            value = minute,
            onValueChange = { onTimeChange(hour, it) },
            range = 0..59
        )
    }
}

@Composable
fun DaySelector(
    selectedDays: Set<DayOfWeek>,
    onDaysChange: (Set<DayOfWeek>) -> Unit
) {
    val days = DayOfWeek.values()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { day ->
            val isSelected = selectedDays.contains(day)
            FilterChip(
                selected = isSelected,
                onClick = {
                    val newSelection = if (isSelected) {
                        selectedDays - day
                    } else {
                        selectedDays + day
                    }
                    onDaysChange(newSelection)
                },
                label = {
                    Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                }
            )
        }
    }
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    Column {
        IconButton(
            onClick = {
                val newValue = if (value >= range.last) range.first else value + 1
                onValueChange(newValue)
            }
        ) {
            Text("▲")
        }
        Text(
            text = String.format("%02d", value),
            style = MaterialTheme.typography.headlineMedium
        )
        IconButton(
            onClick = {
                val newValue = if (value <= range.first) range.last else value - 1
                onValueChange(newValue)
            }
        ) {
            Text("▼")
        }
    }
} 