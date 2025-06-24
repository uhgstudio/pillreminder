package com.example.pillreminder.util

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/**
 * 알람 시간 계산을 위한 유틸리티 클래스
 */
object AlarmUtil {
    /**
     * 주어진 시간과 요일에 대한 다음 알람 시간을 계산
     * @param hour 알람 시간 (시)
     * @param minute 알람 시간 (분)
     * @param repeatDays 반복할 요일 목록
     * @return 다음 알람 시간
     */
    fun calculateNextAlarmTime(
        hour: Int,
        minute: Int,
        repeatDays: Set<DayOfWeek>
    ): LocalDateTime {
        if (repeatDays.isEmpty()) {
            throw IllegalArgumentException("repeatDays must not be empty")
        }

        val now = LocalDateTime.now()
        val alarmTime = LocalTime.of(hour, minute)
        
        // 오늘 요일이 반복 요일에 포함되어 있고, 현재 시간이 알람 시간보다 이전인 경우
        if (repeatDays.contains(now.dayOfWeek) && 
            now.toLocalTime().isBefore(alarmTime)) {
            return now.with(alarmTime)
        }

        // 다음 알람 요일 찾기
        val nextAlarmDay = findNextAlarmDay(now.dayOfWeek, repeatDays)
        return if (nextAlarmDay == now.dayOfWeek) {
            // 다음 알람이 다음 주 같은 요일인 경우
            now.plusWeeks(1).with(alarmTime)
        } else {
            // 이번 주 다음 요일인 경우
            now.with(TemporalAdjusters.next(nextAlarmDay)).with(alarmTime)
        }
    }

    /**
     * 주어진 요일 이후의 다음 알람 요일을 찾음
     * @param currentDay 현재 요일
     * @param repeatDays 반복할 요일 목록
     * @return 다음 알람 요일
     */
    private fun findNextAlarmDay(
        currentDay: DayOfWeek,
        repeatDays: Set<DayOfWeek>
    ): DayOfWeek {
        // 현재 요일부터 시작하여 다음 알람 요일 찾기
        val sortedDays = repeatDays.sortedBy { it.value }
        return sortedDays.firstOrNull { it.value > currentDay.value }
            ?: sortedDays.first() // 다음 주로 넘어가는 경우 첫 번째 요일 선택
    }

    /**
     * 알람 시간이 현재 시간으로부터 얼마나 남았는지 계산
     * @param nextAlarmTime 다음 알람 시간
     * @return 밀리초 단위의 대기 시간
     */
    fun calculateMillisToNextAlarm(nextAlarmTime: LocalDateTime): Long {
        val now = LocalDateTime.now()
        return java.time.Duration.between(now, nextAlarmTime).toMillis()
    }
} 