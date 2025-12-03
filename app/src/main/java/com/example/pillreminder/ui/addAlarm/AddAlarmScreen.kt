package com.example.pillreminder.ui.addAlarm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pillreminder.R
import com.example.pillreminder.ui.theme.GradientPeachStart
import com.example.pillreminder.ui.theme.GradientGoldEnd
import com.example.pillreminder.util.AlarmManagerUtil
import com.example.pillreminder.util.toKoreanShort
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmScreen(
    viewModel: AddAlarmViewModel,
    pillId: String,
    alarmId: String? = null,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    var hour by remember { mutableStateOf(8) }
    var minute by remember { mutableStateOf(0) }
    var selectedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDaySelector by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(alarmId == null) }

    // 기존 알람 정보 불러오기
    LaunchedEffect(alarmId) {
        if (alarmId != null) {
            viewModel.getAlarm(alarmId)?.let { alarm ->
                hour = alarm.hour
                minute = alarm.minute
                selectedDays = alarm.repeatDays
                isLoaded = true
            }
        }
    }

    // 밝은 피치-골드 그라디언트 배경
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GradientPeachStart,
                        GradientGoldEnd
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,  // 배경 투명하게
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (alarmId == null) {
                                stringResource(R.string.title_add_alarm)
                            } else {
                                "알람 수정"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
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
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format("%02d:%02d", hour, minute),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 반복 요일 선택 (iOS 스타일)
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDaySelector = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_repeat_days),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (selectedDays.isEmpty()) {
                                "선택 안함"
                            } else if (selectedDays.size == 7) {
                                "매일"
                            } else {
                                selectedDays.sortedBy { it.value }
                                    .joinToString(" ") { it.toKoreanShort() }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 저장 버튼
            Button(
                onClick = {
                    if (selectedDays.isNotEmpty()) {
                        // 정확한 알람 권한 체크 (Android 12+)
                        val alarmUtil = AlarmManagerUtil(context)
                        if (!alarmUtil.canScheduleExactAlarms()) {
                            // 권한이 없으면 다이얼로그 표시
                            showPermissionDialog = true
                        } else {
                            // 권한이 있으면 알람 저장/수정
                            viewModel.saveAlarm(
                                pillId = pillId,
                                hour = hour,
                                minute = minute,
                                repeatDays = selectedDays,
                                alarmId = alarmId,
                                onComplete = onNavigateUp
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedDays.isNotEmpty() && isLoaded,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(R.string.btn_save),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
    }

    // Material3 TimePicker 다이얼로그
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismissRequest = { showTimePicker = false },
            onConfirm = { selectedHour, selectedMinute ->
                hour = selectedHour
                minute = selectedMinute
                showTimePicker = false
            }
        )
    }

    // 요일 선택 다이얼로그 (iOS 스타일)
    if (showDaySelector) {
        DaySelectorDialog(
            selectedDays = selectedDays,
            onDismissRequest = { showDaySelector = false },
            onConfirm = { days ->
                selectedDays = days
                showDaySelector = false
            }
        )
    }

    // 정확한 알람 권한 요청 다이얼로그
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text(
                    text = "정확한 알람 권한 필요",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "약 복용 알람이 정확한 시간에 울리려면 \"정확한 알람\" 권한이 필요합니다.\n\n" +
                            "설정으로 이동하여 권한을 허용해주세요.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        val alarmUtil = AlarmManagerUtil(context)
                        alarmUtil.requestExactAlarmPermission()
                    }
                ) {
                    Text("설정으로 이동")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun DaySelectorDialog(
    selectedDays: Set<DayOfWeek>,
    onDismissRequest: () -> Unit,
    onConfirm: (Set<DayOfWeek>) -> Unit
) {
    var tempSelectedDays by remember { mutableStateOf(selectedDays) }

    val dayNames = listOf(
        DayOfWeek.SUNDAY to "일요일",
        DayOfWeek.MONDAY to "월요일",
        DayOfWeek.TUESDAY to "화요일",
        DayOfWeek.WEDNESDAY to "수요일",
        DayOfWeek.THURSDAY to "목요일",
        DayOfWeek.FRIDAY to "금요일",
        DayOfWeek.SATURDAY to "토요일"
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.label_repeat_days),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                dayNames.forEach { (day, name) ->
                    val isSelected = day in tempSelectedDays
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempSelectedDays = if (day in tempSelectedDays) {
                                    tempSelectedDays - day
                                } else {
                                    tempSelectedDays + day
                                }
                            }
                            .padding(vertical = 16.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(tempSelectedDays) }) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.dialog_set_alarm_time),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.primaryContainer,
                        clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                        clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        selectorColor = MaterialTheme.colorScheme.primary,
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                        timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
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
