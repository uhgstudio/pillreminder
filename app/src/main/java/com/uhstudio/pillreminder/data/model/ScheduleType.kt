package com.uhstudio.pillreminder.data.model

/**
 * 알람 스케줄 타입
 * 다양한 복용 패턴을 지원
 */
enum class ScheduleType {
    /**
     * 주간 요일 반복
     * 예: 월/수/금 복용
     */
    WEEKLY,

    /**
     * 매일 복용
     */
    DAILY,

    /**
     * N일 간격으로 복용
     * 예: 2일마다, 3일마다
     */
    INTERVAL_DAYS,

    /**
     * 특정 날짜들에만 복용
     * 예: 12월 1일, 12월 5일, 12월 10일
     */
    SPECIFIC_DATES,

    /**
     * 커스텀 (향후 확장용)
     */
    CUSTOM
}
