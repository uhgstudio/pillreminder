package com.uhstudio.pillreminder.util

import android.content.Context
import com.uhstudio.pillreminder.R
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.data.model.ScheduleConfig
import com.uhstudio.pillreminder.data.model.ScheduleType
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * 스케줄 설명 문자열을 생성하는 공통 유틸리티
 * AlarmScreen, AlarmsScreen, PillDetailScreen에서 공유
 */
object ScheduleDescriptionUtil {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun getScheduleDescription(
        alarm: PillAlarm,
        context: Context,
        dailyText: String,
        noRepeatText: String,
        customText: String,
        specificDatesNoneText: String,
        specificDatesText: String
    ): String {
        return try {
            when (alarm.scheduleType) {
                ScheduleType.DAILY -> dailyText
                ScheduleType.WEEKLY -> {
                    val days = if (alarm.scheduleConfig != null && alarm.scheduleConfig.isNotBlank()) {
                        try {
                            val config = json.decodeFromString<ScheduleConfig.Weekly>(alarm.scheduleConfig)
                            config.toDayOfWeekSet()
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to parse scheduleConfig, falling back to repeatDays")
                            @Suppress("DEPRECATION")
                            alarm.repeatDays
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        alarm.repeatDays
                    }

                    when {
                        days.isEmpty() -> noRepeatText
                        days.size == 7 -> dailyText
                        else -> days.sortedBy { it.value }.joinToString(" ") { it.toKoreanShort() }
                    }
                }
                ScheduleType.INTERVAL_DAYS -> {
                    if (alarm.scheduleConfig == null || alarm.scheduleConfig.isBlank()) {
                        return context.getString(R.string.schedule_every_n_days, 0)
                    }
                    val config = json.decodeFromString<ScheduleConfig.IntervalDays>(alarm.scheduleConfig)
                    context.getString(R.string.schedule_every_n_days, config.intervalDays)
                }
                ScheduleType.SPECIFIC_DATES -> {
                    if (alarm.scheduleConfig == null || alarm.scheduleConfig.isBlank()) {
                        return specificDatesText
                    }
                    val config = json.decodeFromString<ScheduleConfig.SpecificDates>(alarm.scheduleConfig)
                    val dates = config.getDatesAsLocalDateSet()
                    if (dates.isEmpty()) {
                        specificDatesNoneText
                    } else {
                        context.getString(R.string.schedule_specific_dates_count, dates.size)
                    }
                }
                ScheduleType.CUSTOM -> customText
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception in getScheduleDescription")
            @Suppress("DEPRECATION")
            val days = alarm.repeatDays
            when {
                days.isEmpty() -> noRepeatText
                days.size == 7 -> dailyText
                else -> days.sortedBy { it.value }.joinToString(" ") { it.toKoreanShort() }
            }
        }
    }
}
