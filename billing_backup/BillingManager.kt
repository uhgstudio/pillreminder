package com.uhstudio.pillreminder.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Google Play Billing을 관리하는 싱글톤 클래스
 */
class BillingManager private constructor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_AD_REMOVAL = "remove_ads_permanent"

        @Volatile
        private var INSTANCE: BillingManager? = null

        fun getInstance(context: Context, scope: CoroutineScope): BillingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(context.applicationContext, scope).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val database = PillReminderDatabase.getDatabase(context)
    private val appSettingsDao = database.appSettingsDao()

    private var billingClient: BillingClient? = null
    private var isInitialized = false

    /**
     * Billing 클라이언트 초기화
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized && billingClient?.isReady == true) {
            return@withContext true
        }

        suspendCancellableCoroutine { continuation ->
            billingClient = BillingClient.newBuilder(context)
                .setListener { billingResult, purchases ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                        scope.launch {
                            handlePurchases(purchases)
                        }
                    }
                }
                .enablePendingPurchases()
                .build()

            billingClient?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        isInitialized = true
                        Log.d(TAG, "Billing 초기화 성공")
                        continuation.resume(true)
                    } else {
                        Log.e(TAG, "Billing 초기화 실패: ${billingResult.debugMessage}")
                        continuation.resume(false)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isInitialized = false
                    Log.w(TAG, "Billing 서비스 연결 끊김")
                    continuation.resume(false)
                }
            })
        }
    }

    /**
     * 프리미엄 상품 정보 조회
     */
    suspend fun queryPremiumProduct(): ProductDetails? = withContext(Dispatchers.IO) {
        if (!ensureConnection()) {
            return@withContext null
        }

        try {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_AD_REMOVAL)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val result = billingClient?.queryProductDetails(params)
            if (result?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                val product = result.productDetailsList?.firstOrNull()
                Log.d(TAG, "상품 정보 조회 성공: ${product?.name}")
                return@withContext product
            } else {
                Log.e(TAG, "상품 정보 조회 실패: ${result?.billingResult?.debugMessage}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "상품 정보 조회 중 예외 발생", e)
        }

        null
    }

    /**
     * 구매 플로우 시작
     */
    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (!ensureConnection()) {
            onResult(false, "Billing 서비스가 준비되지 않았습니다.")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)

        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "구매 플로우 시작 실패: ${billingResult?.debugMessage}")
            onResult(false, billingResult?.debugMessage)
        }
    }

    /**
     * 구매 복원
     */
    suspend fun restorePurchases(): Boolean = withContext(Dispatchers.IO) {
        if (!ensureConnection()) {
            Log.e(TAG, "구매 복원 실패: Billing 서비스가 준비되지 않음")
            return@withContext false
        }

        try {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            val result = billingClient?.queryPurchasesAsync(params)
            if (result?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                val purchases = result.purchasesList

                if (purchases.isNotEmpty()) {
                    handlePurchases(purchases)
                    Log.d(TAG, "구매 복원 성공: ${purchases.size}개 항목")
                    return@withContext true
                } else {
                    Log.d(TAG, "복원할 구매 항목 없음")
                    // 구매가 없으면 프리미엄 상태를 false로 설정
                    appSettingsDao.updatePurchaseStatus(false, null, null)
                    return@withContext false
                }
            } else {
                Log.e(TAG, "구매 조회 실패: ${result?.billingResult?.debugMessage}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "구매 복원 중 예외 발생", e)
        }

        false
    }

    /**
     * 프리미엄 사용자 여부 확인
     */
    suspend fun isPremiumUser(): Boolean = withContext(Dispatchers.IO) {
        appSettingsDao.isPremiumUser() ?: false
    }

    /**
     * 구매 처리
     */
    private suspend fun handlePurchases(purchases: List<Purchase>) = withContext(Dispatchers.IO) {
        for (purchase in purchases) {
            if (purchase.products.contains(PRODUCT_AD_REMOVAL) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            ) {
                // 구매 확인 처리
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }

                // 프리미엄 상태 업데이트
                val purchaseTime = purchase.purchaseTime
                val purchaseToken = purchase.purchaseToken

                appSettingsDao.updatePurchaseStatus(true, purchaseToken, purchaseTime)
                Log.d(TAG, "프리미엄 상태 업데이트 완료")
            }
        }
    }

    /**
     * 구매 확인 (Acknowledge)
     */
    private suspend fun acknowledgePurchase(purchase: Purchase) = withContext(Dispatchers.IO) {
        try {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            val result = billingClient?.acknowledgePurchase(params)
            if (result?.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "구매 확인 성공")
            } else {
                Log.e(TAG, "구매 확인 실패: ${result?.debugMessage}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "구매 확인 중 예외 발생", e)
        }
    }

    /**
     * Billing 연결 확인 및 재연결
     */
    private fun ensureConnection(): Boolean {
        if (billingClient?.isReady == true) {
            return true
        }

        Log.w(TAG, "Billing 클라이언트가 준비되지 않음")
        return false
    }

    /**
     * 리소스 정리
     */
    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
        isInitialized = false
        INSTANCE = null
    }
}
