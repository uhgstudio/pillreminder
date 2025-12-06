package com.uhstudio.pillreminder.ui.settings

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.pm.ApplicationInfo
import com.uhstudio.pillreminder.data.repository.ImportStrategy
import com.uhstudio.pillreminder.ui.theme.GradientGoldStart
import com.uhstudio.pillreminder.ui.theme.GradientGoldEnd
import com.uhstudio.pillreminder.util.FileManagerUtil
import com.uhstudio.pillreminder.R
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()

    // 파일 선택 런처 - 내보내기
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(FileManagerUtil.MIME_TYPE_JSON)
    ) { uri ->
        uri?.let {
            viewModel.exportData(it) { _ ->
                // 완료 처리는 message로 표시
            }
        }
    }

    // 파일 선택 런처 - 가져오기
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importData(it, ImportStrategy.REPLACE_ALL) { _ ->
                // 완료 처리는 message로 표시
            }
        }
    }

    // 메시지 표시
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GradientGoldStart,
                        GradientGoldEnd
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 프리미엄 상태 섹션 (비활성화: 사업자 등록 후 활성화 예정)
            /*
            PremiumStatusSection(
                isPremiumUser = isPremiumUser,
                isLoading = isLoading,
                onPurchaseClick = {
                    activity?.let {
                        viewModel.launchPurchaseFlow(it) { }
                    }
                },
                onRestoreClick = {
                    viewModel.restorePurchases { }
                }
            )
            */

            // 광고 설정 섹션
            if (settings != null) {
                AdSettingsSection(
                    settings = settings!!,
                    onUpdateSettings = { screenVisit, alarmCount, appLaunch, timeBased ->
                        viewModel.updateAdSettings(
                            screenVisitEnabled = screenVisit,
                            alarmCountEnabled = alarmCount,
                            appLaunchEnabled = appLaunch,
                            timeBasedEnabled = timeBased
                        )
                    }
                )
            }

            // 광고 테스트 섹션 (개발자 모드일 때만)
            val isDebuggable = remember {
                (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            }
            if (isDebuggable) {
                AdTestSection(
                    onLoadAdClick = {
                        viewModel.testLoadInterstitialAd { }
                    },
                    onShowAdClick = {
                        activity?.let {
                            viewModel.testShowInterstitialAd(it) { }
                        }
                    }
                )
            }

            // 데이터 관리 섹션
            DataManagementSection(
                onExportClick = {
                    exportLauncher.launch(FileManagerUtil.generateDefaultFileName())
                },
                onImportClick = {
                    importLauncher.launch(FileManagerUtil.MIME_TYPE_JSON)
                }
            )

            // 정보 섹션
            AboutSection()
        }

        // 로딩 인디케이터
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun PremiumStatusSection(
    isPremiumUser: Boolean,
    isLoading: Boolean,
    onPurchaseClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_premium_status),
                style = MaterialTheme.typography.titleMedium
            )

            if (isPremiumUser) {
                Text(
                    text = "✨ ${stringResource(R.string.settings_premium_user)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = stringResource(R.string.settings_free_user),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onPurchaseClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.btn_remove_ads))
                }
            }

            TextButton(
                onClick = onRestoreClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.btn_restore_purchases))
            }
        }
    }
}

@Composable
fun AdSettingsSection(
    settings: com.uhstudio.pillreminder.data.model.AppSettings,
    onUpdateSettings: (Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    var screenVisitEnabled by remember(settings) { mutableStateOf(settings.adOnScreenVisitEnabled) }
    var alarmCountEnabled by remember(settings) { mutableStateOf(settings.adOnAlarmCountEnabled) }
    var appLaunchEnabled by remember(settings) { mutableStateOf(settings.adOnAppLaunchEnabled) }
    var timeBasedEnabled by remember(settings) { mutableStateOf(settings.adOnTimeBased) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_ad_settings),
                style = MaterialTheme.typography.titleMedium
            )

            // 화면 방문 시 광고
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_ad_screen_visit),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_ad_threshold_description, settings.adOnScreenVisitThreshold),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = screenVisitEnabled,
                    onCheckedChange = {
                        screenVisitEnabled = it
                        onUpdateSettings(it, alarmCountEnabled, appLaunchEnabled, timeBasedEnabled)
                    }
                )
            }

            HorizontalDivider()

            // 알람 등록 시 광고
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_ad_alarm_count),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_ad_threshold_description, settings.adOnAlarmCountThreshold),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = alarmCountEnabled,
                    onCheckedChange = {
                        alarmCountEnabled = it
                        onUpdateSettings(screenVisitEnabled, it, appLaunchEnabled, timeBasedEnabled)
                    }
                )
            }

            HorizontalDivider()

            // 앱 실행 시 광고
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_ad_app_launch),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_ad_threshold_description, settings.adOnAppLaunchThreshold),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = appLaunchEnabled,
                    onCheckedChange = {
                        appLaunchEnabled = it
                        onUpdateSettings(screenVisitEnabled, alarmCountEnabled, it, timeBasedEnabled)
                    }
                )
            }

            HorizontalDivider()

            // 시간 기반 광고
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_ad_time_based),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_ad_time_interval_description, settings.adTimeIntervalHours),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = timeBasedEnabled,
                    onCheckedChange = {
                        timeBasedEnabled = it
                        onUpdateSettings(screenVisitEnabled, alarmCountEnabled, appLaunchEnabled, it)
                    }
                )
            }
        }
    }
}

@Composable
fun DataManagementSection(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_data_management),
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedButton(
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_export_data))
            }

            OutlinedButton(
                onClick = onImportClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_import_data))
            }
        }
    }
}

@Composable
fun AdTestSection(
    onLoadAdClick: () -> Unit,
    onShowAdClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_ad_test_title),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(R.string.settings_ad_test_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onLoadAdClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_ad_load))
                }

                Button(
                    onClick = onShowAdClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_ad_show))
                }
            }
        }
    }
}

@Composable
fun AboutSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_about),
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_app_version),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
