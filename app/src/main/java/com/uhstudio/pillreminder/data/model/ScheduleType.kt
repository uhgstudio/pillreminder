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
     * N시간 간격으로 복용
     * 예: 6시간마다, 12시간마다
     */
    INTERVAL_HOURS,

    /**
     * 특정 날짜들에만 복용
     * 예: 12월 1일, 12월 5일, 12월 10일
     */
    SPECIFIC_DATES,

    /**
     * 월 단위 반복
     * 예: 매월 1일, 15일
     */
    MONTHLY,

    /**
     * 평일만 복용 (월~금)
     */
    WEEKDAY_ONLY,

    /**
     * 주말만 복용 (토~일)
     */
    WEEKEND_ONLY,

    /**
     * 커스텀 (향후 확장용)
     */
    CUSTOM
}
