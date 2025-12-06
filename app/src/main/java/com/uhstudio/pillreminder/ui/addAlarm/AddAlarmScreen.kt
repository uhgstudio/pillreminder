package com.uhstudio.pillreminder.ui.addAlarm

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.uhstudio.pillreminder.ads.AdManager
import com.uhstudio.pillreminder.data.model.ScheduleConfig
import com.uhstudio.pillreminder.data.model.ScheduleType
import com.uhstudio.pillreminder.ui.theme.GradientPeachStart
import com.uhstudio.pillreminder.ui.theme.GradientGoldEnd
import com.uhstudio.pillreminder.util.AlarmManagerUtil
import com.uhstudio.pillreminder.util.ScheduleCalculator
import com.uhstudio.pillreminder.util.toKoreanShort
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.encodeToString
import com.uhstudio.pillreminder.R
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete

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
    var scheduleType by remember { mutableStateOf(ScheduleType.WEEKLY) }
    var selectedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }

    // INTERVAL_DAYS용
    var intervalDays by remember { mutableStateOf(2) }
    var intervalStartDate by remember { mutableStateOf(LocalDate.now()) }

    // INTERVAL_HOURS용
    var intervalHours by remember { mutableStateOf(6) }
    var intervalStartTime by remember { mutableStateOf(LocalTime.now()) }

    // SPECIFIC_DATES용
    var specificDates by remember { mutableStateOf(listOf<LocalDate>()) }

    // MONTHLY용
    var monthlyDays by remember { mutableStateOf(setOf<Int>()) }

    // 복용 기간
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }

    var alarmSoundUri by remember { mutableStateOf<String?>(null) }
    var alarmSoundName by remember { mutableStateOf("기본 알람음") }
    var showTimePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showDaySelector by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showSoundPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerMode by remember { mutableStateOf("start") } // "start", "end", "intervalStart", "specificDate"
    var isLoaded by remember { mutableStateOf(alarmId == null) }

    // 기존 알람 정보 불러오기
    LaunchedEffect(alarmId) {
        if (alarmId != null) {
            viewModel.getAlarm(alarmId)?.let { alarm ->
                hour = alarm.hour
                minute = alarm.minute
                scheduleType = alarm.scheduleType

                // 기존 repeatDays 또는 scheduleConfig에서 요일 정보 로드
                @Suppress("DEPRECATION")
                selectedDays = alarm.repeatDays

                alarmSoundUri = alarm.alarmSoundUri

                // 알람음 이름 가져오기
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
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

            // 스케줄 타입 선택
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "복용 방법",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // 첫 번째 행: 기본 타입
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = scheduleType == ScheduleType.DAILY,
                            onClick = {
                                scheduleType = ScheduleType.DAILY
                                selectedDays = emptySet()
                            },
                            label = { Text(stringResource(R.string.schedule_daily)) },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = scheduleType == ScheduleType.WEEKLY,
                            onClick = { scheduleType = ScheduleType.WEEKLY },
                            label = { Text(stringResource(R.string.schedule_weekly)) },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = scheduleType == ScheduleType.INTERVAL_DAYS,
                            onClick = { scheduleType = ScheduleType.INTERVAL_DAYS },
                            label = { Text(stringResource(R.string.schedule_interval_days)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 두 번째 행: 시간/날짜 기반
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = scheduleType == ScheduleType.INTERVAL_HOURS,
                            onClick = { scheduleType = ScheduleType.INTERVAL_HOURS },
                            label = { Text(stringResource(R.string.schedule_interval_hours)) },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = scheduleType == ScheduleType.SPECIFIC_DATES,
                            onClick = { scheduleType = ScheduleType.SPECIFIC_DATES },
                            label = { Text(stringResource(R.string.schedule_specific_dates)) },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = scheduleType == ScheduleType.MONTHLY,
                            onClick = { scheduleType = ScheduleType.MONTHLY },
                            label = { Text(stringResource(R.string.schedule_monthly)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 세 번째 행: 편의 타입
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = scheduleType == ScheduleType.WEEKDAY_ONLY,
                            onClick = { scheduleType = ScheduleType.WEEKDAY_ONLY },
                            label = { Text(stringResource(R.string.schedule_weekday_only)) },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = scheduleType == ScheduleType.WEEKEND_ONLY,
                            onClick = { scheduleType = ScheduleType.WEEKEND_ONLY },
                            label = { Text(stringResource(R.string.schedule_weekend_only)) },
                            modifier = Modifier.weight(1f)
                        )

                        // 빈 공간을 채우기 위한 Spacer
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 반복 요일 선택 (WEEKLY 타입일 때만 표시)
            if (scheduleType == ScheduleType.WEEKLY) {
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
            }

            // N일마다 설정 (INTERVAL_DAYS 타입일 때만 표시)
            if (scheduleType == ScheduleType.INTERVAL_DAYS) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "간격 설정",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        // 간격 일수 선택
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "매",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 감소 버튼
                                FilledTonalButton(
                                    onClick = { if (intervalDays > 1) intervalDays-- },
                                    enabled = intervalDays > 1
                                ) {
                                    Text("-")
                                }
                                Text(
                                    text = "$intervalDays 일",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.width(60.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                // 증가 버튼
                                FilledTonalButton(
                                    onClick = { if (intervalDays < 30) intervalDays++ },
                                    enabled = intervalDays < 30
                                ) {
                                    Text("+")
                                }
                            }
                            Text(
                                text = "마다",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        HorizontalDivider()

                        // 시작 기준일
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    datePickerMode = "intervalStart"
                                    showDatePicker = true
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "시작 기준일",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = intervalStartDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")),
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
                }
            }

            // N시간마다 설정 (INTERVAL_HOURS 타입일 때만 표시)
            if (scheduleType == ScheduleType.INTERVAL_HOURS) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_interval_hours),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        // 시간 간격 선택
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "매",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = { if (intervalHours > 1) intervalHours-- },
                                    enabled = intervalHours > 1
                                ) {
                                    Text("-")
                                }
                                Text(
                                    text = "$intervalHours 시간",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.width(80.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                FilledTonalButton(
                                    onClick = { if (intervalHours < 24) intervalHours++ },
                                    enabled = intervalHours < 24
                                ) {
                                    Text("+")
                                }
                            }
                            Text(
                                text = "마다",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        HorizontalDivider()

                        // 시작 시간 선택
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showStartTimePicker = true
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.label_start_time),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = intervalStartTime.format(DateTimeFormatter.ofPattern("HH:mm")),
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
                }
            }

            // 특정 날짜 설정 (SPECIFIC_DATES 타입일 때만 표시)
            if (scheduleType == ScheduleType.SPECIFIC_DATES) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.label_specific_dates),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            FilledTonalButton(
                                onClick = {
                                    datePickerMode = "specificDate"
                                    showDatePicker = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.btn_add_date))
                            }
                        }

                        if (specificDates.isEmpty()) {
                            Text(
                                text = stringResource(R.string.msg_no_dates_selected),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            HorizontalDivider()
                            specificDates.sortedBy { it }.forEach { date ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = date.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)", java.util.Locale.KOREAN)),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(
                                        onClick = {
                                            specificDates = specificDates.filter { it != date }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "삭제",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 매월 특정일 설정 (MONTHLY 타입일 때만 표시)
            @OptIn(ExperimentalLayoutApi::class)
            if (scheduleType == ScheduleType.MONTHLY) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_monthly_days),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        if (monthlyDays.isEmpty()) {
                            Text(
                                text = stringResource(R.string.msg_no_monthly_days_selected),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            (1..31).forEach { day ->
                                FilterChip(
                                    selected = day in monthlyDays,
                                    onClick = {
                                        monthlyDays = if (day in monthlyDays) {
                                            monthlyDays - day
                                        } else {
                                            monthlyDays + day
                                        }
                                    },
                                    label = { Text("$day") }
                                )
                            }
                        }
                    }
                }
            }

            // 평일만 설정 (WEEKDAY_ONLY 타입일 때만 표시)
            if (scheduleType == ScheduleType.WEEKDAY_ONLY) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.schedule_weekday_only),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.hint_weekday_only),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 주말만 설정 (WEEKEND_ONLY 타입일 때만 표시)
            if (scheduleType == ScheduleType.WEEKEND_ONLY) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.schedule_weekend_only),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.hint_weekend_only),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 복용 기간 설정 (선택사항)
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "복용 기간 (선택사항)",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    // 시작일
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                datePickerMode = "start"
                                showDatePicker = true
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "시작일",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = startDate?.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")) ?: "미설정",
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

                    HorizontalDivider()

                    // 종료일
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                datePickerMode = "end"
                                showDatePicker = true
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "종료일",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = endDate?.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")) ?: "미설정",
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
            }

            // 다음 5회 알람 미리보기
            val nextAlarms = remember(scheduleType, selectedDays, intervalDays, intervalStartDate, intervalHours, intervalStartTime, specificDates, monthlyDays, hour, minute, startDate, endDate) {
                try {
                    val json = kotlinx.serialization.json.Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }

                    // 임시 알람 객체 생성
                    val configJson = when (scheduleType) {
                        ScheduleType.DAILY -> {
                            val config: ScheduleConfig = ScheduleConfig.Daily
                            json.encodeToString(config)
                        }
                        ScheduleType.WEEKLY -> {
                            if (selectedDays.isEmpty()) return@remember emptyList()
                            val config: ScheduleConfig = ScheduleConfig.Weekly.from(selectedDays)
                            json.encodeToString(config)
                        }
                        ScheduleType.INTERVAL_DAYS -> {
                            val config: ScheduleConfig = ScheduleConfig.IntervalDays(
                                intervalDays = intervalDays,
                                startDate = intervalStartDate.toString()
                            )
                            json.encodeToString(config)
                        }
                        ScheduleType.INTERVAL_HOURS -> {
                            val config: ScheduleConfig = ScheduleConfig.IntervalHours(
                                intervalHours = intervalHours,
                                startTime = intervalStartTime.toString()
                            )
                            json.encodeToString(config)
                        }
                        ScheduleType.SPECIFIC_DATES -> {
                            if (specificDates.isEmpty()) return@remember emptyList()
                            val config: ScheduleConfig = ScheduleConfig.SpecificDates(
                                dates = specificDates.map { it.toString() }.toSet()
                            )
                            json.encodeToString(config)
                        }
                        ScheduleType.MONTHLY -> {
                            if (monthlyDays.isEmpty()) return@remember emptyList()
                            val config: ScheduleConfig = ScheduleConfig.Monthly(
                                daysOfMonth = monthlyDays
                            )
                            json.encodeToString(config)
                        }
                        ScheduleType.WEEKDAY_ONLY -> {
                            val config: ScheduleConfig = ScheduleConfig.WeekdayOnly
                            json.encodeToString(config)
                        }
                        ScheduleType.WEEKEND_ONLY -> {
                            val config: ScheduleConfig = ScheduleConfig.WeekendOnly
                            json.encodeToString(config)
                        }
                        else -> return@remember emptyList()
                    }

                    val tempAlarm = com.uhstudio.pillreminder.data.model.PillAlarm(
                        id = "preview",
                        pillId = pillId,
                        hour = hour,
                        minute = minute,
                        scheduleType = scheduleType,
                        scheduleConfig = configJson,
                        startDate = startDate,
                        endDate = endDate,
                        repeatDays = emptySet(),
                        enabled = true
                    )

                    // 다음 5회 알람 계산
                    val alarms = mutableListOf<java.time.LocalDateTime>()
                    var currentTime = java.time.LocalDateTime.now()
                    repeat(5) {
                        val nextAlarm = ScheduleCalculator.calculateNextAlarmTime(tempAlarm, currentTime)
                        if (nextAlarm != null) {
                            alarms.add(nextAlarm)
                            currentTime = nextAlarm.plusMinutes(1)
                        }
                    }
                    alarms
                } catch (e: Exception) {
                    emptyList()
                }
            }

            if (nextAlarms.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "다음 5회 알람 미리보기",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        nextAlarms.forEach { alarmTime ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = alarmTime.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E) HH:mm", java.util.Locale.KOREAN)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // 알람음 선택
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSoundPicker = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "알람음",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = alarmSoundName,
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
                    // 입력 검증
                    val isValid = when (scheduleType) {
                        ScheduleType.DAILY -> true
                        ScheduleType.WEEKLY -> selectedDays.isNotEmpty()
                        ScheduleType.INTERVAL_DAYS -> intervalDays >= 1
                        ScheduleType.INTERVAL_HOURS -> intervalHours >= 1
                        ScheduleType.SPECIFIC_DATES -> specificDates.isNotEmpty()
                        ScheduleType.MONTHLY -> monthlyDays.isNotEmpty()
                        ScheduleType.WEEKDAY_ONLY -> true
                        ScheduleType.WEEKEND_ONLY -> true
                        else -> false
                    }

                    if (isValid) {
                        // 정확한 알람 권한 체크 (Android 12+)
                        val alarmUtil = AlarmManagerUtil(context)
                        if (!alarmUtil.canScheduleExactAlarms()) {
                            showPermissionDialog = true
                        } else {
                            // ScheduleConfig 생성
                            val scheduleConfig = when (scheduleType) {
                                ScheduleType.DAILY -> ScheduleConfig.Daily
                                ScheduleType.WEEKLY -> ScheduleConfig.Weekly.from(selectedDays)
                                ScheduleType.INTERVAL_DAYS -> ScheduleConfig.IntervalDays(
                                    intervalDays = intervalDays,
                                    startDate = intervalStartDate.toString()
                                )
                                ScheduleType.INTERVAL_HOURS -> ScheduleConfig.IntervalHours(
                                    intervalHours = intervalHours,
                                    startTime = intervalStartTime.toString()
                                )
                                ScheduleType.SPECIFIC_DATES -> ScheduleConfig.SpecificDates(
                                    dates = specificDates.map { it.toString() }.toSet()
                                )
                                ScheduleType.MONTHLY -> ScheduleConfig.Monthly(
                                    daysOfMonth = monthlyDays
                                )
                                ScheduleType.WEEKDAY_ONLY -> ScheduleConfig.WeekdayOnly
                                ScheduleType.WEEKEND_ONLY -> ScheduleConfig.WeekendOnly
                                else -> ScheduleConfig.Daily
                            }

                            // 권한이 있으면 알람 저장/수정
                            viewModel.saveAlarmWithSchedule(
                                pillId = pillId,
                                hour = hour,
                                minute = minute,
                                scheduleType = scheduleType,
                                scheduleConfig = scheduleConfig,
                                startDate = startDate,
                                endDate = endDate,
                                alarmSoundUri = alarmSoundUri,
                                alarmId = alarmId,
                                onComplete = onNavigateUp,
                                onShowAd = {
                                    activity?.let {
                                        adManager.showInterstitialAd(it)
                                    }
                                }
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = when (scheduleType) {
                    ScheduleType.DAILY -> isLoaded
                    ScheduleType.WEEKLY -> selectedDays.isNotEmpty() && isLoaded
                    ScheduleType.INTERVAL_DAYS -> intervalDays >= 1 && isLoaded
                    ScheduleType.INTERVAL_HOURS -> intervalHours >= 1 && isLoaded
                    ScheduleType.SPECIFIC_DATES -> specificDates.isNotEmpty() && isLoaded
                    ScheduleType.MONTHLY -> monthlyDays.isNotEmpty() && isLoaded
                    ScheduleType.WEEKDAY_ONLY -> isLoaded
                    ScheduleType.WEEKEND_ONLY -> isLoaded
                    else -> false
                },
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

    // 시작 시간 TimePicker (INTERVAL_HOURS용)
    if (showStartTimePicker) {
        TimePickerDialog(
            initialHour = intervalStartTime.hour,
            initialMinute = intervalStartTime.minute,
            onDismissRequest = { showStartTimePicker = false },
            onConfirm = { selectedHour, selectedMinute ->
                intervalStartTime = LocalTime.of(selectedHour, selectedMinute)
                showStartTimePicker = false
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

    // 날짜 선택 다이얼로그
    if (showDatePicker) {
        DatePickerDialog(
            initialDate = when (datePickerMode) {
                "start" -> startDate ?: LocalDate.now()
                "end" -> endDate ?: LocalDate.now()
                "intervalStart" -> intervalStartDate
                "specificDate" -> LocalDate.now()
                else -> LocalDate.now()
            },
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                when (datePickerMode) {
                    "start" -> startDate = selectedDate
                    "end" -> endDate = selectedDate
                    "intervalStart" -> intervalStartDate = selectedDate
                    "specificDate" -> {
                        if (selectedDate != null && selectedDate !in specificDates) {
                            specificDates = specificDates + selectedDate
                        }
                    }
                }
                showDatePicker = false
            },
            allowClear = datePickerMode in listOf("start", "end")
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
        ringtones.add(Pair(null, "기본 알람음"))

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
        title = { Text("알람음 선택") },
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
                Text("닫기")
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

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "날짜 선택",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false
                )
            }
        },
        confirmButton = {
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
                Text("확인")
            }
        },
        dismissButton = {
            Row {
                if (allowClear) {
                    TextButton(
                        onClick = {
                            onDateSelected(null)
                        }
                    ) {
                        Text("지우기")
                    }
                }
                TextButton(onClick = onDismissRequest) {
                    Text("취소")
                }
            }
        }
    )
}
