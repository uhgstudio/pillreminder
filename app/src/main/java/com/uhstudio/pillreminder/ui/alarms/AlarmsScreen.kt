package com.uhstudio.pillreminder.ui.alarms

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.uhstudio.pillreminder.ads.AdManager
import com.uhstudio.pillreminder.data.model.Pill
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.ui.theme.GradientPeachStart
import com.uhstudio.pillreminder.ui.theme.GradientPeachEnd
import com.uhstudio.pillreminder.util.toKoreanShort
import kotlinx.coroutines.launch
import com.uhstudio.pillreminder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    viewModel: AlarmsViewModel,
    onAddAlarmClick: (String) -> Unit = {},
    onEditAlarmClick: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adManager = remember { AdManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    val alarms by viewModel.allAlarms.collectAsState(initial = emptyList())
    val pills by viewModel.allPills.collectAsState(initial = emptyList())
    var alarmToDelete by remember { mutableStateOf<PillAlarm?>(null) }
    var showPillSelectionDialog by remember { mutableStateOf(false) }

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

    // 밝은 피치-오렌지 그라디언트 배경
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GradientPeachStart,
                        GradientPeachEnd
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,  // 배경 투명하게
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_alarms)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                if (pills.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { showPillSelectionDialog = true },
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.btn_add_alarm)
                        )
                    }
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
                    Text(
                        text = stringResource(R.string.msg_no_alarms),
                        style = MaterialTheme.typography.bodyLarge
                    )
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
                        val pill = pills.find { it.id == alarm.pillId }
                        AlarmItem(
                            alarm = alarm,
                            pill = pill,
                            onToggle = { enabled -> viewModel.toggleAlarm(alarm, enabled) },
                            onEdit = { onEditAlarmClick(alarm.pillId, alarm.id) },
                            onDelete = { alarmToDelete = alarm }
                        )
                    }
                }
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
                        viewModel.deleteAlarm(alarm)
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

    // 약 선택 다이얼로그
    if (showPillSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showPillSelectionDialog = false },
            title = { Text(stringResource(R.string.dialog_select_pill_title)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pills.forEach { pill ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPillSelectionDialog = false
                                    onAddAlarmClick(pill.id)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 약 이미지
                                if (pill.imageUri != null) {
                                    AsyncImage(
                                        model = pill.imageUri,
                                        contentDescription = pill.name,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline,
                                                shape = CircleShape
                                            ),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // 기본 아이콘
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Medication,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // 약 이름
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pill.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!pill.memo.isNullOrEmpty()) {
                                        Text(
                                            text = pill.memo,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPillSelectionDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun AlarmItem(
    alarm: PillAlarm,
    pill: Pill?,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 약 이미지
            if (pill != null) {
                if (pill.imageUri != null) {
                    AsyncImage(
                        model = pill.imageUri,
                        contentDescription = pill.name,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            ),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // 알람 정보
            Column(modifier = Modifier.weight(1f)) {
                if (pill != null) {
                    Text(
                        text = pill.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = if (alarm.repeatDays.isEmpty()) {
                        "반복 없음"
                    } else {
                        alarm.repeatDays.sortedBy { it.value }.joinToString(" ") { it.toKoreanShort() }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 액션 버튼들
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "알람 수정",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.btn_delete_alarm),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}
