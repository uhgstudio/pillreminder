package com.example.pillreminder.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pillreminder.ui.theme.PillReminderTheme

/**
 * 앱의 메인 화면
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 알람에서 전달된 데이터 처리
        val alarmId = intent.getStringExtra("ALARM_ID")
        val pillId = intent.getStringExtra("PILL_ID")
        
        setContent {
            PillReminderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PillReminderApp(alarmId, pillId)
                }
            }
        }
    }
}

@Composable
fun PillReminderApp(alarmId: String?, pillId: String?) {
    val navController = rememberNavController()
    
    Scaffold { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                // TODO: HomeScreen()
            }
            composable("add_pill") {
                // TODO: AddPillScreen()
            }
            composable("pill_detail/{pillId}") {
                // TODO: PillDetailScreen()
            }
            composable("calendar") {
                // TODO: CalendarScreen()
            }
        }
    }
} 