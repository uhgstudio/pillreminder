package com.example.pillreminder.ui.pillDetail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pillreminder.R
import com.example.pillreminder.data.model.PillAlarm
import kotlinx.coroutines.flow.Flow
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PillDetailScreen(
    viewModel: PillDetailViewModel,
    pillId: String,
    onNavigateUp: () -> Unit,
    onAddAlarmClick: (String) -> Unit,
    onEditPillClick: (String) -> Unit,
    onAlarmClick: (String) -> Unit
) {
    val pill by viewModel.pill.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(pillId) {
        viewModel.loadPill(pillId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pill?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                actions = {
                    // 수정 버튼
                    pill?.let { currentPill ->
                        IconButton(
                            onClick = { onEditPillClick(currentPill.id) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "약 정보 수정"
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.btn_delete)
                        )
                    }
                }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 약 이미지
            pill?.imageUri?.let { uri ->
                // 파일 경로인지 URI인지 확인
                val imageModel = if (uri.startsWith("content://") || uri.startsWith("file://")) {
                    uri
                } else {
                    // 내부 저장소의 파일 경로인 경우
                    File(uri)
                }
                
                AsyncImage(
                    model = imageModel,
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
                    onAlarmClick = onAlarmClick,
                    onAlarmDelete = { alarm ->
                        viewModel.deleteAlarm(alarm)
                    }
                )
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
    onAlarmClick: (String) -> Unit,
    onAlarmDelete: (PillAlarm) -> Unit
) {
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
                    onAlarmClick = { onAlarmClick(alarm.id) },
                    onAlarmDelete = onAlarmDelete
                )
            }
        }
    }
}

@Composable
private fun AlarmItem(
    alarm: PillAlarm,
    onAlarmClick: (String) -> Unit,
    onAlarmDelete: (PillAlarm) -> Unit
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
        modifier = Modifier.fillMaxWidth(),
        onClick = { onAlarmClick(alarm.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = koreanDays,
                style = MaterialTheme.typography.bodyMedium
            )

            IconButton(
                onClick = { onAlarmDelete(alarm) }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "알람 삭제"
                )
            }
        }
    }
} 