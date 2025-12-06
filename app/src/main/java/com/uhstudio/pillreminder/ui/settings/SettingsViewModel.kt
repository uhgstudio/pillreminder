package com.uhstudio.pillreminder.ui.settings

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uhstudio.pillreminder.ads.AdManager
// import com.uhstudio.pillreminder.billing.BillingManager // 비활성화
import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import com.uhstudio.pillreminder.data.model.AppSettings
import com.uhstudio.pillreminder.data.repository.ExportRepository
import com.uhstudio.pillreminder.data.repository.ImportResult
import com.uhstudio.pillreminder.data.repository.ImportStrategy
import com.uhstudio.pillreminder.util.FileManagerUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val appSettingsDao = database.appSettingsDao()
    // BillingManager 비활성화: 사업자 등록 후 활성화 예정
    // private val billingManager = BillingManager.getInstance(application, viewModelScope)
    private val exportRepository = ExportRepository(database)
    private val adManager = AdManager.getInstance(application)

    // 설정 정보
    val settings: StateFlow<AppSettings?> = appSettingsDao.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // 프리미엄 상태
    val isPremiumUser: StateFlow<Boolean> = settings.map { it?.isPremiumUser ?: false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // UI 상태
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    /**
     * 광고 설정 업데이트
     */
    fun updateAdSettings(
        screenVisitEnabled: Boolean? = null,
        screenVisitThreshold: Int? = null,
        alarmCountEnabled: Boolean? = null,
        alarmCountThreshold: Int? = null,
        appLaunchEnabled: Boolean? = null,
        appLaunchThreshold: Int? = null,
        timeBasedEnabled: Boolean? = null,
        timeIntervalHours: Int? = null
    ) {
        viewModelScope.launch {
            val current = settings.value ?: return@launch
            val updated = current.copy(
                adOnScreenVisitEnabled = screenVisitEnabled ?: current.adOnScreenVisitEnabled,
                adOnScreenVisitThreshold = screenVisitThreshold ?: current.adOnScreenVisitThreshold,
                adOnAlarmCountEnabled = alarmCountEnabled ?: current.adOnAlarmCountEnabled,
                adOnAlarmCountThreshold = alarmCountThreshold ?: current.adOnAlarmCountThreshold,
                adOnAppLaunchEnabled = appLaunchEnabled ?: current.adOnAppLaunchEnabled,
                adOnAppLaunchThreshold = appLaunchThreshold ?: current.adOnAppLaunchThreshold,
                adOnTimeBased = timeBasedEnabled ?: current.adOnTimeBased,
                adTimeIntervalHours = timeIntervalHours ?: current.adTimeIntervalHours
            )
            appSettingsDao.updateSettings(updated)
        }
    }

    /**
     * 구매 플로우 실행 (비활성화: 사업자 등록 후 활성화 예정)
     */
    /*
    fun launchPurchaseFlow(activity: Activity, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val productDetails = billingManager.queryPremiumProduct()
                if (productDetails != null) {
                    billingManager.launchPurchaseFlow(activity, productDetails) { success, error ->
                        _isLoading.value = false
                        if (success) {
                            _message.value = "구매가 완료되었습니다!"
                        } else {
                            _message.value = "구매 실패: ${error ?: "알 수 없는 오류"}"
                        }
                        onComplete(success)
                    }
                } else {
                    _isLoading.value = false
                    _message.value = "상품 정보를 불러올 수 없습니다."
                    onComplete(false)
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _message.value = "오류: ${e.message}"
                onComplete(false)
            }
        }
    }

    /**
     * 구매 복원 (비활성화: 사업자 등록 후 활성화 예정)
     */
    fun restorePurchases(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val restored = billingManager.restorePurchases()
                _isLoading.value = false
                if (restored) {
                    _message.value = "구매 내역을 복원했습니다."
                } else {
                    _message.value = "복원할 구매 내역이 없습니다."
                }
                onComplete(restored)
            } catch (e: Exception) {
                _isLoading.value = false
                _message.value = "복원 실패: ${e.message}"
                onComplete(false)
            }
        }
    }
    */

    /**
     * 데이터 내보내기
     */
    fun exportData(uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val exportData = exportRepository.exportAllData()
                val json = exportRepository.exportDataToJson(exportData)
                val success = FileManagerUtil.writeToUri(getApplication(), uri, json)
                _isLoading.value = false
                if (success) {
                    _message.value = "데이터를 성공적으로 내보냈습니다."
                } else {
                    _message.value = "데이터 내보내기 실패"
                }
                onComplete(success)
            } catch (e: Exception) {
                _isLoading.value = false
                _message.value = "내보내기 오류: ${e.message}"
                onComplete(false)
            }
        }
    }

    /**
     * 데이터 가져오기
     */
    fun importData(uri: Uri, strategy: ImportStrategy = ImportStrategy.REPLACE_ALL, onComplete: (ImportResult) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val json = FileManagerUtil.readFromUri(getApplication(), uri)
                if (json == null) {
                    _isLoading.value = false
                    _message.value = "파일을 읽을 수 없습니다."
                    onComplete(ImportResult(false, 0, 0, 0, listOf("파일을 읽을 수 없습니다.")))
                    return@launch
                }

                val exportData = exportRepository.parseJsonToExportData(json)
                if (exportData == null) {
                    _isLoading.value = false
                    _message.value = "올바르지 않은 파일 형식입니다."
                    onComplete(ImportResult(false, 0, 0, 0, listOf("올바르지 않은 파일 형식입니다.")))
                    return@launch
                }

                // 유효성 검증
                val validationErrors = exportRepository.validateExportData(exportData)
                if (validationErrors.isNotEmpty()) {
                    _isLoading.value = false
                    _message.value = "데이터 검증 실패: ${validationErrors.first()}"
                    onComplete(ImportResult(false, 0, 0, 0, validationErrors))
                    return@launch
                }

                // 가져오기 실행
                val result = exportRepository.importData(exportData, strategy)
                _isLoading.value = false
                if (result.success) {
                    _message.value = "${result.pillsImported}개의 약, ${result.alarmsImported}개의 알람, ${result.historyImported}개의 복용 기록을 가져왔습니다."
                } else {
                    _message.value = "가져오기 실패: ${result.errors.firstOrNull() ?: "알 수 없는 오류"}"
                }
                onComplete(result)
            } catch (e: Exception) {
                _isLoading.value = false
                _message.value = "가져오기 오류: ${e.message}"
                onComplete(ImportResult(false, 0, 0, 0, listOf(e.message ?: "알 수 없는 오류")))
            }
        }
    }

    /**
     * 메시지 초기화
     */
    fun clearMessage() {
        _message.value = null
    }

    /**
     * 전면 광고 로드 테스트
     */
    fun testLoadInterstitialAd(onComplete: (Boolean) -> Unit) {
        _isLoading.value = true
        adManager.loadInterstitialAd {
            _isLoading.value = false
            _message.value = "전면 광고 로드 완료!"
            onComplete(true)
        }
        // 로드 실패는 AdManager 내부에서 처리되므로, 타임아웃 설정
        viewModelScope.launch {
            kotlinx.coroutines.delay(5000)
            if (_isLoading.value) {
                _isLoading.value = false
                _message.value = "광고 로드 시간 초과 또는 실패"
                onComplete(false)
            }
        }
    }

    /**
     * 전면 광고 표시 테스트
     */
    fun testShowInterstitialAd(activity: Activity, onComplete: (Boolean) -> Unit) {
        _isLoading.value = true
        adManager.showInterstitialAd(activity) {
            _isLoading.value = false
            _message.value = "광고 표시 완료!"
            onComplete(true)
        }
    }
}
