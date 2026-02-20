package com.uhstudio.pillreminder.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 배너 광고를 표시하는 Composable
 * 프리미엄 사용자와 유예 기간 중에는 표시되지 않음
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adManager = remember { AdManager.getInstance(context) }
    var canShowAd by remember { mutableStateOf(false) }

    // 광고 표시 가능 여부 체크 (프리미엄 + 유예 기간)
    LaunchedEffect(Unit) {
        canShowAd = withContext(Dispatchers.IO) {
            adManager.canShowBannerAd()
        }
    }

    // 광고 표시 가능할 때만 렌더링
    if (canShowAd) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdManager.AD_UNIT_ID_BANNER
                    loadAd(AdRequest.Builder().build())
                }
            },
            update = { adView ->
                // 필요시 광고 갱신
            }
        )
    }
}

/**
 * 적응형 배너 광고를 표시하는 Composable
 * 화면 너비에 맞춰 자동으로 크기 조절
 */
@Composable
fun AdaptiveBannerAdView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adManager = remember { AdManager.getInstance(context) }
    var canShowAd by remember { mutableStateOf(false) }

    // 광고 표시 가능 여부 체크 (프리미엄 + 유예 기간)
    LaunchedEffect(Unit) {
        canShowAd = withContext(Dispatchers.IO) {
            adManager.canShowBannerAd()
        }
    }

    // 광고 표시 가능할 때만 렌더링
    if (canShowAd) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    // 화면 너비에 맞는 적응형 배너 크기 계산
                    val displayMetrics = ctx.resources.displayMetrics
                    val adWidthPixels = displayMetrics.widthPixels.toFloat()
                    val density = displayMetrics.density
                    val adWidth = (adWidthPixels / density).toInt()
                    
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth))
                    adUnitId = AdManager.AD_UNIT_ID_BANNER
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}


