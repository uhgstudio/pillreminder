package com.uhstudio.pillreminder.ui.addAlarm

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uhstudio.pillreminder.R
import com.uhstudio.pillreminder.ads.AdManager
import com.uhstudio.pillreminder.data.model.ScheduleConfig
import com.uhstudio.pillreminder.data.model.ScheduleType
import com.uhstudio.pillreminder.ui.components.AlarmScheduleSection
import com.uhstudio.pillreminder.ui.theme.StitchPrimary
import com.uhstudio.pillreminder.util.AlarmManagerUtil
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmScreen(
    viewModel: AddAlarmViewModel,
    pillId: String,
    alarmId: String? = null,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adManager = remember { AdManager.getInstance(context) }

    var hour by remember { mutableStateOf(8) }
    var minute by remember { mutableStateOf(0) }
    var selectedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }

    val isLoading by viewModel.isLoading.collectAsState()

    var alarmSoundUri by remember { mutableStateOf<String?>(null) }
    var alarmSoundName by remember { mutableStateOf("기본 알람음") }

    var showTimePicker by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showSoundPicker by remember { mutableStateOf(false) }

    var isLoaded by remember { mutableStateOf(alarmId == null) }

    // 기존 알람 정보 불러오기
    LaunchedEffect(alarmId) {
        if (alarmId != null) {
            viewModel.getAlarm(alarmId)?.let { alarm ->
                timber.log.Timber.d("AddAlarmScreen: Loading alarm - scheduleType=${alarm.scheduleType}, scheduleConfig=${alarm.scheduleConfig}")

                hour = alarm.hour
                minute = alarm.minute

                // scheduleConfig에서 설정 정보 파싱
                try {
                    if (alarm.scheduleType == ScheduleType.WEEKLY) {
                        if (alarm.scheduleConfig != null) {
                            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                            val config = json.decodeFromString<ScheduleConfig.Weekly>(alarm.scheduleConfig)
                            selectedDays = config.toDayOfWeekSet()
                        } else {
                            @Suppress("DEPRECATION")
                            selectedDays = alarm.repeatDays
                        }
                    } else if (alarm.scheduleType == ScheduleType.DAILY) {
                        selectedDays = setOf(
                            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
                        )
                    }
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "AddAlarmScreen: Failed to parse scheduleConfig")
                    if (alarm.scheduleType == ScheduleType.WEEKLY) {
                        @Suppress("DEPRECATION")
                        selectedDays = alarm.repeatDays
                    }
                }

                alarmSoundUri = alarm.alarmSoundUri
                alarm.alarmSoundUri?.let { uri ->
                    try {
                        val ringtone = android.media.RingtoneManager.getRingtone(context, android.net.Uri.parse(uri))
                        alarmSoundName = ringtone.getTitle(context)
                    } catch (e: Exception) {
                        alarmSoundName = "선택된 알람음"
                    }
                }
                isLoaded = true
            }
        }
    }



    // Modern Clean Layout (Stitch Style)
    Scaffold(
        containerColor = Color(0xFFFFF9F2), // StitchCream
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (alarmId == null) stringResource(R.string.alarm_new_reminder) else stringResource(R.string.alarm_edit_reminder),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF4C7B71)) // StitchSageDark
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back),
                                tint = Color(0xFF4C7B71) // StitchSageDark
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFFFF9F2), // StitchCream
                        titleContentColor = Color(0xFF4C7B71)
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp), // Increased padding
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            // Unified Alarm Schedule Section (same as AddPillScreen)
            AlarmScheduleSection(
                hour = hour,
                minute = minute,
                onTimeClick = { showTimePicker = true },
                selectedDays = selectedDays,
                onDaysChange = { selectedDays = it },
                alarmSoundName = alarmSoundName,
                onSoundClick = { showSoundPicker = true }
            )

            Spacer(modifier = Modifier.weight(1f))

            // 저장 버튼
            Button(
                onClick = {
                    val finalScheduleType = if (selectedDays.isEmpty() || selectedDays.size == 7) ScheduleType.DAILY else ScheduleType.WEEKLY
                    val finalScheduleConfig = if (finalScheduleType == ScheduleType.WEEKLY) ScheduleConfig.Weekly.from(selectedDays) else ScheduleConfig.Daily
                    
                    val alarmUtil = AlarmManagerUtil(context)
                    if (!alarmUtil.canScheduleExactAlarms()) {
                        showPermissionDialog = true
                    } else {
                        // 권한이 있으면 알람 저장/수정
                        viewModel.saveAlarmWithSchedule(
                            pillId = pillId,
                            hour = hour,
                            minute = minute,
                            scheduleType = finalScheduleType,
                            scheduleConfig = finalScheduleConfig,
                            startDate = null, // simplified duration
                            endDate = null,   // simplified duration
                            alarmSoundUri = alarmSoundUri,
                            alarmId = alarmId,
                            onComplete = {
                                Toast.makeText(context, context.getString(R.string.msg_saved), Toast.LENGTH_SHORT).show()
                                onNavigateUp()
                            },
                            onShowAd = {
                                activity?.let {
                                    adManager.showInterstitialAd(it, onAdClosed = onNavigateUp)
                                } ?: onNavigateUp()
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = isLoaded,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StitchPrimary,
                    contentColor = Color.White
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.btn_save),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    )
                }
            }

            // 하단 네비게이션과의 간격 확보
            Spacer(modifier = Modifier.height(80.dp))
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


    // 정확한 알람 권한 요청 다이얼로그
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_alarm_permission_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_alarm_permission_text),
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
                    Text(stringResource(R.string.btn_go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // 알람음 선택 다이얼로그
    if (showSoundPicker) {
        SoundPickerDialog(
            context = context,
            currentUri = alarmSoundUri,
            onDismissRequest = { showSoundPicker = false },
            onSoundSelected = { uri, name ->
                alarmSoundUri = uri
                alarmSoundName = name
                showSoundPicker = false
            }
        )
    }

    // 날짜 선택 다이얼로그 (Not needed anymore given simplification)


    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
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

@Composable
private fun SoundPickerDialog(
    context: Context,
    currentUri: String?,
    onDismissRequest: () -> Unit,
    onSoundSelected: (uri: String?, name: String) -> Unit
) {
    val ringtoneManager = remember { android.media.RingtoneManager(context) }
    val alarmRingtones = remember {
        ringtoneManager.setType(android.media.RingtoneManager.TYPE_ALARM)
        val cursor = ringtoneManager.cursor
        val ringtones = mutableListOf<Pair<String?, String>>()

        // 기본 알람음 추가
        ringtones.add(Pair(null, context.getString(R.string.default_sound)))

        // 시스템 알람음 추가
        while (cursor.moveToNext()) {
            val title = cursor.getString(android.media.RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = ringtoneManager.getRingtoneUri(cursor.position).toString()
            ringtones.add(Pair(uri, title))
        }
        ringtones
    }

    var selectedUri by remember { mutableStateOf(currentUri) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.label_alarm_sound)) },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(alarmRingtones.size) { index ->
                    val (uri, name) = alarmRingtones[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedUri = uri
                                onSoundSelected(uri, name)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedUri == uri,
                            onClick = {
                                selectedUri = uri
                                onSoundSelected(uri, name)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    initialDate: LocalDate,
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate?) -> Unit,
    allowClear: Boolean = false
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
    )

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_specific_dates),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (allowClear) {
                        TextButton(
                            onClick = {
                                onDateSelected(null)
                            }
                        ) {
                            Text(stringResource(R.string.btn_clear))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val selectedDate = java.time.Instant.ofEpochMilli(millis)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate()
                                onDateSelected(selectedDate)
                            }
                        }
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                }
            }
        }
    }
}

