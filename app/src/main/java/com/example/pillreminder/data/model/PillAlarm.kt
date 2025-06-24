package com.example.pillreminder.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.DayOfWeek

/**
 * 약 복용 알람 정보를 저장하는 데이터 클래스
 * @property id 알람 고유 식별자
 * @property pillId 약 ID (외래키)
 * @property hour 알람 시간 (시)
 * @property minute 알람 시간 (분)
 * @property repeatDays 반복할 요일 목록
 * @property enabled 알람 활성화 여부
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
    ]
)
data class PillAlarm(
    @PrimaryKey
    val id: String,
    val pillId: String,
    val hour: Int,
    val minute: Int,
    val repeatDays: Set<DayOfWeek>,
    val enabled: Boolean = true
) 