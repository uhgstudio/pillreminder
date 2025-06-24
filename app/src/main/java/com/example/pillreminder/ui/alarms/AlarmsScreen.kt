package com.example.pillreminder.ui.alarms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pillreminder.R
import com.example.pillreminder.data.model.Pill
import com.example.pillreminder.data.model.PillAlarm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    viewModel: AlarmsViewModel,
    onAddAlarmClick: (String) -> Unit = {}
) {
    val alarms by viewModel.allAlarms.collectAsState(initial = emptyList())
    val pills by viewModel.allPills.collectAsState(initial = emptyList())
    var showPillSelectionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_alarms)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (pills.isNotEmpty()) {
                        showPillSelectionDialog = true
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.btn_add_alarm)
                )
            }
        }
    ) { paddingValues ->
        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.msg_no_alarms),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (pills.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.msg_add_alarm_hint),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "먼저 약을 등록해주세요.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alarms) { alarm ->
                    AlarmItem(
                        alarm = alarm,
                        pillName = pills.find { it.id == alarm.pillId }?.name ?: "알 수 없는 약",
                        onToggle = { enabled ->
                            viewModel.toggleAlarm(alarm.id, enabled)
                        }
                    )
                }
            }
        }
    }

    if (showPillSelectionDialog) {
        PillSelectionDialog(
            pills = pills,
            onPillSelected = { pill ->
                onAddAlarmClick(pill.id)
                showPillSelectionDialog = false
            },
            onDismissRequest = { showPillSelectionDialog = false }
        )
    }
}

@Composable
private fun AlarmItem(
    alarm: PillAlarm,
    pillName: String,
    onToggle: (Boolean) -> Unit
) {
    val koreanDays = alarm.repeatDays.map { day ->
        when (day) {
            java.time.DayOfWeek.MONDAY -> "월"
            java.time.DayOfWeek.TUESDAY -> "화"
            java.time.DayOfWeek.WEDNESDAY -> "수"
            java.time.DayOfWeek.THURSDAY -> "목"
            java.time.DayOfWeek.FRIDAY -> "금"
            java.time.DayOfWeek.SATURDAY -> "토"
            java.time.DayOfWeek.SUNDAY -> "일"
        }
    }.joinToString(", ")

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = pillName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = koreanDays,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Switch(
                checked = alarm.enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun PillSelectionDialog(
    pills: List<Pill>,
    onPillSelected: (Pill) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("약 선택") },
        text = {
            LazyColumn {
                items(pills) { pill ->
                    TextButton(
                        onClick = { onPillSelected(pill) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = pill.name,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
} 