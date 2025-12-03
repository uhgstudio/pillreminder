package com.example.pillreminder.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pillreminder.R
import com.example.pillreminder.ui.theme.GradientGoldStart
import com.example.pillreminder.ui.theme.GradientPeachEnd
import com.example.pillreminder.data.model.IntakeHistory
import com.example.pillreminder.data.model.IntakeHistoryWithPill
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    
    val intakeHistory by viewModel.getIntakeHistoryForDate(selectedDate)
        .collectAsState(initial = emptyList())
    val intakeDates by viewModel.getIntakeDatesInRange(
        currentYearMonth.atDay(1),
        currentYearMonth.atEndOfMonth()
    ).collectAsState(initial = emptySet())

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
                CalendarHeader(
                    yearMonth = currentYearMonth,
                    onPreviousMonth = { currentYearMonth = currentYearMonth.minusMonths(1) },
                    onNextMonth = { currentYearMonth = currentYearMonth.plusMonths(1) }
                )

                CalendarGrid(
                    yearMonth = currentYearMonth,
                    selectedDate = selectedDate,
                    intakeDates = intakeDates,
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
    intakeDates: Set<LocalDate>,
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
            val hasIntake = intakeDates.contains(date)
            
            CalendarDay(
                date = date,
                isSelected = isSelected,
                isCurrentMonth = isCurrentMonth,
                hasIntake = hasIntake,
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
    hasIntake: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    hasIntake -> MaterialTheme.colorScheme.secondaryContainer
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
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
                    com.example.pillreminder.data.model.IntakeStatus.TAKEN ->
                        stringResource(R.string.status_taken)
                    com.example.pillreminder.data.model.IntakeStatus.SKIPPED ->
                        stringResource(R.string.status_skipped)
                },
                color = when (history.status) {
                    com.example.pillreminder.data.model.IntakeStatus.TAKEN ->
                        MaterialTheme.colorScheme.primary
                    com.example.pillreminder.data.model.IntakeStatus.SKIPPED ->
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