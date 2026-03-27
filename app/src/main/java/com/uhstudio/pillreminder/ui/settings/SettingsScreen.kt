package com.uhstudio.pillreminder.ui.settings

import android.app.Activity
import android.content.pm.ApplicationInfo
import com.uhstudio.pillreminder.BuildConfig
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uhstudio.pillreminder.R
import com.uhstudio.pillreminder.data.repository.ImportStrategy
import com.uhstudio.pillreminder.util.FileManagerUtil

@OptIn(ExperimentalMaterial3Api::class)
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

    // Modern Clean Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.title_settings), // Assuming this exists or using a literal if not
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
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



                // 광고 테스트 섹션 (개발자 모드일 때만)
                val isDebuggable = remember {
                    (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                }
                if (isDebuggable) {

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

                    // 광고 유예 기간 설정 (테스트용)
                    if (settings != null) {
                        AdGracePeriodSection(
                            settings = settings!!,
                            onResetGracePeriod = {
                                viewModel.resetAdGracePeriod {
                                    viewModel.clearMessage()
                                }
                            },
                            onSkipGracePeriod = {
                                viewModel.skipAdGracePeriod {
                                    viewModel.clearMessage()
                                }
                            }
                        )
                    }
                }

                // 사용자 이름 설정 섹션
                UserNameSection()

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.btn_export_data))
            }

            OutlinedButton(
                onClick = onImportClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
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
fun AdGracePeriodSection(
    settings: com.uhstudio.pillreminder.data.model.AppSettings,
    onResetGracePeriod: () -> Unit,
    onSkipGracePeriod: () -> Unit
) {
    val context = LocalContext.current

    // 유예 기간 상태 계산
    val gracePeriodStatus = remember(settings.firstLaunchTime, settings.adGracePeriodHours) {
        if (settings.firstLaunchTime == null) {
            context.getString(R.string.settings_ad_grace_period_not_set)
        } else {
            val currentTime = System.currentTimeMillis()
            val elapsedHours = (currentTime - settings.firstLaunchTime) / 1000 / 60 / 60
            val remainingHours = settings.adGracePeriodHours - elapsedHours.toInt()
            if (remainingHours > 0) {
                context.getString(R.string.settings_ad_grace_period_active, remainingHours)
            } else {
                context.getString(R.string.settings_ad_grace_period_expired)
            }
        }
    }

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
                text = stringResource(R.string.settings_ad_grace_period),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(R.string.settings_ad_grace_period_status, gracePeriodStatus),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onResetGracePeriod,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.btn_reset_grace_period),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = onSkipGracePeriod,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.btn_skip_grace_period),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun AboutSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    text = BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun UserNameSection() {
    val context = LocalContext.current
    val userPrefs = remember { com.uhstudio.pillreminder.data.preferences.UserPreferencesManager(context) }
    var userName by remember { mutableStateOf(userPrefs.getUserName()) }
    var isEditing by remember { mutableStateOf(false) }
    val savedMessage = stringResource(R.string.msg_saved)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_user_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.settings_user_name_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = userName,
                onValueChange = { 
                    userName = it
                    isEditing = true
                },
                label = { Text(stringResource(R.string.settings_user_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        userName = userPrefs.getUserName()
                        isEditing = false
                    }) {
                        Text(stringResource(R.string.btn_cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            userPrefs.setUserName(userName)
                            isEditing = false
                            Toast.makeText(
                                context,
                                savedMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.btn_save))
                    }
                }
            }

            // 미리보기
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "미리보기: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.home_greeting, userName),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
    }
}
