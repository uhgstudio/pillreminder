package com.example.pillreminder.ui.pillDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.pillreminder.R
import com.example.pillreminder.data.model.PillAlarm
import com.example.pillreminder.ui.theme.GradientPinkStart
import com.example.pillreminder.ui.theme.GradientPeachEnd
import com.example.pillreminder.util.toKoreanShort
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PillDetailScreen(
    viewModel: PillDetailViewModel,
    pillId: String,
    onNavigateUp: () -> Unit,
    onAddAlarmClick: (String) -> Unit,
    onEditPillClick: () -> Unit
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
                                imageVector = Icons.Default.ArrowBack,
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
                    onDeleteAlarm = { alarm -> viewModel.deleteAlarm(alarm) }
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
    onDeleteAlarm: (PillAlarm) -> Unit
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
                    onDelete = { alarmToDelete = alarm }
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
    onDelete: () -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = alarm.repeatDays.sortedBy { it.value }.joinToString(" ") { it.toKoreanShort() },
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