package com.example.productivityapp.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    onNavigateToWater: () -> Unit,
    waterViewModel: WaterViewModel
) {
    val waterData by waterViewModel.todayData.collectAsState()
    val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ProductivityApp", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                        Text(dateStr, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
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
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            GreetingCard()
            Spacer(Modifier.height(16.dp))
            SectionLabel("HEALTH")
            Spacer(Modifier.height(8.dp))
            WaterIntakeCard(waterData = waterData, onClick = onNavigateToWater)
            Spacer(Modifier.height(16.dp))
            SectionLabel("COMING SOON")
            Spacer(Modifier.height(8.dp))
            ComingSoonCard(emoji = "😴", title = "Sleep tracker")
            Spacer(Modifier.height(8.dp))
            ComingSoonCard(emoji = "🏃", title = "Step counter")
            Spacer(Modifier.height(8.dp))
            ComingSoonCard(emoji = "🧘", title = "Meditation timer")
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
            Text("Track your habits and stay productive.", fontSize = 13.sp, color = TextSecondary)
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
private fun ComingSoonCard(emoji: String, title: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextTertiary)
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF3F4F6))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("soon", fontSize = 10.sp, color = TextTertiary)
                }
            }
        }
    }
}
