package com.uhstudio.pillreminder.ui.home

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.uhstudio.pillreminder.R
import com.uhstudio.pillreminder.ads.AdManager
import com.uhstudio.pillreminder.ads.BannerAdView
import com.uhstudio.pillreminder.data.model.Pill
import com.uhstudio.pillreminder.data.preferences.UserPreferencesManager
import com.uhstudio.pillreminder.ui.theme.StitchCream
import com.uhstudio.pillreminder.ui.theme.StitchDeepBrown
import com.uhstudio.pillreminder.ui.theme.StitchMint
import com.uhstudio.pillreminder.ui.theme.StitchPeach
import com.uhstudio.pillreminder.ui.theme.StitchPrimary
import com.uhstudio.pillreminder.ui.theme.StitchSecondary
import com.uhstudio.pillreminder.ui.theme.StitchSoftPeach
import com.uhstudio.pillreminder.ui.theme.StitchWarmGray
import com.uhstudio.pillreminder.ui.theme.WarmPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddPillClick: () -> Unit,
    onPillClick: (String) -> Unit,
    onBadgesClick: () -> Unit,
    onCalendarClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adManager = remember { AdManager.getInstance(context) }
    val userPrefs = remember { UserPreferencesManager(context) }
    val userName = remember { userPrefs.getUserName() }

    val pills by viewModel.pills.collectAsState(initial = emptyList())
    val lowStockPills by viewModel.lowStockPills.collectAsState(initial = emptyList())
    val todayStats by viewModel.todayStats.collectAsState()
    val todayAlarms by viewModel.todayAlarms.collectAsState()
    val timeFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("hh:mm a") }
    var pillToDelete by remember { mutableStateOf<Pill?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(StitchCream)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stitch Original Header
            StitchHeader(userName = userName, onProfileClick = onBadgesClick, onCalendarClick = onCalendarClick)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Daily Goal Card (Stitch Style)
            DailyGoalCard(stats = todayStats)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Your Schedule Section
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_your_schedule),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = StitchDeepBrown
                        )
                    )
                    Text(
                        text = stringResource(R.string.home_view_all),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = StitchPrimary
                        ),
                        modifier = Modifier.clickable { onCalendarClick() }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Running Low Card (재고 부족 알림)
                if (lowStockPills.isNotEmpty()) {
                    RunningLowCard(lowStockPills = lowStockPills)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                if (pills.isEmpty()) {
                    EmptyPillList(modifier = Modifier.padding(vertical = 32.dp))
                } else {
                    pills.forEach { pill ->
                        val nextAlarm = todayAlarms.firstOrNull { it.pill.id == pill.id && !it.isTaken }
                        StitchPillItem(
                            pill = pill,
                            nextDoseTime = nextAlarm?.time?.format(timeFormatter),
                            onPillClick = { onPillClick(pill.id) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                BannerAdView(modifier = Modifier.fillMaxWidth())
                
                Spacer(modifier = Modifier.height(120.dp)) // Nav bar space
            }
        }

        // Floating Action Button (Organic Shape)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 100.dp)
                .size(64.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 32.dp, bottomStart = 32.dp, bottomEnd = 24.dp))
                .background(StitchPrimary)
                .clickable { onAddPillClick() }
                .shadow(elevation = 12.dp, shape = CircleShape, spotColor = StitchPrimary.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }

    // 삭제 확인 다이얼로그 (생략 - 기존 로직 유지)
}

@Composable
fun StitchHeader(userName: String, onProfileClick: () -> Unit, onCalendarClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Profile Image (Stitch Style)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(2.dp, StitchPeach, RoundedCornerShape(18.dp))
                    .clickable { onProfileClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = StitchWarmGray,
                    modifier = Modifier.size(24.dp).align(Alignment.Center)
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.home_greeting, userName),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = StitchWarmGray
                    )
                )
                val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d"))
                Text(
                    text = today,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = StitchWarmGray.copy(alpha = 0.6f)
                    )
                )
            }
        }
        
        // Calendar Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(2.dp, RoundedCornerShape(16.dp))
                .background(Color.White, RoundedCornerShape(16.dp))
                .clickable { onCalendarClick() }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CalendarToday, null, tint = StitchPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun DailyGoalCard(stats: com.uhstudio.pillreminder.ui.home.IntakeStats) {
    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(32.dp), spotColor = StitchWarmGray.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, StitchPeach.copy(alpha = 0.2f))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background Blob Blur (from HTML)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 16.dp, y = (-16.dp))
                        .size(96.dp)
                        .background(StitchMint.copy(alpha = 0.3f), CircleShape)
                        .clip(CircleShape)
                )
                
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.home_daily_goal),
                                style = MaterialTheme.typography.titleLarge.copy(color = StitchWarmGray)
                            )
                            Text(
                                text = stringResource(R.string.home_doses_summary, stats.takenCount, stats.totalCount),
                                style = MaterialTheme.typography.bodySmall.copy(color = StitchWarmGray.copy(alpha = 0.6f))
                            )
                        }
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "${stats.adherenceRate.toInt()}",
                                style = MaterialTheme.typography.headlineLarge.copy(color = StitchPrimary)
                            )
                            Text(
                                text = "%",
                                style = MaterialTheme.typography.titleMedium.copy(color = StitchPrimary.copy(alpha = 0.6f))
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Custom Progress Bar (Stitch Inner Shadow Style)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(StitchCream, CircleShape)
                            .padding(4.dp)
                    ) {
                        val progress = if (stats.totalCount > 0) stats.takenCount.toFloat() / stats.totalCount else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(listOf(StitchPrimary, StitchPeach)),
                                    CircleShape
                                )
                                .shadow(2.dp, CircleShape)
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun StitchPillItem(
    pill: Pill,
    nextDoseTime: String? = null,
    onPillClick: () -> Unit
) {
    // Choose a blob color based on pill ID or type (from Stitch system)
    val (blobColor, iconTint) = when (pill.id.hashCode() % 3) {
        0 -> StitchMint to StitchSecondary
        1 -> StitchPeach to StitchPrimary
        else -> Color(0xFFE0C3FC) to Color(0xFF916BBF) // Lavender
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, StitchPeach.copy(alpha = 0.1f)),
        onClick = onPillClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Organic Shape Icon Background (from HTML)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(2.dp, RoundedCornerShape(topStart = 18.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 18.dp))
                        .background(blobColor, RoundedCornerShape(topStart = 18.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 18.dp))
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (pill.imageUri != null) {
                        coil.compose.AsyncImage(
                            model = pill.imageUri,
                            contentDescription = pill.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = pill.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = StitchWarmGray
                        )
                    )
                    if (nextDoseTime != null) {
                        Text(
                            text = stringResource(R.string.home_next_dose, nextDoseTime),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = StitchWarmGray.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
            
            // Status Action (Check or Take)
            IconButton(
                onClick = { /* Action */ },
                modifier = Modifier
                    .size(40.dp)
                    .background(blobColor.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) StitchPrimary else StitchWarmGray.copy(alpha = 0.3f),
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (isSelected) StitchPrimary else StitchWarmGray.copy(alpha = 0.3f)
            )
        )
    }
}
@Composable
fun StitchBottomNavigation(navController: androidx.navigation.NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp), spotColor = StitchWarmGray.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = currentRoute == "home",
                onClick = {
                    navController.navigate("home") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
            NavItem(
                icon = Icons.Default.BarChart,
                label = "Report",
                isSelected = currentRoute == "calendar", // Changed to calendar route based on usage
                onClick = {
                    navController.navigate("calendar") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
            NavItem(
                icon = Icons.Default.Settings,
                label = "Me",
                isSelected = currentRoute == "settings",
                onClick = {
                    navController.navigate("settings") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}




@Composable
fun RunningLowCard(lowStockPills: List<Pill>) {
    if (lowStockPills.isEmpty()) return
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F0)),
        border = BorderStroke(1.dp, StitchPrimary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(StitchPrimary.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = StitchPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.home_running_low),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = StitchWarmGray
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    lowStockPills.forEach { pill ->
                        Text(
                            text = stringResource(
                                R.string.home_running_low_message,
                                pill.name,
                                pill.quantity
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = StitchWarmGray.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StayHydratedBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StitchMint.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, StitchSecondary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = StitchSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.home_water_reminder),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = StitchWarmGray
                )
            )
        }
    }
}

@Composable
fun EmptyPillList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(StitchCream, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = StitchWarmGray.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_no_meds_today),
            style = MaterialTheme.typography.titleMedium.copy(color = StitchWarmGray.copy(alpha = 0.6f))
        )
    }
}

@Composable
fun DailyProgressSection(stats: com.uhstudio.pillreminder.ui.home.IntakeStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daily Progress Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = if (stats.totalCount > 0) stats.takenCount.toFloat() / stats.totalCount else 0.5f,
                        modifier = Modifier.size(80.dp),
                        color = WarmPrimary,
                        strokeWidth = 8.dp,
                        trackColor = StitchSoftPeach
                    )
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = WarmPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${stats.takenCount}/${stats.totalCount}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = StitchDeepBrown
                )
                Text(
                    text = stringResource(R.string.home_done_today),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
        
        // Feeling Status Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = WarmPrimary.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                 Icon(
                    imageVector = Icons.Default.SentimentSatisfiedAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Column {
                    Text(
                        text = stringResource(R.string.home_current_status),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(R.string.home_feeling_great),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}


