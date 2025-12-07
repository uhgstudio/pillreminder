package com.uhstudio.pillreminder.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Google AdMob을 관리하는 싱글톤 클래스
 */
class AdManager private constructor(private val context: Context) {
    companion object {
        private const val TAG = "AdManager"

        // Google 공식 테스트 Interstitial Ad Unit ID
        // 실제 배포 시: ca-app-pub-5530707072224199/7065363535 로 변경
        private const val AD_UNIT_ID_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
        //private const val AD_UNIT_ID_INTERSTITIAL = "ca-app-pub-5530707072224199/7065363535"


        @Volatile
        private var INSTANCE: AdManager? = null

        fun getInstance(context: Context): AdManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val database = PillReminderDatabase.getDatabase(context)
    private val appSettingsDao = database.appSettingsDao()

    private var interstitialAd: InterstitialAd? = null
    private var isInitialized = false
    private var isLoading = false

    /**
     * AdMob SDK 초기화
     */
    fun initialize() {
        if (isInitialized) {
            return
        }

        MobileAds.initialize(context) { initializationStatus ->
            isInitialized = true
            Log.d(TAG, "AdMob 초기화 완료: ${initializationStatus.adapterStatusMap}")
        }
    }

    /**
     * 광고를 표시해야 하는지 체크 (모든 트리거 조건 확인)
     */
    suspend fun shouldShowAd(): Boolean = withContext(Dispatchers.IO) {
        try {
            val settings = appSettingsDao.getSettingsOnce() ?: return@withContext false

            Log.d(TAG, "=== 광고 표시 조건 체크 시작 ===")
            Log.d(TAG, "프리미엄 사용자: ${settings.isPremiumUser}")
            Log.d(TAG, "화면 방문 카운터: ${settings.adOnScreenVisitCounter}/${settings.adOnScreenVisitThreshold} (활성화: ${settings.adOnScreenVisitEnabled})")
            Log.d(TAG, "알람 등록 카운터: ${settings.totalAlarmRegistrations} (임계값: ${settings.adOnAlarmCountThreshold}, 활성화: ${settings.adOnAlarmCountEnabled})")
            Log.d(TAG, "앱 실행 카운터: ${settings.totalAppLaunches} (임계값: ${settings.adOnAppLaunchThreshold}, 활성화: ${settings.adOnAppLaunchEnabled})")
            Log.d(TAG, "시간 기반: ${settings.adOnTimeBased}, 마지막 광고: ${settings.lastAdShownTime}")

            // 프리미엄 사용자는 광고 표시 안 함
            if (settings.isPremiumUser) {
                Log.d(TAG, "프리미엄 사용자 - 광고 표시 안 함")
                return@withContext false
            }

            val currentTime = System.currentTimeMillis()

            // 각 트리거 체크
            val shouldShow = when {
                // 화면 방문 트리거
                settings.adOnScreenVisitEnabled &&
                        settings.adOnScreenVisitCounter >= settings.adOnScreenVisitThreshold -> {
                    Log.d(TAG, "화면 방문 트리거 조건 충족: ${settings.adOnScreenVisitCounter}/${settings.adOnScreenVisitThreshold}")
                    true
                }

                // 알람 등록 트리거
                settings.adOnAlarmCountEnabled &&
                        settings.totalAlarmRegistrations > 0 &&
                        settings.totalAlarmRegistrations % settings.adOnAlarmCountThreshold == 0 -> {
                    Log.d(TAG, "알람 등록 트리거 조건 충족: ${settings.totalAlarmRegistrations}개")
                    true
                }

                // 앱 실행 트리거
                settings.adOnAppLaunchEnabled &&
                        settings.totalAppLaunches > 0 &&
                        settings.totalAppLaunches % settings.adOnAppLaunchThreshold == 0 -> {
                    Log.d(TAG, "앱 실행 트리거 조건 충족: ${settings.totalAppLaunches}회")
                    true
                }

                // 시간 기반 트리거
                settings.adOnTimeBased &&
                        settings.lastAdShownTime != null &&
                        (currentTime - settings.lastAdShownTime) >= (settings.adTimeIntervalHours * 60 * 60 * 1000) -> {
                    Log.d(TAG, "시간 기반 트리거 조건 충족: ${(currentTime - settings.lastAdShownTime) / 1000 / 60 / 60}시간 경과")
                    true
                }

                else -> false
            }

            Log.d(TAG, "광고 표시 여부: $shouldShow")
            Log.d(TAG, "=== 광고 표시 조건 체크 종료 ===")
            return@withContext shouldShow
        } catch (e: Exception) {
            Log.e(TAG, "광고 표시 여부 체크 중 오류", e)
            return@withContext false
        }
    }

    /**
     * Interstitial 광고 로드
     */
    fun loadInterstitialAd(onAdLoaded: () -> Unit = {}) {
        if (!isInitialized) {
            Log.w(TAG, "AdMob이 초기화되지 않음")
            return
        }

        if (isLoading) {
            Log.d(TAG, "이미 광고를 로드 중입니다")
            return
        }

        if (interstitialAd != null) {
            Log.d(TAG, "이미 로드된 광고가 있습니다")
            onAdLoaded()
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            AD_UNIT_ID_INTERSTITIAL,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d(TAG, "Interstitial 광고 로드 성공")
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.e(TAG, "Interstitial 광고 로드 실패: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Interstitial 광고 표시
     */
    fun showInterstitialAd(activity: Activity, onAdClosed: () -> Unit = {}) {
        val ad = interstitialAd
        if (ad == null) {
            Log.w(TAG, "표시할 광고가 없습니다")
            onAdClosed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "광고가 닫혔습니다")
                interstitialAd = null

                // 광고 카운터 리셋
                CoroutineScope(Dispatchers.IO).launch {
                    resetAdCounters()
                }

                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "광고 표시 실패: ${adError.message}")
                interstitialAd = null
                onAdClosed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "광고가 표시되었습니다")
            }
        }

        ad.show(activity)
    }

    /**
     * 광고 표시 후 카운터 리셋
     */
    private suspend fun resetAdCounters() = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            appSettingsDao.resetAdCounters(currentTime)
            Log.d(TAG, "광고 카운터 리셋 완료")
        } catch (e: Exception) {
            Log.e(TAG, "광고 카운터 리셋 중 오류", e)
        }
    }

    /**
     * 화면 방문 카운터 증가 및 광고 표시 여부 체크
     */
    suspend fun incrementAndCheckScreenVisit(): Boolean = withContext(Dispatchers.IO) {
        try {
            appSettingsDao.incrementScreenVisit()
            return@withContext shouldShowAd()
        } catch (e: Exception) {
            Log.e(TAG, "화면 방문 카운터 증가 중 오류", e)
            return@withContext false
        }
    }

    /**
     * 알람 등록 카운터 증가 및 광고 표시 여부 체크
     */
    suspend fun incrementAndCheckAlarmRegistration(): Boolean = withContext(Dispatchers.IO) {
        try {
            appSettingsDao.incrementAlarmRegistration()
            return@withContext shouldShowAd()
        } catch (e: Exception) {
            Log.e(TAG, "알람 등록 카운터 증가 중 오류", e)
            return@withContext false
        }
    }

    /**
     * 시간 기반 광고 체크
     */
    suspend fun checkTimeBasedAd(): Boolean = withContext(Dispatchers.IO) {
        try {
            val settings = appSettingsDao.getSettingsOnce() ?: return@withContext false

            if (!settings.adOnTimeBased || settings.isPremiumUser) {
                return@withContext false
            }

            val currentTime = System.currentTimeMillis()
            val lastAdTime = settings.lastAdShownTime

            // 처음 실행이거나 설정된 간격이 지났는지 확인
            return@withContext if (lastAdTime == null) {
                true
            } else {
                val elapsedHours = (currentTime - lastAdTime) / 1000 / 60 / 60
                elapsedHours >= settings.adTimeIntervalHours
            }
        } catch (e: Exception) {
            Log.e(TAG, "시간 기반 광고 체크 중 오류", e)
            return@withContext false
        }
    }

    /**
     * 리소스 정리
     */
    fun destroy() {
        interstitialAd = null
        INSTANCE = null
    }
}
