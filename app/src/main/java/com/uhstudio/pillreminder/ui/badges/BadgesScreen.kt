package com.uhstudio.pillreminder.ui.badges

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uhstudio.pillreminder.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(
    onNavigateUp: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Praise Badges", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StitchDeepBrown
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            // Achievement Hero Section
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                shape = RoundedCornerShape(40.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile/Badge Icon
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(BlobOrange.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Smiley character mock
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = WarmPrimary,
                            modifier = Modifier.size(64.dp)
                        )
                        // Star badge overlap
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .background(WarmPrimary, CircleShape)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "You're doing great!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = StitchDeepBrown
                    )
                    Text(
                        text = "Health Hero Level",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Progress Bar Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Perfect Month", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = StitchDeepBrown)
                            Text("OCTOBER CHALLENGE", style = MaterialTheme.typography.labelSmall, color = WarmPrimary)
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = WarmPrimary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                "21/30 days",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LinearProgressIndicator(
                        progress = 0.7f,
                        modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                        color = WarmPrimary,
                        trackColor = StitchSoftPeach
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "9 days to go for the Golden Heart badge!",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Badge Collection Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your Collection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StitchDeepBrown
                )
                Text(
                    "3 / 8",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WarmPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Badges Grid
            val badges = listOf(
                BadgeItem("7-Day Streak", "EARNED", BlobMint, Icons.Default.CheckCircle),
                BadgeItem("Early Bird", "EARNED", BlobOrange, Icons.Default.WbSunny),
                BadgeItem("Reliable", "EARNED", Color(0xFFFFF9C4), Icons.Default.Handshake),
                BadgeItem("30-Day Perfect", "LOCKED", Color(0xFFF5F5F5), Icons.Default.Lock),
                BadgeItem("Health Master", "LOCKED", Color(0xFFF5F5F5), Icons.Default.Lock),
                BadgeItem("First Refill", "LOCKED", Color(0xFFF5F5F5), Icons.Default.Lock)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(badges) { badge ->
                    BadgeCard(badge)
                }
            }
            
            // Share Button
            Button(
                onClick = { /* Share */ },
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmPrimary),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Achievements", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun BadgeCard(badge: BadgeItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = if (badge.status == "LOCKED") Color(0xFFFBFBFB) else badge.color.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(if (badge.status == "LOCKED") Color(0xFFEEEEEE) else badge.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badge.icon,
                    contentDescription = null,
                    tint = if (badge.status == "LOCKED") Color.Gray else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = badge.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (badge.status == "LOCKED") Color.Gray else StitchDeepBrown,
                textAlign = TextAlign.Center
            )
            Text(
                text = badge.status,
                style = MaterialTheme.typography.labelSmall,
                color = if (badge.status == "EARNED") WarmPrimary else Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class BadgeItem(
    val name: String,
    val status: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
