package com.example.pillreminder

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
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
import com.example.pillreminder.ui.pill.EditPillScreen
import com.example.pillreminder.ui.theme.PillReminderTheme
import com.example.pillreminder.ui.pillDetail.PillDetailScreen
import com.example.pillreminder.ui.pillDetail.PillDetailViewModel
import com.example.pillreminder.ui.alarms.AlarmsScreen
import com.example.pillreminder.ui.alarms.AlarmsViewModel
import com.example.pillreminder.ui.addAlarm.AddAlarmScreen
import com.example.pillreminder.ui.addAlarm.AddAlarmViewModel
import com.example.pillreminder.ui.addAlarm.EditAlarmScreen
import com.example.pillreminder.ui.addAlarm.EditAlarmViewModel
import com.example.pillreminder.ui.calendar.CalendarScreen
import com.example.pillreminder.ui.calendar.CalendarViewModel
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {
    private val exactAlarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 정확한 알람 권한 확인 및 요청
        checkExactAlarmPermission()

        // 알람에서 전달된 데이터 처리
        val alarmId = intent.getStringExtra("ALARM_ID")
        val pillId = intent.getStringExtra("PILL_ID")
        
        setContent {
            PillReminderTheme {
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
                                    onPillClick = { pill -> 
                                        navController.navigate("pill_detail/${pill.id}")
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
                                "edit_pill/{pillId}",
                                arguments = listOf(navArgument("pillId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val addPillViewModel: AddPillViewModel = viewModel()
                                val pillDetailViewModel: PillDetailViewModel = viewModel()
                                val pillId = backStackEntry.arguments?.getString("pillId") ?: ""
                                
                                // Pill 객체를 가져오기 위한 상태
                                val pill by pillDetailViewModel.pill.collectAsState()
                                
                                LaunchedEffect(pillId) {
                                    pillDetailViewModel.loadPill(pillId)
                                }
                                
                                pill?.let { currentPill ->
                                    EditPillScreen(
                                        pill = currentPill,
                                        viewModel = addPillViewModel,
                                        onNavigateUp = { navController.navigateUp() }
                                    )
                                }
                            }
                            
                            composable(
                                "pill_detail/{pillId}",
                                arguments = listOf(navArgument("pillId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val pillDetailViewModel: PillDetailViewModel = viewModel()
                                PillDetailScreen(
                                    viewModel = pillDetailViewModel,
                                    pillId = backStackEntry.arguments?.getString("pillId") ?: "",
                                    onNavigateUp = { navController.navigateUp() },
                                    onAddAlarmClick = { pillId ->
                                        navController.navigate("add_alarm/$pillId")
                                    },
                                    onEditPillClick = { pillId ->
                                        navController.navigate("edit_pill/$pillId")
                                    },
                                    onAlarmClick = { alarmId ->
                                        navController.navigate("edit_alarm/$alarmId")
                                    }
                                )
                            }
                            
                            composable("alarms") {
                                val alarmsViewModel: AlarmsViewModel = viewModel()
                                AlarmsScreen(
                                    viewModel = alarmsViewModel,
                                    onAddAlarmClick = { pillId ->
                                        navController.navigate("add_alarm/$pillId")
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
                                "edit_alarm/{alarmId}",
                                arguments = listOf(navArgument("alarmId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val editAlarmViewModel: EditAlarmViewModel = viewModel()
                                EditAlarmScreen(
                                    viewModel = editAlarmViewModel,
                                    alarmId = backStackEntry.arguments?.getString("alarmId") ?: "",
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

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // 권한 요청 인텐트 생성
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                exactAlarmPermissionLauncher.launch(intent)
            }
        }
    }
} 