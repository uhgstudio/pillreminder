package com.uhstudio.pillreminder.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uhstudio.pillreminder.R
import com.uhstudio.pillreminder.data.model.IntakeHistoryWithPill
import com.uhstudio.pillreminder.data.model.IntakeStatus
import com.uhstudio.pillreminder.ui.calendar.CalendarViewModel
import com.uhstudio.pillreminder.ui.calendar.DateStatus
import com.uhstudio.pillreminder.ui.calendar.MonthlyStats
import com.uhstudio.pillreminder.ui.theme.StitchCream
import com.uhstudio.pillreminder.ui.theme.StitchMint
import com.uhstudio.pillreminder.ui.theme.StitchPeach
import com.uhstudio.pillreminder.ui.theme.StitchPrimary
import com.uhstudio.pillreminder.ui.theme.StitchSageDark
import com.uhstudio.pillreminder.ui.theme.StitchSecondary
import com.uhstudio.pillreminder.ui.theme.StitchTerracotta
import com.uhstudio.pillreminder.ui.theme.StitchTypography
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }

    val intakeHistory by viewModel.getIntakeHistoryForDate(selectedDate)
        .collectAsState(initial = emptyList())
    val dateStatusMap by viewModel.getDateStatusMap(
        currentYearMonth.atDay(1),
        currentYearMonth.atEndOfMonth()
    ).collectAsState(initial = emptyMap())

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFDFCF8))) { // Paper Background
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.calendar_wellness_history_title),
                            style = StitchTypography.headlineMedium.copy(color = StitchSageDark, fontWeight = FontWeight.Bold)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // Journal Header Section
                Text(
                    stringResource(R.string.calendar_motivation_quote),
                    style = StitchTypography.bodyMedium.copy(color = StitchSageDark.copy(alpha = 0.5f), lineHeight = 20.sp),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Calendar Container
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(32.dp), spotColor = StitchSageDark.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, StitchPeach.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CalendarHeader(
                            yearMonth = currentYearMonth,
                            onPreviousMonth = { currentYearMonth = currentYearMonth.minusMonths(1) },
                            onNextMonth = { currentYearMonth = currentYearMonth.plusMonths(1) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CalendarGrid(
                            yearMonth = currentYearMonth,
                            selectedDate = selectedDate,
                            dateStatusMap = dateStatusMap,
                            onDateSelected = { selectedDate = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Daily Log Section Title
                Text(
                    stringResource(R.string.calendar_daily_log),
                    style = StitchTypography.headlineSmall.copy(color = StitchSageDark, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
                )

                // Intake History List
                IntakeHistoryList(
                    intakeHistory = intakeHistory,
                    selectedDate = selectedDate,
                    modifier = Modifier.fillMaxWidth(),
                    viewModel = viewModel
                )

                Spacer(modifier = Modifier.height(100.dp))
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
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = yearMonth.format(DateTimeFormatter.ofPattern(stringResource(R.string.calendar_month_year_format), Locale.getDefault())),
                style = StitchTypography.titleLarge.copy(color = StitchSageDark, fontWeight = FontWeight.Bold)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.size(36.dp).background(StitchCream, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = StitchSageDark, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(36.dp).background(StitchCream, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = StitchSageDark, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekday Headers
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val days = listOf(
                stringResource(R.string.day_sun_short),
                stringResource(R.string.day_mon_short),
                stringResource(R.string.day_tue_short),
                stringResource(R.string.day_wed_short),
                stringResource(R.string.day_thu_short),
                stringResource(R.string.day_fri_short),
                stringResource(R.string.day_sat_short)
            )
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = StitchTypography.labelSmall.copy(color = StitchSageDark.copy(alpha = 0.3f), fontWeight = FontWeight.Bold)
                )
            }
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
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)
    val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0
    val totalCells = daysInMonth + dayOfWeekOffset
    val weeks = (totalCells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until weeks) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (j in 0 until 7) {
                    val dayIndex = i * 7 + j - dayOfWeekOffset + 1
                    if (dayIndex in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayIndex)
                        val isSelected = date == selectedDate
                        val isCurrentMonth = true // Optimized logic
                        // Fix for DateStatus logic: user might pass null if no data
                        val status = dateStatusMap[date]

                        Box(modifier = Modifier.weight(1f)) {
                            CalendarDay(
                                date = date,
                                isSelected = isSelected,
                                isCurrentMonth = isCurrentMonth,
                                status = status,
                                onClick = { onDateSelected(date) }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
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
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(if (isSelected) StitchTerracotta else Color.Transparent)
            .clickable(enabled = isCurrentMonth, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = StitchTypography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        !isCurrentMonth -> Color.Transparent
                        isSelected -> Color.White
                        else -> StitchSageDark
                    }
                )
            )
            
            // Minimal Dot status (Stitch style)
            if (status != null && isCurrentMonth && !isSelected) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .background(
                            if (status == DateStatus.TAKEN) StitchSecondary else StitchPrimary,
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun IntakeHistoryList(
    intakeHistory: List<IntakeHistoryWithPill>,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate = LocalDate.now(),
    viewModel: CalendarViewModel
) {
    if (intakeHistory.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.msg_no_intake_history),
                style = StitchTypography.bodyMedium.copy(color = StitchSageDark.copy(alpha = 0.3f))
            )
        }
    } else {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            intakeHistory.forEach { historyWithPill ->
                IntakeHistoryItem(
                    historyWithPill = historyWithPill,
                    onStatusToggle = { item ->
                        val newStatus = if (item.history.status == IntakeStatus.TAKEN) IntakeStatus.SKIPPED else IntakeStatus.TAKEN
                        viewModel.updateIntakeStatus(item.history.id, newStatus)
                    }
                )
            }
        }
    }
}

@Composable
fun IntakeHistoryItem(
    historyWithPill: IntakeHistoryWithPill,
    onStatusToggle: (IntakeHistoryWithPill) -> Unit
) {
    val history = historyWithPill.history
    val isTaken = history.status == IntakeStatus.TAKEN

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time Display
        Text(
            text = history.intakeTime.format(DateTimeFormatter.ofPattern("a hh:mm", Locale.getDefault())),
            style = StitchTypography.labelMedium.copy(color = StitchSageDark, fontWeight = FontWeight.Bold),
            modifier = Modifier.width(64.dp)
        )

        // Pill Card (Stitch Style)
        Card(
            modifier = Modifier
                .weight(1f)
                .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = StitchSageDark.copy(alpha = 0.05f))
                .clickable {
                    onStatusToggle(historyWithPill)
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, StitchPeach.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).background(StitchMint.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Medication, null, tint = StitchSecondary, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(historyWithPill.pill.name, style = StitchTypography.titleSmall.copy(color = StitchSageDark, fontWeight = FontWeight.Bold))
                        Text(
                             if(isTaken) stringResource(R.string.status_taken_label) else stringResource(R.string.status_skipped_label), 
                             style = StitchTypography.labelSmall.copy(color = StitchSageDark.copy(alpha = 0.4f))
                        )
                    }
                }

                // Status Icon
                Icon(
                    imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isTaken) StitchSecondary else StitchPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
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
        modifier = modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(32.dp), spotColor = StitchSageDark.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, StitchTerracotta.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.calendar_monthly_review, yearMonth.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))),
                        style = StitchTypography.titleLarge.copy(color = StitchSageDark, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        stringResource(R.string.calendar_doses_taken_summary, stats.takenCount, stats.totalCount),
                        style = StitchTypography.bodySmall.copy(color = StitchSageDark.copy(alpha = 0.4f))
                    )
                }
                
                // Progress Circle with Gradient
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { stats.adherenceRate / 100f },
                        modifier = Modifier.size(64.dp),
                        color = StitchTerracotta,
                        strokeWidth = 6.dp,
                        trackColor = StitchCream
                    )
                    Text(
                        "${stats.adherenceRate.toInt()}%",
                        style = StitchTypography.labelSmall.copy(fontWeight = FontWeight.Bold, color = StitchTerracotta)
                    )
                }
            }
        }
    }
}
