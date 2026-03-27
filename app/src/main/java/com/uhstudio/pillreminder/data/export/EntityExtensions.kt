package com.uhstudio.pillreminder.data.export

import com.uhstudio.pillreminder.data.model.IntakeHistory
import com.uhstudio.pillreminder.data.model.IntakeStatus
import com.uhstudio.pillreminder.data.model.Pill
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.data.model.ScheduleType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Entity를 Export 모델로 변환하는 extension functions
 */

// Pill 변환
fun Pill.toExport(): PillExport {
    return PillExport(
        id = this.id,
        name = this.name,
        imageUri = this.imageUri,
        memo = this.memo,
        type = this.type,
        dosage = this.dosage,
        quantity = this.quantity,
        lowStockThreshold = this.lowStockThreshold
    )
}

fun PillExport.toPill(): Pill {
    return Pill(
        id = this.id,
        name = this.name,
        imageUri = this.imageUri,
        memo = this.memo,
        type = this.type,
        dosage = this.dosage,
        quantity = this.quantity,
        lowStockThreshold = this.lowStockThreshold
    )
}

// PillAlarm 변환
fun PillAlarm.toExport(): PillAlarmExport {
    @Suppress("DEPRECATION")
    return PillAlarmExport(
        id = this.id,
        pillId = this.pillId,
        hour = this.hour,
        minute = this.minute,
        repeatDays = this.repeatDays.map { it.name },
        enabled = this.enabled,
        alarmSoundUri = this.alarmSoundUri,
        scheduleType = this.scheduleType.name,
        scheduleConfig = this.scheduleConfig,
        startDate = this.startDate?.toString(),
        endDate = this.endDate?.toString(),
        createdAt = this.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        updatedAt = this.updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}

fun PillAlarmExport.toPillAlarm(): PillAlarm {
    return PillAlarm(
        id = this.id,
        pillId = this.pillId,
        hour = this.hour,
        minute = this.minute,
        repeatDays = this.repeatDays.mapNotNull { dayName ->
            try {
                DayOfWeek.valueOf(dayName)
            } catch (e: IllegalArgumentException) {
                null
            }
        }.toSet(),
        enabled = this.enabled,
        alarmSoundUri = this.alarmSoundUri,
        scheduleType = try {
            ScheduleType.valueOf(this.scheduleType)
        } catch (e: IllegalArgumentException) {
            ScheduleType.WEEKLY
        },
        scheduleConfig = this.scheduleConfig,
        startDate = this.startDate?.let { LocalDate.parse(it) },
        endDate = this.endDate?.let { LocalDate.parse(it) },
        createdAt = this.createdAt?.let {
            LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } ?: LocalDateTime.now(),
        updatedAt = this.updatedAt?.let {
            LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } ?: LocalDateTime.now()
    )
}

// IntakeHistory 변환
fun IntakeHistory.toExport(): IntakeHistoryExport {
    return IntakeHistoryExport(
        id = this.id,
        pillId = this.pillId,
        alarmId = this.alarmId,
        intakeTime = this.intakeTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        status = this.status.name
    )
}

fun IntakeHistoryExport.toIntakeHistory(): IntakeHistory {
    return IntakeHistory(
        id = this.id,
        pillId = this.pillId,
        alarmId = this.alarmId,
        intakeTime = LocalDateTime.parse(this.intakeTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        status = try {
            IntakeStatus.valueOf(this.status)
        } catch (e: IllegalArgumentException) {
            IntakeStatus.TAKEN // 기본값
        }
    )
}
