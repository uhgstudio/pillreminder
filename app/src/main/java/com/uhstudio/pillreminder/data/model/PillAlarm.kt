package com.uhstudio.pillreminder.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 약 복용 알람 정보를 저장하는 데이터 클래스
 * @property id 알람 고유 식별자
 * @property pillId 약 ID (외래키)
 * @property hour 알람 시간 (시)
 * @property minute 알람 시간 (분)
 * @property scheduleType 스케줄 타입 (v5+)
 * @property scheduleConfig 스케줄 설정 (JSON 문자열, v5+)
 * @property startDate 복용 시작일 (v5+)
 * @property endDate 복용 종료일 (v5+)
 * @property repeatDays 반복할 요일 목록 (하위 호환성, deprecated)
 * @property enabled 알람 활성화 여부
 * @property alarmSoundUri 알람음 URI (null이면 기본 알람음 사용)
 * @property createdAt 생성 시간 (v5+)
 * @property updatedAt 수정 시간 (v5+)
 */
@Entity(
    tableName = "pill_alarms",
    foreignKeys = [
        ForeignKey(
            entity = Pill::class,
            parentColumns = ["id"],
            childColumns = ["pillId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["pillId"]),
        androidx.room.Index(value = ["enabled"]),
        androidx.room.Index(value = ["hour", "minute"]),
        androidx.room.Index(value = ["scheduleType"]),
        androidx.room.Index(value = ["startDate"]),
        androidx.room.Index(value = ["endDate"])
    ]
)
data class PillAlarm(
    @PrimaryKey
    val id: String,
    val pillId: String,
    val hour: Int,
    val minute: Int,

    // 🆕 새로운 스케줄 시스템 (v5+)
    val scheduleType: ScheduleType = ScheduleType.WEEKLY,
    val scheduleConfig: String? = null,  // JSON 직렬화된 ScheduleConfig

    // 🆕 복용 기간 지원 (v5+)
    val startDate: LocalDate? = null,  // 복용 시작일
    val endDate: LocalDate? = null,    // 복용 종료일

    // 하위 호환성을 위한 레거시 필드 (v4 이하)
    @Deprecated("Use scheduleType and scheduleConfig instead", ReplaceWith("scheduleConfig"))
    val repeatDays: Set<DayOfWeek> = emptySet(),

    val enabled: Boolean = true,
    val alarmSoundUri: String? = null,

    // 🆕 메타데이터 (v5+)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) 