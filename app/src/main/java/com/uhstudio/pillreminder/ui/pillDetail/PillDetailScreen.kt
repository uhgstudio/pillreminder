package com.uhstudio.pillreminder.ui.pillDetail

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uhstudio.pillreminder.R
import com.uhstudio.pillreminder.data.model.IntakeHistory
import com.uhstudio.pillreminder.data.model.IntakeStatus
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.data.model.ScheduleConfig
import com.uhstudio.pillreminder.data.model.ScheduleType
import com.uhstudio.pillreminder.ui.components.AlarmCreationDialog
import com.uhstudio.pillreminder.ui.theme.StitchCream
import com.uhstudio.pillreminder.ui.theme.StitchPeach
import com.uhstudio.pillreminder.ui.theme.StitchPrimary
import com.uhstudio.pillreminder.ui.theme.StitchSageDark
import com.uhstudio.pillreminder.ui.theme.StitchSecondary
import com.uhstudio.pillreminder.ui.theme.StitchTerracotta
import com.uhstudio.pillreminder.util.toKoreanShort
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PillDetailScreen(
    viewModel: PillDetailViewModel,
    pillId: String,
    onNavigateUp: () -> Unit,
    onAddAlarmClick: (String) -> Unit,
    onEditPillClick: () -> Unit,
    onEditAlarmClick: (String, String) -> Unit
) {
    val pill by viewModel.pill.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAlarmDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<PillAlarm?>(null) }
    
    LaunchedEffect(pillId) {
        viewModel.loadPill(pillId)
    }

    Box(modifier = Modifier.fillMaxSize().background(StitchCream)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                pill?.name ?: "",
                                style = MaterialTheme.typography.titleLarge.copy(color = StitchSageDark, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                stringResource(R.string.pill_detail_wellness_title),
                                style = MaterialTheme.typography.labelSmall.copy(color = StitchSageDark.copy(alpha = 0.5f))
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateUp,
                            modifier = Modifier.padding(8.dp).size(40.dp).background(Color.White, CircleShape).shadow(2.dp, CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = StitchSageDark, modifier = Modifier.size(18.dp))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onEditPillClick,
                            modifier = Modifier.padding(4.dp).size(40.dp).background(Color.White, CircleShape).shadow(2.dp, CircleShape)
                        ) {
                            Icon(Icons.Default.Settings, null, tint = StitchSageDark, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.padding(4.dp).size(40.dp).background(Color.White, CircleShape).shadow(2.dp, CircleShape)
                        ) {
                            Icon(Icons.Default.Delete, null, tint = StitchTerracotta, modifier = Modifier.size(18.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = StitchCream.copy(alpha = 0.9f))
                )
            }
        ) { paddingValues ->
            if (pill == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StitchPrimary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Your Routine Card (Stitch Style)
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(32.dp), spotColor = StitchSageDark.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
                        border = BorderStroke(1.dp, StitchSecondary.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(StitchPeach.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (pill?.imageUri != null) {
                                    AsyncImage(
                                        model = pill?.imageUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Medication, null, tint = StitchTerracotta, modifier = Modifier.size(32.dp))
                                }
                            }
                            Column {
                                Text(stringResource(R.string.pill_detail_routine_title), style = MaterialTheme.typography.titleMedium.copy(color = StitchSageDark, fontWeight = FontWeight.Bold))
                                Text(stringResource(R.string.pill_detail_reminders_active), style = MaterialTheme.typography.bodySmall.copy(color = StitchSageDark.copy(alpha = 0.6f)))
                            }
                        }
                    }

                    // Daily Alarms Section
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.pill_detail_daily_alarms), style = MaterialTheme.typography.headlineSmall.copy(color = StitchSageDark, fontWeight = FontWeight.Bold))
                            Surface(color = StitchSecondary.copy(alpha = 0.2f), shape = CircleShape) {
                                Text(
                                    stringResource(R.string.pill_detail_enabled),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(color = StitchSageDark, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        val alarms = viewModel.getAlarms(pill!!.id).collectAsState(initial = emptyList())
                        for (alarm in alarms.value) {
                            AlarmItem(
                                alarm = alarm,
                                onEdit = { 
                                    editingAlarm = alarm
                                    showAlarmDialog = true
                                },
                                onToggle = { enabled -> viewModel.toggleAlarm(alarm, enabled) }
                            )
                        }
                    }

                    // New Reminder Button (Stitch Style)
                    Button(
                        onClick = { 
                            editingAlarm = null
                            showAlarmDialog = true 
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp).shadow(12.dp, RoundedCornerShape(24.dp), spotColor = StitchSageDark.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StitchSageDark)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(24.dp))
                            Text(stringResource(R.string.alarm_new_reminder), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    // Motivational Quote (from HTML)
                    Text(
                        stringResource(R.string.pill_detail_motivation_quote),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.bodySmall.copy(color = StitchSageDark.copy(alpha = 0.4f), textAlign = TextAlign.Center, lineHeight = 18.sp)
                    )

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }

        // Pill Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text(stringResource(R.string.dialog_delete_pill_title), fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.dialog_delete_pill_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pill?.let {
                                viewModel.deletePill(it) {
                                    showDeleteConfirmDialog = false
                                    onNavigateUp()
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = StitchTerracotta)
                    ) {
                        Text(stringResource(R.string.btn_delete), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text(stringResource(R.string.btn_cancel), color = Color.Gray)
                    }
                }
            )
        }

        // Alarm Creation/Edit Dialog
        if (showAlarmDialog) {
            AlarmCreationDialog(
                pillId = pillId,
                editingAlarm = editingAlarm,
                onDismiss = { showAlarmDialog = false },
                onSave = { h, m, type, config, start, end, soundUri, soundName ->
                    viewModel.saveAlarm(
                        pillId = pillId,
                        hour = h,
                        minute = m,
                        scheduleType = type,
                        scheduleConfig = config,
                        startDate = start,
                        endDate = end,
                        alarmSoundUri = soundUri,
                        alarmId = editingAlarm?.id,
                        onComplete = {
                            showAlarmDialog = false
                        }
                    )
                }
            )
        }
    }
}

@Composable private fun AlarmItem(
    alarm: PillAlarm,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    // Choose icon and color based on time (from HTML)
    val (icon, bgColor, tint) = when {
        alarm.hour < 11 -> Triple(Icons.Default.WbSunny, Color(0xFFF4F7F4), StitchSageDark)
        alarm.hour < 17 -> Triple(Icons.Default.WbTwilight, Color(0xFFFDF8F6), StitchTerracotta)
        else -> Triple(Icons.Default.Bedtime, Color(0xFFF4F7F4), StitchSageDark.copy(alpha = 0.6f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = StitchSageDark.copy(alpha = 0.05f))
            .clickable { onEdit() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, StitchPeach.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(bgColor, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
                }
                Column {
                    val amPm = if (alarm.hour < 12) stringResource(R.string.label_am) else stringResource(R.string.label_pm)
                    val hour12 = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%02d:%02d", hour12, alarm.minute),
                            style = MaterialTheme.typography.headlineMedium.copy(color = StitchSageDark, fontWeight = FontWeight.Bold)
                        )
                        Text(amPm, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp), style = MaterialTheme.typography.labelMedium.copy(color = Color.LightGray))
                    }
                    Text(
                        text = if (alarm.hour < 11) stringResource(R.string.pill_detail_morning_schedule) else stringResource(R.string.pill_detail_scheduled_dose),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                }
            }

            // iOS-style Toggle (Custom)
            Switch(
                checked = alarm.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = StitchSageDark,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.LightGray.copy(alpha = 0.3f),
                    checkedBorderColor = Color.Transparent,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

/**
 * 복용 기록 섹션 - 접힘/펼침 가능, 최근 기록만 표시
 */
@Composable private fun IntakeHistorySection(
    history: List<IntakeHistory>,
    onStatusChange: (String, IntakeStatus) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }

    // 날짜별로 그룹화
    val groupedHistory = remember(history) {
        history.groupBy { it.intakeTime.toLocalDate() }
            .toSortedMap(reverseOrder())
    }

    // 표시할 기록 수 (최근 5개 또는 전체)
    val displayLimit = if (showAll) Int.MAX_VALUE else 5

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 헤더 (클릭 시 펼침/접힘)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.pill_detail_intake_history),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 총 기록 수 표시
                    Text(
                        text = "${history.size}${stringResource(R.string.pill_detail_history_count)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) stringResource(R.string.btn_collapse) else stringResource(R.string.btn_expand),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 펼쳐진 내용
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (history.isEmpty()) {
                        Text(
                            text = stringResource(R.string.pill_detail_no_history),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        var displayedCount = 0

                        val todayText = stringResource(R.string.label_today)
                        val yesterdayText = stringResource(R.string.label_yesterday)
                        val dateFormatPattern = stringResource(R.string.date_format_ymd)

                        groupedHistory.forEach { (date, records) ->
                            if (displayedCount >= displayLimit) return@forEach

                            // 날짜 헤더
                            val dateFormatter = DateTimeFormatter.ofPattern(dateFormatPattern, Locale.getDefault())
                            val isToday = date == LocalDate.now()
                            val isYesterday = date == LocalDate.now().minusDays(1)

                            val dateText = when {
                                isToday -> todayText
                                isYesterday -> yesterdayText
                                else -> date.format(dateFormatter)
                            }

                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = if (displayedCount > 0) 8.dp else 0.dp, bottom = 4.dp)
                            )

                            // 해당 날짜의 기록들
                            records.take(displayLimit - displayedCount).forEach { record ->
                                IntakeHistoryItem(
                                    history = record,
                                    onStatusChange = onStatusChange
                                )
                                displayedCount++
                            }
                        }

                        // 더 보기 / 접기 버튼
                        if (history.size > 5) {
                            TextButton(
                                onClick = { showAll = !showAll },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text = if (showAll) {
                                        stringResource(R.string.pill_detail_show_less)
                                    } else {
                                        stringResource(R.string.pill_detail_show_more, history.size - 5)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 복용 기록 개별 아이템
 */
@Composable private fun IntakeHistoryItem(
    history: IntakeHistory,
    onStatusChange: (String, IntakeStatus) -> Unit
) {
    var isTaken by remember(history.id, history.status) {
        mutableStateOf(history.status == IntakeStatus.TAKEN)
    }

    val timeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // 상태 아이콘
            Icon(
                imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Default.RemoveCircle,
                contentDescription = null,
                tint = if (isTaken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )

            // 시간
            Text(
                text = history.intakeTime.format(timeFormatter),
                style = MaterialTheme.typography.bodyMedium
            )

            // 상태 텍스트
            Text(
                text = if (isTaken) stringResource(R.string.status_taken) else stringResource(R.string.status_skipped),
                style = MaterialTheme.typography.bodySmall,
                color = if (isTaken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        // 토글 스위치
        Switch(
            checked = isTaken,
            onCheckedChange = { checked ->
                isTaken = checked
                val newStatus = if (checked) IntakeStatus.TAKEN else IntakeStatus.SKIPPED
                onStatusChange(history.id, newStatus)
            },
            modifier = Modifier.height(24.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}


private fun getScheduleDescriptionInternal(
    alarm: PillAlarm,
    context: Context,
    dailyText: String,
    noRepeatText: String,
    customText: String,
    specificDatesNoneText: String,
    specificDatesText: String
): String {
    timber.log.Timber.d("PillDetail getScheduleDescription: alarmId=${alarm.id}, scheduleType=${alarm.scheduleType}, scheduleConfig=${alarm.scheduleConfig}, repeatDays=${alarm.repeatDays}")

    return try {
        when (alarm.scheduleType) {
            ScheduleType.DAILY -> {
                timber.log.Timber.d("PillDetail: DAILY type")
                dailyText
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
                        noRepeatText
                    }
                    days.size == 7 -> dailyText
                    else -> days.sortedBy { it.value }.joinToString(" ") { it.toKoreanShort() }
                }
            }
            ScheduleType.INTERVAL_DAYS -> {
                timber.log.Timber.d("PillDetail: INTERVAL_DAYS type")
                if (alarm.scheduleConfig == null || alarm.scheduleConfig.isBlank()) {
                    return context.getString(R.string.schedule_every_n_days, 0)
                }
                val config = Json { ignoreUnknownKeys = true }.decodeFromString<ScheduleConfig.IntervalDays>(alarm.scheduleConfig)
                context.getString(R.string.schedule_every_n_days, config.intervalDays)
            }
            ScheduleType.SPECIFIC_DATES -> {
                timber.log.Timber.d("PillDetail: SPECIFIC_DATES type")
                if (alarm.scheduleConfig == null || alarm.scheduleConfig.isBlank()) {
                    return specificDatesText
                }
                val config = Json { ignoreUnknownKeys = true }.decodeFromString<ScheduleConfig.SpecificDates>(alarm.scheduleConfig)
                val dates = config.getDatesAsLocalDateSet()
                if (dates.isEmpty()) {
                    specificDatesNoneText
                } else {
                    context.getString(R.string.schedule_specific_dates_count, dates.size)
                }
            }
            ScheduleType.CUSTOM -> customText
        }
    } catch (e: Exception) {
        timber.log.Timber.e(e, "PillDetail: Exception in getScheduleDescription")
        // 파싱 실패 시 레거시 방식으로 fallback
        @Suppress("DEPRECATION")
        val days = alarm.repeatDays
        timber.log.Timber.d("PillDetail: Fallback to repeatDays: $days")
        when {
            days.isEmpty() -> noRepeatText
            days.size == 7 -> dailyText
            else -> days.sortedBy { it.value }.joinToString(" ") { it.toKoreanShort() }
        }
    }
} 