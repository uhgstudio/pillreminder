package com.uhstudio.pillreminder.data.export

/**
 * 전체 데이터를 내보내기/가져오기 위한 최상위 데이터 모델
 */
data class ExportData(
    val exportVersion: Int = 1,
    val exportDate: String,  // ISO 날짜-시간 형식
    val pills: List<PillExport>,
    val alarms: List<PillAlarmExport>,
    val history: List<IntakeHistoryExport>
)

/**
 * Pill 엔티티의 내보내기 형식
 */
data class PillExport(
    val id: String,
    val name: String,
    val imageUri: String?,
    val memo: String?
)

/**
 * PillAlarm 엔티티의 내보내기 형식
 */
data class PillAlarmExport(
    val id: String,
    val pillId: String,
    val hour: Int,
    val minute: Int,
    val repeatDays: List<String>,  // ["MONDAY", "TUESDAY", ...]
    val enabled: Boolean,
    val alarmSoundUri: String?
)

/**
 * IntakeHistory 엔티티의 내보내기 형식
 */
data class IntakeHistoryExport(
    val id: String,
    val pillId: String,
    val alarmId: String,
    val intakeTime: String,  // ISO 형식 (LocalDateTime을 문자열로)
    val status: String  // "TAKEN" 또는 "SKIPPED"
)
