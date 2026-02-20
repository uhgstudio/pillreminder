package com.uhstudio.pillreminder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 앱 설정 및 프리미엄 상태를 저장하는 엔티티
 * 싱글톤 패턴으로 id는 항상 1
 */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,  // 싱글톤 엔티티

    // 프리미엄 상태
    val isPremiumUser: Boolean = false,
    val purchaseToken: String? = null,
    val purchaseTime: Long? = null,

    // 광고 트리거 설정 - 화면 방문 (비활성화됨 - Google Play 정책 준수)
    val adOnScreenVisitEnabled: Boolean = false,
    val adOnScreenVisitThreshold: Int = 4,  // 4번 화면 방문마다
    val adOnScreenVisitCounter: Int = 0,

    // 광고 트리거 설정 - 알람 등록 개수
    val adOnAlarmCountEnabled: Boolean = true,
    val adOnAlarmCountThreshold: Int = 1,  // 매번 알람/약 등록마다 (테스트: 1, 배포: 3)

    // 광고 트리거 설정 - 앱 실행 횟수
    val adOnAppLaunchEnabled: Boolean = true,
    val adOnAppLaunchThreshold: Int = 1,  // 매번 앱 실행마다 (테스트: 1, 배포: 4)

    // 광고 트리거 설정 - 시간 기반
    val adOnTimeBased: Boolean = true,
    val lastAdShownTime: Long? = null,
    val adTimeIntervalHours: Int = 24,  // 24시간마다

    // 카운터
    val totalScreenVisits: Int = 0,
    val totalAlarmRegistrations: Int = 0,
    val totalAppLaunches: Int = 0,

    // 첫 실행 시간 (광고 유예 기간 계산용)
    val firstLaunchTime: Long? = null,
    // 광고 유예 기간 (시간 단위, 테스트: 0, 배포: 24)
    val adGracePeriodHours: Int = 0,

    // 마지막 앱 오프닝 광고 표시 시간 (하루에 한 번 제한용)
    val lastAppOpenAdShownTime: Long? = null
)
