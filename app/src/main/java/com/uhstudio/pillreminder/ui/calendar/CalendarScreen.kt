package com.uhstudio.pillreminder.ui.calendar

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uhstudio.pillreminder.ads.AdManager
import com.uhstudio.pillreminder.ui.theme.GradientGoldStart
import com.uhstudio.pillreminder.ui.theme.GradientPeachEnd
import com.uhstudio.pillreminder.data.model.IntakeHistoryWithPill
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import com.uhstudio.pillreminder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adManager = remember { AdManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }

    val intakeHistory by viewModel.getIntakeHistoryForDate(selectedDate)
        .collectAsState(initial = emptyList())
    val monthlyStats by viewModel.getMonthlyStats(currentYearMonth)
        .collectAsState(initial = MonthlyStats())
    val dateStatusMap by viewModel.getDateStatusMap(
        currentYearMonth.atDay(1),
        currentYearMonth.atEndOfMonth()
    ).collectAsState(initial = emptyMap())

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

    // 밝은 골드-피치 그라디언트 배경
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GradientGoldStart,
                        GradientPeachEnd
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,  // 배경 투명하게
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_calendar)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 월간 통계 카드
                MonthlyStatsCard(
                    yearMonth = currentYearMonth,
                    stats = monthlyStats,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                CalendarHeader(
                    yearMonth = currentYearMonth,
                    onPreviousMonth = { currentYearMonth = currentYearMonth.minusMonths(1) },
                    onNextMonth = { currentYearMonth = currentYearMonth.plusMonths(1) }
                )

                // 범례
                LegendRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                CalendarGrid(
                    yearMonth = currentYearMonth,
                    selectedDate = selectedDate,
                    dateStatusMap = dateStatusMap,
                    onDateSelected = { selectedDate = it }
                )

                IntakeHistoryList(
                    intakeHistory = intakeHistory,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CalendarHeader(
    yearMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        
        Text(
            text = yearMonth.format(DateTimeFormatter.ofPattern("yyyy년 M월")),
            style = MaterialTheme.typography.titleLarge
        )
        
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
        }
    }

    // 요일 헤더
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DayOfWeek.values().forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    dateStatusMap: Map<LocalDate, DateStatus>,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val firstDayOfGrid = firstDayOfMonth.minusDays(firstDayOfMonth.dayOfWeek.value.toLong() - 1)

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        items(42) { index ->
            val date = firstDayOfGrid.plusDays(index.toLong())
            val isSelected = date == selectedDate
            val isCurrentMonth = date.month == yearMonth.month
            val status = dateStatusMap[date]

            CalendarDay(
                date = date,
                isSelected = isSelected,
                isCurrentMonth = isCurrentMonth,
                status = status,
                onClick = { onDateSelected(date) }
            )
        }
    }
}

@Composable
fun CalendarDay(
    date: LocalDate,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    status: DateStatus?,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        status == DateStatus.TAKEN -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        status == DateStatus.SKIPPED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    !isCurrentMonth -> MaterialTheme.colorScheme.outline
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            // 상태 표시 점
            if (status != null && isCurrentMonth) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            when (status) {
                                DateStatus.TAKEN -> MaterialTheme.colorScheme.primary
                                DateStatus.SKIPPED -> MaterialTheme.colorScheme.error
                                DateStatus.NONE -> Color.Transparent
                            },
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        }
    }
}

@Composable
fun IntakeHistoryList(
    intakeHistory: List<IntakeHistoryWithPill>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.title_intake_history),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (intakeHistory.isEmpty()) {
                Text(
                    text = stringResource(R.string.msg_no_intake_history),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(intakeHistory.size) { index ->
                        IntakeHistoryItem(
                            historyWithPill = intakeHistory[index],
                            isLast = index == intakeHistory.size - 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IntakeHistoryItem(
    historyWithPill: IntakeHistoryWithPill,
    isLast: Boolean
) {
    val history = historyWithPill.history
    val pill = historyWithPill.pill

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pill.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = history.intakeTime.format(
                        DateTimeFormatter.ofPattern("a hh:mm")
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = when (history.status) {
                    com.uhstudio.pillreminder.data.model.IntakeStatus.TAKEN ->
                        stringResource(R.string.status_taken)
                    com.uhstudio.pillreminder.data.model.IntakeStatus.SKIPPED ->
                        stringResource(R.string.status_skipped)
                },
                color = when (history.status) {
                    com.uhstudio.pillreminder.data.model.IntakeStatus.TAKEN ->
                        MaterialTheme.colorScheme.primary
                    com.uhstudio.pillreminder.data.model.IntakeStatus.SKIPPED ->
                        MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
fun MonthlyStatsCard(
    yearMonth: YearMonth,
    stats: MonthlyStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.calendar_monthly_stats,
                    yearMonth.format(DateTimeFormatter.ofPattern("M월"))
                ),
                style = MaterialTheme.typography.titleMedium
            )

            if (stats.totalCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(
                                R.string.calendar_adherence_rate,
                                stats.adherenceRate.toInt()
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(
                                R.string.calendar_stats_detail,
                                stats.takenCount,
                                stats.skippedCount
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 복용률 원형 차트
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { stats.adherenceRate / 100f },
                            modifier = Modifier.size(60.dp),
                            strokeWidth = 6.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "${stats.adherenceRate.toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.msg_no_intake_history),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LegendRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.calendar_legend_title),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.calendar_legend_taken),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.calendar_legend_skipped),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}