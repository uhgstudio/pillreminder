package com.uhstudio.pillreminder.data.model

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * 스케줄 설정을 나타내는 sealed class
 * 각 ScheduleType에 대응하는 설정 데이터
 */
@Serializable
sealed class ScheduleConfig {
    /**
     * 주간 요일 반복 설정
     * @param days 반복할 요일 목록
     */
    @Serializable
    data class Weekly(
        val days: Set<String>  // DayOfWeek를 String으로 저장 (JSON 직렬화 용이)
    ) : ScheduleConfig() {
        fun toDayOfWeekSet(): Set<DayOfWeek> {
            return days.map { DayOfWeek.valueOf(it) }.toSet()
        }

        companion object {
            fun from(days: Set<DayOfWeek>): Weekly {
                return Weekly(days.map { it.name }.toSet())
            }
        }
    }

    /**
     * 매일 복용 설정
     */
    @Serializable
    data object Daily : ScheduleConfig()

    /**
     * N일 간격 복용 설정
     * @param intervalDays 간격 (예: 2 = 2일마다)
     * @param startDate 시작 기준일 (ISO 8601 형식: YYYY-MM-DD)
     */
    @Serializable
    data class IntervalDays(
        val intervalDays: Int,
        val startDate: String  // LocalDate를 String으로 저장
    ) : ScheduleConfig() {
        fun getStartDateAsLocalDate(): LocalDate {
            return LocalDate.parse(startDate)
        }

        companion object {
            fun from(intervalDays: Int, startDate: LocalDate): IntervalDays {
                return IntervalDays(intervalDays, startDate.toString())
            }
        }
    }

    /**
     * N시간 간격 복용 설정
     * @param intervalHours 간격 (예: 6 = 6시간마다)
     * @param startTime 시작 시간 (ISO 8601 형식: HH:MM:SS)
     */
    @Serializable
    data class IntervalHours(
        val intervalHours: Int,
        val startTime: String  // LocalTime을 String으로 저장
    ) : ScheduleConfig() {
        fun getStartTimeAsLocalTime(): LocalTime {
            return LocalTime.parse(startTime)
        }

        companion object {
            fun from(intervalHours: Int, startTime: LocalTime): IntervalHours {
                return IntervalHours(intervalHours, startTime.toString())
            }
        }
    }

    /**
     * 특정 날짜들 복용 설정
     * @param dates 복용할 날짜 목록 (ISO 8601 형식)
     */
    @Serializable
    data class SpecificDates(
        val dates: Set<String>  // LocalDate를 String으로 저장
    ) : ScheduleConfig() {
        fun getDatesAsLocalDateSet(): Set<LocalDate> {
            return dates.map { LocalDate.parse(it) }.toSet()
        }

        companion object {
            fun from(dates: Set<LocalDate>): SpecificDates {
                return SpecificDates(dates.map { it.toString() }.toSet())
            }
        }
    }

    /**
     * 월 단위 반복 설정
     * @param daysOfMonth 월 중 복용할 날짜들 (1-31)
     */
    @Serializable
    data class Monthly(
        val daysOfMonth: Set<Int>  // 예: [1, 15] = 매월 1일, 15일
    ) : ScheduleConfig()

    /**
     * 평일만 복용 설정 (월~금)
     */
    @Serializable
    data object WeekdayOnly : ScheduleConfig()

    /**
     * 주말만 복용 설정 (토~일)
     */
    @Serializable
    data object WeekendOnly : ScheduleConfig()

    /**
     * 커스텀 설정 (향후 확장용)
     */
    @Serializable
    data class Custom(
        val customData: String
    ) : ScheduleConfig()
}
