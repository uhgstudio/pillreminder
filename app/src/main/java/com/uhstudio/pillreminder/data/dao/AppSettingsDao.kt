package com.uhstudio.pillreminder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.uhstudio.pillreminder.data.model.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * 앱 설정 데이터 접근 객체
 */
@Dao
interface AppSettingsDao {
    /**
     * 앱 설정 가져오기 (Flow로 반응형)
     */
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<AppSettings?>

    /**
     * 앱 설정 가져오기 (일회성)
     */
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsOnce(): AppSettings?

    /**
     * 앱 설정 삽입
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettings)

    /**
     * 앱 설정 업데이트
     */
    @Update
    suspend fun updateSettings(settings: AppSettings)

    /**
     * 화면 방문 카운터 증가
     */
    @Query("""
        UPDATE app_settings
        SET totalScreenVisits = totalScreenVisits + 1,
            adOnScreenVisitCounter = adOnScreenVisitCounter + 1
        WHERE id = 1
    """)
    suspend fun incrementScreenVisit()

    /**
     * 알람 등록 카운터 증가
     */
    @Query("""
        UPDATE app_settings
        SET totalAlarmRegistrations = totalAlarmRegistrations + 1
        WHERE id = 1
    """)
    suspend fun incrementAlarmRegistration()

    /**
     * 앱 실행 카운터 증가
     */
    @Query("""
        UPDATE app_settings
        SET totalAppLaunches = totalAppLaunches + 1
        WHERE id = 1
    """)
    suspend fun incrementAppLaunch()

    /**
     * 프리미엄 구매 상태 업데이트
     */
    @Query("""
        UPDATE app_settings
        SET isPremiumUser = :isPremium,
            purchaseToken = :token,
            purchaseTime = :time
        WHERE id = 1
    """)
    suspend fun updatePurchaseStatus(isPremium: Boolean, token: String?, time: Long?)

    /**
     * 광고 표시 후 카운터 리셋
     */
    @Query("""
        UPDATE app_settings
        SET lastAdShownTime = :time,
            adOnScreenVisitCounter = 0
        WHERE id = 1
    """)
    suspend fun resetAdCounters(time: Long)

    /**
     * 프리미엄 사용자 여부 확인
     */
    @Query("SELECT isPremiumUser FROM app_settings WHERE id = 1")
    suspend fun isPremiumUser(): Boolean?
}
