package com.example.productivityapp.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// removed compose viewModel import — ViewModel is provided by the Activity
import com.example.productivityapp.app.data.model.WaterDayData
import com.example.productivityapp.app.ui.theme.*
import com.example.productivityapp.app.viewmodel.WaterViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSteps: () -> Unit,
    onNavigateToStepsLegacy: () -> Unit,
    onNavigateToRun: () -> Unit,
    onNavigateToSleep: () -> Unit,
    onNavigateToWater: () -> Unit,
    onNavigateToSettings: () -> Unit,
    waterViewModel: WaterViewModel
) {
    val waterData by waterViewModel.todayData.collectAsState()
    val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        waterViewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ProductivityApp", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                        Text(dateStr, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToSettings) {
                        Text("Settings", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Blue700,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            GreetingCard()
            Spacer(Modifier.height(16.dp))
            SectionLabel("TODAY")
            Spacer(Modifier.height(8.dp))
            WaterIntakeCard(waterData = waterData, onClick = onNavigateToWater)
            Spacer(Modifier.height(16.dp))
            SectionLabel("TRACKERS")
            Spacer(Modifier.height(8.dp))
            FeatureCard(
                emoji = "👟",
                title = "Steps",
                description = "Count steps, add manual entries, and start automatic tracking.",
                accentColor = Color(0xFFFFF3E0),
                titleColor = Color(0xFFB45309),
                onClick = onNavigateToSteps,
            )
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onNavigateToStepsLegacy) { Text("Open legacy Steps view") }
            Spacer(Modifier.height(8.dp))
            FeatureCard(
                emoji = "🏃",
                title = "Run",
                description = "Track outdoor runs, map your route, and replay recent sessions.",
                accentColor = Color(0xFFF3E8FF),
                titleColor = Color(0xFF7E22CE),
                onClick = onNavigateToRun,
            )
            Spacer(Modifier.height(8.dp))
            FeatureCard(
                emoji = "😴",
                title = "Sleep",
                description = "Start sleep sessions, rate sleep quality, and review weekly history.",
                accentColor = Color(0xFFECFDF5),
                titleColor = Color(0xFF047857),
                onClick = onNavigateToSleep,
            )
            Spacer(Modifier.height(16.dp))
            SectionLabel("SETUP")
            Spacer(Modifier.height(8.dp))
            SettingsCard(onClick = onNavigateToSettings)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "All of the trackers above are live and connected to the current navigation graph.",
                fontSize = 12.sp,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun GreetingCard() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Good day! 👋", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text("Track your steps, runs, sleep, water, and settings from one place.", fontSize = 13.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = TextSecondary,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun WaterIntakeCard(waterData: WaterDayData, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Blue50),
                contentAlignment = Alignment.Center
            ) {
                Text("💧", fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text("Water intake", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                // Progress bar
                LinearProgressBar(fraction = waterData.progressFraction)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${waterData.totalMl} / ${waterData.goalMl} ml · ${waterData.progressPercent}%",
                    fontSize = 12.sp,
                    color = Blue700,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("›", fontSize = 22.sp, color = TextTertiary)
        }
    }
}

@Composable
private fun FeatureCard(
    emoji: String,
    title: String,
    description: String,
    accentColor: Color,
    titleColor: Color,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = titleColor)
                Spacer(Modifier.height(4.dp))
                Text(description, fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Text("›", fontSize = 22.sp, color = TextTertiary)
        }
    }
}

@Composable
private fun LinearProgressBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(Blue50)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .clip(CircleShape)
                .background(Blue700)
        )
    }
}

@Composable
private fun SettingsCard(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                Text("⚙️", fontSize = 20.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Settings", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Update profile, preferred units, and your daily water and step goals.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("›", fontSize = 22.sp, color = TextTertiary)
        }
    }
}
