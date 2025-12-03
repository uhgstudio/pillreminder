package com.example.pillreminder

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.pillreminder.ui.home.HomeScreen
import com.example.pillreminder.ui.home.HomeViewModel
import com.example.pillreminder.ui.pill.AddPillScreen
import com.example.pillreminder.ui.pill.AddPillViewModel
import com.example.pillreminder.ui.theme.PillReminderTheme
import com.example.pillreminder.ui.pillDetail.PillDetailScreen
import com.example.pillreminder.ui.pillDetail.PillDetailViewModel
import com.example.pillreminder.ui.alarms.AlarmsScreen
import com.example.pillreminder.ui.alarms.AlarmsViewModel
import com.example.pillreminder.ui.addAlarm.AddAlarmScreen
import com.example.pillreminder.ui.addAlarm.AddAlarmViewModel
import com.example.pillreminder.ui.calendar.CalendarScreen
import com.example.pillreminder.ui.calendar.CalendarViewModel
import androidx.compose.ui.res.stringResource
import com.example.pillreminder.util.AlarmManagerUtil
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PillReminderTheme {
                val context = androidx.compose.ui.platform.LocalContext.current
                val alarmUtil = remember { AlarmManagerUtil(context) }
                var showFullScreenIntentDialog by remember { mutableStateOf(false) }

                // Android 13+ (API 33)에서 POST_NOTIFICATIONS 권한 요청
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationPermissionState = rememberPermissionState(
                        Manifest.permission.POST_NOTIFICATIONS
                    )

                    LaunchedEffect(Unit) {
                        if (!notificationPermissionState.status.isGranted) {
                            notificationPermissionState.launchPermissionRequest()
                        }
                    }
                }

                // Android 14+ (API 34)에서 전체 화면 알림 권한 확인
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        if (!alarmUtil.canUseFullScreenIntent()) {
                            showFullScreenIntentDialog = true
                        }
                    }
                }

                // 전체 화면 알림 권한 요청 다이얼로그
                if (showFullScreenIntentDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showFullScreenIntentDialog = false },
                        title = { Text("전체 화면 알림 권한 필요") },
                        text = {
                            Text(
                                "약 복용 알람이 화면이 꺼진 상태에서도 표시되려면 \"전체 화면 알림\" 권한이 필요합니다.\n\n" +
                                        "설정으로 이동하여 권한을 허용해주세요."
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showFullScreenIntentDialog = false
                                    alarmUtil.requestFullScreenIntentPermission()
                                }
                            ) {
                                Text("설정으로 이동")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { showFullScreenIntentDialog = false }
                            ) {
                                Text("나중에")
                            }
                        }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                val navBackStackEntry by navController.currentBackStackEntryAsState()
                                val currentRoute = navBackStackEntry?.destination?.route
                                
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text(stringResource(R.string.title_home)) },
                                    selected = currentRoute == "home",
                                    onClick = {
                                        navController.navigate("home") {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                                
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Alarm, contentDescription = null) },
                                    label = { Text(stringResource(R.string.title_alarms)) },
                                    selected = currentRoute == "alarms",
                                    onClick = {
                                        navController.navigate("alarms") {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                                
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                    label = { Text(stringResource(R.string.title_calendar)) },
                                    selected = currentRoute == "calendar",
                                    onClick = {
                                        navController.navigate("calendar") {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }
                    ) { paddingValues ->
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            composable("home") {
                                val homeViewModel: HomeViewModel = viewModel()
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    onAddPillClick = { navController.navigate("add_pill") },
                                    onPillClick = { pillId -> 
                                        navController.navigate("pill_detail/$pillId")
                                    }
                                )
                            }
                            
                            composable("add_pill") {
                                val addPillViewModel: AddPillViewModel = viewModel()
                                AddPillScreen(
                                    viewModel = addPillViewModel,
                                    onNavigateUp = { navController.navigateUp() }
                                )
                            }
                            
                            composable(
                                "pill_detail/{pillId}",
                                arguments = listOf(navArgument("pillId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val pillDetailViewModel: PillDetailViewModel = viewModel()
                                val pillId = backStackEntry.arguments?.getString("pillId") ?: ""
                                PillDetailScreen(
                                    viewModel = pillDetailViewModel,
                                    pillId = pillId,
                                    onNavigateUp = { navController.navigateUp() },
                                    onAddAlarmClick = { id ->
                                        navController.navigate("add_alarm/$id")
                                    },
                                    onEditPillClick = {
                                        navController.navigate("edit_pill/$pillId")
                                    }
                                )
                            }

                            composable(
                                "edit_pill/{pillId}",
                                arguments = listOf(navArgument("pillId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val addPillViewModel: AddPillViewModel = viewModel()
                                AddPillScreen(
                                    viewModel = addPillViewModel,
                                    pillId = backStackEntry.arguments?.getString("pillId"),
                                    onNavigateUp = { navController.navigateUp() }
                                )
                            }
                            
                            composable("alarms") {
                                val alarmsViewModel: AlarmsViewModel = viewModel()
                                AlarmsScreen(
                                    viewModel = alarmsViewModel,
                                    onAddAlarmClick = { pillId ->
                                        navController.navigate("add_alarm/$pillId")
                                    },
                                    onEditAlarmClick = { pillId, alarmId ->
                                        navController.navigate("edit_alarm/$pillId/$alarmId")
                                    }
                                )
                            }

                            composable(
                                "add_alarm/{pillId}",
                                arguments = listOf(navArgument("pillId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val addAlarmViewModel: AddAlarmViewModel = viewModel()
                                AddAlarmScreen(
                                    viewModel = addAlarmViewModel,
                                    pillId = backStackEntry.arguments?.getString("pillId") ?: "",
                                    onNavigateUp = { navController.navigateUp() }
                                )
                            }

                            composable(
                                "edit_alarm/{pillId}/{alarmId}",
                                arguments = listOf(
                                    navArgument("pillId") { type = NavType.StringType },
                                    navArgument("alarmId") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val addAlarmViewModel: AddAlarmViewModel = viewModel()
                                AddAlarmScreen(
                                    viewModel = addAlarmViewModel,
                                    pillId = backStackEntry.arguments?.getString("pillId") ?: "",
                                    alarmId = backStackEntry.arguments?.getString("alarmId"),
                                    onNavigateUp = { navController.navigateUp() }
                                )
                            }
                            
                            composable("calendar") {
                                val calendarViewModel: CalendarViewModel = viewModel()
                                CalendarScreen(
                                    viewModel = calendarViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 