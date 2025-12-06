package com.uhstudio.pillreminder.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 복용 기록과 약 정보를 함께 담는 데이터 클래스
 */
data class IntakeHistoryWithPill(
    @Embedded val history: IntakeHistory,
    @Relation(
        parentColumn = "pillId",
        entityColumn = "id"
    )
    val pill: Pill
)
