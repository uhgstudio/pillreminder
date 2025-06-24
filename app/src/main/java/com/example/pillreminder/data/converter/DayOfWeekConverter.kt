package com.example.pillreminder.data.converter

import androidx.room.TypeConverter
import java.time.DayOfWeek

/**
 * DayOfWeek Set을 문자열로 변환하는 컨버터
 */
class DayOfWeekConverter {
    @TypeConverter
    fun fromString(value: String?): Set<DayOfWeek> {
        return value?.split(",")
            ?.map { DayOfWeek.valueOf(it) }
            ?.toSet() ?: emptySet()
    }

    @TypeConverter
    fun toString(days: Set<DayOfWeek>?): String {
        return days?.joinToString(",") { it.name } ?: ""
    }
} 