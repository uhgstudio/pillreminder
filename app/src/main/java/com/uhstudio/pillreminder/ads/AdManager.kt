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
import com.google.android.gms.ads.appopen.AppOpenAd
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
        //private const val AD_UNIT_ID_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
        private const val AD_UNIT_ID_INTERSTITIAL = "ca-app-pub-5530707072224199/7065363535"

        // Google 공식 테스트 Banner Ad Unit ID
        // 실제 배포 시 교체 필요
        //const val AD_UNIT_ID_BANNER = "ca-app-pub-3940256099942544/6300978111"
        const val AD_UNIT_ID_BANNER = "ca-app-pub-5530707072224199/7260874465"

        // App Open Ad Unit ID (앱 오프닝 광고)
        // 테스트용: ca-app-pub-3940256099942544/9257395921
        // 실제 배포용: ca-app-pub-5530707072224199/4992420881
        //private const val AD_UNIT_ID_APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
        private const val AD_UNIT_ID_APP_OPEN = "ca-app-pub-5530707072224199/4992420881"


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
    private var appOpenAd: AppOpenAd? = null
    private var isInitialized = false
    private var isLoading = false
    private var isAppOpenAdLoading = false
    private var appOpenAdLoadTime: Long = 0

    // 광고 쿨다운 (마지막 광고 표시 시간)
    private var lastInterstitialAdShownTime: Long = 0
    // 쿨다운 시간 (5분 = 300,000ms)
    private val AD_COOLDOWN_MS: Long = 5 * 60 * 1000

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
            Log.d(TAG, "첫 실행 시간: ${settings.firstLaunchTime}, 유예 기간: ${settings.adGracePeriodHours}시간")
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

            // 첫 실행 후 유예 기간 체크
            if (settings.firstLaunchTime != null) {
                val elapsedHours = (currentTime - settings.firstLaunchTime) / 1000 / 60 / 60
                if (elapsedHours < settings.adGracePeriodHours) {
                    Log.d(TAG, "광고 유예 기간 중: ${elapsedHours}/${settings.adGracePeriodHours}시간 경과")
                    return@withContext false
                }
            }

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
    fun loadInterstitialAd(onAdLoaded: () -> Unit = {}, onAdFailed: () -> Unit = {}) {
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
                    onAdFailed()
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
                // 쿨다운 시작
                lastInterstitialAdShownTime = System.currentTimeMillis()
            }
        }

        ad.show(activity)
    }

    /**
     * App Open Ad 로드
     */
    fun loadAppOpenAd(onAdLoaded: () -> Unit = {}) {
        if (!isInitialized) {
            Log.w(TAG, "AdMob이 초기화되지 않음")
            return
        }

        if (isAppOpenAdLoading) {
            Log.d(TAG, "이미 앱 오프닝 광고를 로드 중입니다")
            return
        }

        // 4시간 이내에 로드된 광고가 있으면 재사용
        if (appOpenAd != null && wasAppOpenAdLoadedLessThan4HoursAgo()) {
            Log.d(TAG, "이미 로드된 앱 오프닝 광고가 있습니다")
            onAdLoaded()
            return
        }

        isAppOpenAdLoading = true
        val adRequest = AdRequest.Builder().build()

        AppOpenAd.load(
            context,
            AD_UNIT_ID_APP_OPEN,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    appOpenAdLoadTime = System.currentTimeMillis()
                    isAppOpenAdLoading = false
                    Log.d(TAG, "앱 오프닝 광고 로드 성공")
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                    isAppOpenAdLoading = false
                    Log.e(TAG, "앱 오프닝 광고 로드 실패: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * App Open Ad 표시
     */
    fun showAppOpenAd(activity: Activity, onAdClosed: () -> Unit = {}) {
        val ad = appOpenAd
        if (ad == null) {
            Log.w(TAG, "표시할 앱 오프닝 광고가 없습니다")
            onAdClosed()
            return
        }

        // 4시간 이상 지난 광고는 무효
        if (!wasAppOpenAdLoadedLessThan4HoursAgo()) {
            Log.w(TAG, "앱 오프닝 광고가 만료되었습니다")
            appOpenAd = null
            onAdClosed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "앱 오프닝 광고가 닫혔습니다")
                appOpenAd = null

                // 광고 카운터 리셋
                CoroutineScope(Dispatchers.IO).launch {
                    resetAdCounters()
                }

                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "앱 오프닝 광고 표시 실패: ${adError.message}")
                appOpenAd = null
                onAdClosed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "앱 오프닝 광고가 표시되었습니다")
                // 쿨다운 시작
                lastInterstitialAdShownTime = System.currentTimeMillis()
                // 하루에 한 번 제한을 위해 DB에 저장
                CoroutineScope(Dispatchers.IO).launch {
                    appSettingsDao.updateLastAppOpenAdShownTime(System.currentTimeMillis())
                }
            }
        }

        ad.show(activity)
    }

    /**
     * App Open Ad가 4시간 이내에 로드되었는지 확인
     */
    private fun wasAppOpenAdLoadedLessThan4HoursAgo(): Boolean {
        val dateDifference = System.currentTimeMillis() - appOpenAdLoadTime
        val numMillisPerHour: Long = 3600000
        return dateDifference < numMillisPerHour * 4
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

            // 쿨다운 체크 - 마지막 광고 표시 후 일정 시간이 지나지 않았으면 표시 안 함
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastInterstitialAdShownTime < AD_COOLDOWN_MS) {
                val remainingSeconds = (AD_COOLDOWN_MS - (currentTime - lastInterstitialAdShownTime)) / 1000
                Log.d(TAG, "광고 쿨다운 중: ${remainingSeconds}초 남음")
                return@withContext false
            }

            return@withContext shouldShowAd()
        } catch (e: Exception) {
            Log.e(TAG, "알람 등록 카운터 증가 중 오류", e)
            return@withContext false
        }
    }

    /**
     * 앱 실행 시 광고 표시 여부 체크
     */
    suspend fun checkAppLaunchAd(): Boolean = withContext(Dispatchers.IO) {
        try {
            val settings = appSettingsDao.getSettingsOnce() ?: return@withContext false

            Log.d(TAG, "=== 앱 실행 광고 체크 ===")
            Log.d(TAG, "프리미엄 사용자: ${settings.isPremiumUser}")
            Log.d(TAG, "앱 실행 광고 활성화: ${settings.adOnAppLaunchEnabled}")

            // 프리미엄 사용자는 광고 표시 안 함
            if (settings.isPremiumUser) {
                Log.d(TAG, "앱 실행 광고: 프리미엄 사용자 - 표시 안 함")
                return@withContext false
            }

            // 앱 실행 광고가 비활성화된 경우
            if (!settings.adOnAppLaunchEnabled) {
                Log.d(TAG, "앱 실행 광고: 비활성화됨")
                return@withContext false
            }

            val currentTime = System.currentTimeMillis()

            // 첫 실행 후 유예 기간 체크
            if (settings.firstLaunchTime != null) {
                val elapsedHours = (currentTime - settings.firstLaunchTime) / 1000 / 60 / 60
                if (elapsedHours < settings.adGracePeriodHours) {
                    Log.d(TAG, "앱 실행 광고: 유예 기간 중 - 표시 안 함 (${elapsedHours}/${settings.adGracePeriodHours}시간)")
                    return@withContext false
                }
            }

            // 하루에 한 번만 표시 - 마지막 앱 오프닝 광고 표시 후 24시간 체크
            val lastAppOpenAdTime = settings.lastAppOpenAdShownTime
            if (lastAppOpenAdTime != null) {
                val elapsedHours = (currentTime - lastAppOpenAdTime) / 1000 / 60 / 60
                if (elapsedHours < 24) {
                    Log.d(TAG, "앱 실행 광고: 하루에 한 번 제한 - ${24 - elapsedHours}시간 후 표시 가능")
                    return@withContext false
                }
            }

            Log.d(TAG, "앱 실행 광고 표시: 조건 충족")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "앱 실행 광고 체크 중 오류", e)
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
     * 배너 광고 표시 가능 여부 체크 (프리미엄 + 유예 기간)
     */
    suspend fun canShowBannerAd(): Boolean = withContext(Dispatchers.IO) {
        try {
            val settings = appSettingsDao.getSettingsOnce() ?: return@withContext false

            // 프리미엄 사용자는 광고 표시 안 함
            if (settings.isPremiumUser) {
                Log.d(TAG, "배너 광고: 프리미엄 사용자 - 표시 안 함")
                return@withContext false
            }

            // 첫 실행 후 유예 기간 체크
            if (settings.firstLaunchTime != null) {
                val currentTime = System.currentTimeMillis()
                val elapsedHours = (currentTime - settings.firstLaunchTime) / 1000 / 60 / 60
                if (elapsedHours < settings.adGracePeriodHours) {
                    Log.d(TAG, "배너 광고: 유예 기간 중 - 표시 안 함 (${elapsedHours}/${settings.adGracePeriodHours}시간)")
                    return@withContext false
                }
            }

            Log.d(TAG, "배너 광고: 표시 가능")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "배너 광고 표시 가능 여부 체크 중 오류", e)
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
