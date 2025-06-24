package com.example.pillreminder.ui.alarm

import android.text.format.DateFormat
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
import com.example.pillreminder.R
import com.example.pillreminder.data.model.PillAlarm
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*

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
    val context = LocalContext.current

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
                    text = alarm.repeatDays.joinToString(", ") {
                        it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    },
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