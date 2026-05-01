package com.example.productivityapp.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.productivityapp.app.viewmodel.HomeDashboardUiState
import com.example.productivityapp.app.viewmodel.HomeFeatureSummary
import com.example.productivityapp.app.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSteps: () -> Unit,
    onNavigateToStepsLegacy: () -> Unit,
    onNavigateToRun: () -> Unit,
    onNavigateToWorkout: () -> Unit,
    onNavigateToMindfulness: () -> Unit,
    onNavigateToSleep: () -> Unit,
    onNavigateToWater: () -> Unit,
    onNavigateToSettings: () -> Unit,
    homeViewModel: HomeViewModel,
    onOpenTermsOfService: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
) {
    val uiState by homeViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.refresh()
    }

    HomeScreenContent(
        uiState = uiState,
        onNavigateToSteps = onNavigateToSteps,
        onNavigateToStepsLegacy = onNavigateToStepsLegacy,
        onNavigateToRun = onNavigateToRun,
        onNavigateToWorkout = onNavigateToWorkout,
        onNavigateToMindfulness = onNavigateToMindfulness,
        onNavigateToSleep = onNavigateToSleep,
        onNavigateToWater = onNavigateToWater,
        onNavigateToSettings = onNavigateToSettings,
        onOpenTermsOfService = onOpenTermsOfService,
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeDashboardUiState,
    onNavigateToSteps: () -> Unit,
    onNavigateToStepsLegacy: () -> Unit,
    onNavigateToRun: () -> Unit,
    onNavigateToWorkout: () -> Unit,
    onNavigateToMindfulness: () -> Unit,
    onNavigateToSleep: () -> Unit,
    onNavigateToWater: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenTermsOfService: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
) {
    val isDark = isSystemInDarkTheme()
    val backdrop = MaterialTheme.colorScheme.background
    val heroSurface = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.96f) else Color.White

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Health App",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.semantics { contentDescription = "Open settings" },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                expandedHeight = 56.dp,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backdrop,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = backdrop,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DashboardHeroCard(uiState = uiState, surface = heroSurface)
            }
            item {
                FeatureDashboardCard(
                    summary = uiState.waterSummary,
                    tone = featureTone(HomeFeature.WATER, isDark),
                    icon = Icons.Filled.WaterDrop,
                    onClick = onNavigateToWater,
                    cardContentDescription = "Open water intake",
                )
            }
            item {
                FeatureDashboardCard(
                    summary = uiState.stepsSummary,
                    tone = featureTone(HomeFeature.STEPS, isDark),
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    onClick = onNavigateToSteps,
                    cardContentDescription = "Open steps",
                )
            }
            item {
                FeatureDashboardCard(
                    summary = uiState.runSummary,
                    tone = featureTone(HomeFeature.RUN, isDark),
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    onClick = onNavigateToRun,
                    cardContentDescription = "Open run and walk",
                )
            }
            item {
                FeatureDashboardCard(
                    summary = uiState.workoutSummary,
                    tone = featureTone(HomeFeature.WORKOUT, isDark),
                    icon = Icons.Filled.FitnessCenter,
                    onClick = onNavigateToWorkout,
                    cardContentDescription = "Open workout",
                )
            }
            item {
                FeatureDashboardCard(
                    summary = uiState.mindfulnessSummary,
                    tone = featureTone(HomeFeature.MINDFULNESS, isDark),
                    icon = Icons.Filled.SelfImprovement,
                    onClick = onNavigateToMindfulness,
                    cardContentDescription = "Open mindfulness",
                )
            }
            item {
                FeatureDashboardCard(
                    summary = uiState.sleepSummary,
                    tone = featureTone(HomeFeature.SLEEP, isDark),
                    icon = Icons.Filled.Hotel,
                    onClick = onNavigateToSleep,
                    cardContentDescription = "Open sleep",
                )
            }
            item {
                FeatureDashboardCard(
                    summary = uiState.settingsSummary,
                    tone = featureTone(HomeFeature.SETTINGS, isDark),
                    icon = Icons.Filled.Settings,
                    onClick = onNavigateToSettings,
                    cardContentDescription = "Open settings card",
                )
            }
            item {
                FooterPolicyCard(
                    onOpenTermsOfService = onOpenTermsOfService,
                    onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                )
            }
        }
    }
}

@Composable
private fun DashboardHeroCard(
    uiState: HomeDashboardUiState,
    surface: Color,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(uiState.dateLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(uiState.greetingTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.heroChips.forEach { chip ->
                    HeroChip(chip)
                }
            }
        }
    }
}

@Composable
private fun HeroChip(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun FeatureDashboardCard(
    summary: HomeFeatureSummary,
    tone: FeatureTone,
    icon: ImageVector,
    onClick: () -> Unit,
    cardContentDescription: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = tone.container),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = cardContentDescription },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(tone.badge, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = tone.icon)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(summary.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(summary.headline, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            summary.progressFraction?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = tone.accent,
                    trackColor = tone.badge,
                )
            } ?: run {
                Text(summary.supporting, style = MaterialTheme.typography.bodyMedium, color = tone.accent)
                Text(summary.secondary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FooterPolicyCard(
    onOpenTermsOfService: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "By using ProductivityApp, you agree to our Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpenTermsOfService) {
                    Text("Terms of Service")
                }
                TextButton(onClick = onOpenPrivacyPolicy) {
                    Text("Privacy Policy")
                }
            }
        }
    }
}

private enum class HomeFeature { WATER, STEPS, RUN, WORKOUT, MINDFULNESS, SLEEP, SETTINGS }

private data class FeatureTone(
    val container: Color,
    val badge: Color,
    val accent: Color,
    val icon: Color,
)

@Composable
private fun featureTone(feature: HomeFeature, isDark: Boolean): FeatureTone {
    return when (feature) {
        HomeFeature.WATER -> FeatureTone(
            container = if (isDark) Color(0xFF10253C) else Color(0xFFEAF5FF),
            badge = if (isDark) Color(0xFF18395B) else Color(0xFFD7EBFF),
            accent = if (isDark) Color(0xFF7EC3FF) else Color(0xFF2A6CC1),
            icon = if (isDark) Color(0xFF7EC3FF) else Color(0xFF2A6CC1),
        )
        HomeFeature.STEPS -> FeatureTone(
            container = if (isDark) Color(0xFF2A2213) else Color(0xFFFFF3E0),
            badge = if (isDark) Color(0xFF3A2C12) else Color(0xFFFFE3B2),
            accent = if (isDark) Color(0xFFFFC24A) else Color(0xFFB45309),
            icon = if (isDark) Color(0xFFFFC24A) else Color(0xFFB45309),
        )
        HomeFeature.RUN -> FeatureTone(
            container = if (isDark) Color(0xFF20162F) else Color(0xFFF3E8FF),
            badge = if (isDark) Color(0xFF2D1E42) else Color(0xFFE9D5FF),
            accent = if (isDark) Color(0xFFC3A8FF) else Color(0xFF7E22CE),
            icon = if (isDark) Color(0xFFC3A8FF) else Color(0xFF7E22CE),
        )
        HomeFeature.WORKOUT -> FeatureTone(
            container = if (isDark) Color(0xFF18243B) else Color(0xFFEAF0FF),
            badge = if (isDark) Color(0xFF22304A) else Color(0xFFDCE7FF),
            accent = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA),
            icon = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA),
        )
        HomeFeature.MINDFULNESS -> FeatureTone(
            container = if (isDark) Color(0xFF132327) else Color(0xFFE9F8F5),
            badge = if (isDark) Color(0xFF1D343A) else Color(0xFFD5F0EA),
            accent = if (isDark) Color(0xFF8DE2D5) else Color(0xFF0F8C83),
            icon = if (isDark) Color(0xFF8DE2D5) else Color(0xFF0F8C83),
        )
        HomeFeature.SLEEP -> FeatureTone(
            container = if (isDark) Color(0xFF13261C) else Color(0xFFECFDF5),
            badge = if (isDark) Color(0xFF1C3528) else Color(0xFFD1FAE5),
            accent = if (isDark) Color(0xFF8EDC9A) else Color(0xFF047857),
            icon = if (isDark) Color(0xFF8EDC9A) else Color(0xFF047857),
        )
        HomeFeature.SETTINGS -> FeatureTone(
            container = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFFFFFF),
            badge = if (isDark) Color(0xFF2A2927) else Color(0xFFF3F4F6),
            accent = MaterialTheme.colorScheme.onSurface,
            icon = MaterialTheme.colorScheme.onSurface,
        )
    }
}
