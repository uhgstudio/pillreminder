package com.example.pillreminder.data.converter

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Room 데이터베이스를 위한 타입 컨버터
 */
class Converters {
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, dateTimeFormatter) }
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
} 