package com.example.pillreminder.ui.addAlarm

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pillreminder.R
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmScreen(
    viewModel: AddAlarmViewModel,
    onNavigateUp: () -> Unit
) {
    var hour by remember { mutableStateOf(8) }
    var minute by remember { mutableStateOf(0) }
    var selectedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_add_alarm)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 시간 선택
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showTimePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format("%02d:%02d", hour, minute),
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }

            // 요일 선택
            Text(
                text = stringResource(R.string.label_repeat_days),
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DayOfWeek.values().forEach { day ->
                    DayButton(
                        day = day,
                        selected = day in selectedDays,
                        onSelectedChange = { selected ->
                            selectedDays = if (selected) {
                                selectedDays + day
                            } else {
                                selectedDays - day
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 저장 버튼
            Button(
                onClick = {
                    if (selectedDays.isNotEmpty()) {
                        viewModel.saveAlarm(
                            pillId = "", // TODO: Pass pillId from navigation
                            hour = hour,
                            minute = minute,
                            repeatDays = selectedDays,
                            onComplete = onNavigateUp
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedDays.isNotEmpty()
            ) {
                Text(stringResource(R.string.btn_save))
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = { selectedHour, selectedMinute ->
                hour = selectedHour
                minute = selectedMinute
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun DayButton(
    day: DayOfWeek,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    FilledTonalButton(
        onClick = { onSelectedChange(!selected) },
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Text(day.name.take(1))
    }
}

@Composable
private fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(8) }
    var selectedMinute by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.dialog_set_alarm_time)) },
        text = {
            Column {
                // 시간 선택
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 시간
                    NumberPicker(
                        value = selectedHour,
                        onValueChange = { selectedHour = it },
                        range = 0..23
                    )

                    Text(":")

                    // 분
                    NumberPicker(
                        value = selectedMinute,
                        onValueChange = { selectedMinute = it },
                        range = 0..59
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedHour, selectedMinute)
                }
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
private fun NumberPicker(
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
            style = MaterialTheme.typography.titleLarge
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