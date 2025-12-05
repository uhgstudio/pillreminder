package com.uhstudio.pillreminder.ui.home

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.uhstudio.pillreminder.ads.AdManager
import com.uhstudio.pillreminder.data.model.Pill
import com.uhstudio.pillreminder.ui.theme.GradientPeachStart
import com.uhstudio.pillreminder.ui.theme.GradientLightGrayEnd
import kotlinx.coroutines.launch
import com.uhstudio.pillreminder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddPillClick: () -> Unit,
    onPillClick: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adManager = remember { AdManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    val pills by viewModel.pills.collectAsState(initial = emptyList())
    val todayAlarms by viewModel.todayAlarms.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()
    val missedAlarmsCount by viewModel.missedAlarmsCount.collectAsState()
    var pillToDelete by remember { mutableStateOf<Pill?>(null) }

    // 화면 방문 시 광고 체크
    LaunchedEffect(Unit) {
        scope.launch {
            val shouldShow = adManager.incrementAndCheckScreenVisit()
            if (shouldShow) {
                adManager.loadInterstitialAd {
                    activity?.let {
                        adManager.showInterstitialAd(it)
                    }
                }
            }
        }
    }

    // 깔끔한 화이트-그레이 그라디언트 배경
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GradientPeachStart,
                        GradientLightGrayEnd
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,  // 배경 투명하게
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_home)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddPillClick,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.btn_add_pill)
                    )
                }
            }
        ) { paddingValues ->
            if (pills.isEmpty()) {
                EmptyPillList(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 미복용 알람 경고 배너
                    if (missedAlarmsCount > 0) {
                        item {
                            MissedAlarmsWarningBanner(count = missedAlarmsCount)
                        }
                    }

                    // 오늘의 알람 섹션
                    if (todayAlarms.isNotEmpty()) {
                        item {
                            TodayAlarmsSection(alarms = todayAlarms)
                        }
                    }

                    // 복용 통계 카드
                    if (todayAlarms.isNotEmpty()) {
                        item {
                            IntakeStatsCard(stats = todayStats)
                        }
                    }

                    // 내 약 목록 섹션 헤더
                    item {
                        Text(
                            text = stringResource(R.string.home_my_pills),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    // 약 목록
                    items(pills) { pill ->
                        PillItem(
                            pill = pill,
                            onPillClick = { onPillClick(pill.id) },
                            onDeleteClick = { pillToDelete = pill }
                        )
                    }
                }
            }
        }
    }

    // 삭제 확인 다이얼로그
    pillToDelete?.let { pill ->
        AlertDialog(
            onDismissRequest = { pillToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_pill_title)) },
            text = { Text(stringResource(R.string.dialog_delete_pill_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePill(pill)
                        pillToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pillToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
fun EmptyPillList(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.empty_pill_list),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PillItem(
    pill: Pill,
    onPillClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onPillClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (pill.imageUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(pill.imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = pill.name,
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.CenterVertically),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.small
                        )
                        .align(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = pill.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (!pill.memo.isNullOrBlank()) {
                    Text(
                        text = pill.memo,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.align(Alignment.Top)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.btn_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun MissedAlarmsWarningBanner(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = stringResource(R.string.home_missed_alarms_warning, count),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun TodayAlarmsSection(alarms: List<TodayAlarm>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_today_alarms),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.home_alarm_count, alarms.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            if (alarms.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_no_alarms_today),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                alarms.forEach { todayAlarm ->
                    TodayAlarmItem(todayAlarm)
                }
            }
        }
    }
}

@Composable
fun TodayAlarmItem(todayAlarm: TodayAlarm) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 시간 표시
        Text(
            text = String.format("%02d:%02d", todayAlarm.time.hour, todayAlarm.time.minute),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(60.dp)
        )

        // 약 이름
        Text(
            text = todayAlarm.pill.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // 상태 표시
        if (todayAlarm.isTaken) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.home_alarm_taken),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            val now = java.time.LocalDateTime.now()
            val status = if (todayAlarm.time.isAfter(now)) {
                stringResource(R.string.home_alarm_upcoming)
            } else {
                stringResource(R.string.home_alarm_missed)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (todayAlarm.time.isAfter(now))
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (todayAlarm.time.isAfter(now))
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun IntakeStatsCard(stats: IntakeStats) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.home_today_stats),
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 복용률 원형 차트
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { stats.adherenceRate / 100f },
                        modifier = Modifier.size(80.dp),
                        strokeWidth = 8.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.home_intake_rate, stats.adherenceRate.toInt()),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // 통계 정보
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_taken_count, stats.takenCount, stats.totalCount),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${stringResource(R.string.status_skipped)}: ${stats.skippedCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}