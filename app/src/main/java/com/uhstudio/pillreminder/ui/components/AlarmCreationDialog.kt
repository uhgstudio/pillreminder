package com.uhstudio.pillreminder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uhstudio.pillreminder.R
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.data.model.ScheduleConfig
import com.uhstudio.pillreminder.data.model.ScheduleType
import com.uhstudio.pillreminder.ui.theme.StitchDeepBrown
import com.uhstudio.pillreminder.ui.theme.WarmPrimary
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmCreationDialog(
    pillId: String,
    editingAlarm: PillAlarm? = null,
    onDismiss: () -> Unit,
    onSave: (
        hour: Int,
        minute: Int,
        scheduleType: ScheduleType,
        scheduleConfig: ScheduleConfig,
        startDate: LocalDate?,
        endDate: LocalDate?,
        alarmSoundUri: String?,
        alarmSoundName: String
    ) -> Unit
) {
    val context = LocalContext.current
    
    // Initial data parsing
    val initialConfig = remember(editingAlarm) {
        if (editingAlarm?.scheduleConfig != null && editingAlarm.scheduleConfig.isNotBlank()) {
            try {
                when (editingAlarm.scheduleType) {
                    ScheduleType.DAILY -> ScheduleConfig.Daily
                    ScheduleType.WEEKLY -> Json { ignoreUnknownKeys = true }.decodeFromString<ScheduleConfig.Weekly>(editingAlarm.scheduleConfig)
                    ScheduleType.INTERVAL_DAYS -> Json { ignoreUnknownKeys = true }.decodeFromString<ScheduleConfig.IntervalDays>(editingAlarm.scheduleConfig)
                    ScheduleType.SPECIFIC_DATES -> Json { ignoreUnknownKeys = true }.decodeFromString<ScheduleConfig.SpecificDates>(editingAlarm.scheduleConfig)
                    else -> ScheduleConfig.Daily
                }
            } catch (e: Exception) {
                ScheduleConfig.Daily
            }
        } else {
            ScheduleConfig.Daily
        }
    }

    // State
    var hour by remember { mutableStateOf(editingAlarm?.hour ?: 8) }
    var minute by remember { mutableStateOf(editingAlarm?.minute ?: 30) }
    var scheduleType by remember { mutableStateOf(editingAlarm?.scheduleType ?: ScheduleType.DAILY) }
    
    var selectedDays by remember {
        mutableStateOf(
            if (initialConfig is ScheduleConfig.Weekly) initialConfig.toDayOfWeekSet()
            else setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        )
    }
    
    var dosage by remember { mutableStateOf("") } // Injected from caller or handled separately if needed
    var alarmSoundUri by remember { mutableStateOf(editingAlarm?.alarmSoundUri) }
    var alarmSoundName by remember { mutableStateOf("Birdsong") } // Default or from URI lookup

    // View States
    var showTimePicker by remember { mutableStateOf(false) }
    var showDosageDialog by remember { mutableStateOf(false) }
    var showSoundPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(32.dp),
        title = {
            Text(
                text = if (editingAlarm == null) stringResource(R.string.alarm_new_reminder) else stringResource(R.string.title_edit_medication),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = StitchDeepBrown)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AlarmScheduleSection(
                    hour = hour,
                    minute = minute,
                    onTimeClick = { showTimePicker = true },
                    selectedDays = selectedDays,
                    onDaysChange = { 
                        selectedDays = it
                        scheduleType = if (it.size == 7) ScheduleType.DAILY else ScheduleType.WEEKLY
                    },
                    // dosage = dosage, // Optional: if we want to include dosage in the dialog
                    // onDosageClick = { showDosageDialog = true },
                    alarmSoundName = alarmSoundName,
                    onSoundClick = { showSoundPicker = true }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val config = if (scheduleType == ScheduleType.DAILY) {
                        ScheduleConfig.Daily
                    } else {
                        ScheduleConfig.Weekly.from(selectedDays)
                    }
                    onSave(hour, minute, scheduleType, config, null, null, alarmSoundUri, alarmSoundName)
                },
                colors = ButtonDefaults.buttonColors(containerColor = WarmPrimary),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.label_done), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_cancel), color = Color.Gray)
            }
        }
    )

    // Sub-dialogs
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(32.dp),
            confirmButton = {
                TextButton(onClick = {
                    hour = timePickerState.hour
                    minute = timePickerState.minute
                    showTimePicker = false
                }) { Text(stringResource(R.string.ok), color = WarmPrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.btn_cancel), color = Color.Gray) }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            selectorColor = WarmPrimary,
                            periodSelectorSelectedContainerColor = Color(0xFFFFF3E0),
                            periodSelectorSelectedContentColor = WarmPrimary,
                            timeSelectorSelectedContainerColor = Color(0xFFFFF3E0),
                            timeSelectorSelectedContentColor = WarmPrimary
                        )
                    )
                }
            }
        )
    }

    if (showSoundPicker) {
        SoundPickerDialog(
            context = context,
            currentUri = alarmSoundUri,
            onDismissRequest = { showSoundPicker = false },
            onSoundSelected = { uri, name ->
                alarmSoundUri = uri
                alarmSoundName = name
                showSoundPicker = false
            }
        )
    }
}
