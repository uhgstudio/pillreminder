package com.example.pillreminder.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 약 복용 기록을 저장하는 데이터 클래스
 * @property id 복용 기록 고유 식별자
 * @property pillId 약 ID (외래키)
 * @property alarmId 알람 ID (외래키)
 * @property intakeTime 복용 시간
 * @property status 복용 상태 (TAKEN: 복용, SKIPPED: 건너뜀)
 */
@Entity(
    tableName = "intake_history",
    foreignKeys = [
        ForeignKey(
            entity = Pill::class,
            parentColumns = ["id"],
            childColumns = ["pillId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PillAlarm::class,
            parentColumns = ["id"],
            childColumns = ["alarmId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class IntakeHistory(
    @PrimaryKey
    val id: String,
    val pillId: String,
    val alarmId: String,
    val intakeTime: LocalDateTime,
    val status: IntakeStatus
)

/**
 * 복용 상태를 나타내는 열거형
 */
enum class IntakeStatus {
    TAKEN,      // 복용함
    SKIPPED     // 건너뜀
} 