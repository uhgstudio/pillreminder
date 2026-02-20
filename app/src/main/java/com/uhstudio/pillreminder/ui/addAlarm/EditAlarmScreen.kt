package com.uhstudio.pillreminder.ui.addAlarm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalTime
import com.uhstudio.pillreminder.R
import com.uhstudio.pillreminder.util.toKoreanShort

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmScreen(
    viewModel: EditAlarmViewModel,
    alarmId: String,
    onNavigateUp: () -> Unit
) {
    val selectedTime by viewModel.selectedTime.collectAsState()
    val selectedDays by viewModel.selectedDays.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute,
        is24Hour = true
    )

    LaunchedEffect(alarmId) {
        viewModel.loadAlarm(alarmId)
    }

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        viewModel.updateTime(LocalTime.of(timePickerState.hour, timePickerState.minute))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_edit_alarm)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.updateAlarm {
                                onNavigateUp()
                            }
                        },
                        enabled = !isLoading && selectedDays.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.save))
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 시간 선택
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dialog_set_alarm_time),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 요일 선택
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_repeat_days),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf(
                            1 to DayOfWeek.MONDAY,
                            2 to DayOfWeek.TUESDAY,
                            3 to DayOfWeek.WEDNESDAY,
                            4 to DayOfWeek.THURSDAY,
                            5 to DayOfWeek.FRIDAY,
                            6 to DayOfWeek.SATURDAY,
                            7 to DayOfWeek.SUNDAY
                        )) { (day, dayOfWeek) ->
                            val dayName = dayOfWeek.toKoreanShort()
                            
                            FilterChip(
                                onClick = { viewModel.toggleDay(day) },
                                label = { Text(dayName) },
                                selected = selectedDays.contains(day)
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
} 