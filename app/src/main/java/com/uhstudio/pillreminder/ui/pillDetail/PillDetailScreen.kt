package com.uhstudio.pillreminder.ui.pillDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.data.model.ScheduleConfig
import com.uhstudio.pillreminder.data.model.ScheduleType
import com.uhstudio.pillreminder.ui.theme.GradientPinkStart
import com.uhstudio.pillreminder.ui.theme.GradientPeachEnd
import com.uhstudio.pillreminder.util.toKoreanShort
import com.uhstudio.pillreminder.R
import kotlinx.serialization.json.Json
import timber.log.Timber
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PillDetailScreen(
    viewModel: PillDetailViewModel,
    pillId: String,
    onNavigateUp: () -> Unit,
    onAddAlarmClick: (String) -> Unit,
    onEditPillClick: () -> Unit,
    onEditAlarmClick: (String, String) -> Unit  // (pillId, alarmId) -> Unit
) {
    val pill by viewModel.pill.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(pillId) {
        viewModel.loadPill(pillId)
    }

    // 밝은 핑크-피치 그라디언트 배경
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GradientPinkStart,
                        GradientPeachEnd
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,  // 배경 투명하게
            topBar = {
                TopAppBar(
                    title = { Text(pill?.name ?: "") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onEditPillClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.title_edit_pill)
                            )
                        }
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.btn_delete)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { pill?.let { onAddAlarmClick(it.id) } }
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = stringResource(R.string.btn_add_alarm)
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // 약 이미지
            pill?.imageUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // 약 정보
            pill?.let { pill ->
                Text(
                    text = pill.name,
                    style = MaterialTheme.typography.headlineMedium
                )

                pill.memo?.let { memo ->
                    Text(
                        text = memo,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // 알람 목록
                val alarms = viewModel.getAlarms(pill.id).collectAsState(initial = emptyList())
                AlarmList(
                    alarms = alarms.value,
                    onDeleteAlarm = { alarm -> viewModel.deleteAlarm(alarm) },
                    onEditAlarm = { alarm -> onEditAlarmClick(pill.id, alarm.id) }
                )
            }
        }
    }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_pill_title)) },
            text = { Text(stringResource(R.string.dialog_delete_pill_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pill?.let { viewModel.deletePill(it, onNavigateUp) }
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun AlarmList(
    alarms: List<PillAlarm>,
    onDeleteAlarm: (PillAlarm) -> Unit,
    onEditAlarm: (PillAlarm) -> Unit
) {
    var alarmToDelete by remember { mutableStateOf<PillAlarm?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.title_alarms),
            style = MaterialTheme.typography.titleMedium
        )

        if (alarms.isEmpty()) {
            Text(
                text = stringResource(R.string.msg_no_alarms),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            alarms.forEach { alarm ->
                AlarmItem(
                    alarm = alarm,
                    onDelete = { alarmToDelete = alarm },
                    onEdit = { onEditAlarm(alarm) }
                )
            }
        }
    }

    // 알람 삭제 확인 다이얼로그
    alarmToDelete?.let { alarm ->
        AlertDialog(
            onDismissRequest = { alarmToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_alarm_title)) },
            text = { Text(stringResource(R.string.dialog_delete_alarm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAlarm(alarm)
                        alarmToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { alarmToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun AlarmItem(
    alarm: PillAlarm,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val scheduleDescription = remember(alarm) {
        getScheduleDescription(alarm)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = scheduleDescription,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.btn_delete_alarm),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun getScheduleDescription(alarm: PillAlarm): String {
    timber.log.Timber.d("PillDetail getScheduleDescription: alarmId=${alarm.id}, scheduleType=${alarm.scheduleType}, scheduleConfig=${alarm.scheduleConfig}, repeatDays=${alarm.repeatDays}")

    return try {
        when (alarm.scheduleType) {
            ScheduleType.DAILY -> {
                timber.log.Timber.d("PillDetail: DAILY type")
                "매일"
            }
            ScheduleType.WEEKLY -> {
                timber.log.Timber.d("PillDetail: WEEKLY type")

                // 먼저 scheduleConfig 시도
                val days = if (alarm.scheduleConfig != null && alarm.scheduleConfig.isNotBlank()) {
                    try {
                        val config = Json { ignoreUnknownKeys = true }.decodeFromString<ScheduleConfig.Weekly>(alarm.scheduleConfig)
                        val parsedDays = config.toDayOfWeekSet()
                        timber.log.Timber.d("PillDetail: Parsed days from scheduleConfig: $parsedDays")
                        parsedDays
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "PillDetail: Failed to parse scheduleConfig, falling back to repeatDays")
                        @Suppress("DEPRECATION")
                        alarm.repeatDays
                    }
                } else {
                    timber.log.Timber.d("PillDetail: No scheduleConfig, using repeatDays: ${alarm.repeatDays}")
                    @Suppress("DEPRECATION")
                    alarm.repeatDays
                }

                timber.log.Timber.d("PillDetail: Final days for WEEKLY: $days")
                when {
                    days.isEmpty() -> {
                        timber.log.Timber.w("PillDetail: Days is empty!")
                        "반복 없음"
                    }
                    days.size == 7 -> "매일"
                    else -> days.sortedBy { it.value }.joinToString(" ") { it.toKoreanShort() }
                }
            }
            ScheduleType.INTERVAL_DAYS -> {
                timber.log.Timber.d("PillDetail: INTERVAL_DAYS type")
                if (alarm.scheduleConfig == null || alarm.scheduleConfig.isBlank()) {
                    return "N일 마다"
                }
                val config = Json { ignoreUnknownKeys = true }.decodeFromString<ScheduleConfig.IntervalDays>(alarm.scheduleConfig)
                "${config.intervalDays}일 마다"
            }
            ScheduleType.SPECIFIC_DATES -> {
                timber.log.Timber.d("PillDetail: SPECIFIC_DATES type")
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
        timber.log.Timber.e(e, "PillDetail: Exception in getScheduleDescription")
        // 파싱 실패 시 레거시 방식으로 fallback
        @Suppress("DEPRECATION")
        val days = alarm.repeatDays
        timber.log.Timber.d("PillDetail: Fallback to repeatDays: $days")
        when {
            days.isEmpty() -> "반복 없음"
            days.size == 7 -> "매일"
            else -> days.sortedBy { it.value }.joinToString(" ") { it.toKoreanShort() }
        }
    }
} 