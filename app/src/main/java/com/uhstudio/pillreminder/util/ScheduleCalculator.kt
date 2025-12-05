package com.uhstudio.pillreminder.util

import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.data.model.ScheduleConfig
import com.uhstudio.pillreminder.data.model.ScheduleType
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * 알람 스케줄 계산 유틸리티
 * 다양한 스케줄 타입에 따라 다음 알람 시간을 계산
 */
object ScheduleCalculator {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 다음 알람 시간 계산
     * @param pillAlarm 알람 정보
     * @param from 기준 시간 (기본값: 현재 시간)
     * @return 다음 알람 시간 (null이면 더 이상 알람 없음)
     */
    fun calculateNextAlarmTime(
        pillAlarm: PillAlarm,
        from: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime? {
        try {
            // 종료일 체크
            if (pillAlarm.endDate != null && from.toLocalDate().isAfter(pillAlarm.endDate)) {
                Timber.d("calculateNextAlarmTime: alarm ended - ${pillAlarm.id}")
                return null
            }

            // 시작일 체크
            val effectiveFrom = if (pillAlarm.startDate != null &&
                from.toLocalDate().isBefore(pillAlarm.startDate)) {
                pillAlarm.startDate.atTime(pillAlarm.hour, pillAlarm.minute)
            } else {
                from
            }

            val nextTime = when (pillAlarm.scheduleType) {
                ScheduleType.DAILY -> calculateDaily(pillAlarm, effectiveFrom)
                ScheduleType.WEEKLY -> calculateWeekly(pillAlarm, effectiveFrom)
                ScheduleType.INTERVAL_DAYS -> calculateIntervalDays(pillAlarm, effectiveFrom)
                ScheduleType.INTERVAL_HOURS -> calculateIntervalHours(pillAlarm, effectiveFrom)
                ScheduleType.SPECIFIC_DATES -> calculateSpecificDates(pillAlarm, effectiveFrom)
                ScheduleType.MONTHLY -> calculateMonthly(pillAlarm, effectiveFrom)
                ScheduleType.WEEKDAY_ONLY -> calculateWeekdayOnly(pillAlarm, effectiveFrom)
                ScheduleType.WEEKEND_ONLY -> calculateWeekendOnly(pillAlarm, effectiveFrom)
                ScheduleType.CUSTOM -> null // 향후 구현
            }

            // 종료일 최종 체크
            if (nextTime != null && pillAlarm.endDate != null &&
                nextTime.toLocalDate().isAfter(pillAlarm.endDate)) {
                Timber.d("calculateNextAlarmTime: next time exceeds endDate - ${pillAlarm.id}")
                return null
            }

            return nextTime
        } catch (e: Exception) {
            Timber.e(e, "Failed to calculate next alarm time: ${pillAlarm.id}")
            return null
        }
    }

    /**
     * 매일 복용
     */
    private fun calculateDaily(pillAlarm: PillAlarm, from: LocalDateTime): LocalDateTime {
        val todayAlarmTime = from.toLocalDate()
            .atTime(pillAlarm.hour, pillAlarm.minute)

        return if (todayAlarmTime.isAfter(from)) {
            todayAlarmTime
        } else {
            todayAlarmTime.plusDays(1)
        }
    }

    /**
     * 주간 요일 반복 (기존 방식 호환)
     */
    private fun calculateWeekly(pillAlarm: PillAlarm, from: LocalDateTime): LocalDateTime? {
        val config = try {
            if (pillAlarm.scheduleConfig != null) {
                json.decodeFromString<ScheduleConfig.Weekly>(pillAlarm.scheduleConfig)
            } else {
                // 하위 호환성: repeatDays 사용
                @Suppress("DEPRECATION")
                ScheduleConfig.Weekly.from(pillAlarm.repeatDays)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Weekly config, using repeatDays")
            @Suppress("DEPRECATION")
            ScheduleConfig.Weekly.from(pillAlarm.repeatDays)
        }

        val days = config.toDayOfWeekSet()
        if (days.isEmpty()) {
            Timber.w("calculateWeekly: no days specified")
            return null
        }

        val todayAlarmTime = from.toLocalDate()
            .atTime(pillAlarm.hour, pillAlarm.minute)

        // 오늘이 반복 요일이고 아직 시간이 안 지났으면 오늘
        if (from.dayOfWeek in days && todayAlarmTime.isAfter(from)) {
            return todayAlarmTime
        }

        // 다음 반복 요일 찾기
        var nextDate = from.toLocalDate().plusDays(1)
        for (i in 1..7) {
            if (nextDate.dayOfWeek in days) {
                return nextDate.atTime(pillAlarm.hour, pillAlarm.minute)
            }
            nextDate = nextDate.plusDays(1)
        }

        return null
    }

    /**
     * N일마다 복용
     */
    private fun calculateIntervalDays(pillAlarm: PillAlarm, from: LocalDateTime): LocalDateTime? {
        val config = try {
            json.decodeFromString<ScheduleConfig.IntervalDays>(
                pillAlarm.scheduleConfig ?: return null
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse IntervalDays config")
            return null
        }

        val startDate = config.getStartDateAsLocalDate()
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, from.toLocalDate())

        if (daysSinceStart < 0) {
            // 시작일 이전이면 시작일 반환
            return startDate.atTime(pillAlarm.hour, pillAlarm.minute)
        }

        // 다음 복용일 계산
        val remainder = daysSinceStart % config.intervalDays
        val daysUntilNext = if (remainder == 0L) {
            // 오늘이 복용일
            val todayAlarmTime = from.toLocalDate().atTime(pillAlarm.hour, pillAlarm.minute)
            if (todayAlarmTime.isAfter(from)) {
                return todayAlarmTime
            }
            config.intervalDays.toLong()
        } else {
            config.intervalDays - remainder
        }

        return from.toLocalDate()
            .plusDays(daysUntilNext)
            .atTime(pillAlarm.hour, pillAlarm.minute)
    }

    /**
     * N시간마다 복용
     */
    private fun calculateIntervalHours(pillAlarm: PillAlarm, from: LocalDateTime): LocalDateTime? {
        val config = try {
            json.decodeFromString<ScheduleConfig.IntervalHours>(
                pillAlarm.scheduleConfig ?: return null
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse IntervalHours config")
            return null
        }

        val startTime = config.getStartTimeAsLocalTime()
        val startDateTime = from.toLocalDate().atTime(startTime)

        // 오늘 시작 시간이 아직 안 지났으면
        if (startDateTime.isAfter(from)) {
            return startDateTime
        }

        // 시작 시간부터 지난 시간 계산
        val hoursSinceStart = ChronoUnit.HOURS.between(startDateTime, from)
        val nextInterval = ((hoursSinceStart / config.intervalHours) + 1) * config.intervalHours

        return startDateTime.plusHours(nextInterval)
    }

    /**
     * 특정 날짜들에만 복용
     */
    private fun calculateSpecificDates(pillAlarm: PillAlarm, from: LocalDateTime): LocalDateTime? {
        val config = try {
            json.decodeFromString<ScheduleConfig.SpecificDates>(
                pillAlarm.scheduleConfig ?: return null
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse SpecificDates config")
            return null
        }

        val dates = config.getDatesAsLocalDateSet()
            .filter { !it.isBefore(from.toLocalDate()) }
            .sorted()

        for (date in dates) {
            val alarmTime = date.atTime(pillAlarm.hour, pillAlarm.minute)
            if (alarmTime.isAfter(from)) {
                return alarmTime
            }
        }

        return null
    }

    /**
     * 월 단위 반복
     */
    private fun calculateMonthly(pillAlarm: PillAlarm, from: LocalDateTime): LocalDateTime? {
        val config = try {
            json.decodeFromString<ScheduleConfig.Monthly>(
                pillAlarm.scheduleConfig ?: return null
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Monthly config")
            return null
        }

        val daysOfMonth = config.daysOfMonth.sorted()

        // 이번 달에서 다음 복용일 찾기
        for (day in daysOfMonth) {
            if (day > from.dayOfMonth) {
                val nextDate = from.toLocalDate().withDayOfMonth(day)
                val alarmTime = nextDate.atTime(pillAlarm.hour, pillAlarm.minute)
                if (alarmTime.isAfter(from)) {
                    return alarmTime
                }
            }
        }

        // 다음 달 첫 번째 복용일
        val nextMonth = from.toLocalDate().plusMonths(1)
        val firstDay = daysOfMonth.firstOrNull() ?: return null
        return nextMonth.withDayOfMonth(firstDay)
            .atTime(pillAlarm.hour, pillAlarm.minute)
    }

    /**
     * 평일만 복용 (월~금)
     */
    private fun calculateWeekdayOnly(pillAlarm: PillAlarm, from: LocalDateTime): LocalDateTime {
        val todayAlarmTime = from.toLocalDate()
            .atTime(pillAlarm.hour, pillAlarm.minute)

        // 오늘이 평일이고 아직 시간 안 지났으면
        if (from.dayOfWeek.value in 1..5 && todayAlarmTime.isAfter(from)) {
            return todayAlarmTime
        }

        // 다음 평일 찾기
        var nextDate = from.toLocalDate().plusDays(1)
        while (nextDate.dayOfWeek.value !in 1..5) {
            nextDate = nextDate.plusDays(1)
        }

        return nextDate.atTime(pillAlarm.hour, pillAlarm.minute)
    }

    /**
     * 주말만 복용 (토~일)
     */
    private fun calculateWeekendOnly(pillAlarm: PillAlarm, from: LocalDateTime): LocalDateTime {
        val todayAlarmTime = from.toLocalDate()
            .atTime(pillAlarm.hour, pillAlarm.minute)

        // 오늘이 주말이고 아직 시간 안 지났으면
        if (from.dayOfWeek.value in 6..7 && todayAlarmTime.isAfter(from)) {
            return todayAlarmTime
        }

        // 다음 주말 찾기
        var nextDate = from.toLocalDate().plusDays(1)
        while (nextDate.dayOfWeek.value !in 6..7) {
            nextDate = nextDate.plusDays(1)
        }

        return nextDate.atTime(pillAlarm.hour, pillAlarm.minute)
    }
}
