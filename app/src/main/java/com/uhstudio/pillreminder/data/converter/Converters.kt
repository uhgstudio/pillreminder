package com.uhstudio.pillreminder.data.converter

import androidx.room.TypeConverter
import com.uhstudio.pillreminder.data.model.ScheduleType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Room 데이터베이스를 위한 타입 컨버터
 */
class Converters {
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? {
        return value?.let {
            // 날짜 형식('2025-11-25')과 날짜시간 형식('2025-11-25T10:30:00') 모두 처리
            if (it.contains('T')) {
                LocalDateTime.parse(it, dateTimeFormatter)
            } else {
                LocalDate.parse(it, dateFormatter).atStartOfDay()
            }
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? {
        return date?.format(dateTimeFormatter)
    }

    @TypeConverter
    fun fromDayOfWeekList(value: String?): List<DayOfWeek> {
        return value?.split(",")?.map { DayOfWeek.valueOf(it) } ?: emptyList()
    }

    @TypeConverter
    fun toDayOfWeekList(days: List<DayOfWeek>): String {
        return days.joinToString(",") { it.name }
    }

    @TypeConverter
    fun fromDayOfWeekSet(value: String?): Set<DayOfWeek> {
        return value?.split(",")
            ?.map { DayOfWeek.valueOf(it) }
            ?.toSet() ?: emptySet()
    }

    @TypeConverter
    fun toDayOfWeekSet(days: Set<DayOfWeek>?): String {
        return days?.joinToString(",") { it.name } ?: ""
    }

    // LocalDate 변환기 (v5+)
    @TypeConverter
    fun fromLocalDateString(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, dateFormatter) }
    }

    @TypeConverter
    fun localDateToString(date: LocalDate?): String? {
        return date?.format(dateFormatter)
    }

    // ScheduleType 변환기 (v5+)
    @TypeConverter
    fun fromScheduleTypeString(value: String?): ScheduleType {
        return value?.let { ScheduleType.valueOf(it) } ?: ScheduleType.WEEKLY
    }

    @TypeConverter
    fun scheduleTypeToString(type: ScheduleType): String {
        return type.name
    }
} 