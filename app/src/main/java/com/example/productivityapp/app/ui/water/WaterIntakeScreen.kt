package com.example.productivityapp.app.ui.water

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.productivityapp.app.data.model.WaterDayData
import com.example.productivityapp.app.data.model.WaterEntry
import com.example.productivityapp.app.viewmodel.WaterUiState
import com.example.productivityapp.app.viewmodel.WaterViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val quickAmounts = listOf(
    150 to "\uD83C\uDF75",
    250 to "🥛",
    350 to "🍶",
    500 to "🥤",
)

private val WaterBackdropLight = Color(0xFFF4FAFF)
private val WaterBackdropDark = Color(0xFF081C31)
private val WaterSurfaceLight = Color(0xFFEAF5FF)
private val WaterSurfaceDark = Color(0xFF122A41)
private val WaterSurfaceAltLight = Color(0xFFFFFFFF)
private val WaterSurfaceAltDark = Color(0xFF0D2337)
private val WaterTrackLight = Color(0xFF8BC5FF)
private val WaterTrackDark = Color(0xFF1C4B74)
private val WaterAccentLight = Color(0xFF2A6CC1)
private val WaterAccentDark = Color(0xFF7EC3FF)
private val WaterToneLight = Color(0xFF5E7C9A)
private val WaterToneDark = Color(0xFFB6D9F6)
private val WaterChipLight = Color(0xFFDDEEFF)
private val WaterChipDark = Color(0xFF183552)
private val WaterActionLight = Color(0xFFD7EBFF)
private val WaterActionDark = Color(0xFF13304D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterIntakeScreen(
    onBack: () -> Unit,
    viewModel: WaterViewModel,
) {
    val dayData by viewModel.todayData.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var selectedQuick by remember { mutableStateOf<Int?>(null) }
    var customText by remember { mutableStateOf("") }
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val isDark = isSystemInDarkTheme()
    val backdrop = if (isDark) WaterBackdropDark else WaterBackdropLight
    val surface = if (isDark) WaterSurfaceDark else WaterSurfaceLight
    val surfaceAlt = if (isDark) WaterSurfaceAltDark else WaterSurfaceAltLight
    val track = if (isDark) WaterTrackDark else WaterTrackLight
    val accent = if (isDark) WaterAccentDark else WaterAccentLight
    val tone = if (isDark) WaterToneDark else WaterToneLight
    val chipColor = if (isDark) WaterChipDark else WaterChipLight
    val actionSurface = if (isDark) WaterActionDark else WaterActionLight

    QuickSelectionClearEffect(selectedQuick) { selectedQuick = null }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(dayData.date) {
        val today = LocalDate.now().format(dateFormatter)
        if (dayData.date != today) {
            viewModel.refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Water intake",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp)
                    }
                },
                expandedHeight = 56.dp,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = backdrop,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = surfaceAlt,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = data.visuals.message,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        data.visuals.actionLabel?.let { action ->
                            TextButton(
                                onClick = { data.performAction() },
                                colors = ButtonDefaults.textButtonColors(contentColor = accent),
                                modifier = Modifier.defaultMinSize(minWidth = 64.dp),
                            ) {
                                Text(action.uppercase(), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backdrop),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                WaterHeroSection(
                    uiState = uiState,
                    accent = accent,
                    track = track,
                    surface = surfaceAlt,
                    chipColor = chipColor,
                )
            }

            item {
                WaterQuickAddCard(
                    selectedQuick = selectedQuick,
                    customText = customText,
                    surface = surface,
                    actionSurface = actionSurface,
                    accent = accent,
                    tone = tone,
                    onCustomTextChanged = { customText = it.filter(Char::isDigit).take(4) },
                    onQuickAdd = { ml ->
                        selectedQuick = ml
                        coroutineScope.launch {
                            handleWaterAdd(
                                amountMl = ml,
                                viewModel = viewModel,
                                listStateScroll = { listState.animateScrollToItem(0) },
                                snackbarHostState = snackbarHostState,
                                onUndo = { id -> viewModel.removeEntry(id) },
                            )
                        }
                    },
                    onCustomAdd = {
                        val amount = selectedQuick ?: customText.toIntOrNull() ?: 0
                        if (amount > 0) {
                            selectedQuick = null
                            customText = ""
                            keyboard?.hide()
                            coroutineScope.launch {
                                handleWaterAdd(
                                    amountMl = amount,
                                    viewModel = viewModel,
                                    listStateScroll = { listState.animateScrollToItem(0) },
                                    snackbarHostState = snackbarHostState,
                                    onUndo = { id -> viewModel.removeEntry(id) },
                                )
                            }
                        }
                    },
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel("TODAY'S LOG", tone)
                    Text(uiState.entriesSummary, fontSize = 12.sp, color = tone)
                }
            }

            if (dayData.entries.isEmpty()) {
                item {
                    EmptyWaterLogCard(surfaceAlt = surfaceAlt, tone = tone)
                }
            } else {
                items(dayData.entries.reversed(), key = { it.id }) { entry ->
                    WaterLogItem(
                        entry = entry,
                        surface = surfaceAlt,
                        accent = accent,
                        actionSurface = actionSurface,
                        onDelete = { viewModel.removeEntry(entry.id) },
                    )
                }
            }

            item {
                WaterInsightsCard(
                    uiState = uiState,
                    surface = surface,
                    surfaceAlt = surfaceAlt,
                    accent = accent,
                    tone = tone,
                )
            }
        }
    }
}

private suspend fun handleWaterAdd(
    amountMl: Int,
    viewModel: WaterViewModel,
    listStateScroll: suspend () -> Unit,
    snackbarHostState: SnackbarHostState,
    onUndo: (Long) -> Unit,
) {
    val id = viewModel.addWaterAndGetId(amountMl)
    if (id < 0L) return
    listStateScroll()
    coroutineScope {
        val autoDismissJob = launch {
            delay(12_000)
            snackbarHostState.currentSnackbarData?.dismiss()
        }
        val result = snackbarHostState.showSnackbar(
            message = "Added ${amountMl} ml",
            actionLabel = "Undo",
            duration = SnackbarDuration.Indefinite,
        )
        autoDismissJob.cancel()
        if (result == SnackbarResult.ActionPerformed) {
            onUndo(id)
        }
    }
}

@Composable
private fun QuickSelectionClearEffect(selectedQuick: Int?, onClear: () -> Unit) {
    LaunchedEffect(selectedQuick) {
        if (selectedQuick != null) {
            delay(700)
            onClear()
        }
    }
}

@Composable
private fun WaterHeroSection(
    uiState: WaterUiState,
    accent: Color,
    track: Color,
    surface: Color,
    chipColor: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = surface),
            shape = RoundedCornerShape(999.dp),
        ) {
            Text(
                text = "Today's water",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        WaterProgressRing(
            totalMl = uiState.dayData.totalMl,
            goalMl = uiState.dayData.goalMl,
            fraction = uiState.dayData.progressFraction,
            accent = accent,
            track = track,
            surface = surface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WaterInfoChip(text = uiState.completionText, background = chipColor, color = accent)
            WaterInfoChip(text = "${uiState.remainingMl} ml left", background = chipColor, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WaterProgressRing(
    totalMl: Int,
    goalMl: Int,
    fraction: Float,
    accent: Color,
    track: Color,
    surface: Color,
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "waterRing",
    )
    val isDark = isSystemInDarkTheme()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(260.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(
                Brush.radialGradient(
                    listOf(
                        surface,
                        if (isDark) surface.copy(alpha = 0.92f) else Color.White,
                    )
                )
            )
            .padding(14.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 28f
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * animatedFraction,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawCircle(
                color = accent.copy(alpha = 0.10f),
                radius = size.minDimension * 0.38f,
                center = center,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$totalMl",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "of $goalMl ml",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${(animatedFraction * 100).toInt()}% complete",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
        }
    }
}

@Composable
private fun WaterQuickAddCard(
    selectedQuick: Int?,
    customText: String,
    surface: Color,
    actionSurface: Color,
    accent: Color,
    tone: Color,
    onCustomTextChanged: (String) -> Unit,
    onQuickAdd: (Int) -> Unit,
    onCustomAdd: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Quick add", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                "Log a drink fast, or enter a custom amount when you need something different.",
                style = MaterialTheme.typography.bodyMedium,
                color = tone,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickAmounts.forEach { (ml, emoji) ->
                    QuickAddButton(
                        ml = ml,
                        emoji = emoji,
                        selected = selectedQuick == ml,
                        accent = accent,
                        surface = actionSurface,
                        modifier = Modifier.weight(1f),
                        onClick = { onQuickAdd(ml) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = customText,
                    onValueChange = onCustomTextChanged,
                    placeholder = {
                        Text("Custom ml...", fontSize = 13.sp, color = tone)
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = accent.copy(alpha = 0.30f),
                        focusedContainerColor = actionSurface,
                        unfocusedContainerColor = actionSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = onCustomAdd,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    Text("Add", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isSystemInDarkTheme()) WaterBackdropDark else Color.White)
                }
            }
        }
    }
}

@Composable
private fun QuickAddButton(
    ml: Int,
    emoji: String,
    selected: Boolean,
    accent: Color,
    surface: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) accent.copy(alpha = 0.14f) else surface),
        modifier = modifier
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 1.dp else 0.5.dp,
                color = if (selected) accent else accent.copy(alpha = 0.18f),
                shape = RoundedCornerShape(14.dp),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${ml}ml",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun EmptyWaterLogCard(surfaceAlt: Color, tone: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surfaceAlt),
        shape = RoundedCornerShape(22.dp),
    ) {
        Text(
            "No entries yet. Add water above to start building today's hydration rhythm.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = tone,
        )
    }
}

@Composable
private fun WaterLogItem(
    entry: WaterEntry,
    surface: Color,
    accent: Color,
    actionSurface: Color,
    onDelete: () -> Unit,
) {
    val timeStr = entry.timestamp.format(DateTimeFormatter.ofPattern("h:mm a"))
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(actionSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.WaterDrop,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(timeStr, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "+${entry.amountMl} ml",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete water entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WaterInsightsCard(
    uiState: WaterUiState,
    surface: Color,
    surfaceAlt: Color,
    accent: Color,
    tone: Color,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Hydration insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceAlt),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(uiState.paceTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(uiState.paceMessage, style = MaterialTheme.typography.bodyMedium, color = tone)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WaterMetricCard(
                    title = "Today",
                    value = uiState.entriesSummary,
                    subtitle = uiState.latestIntakeSummary,
                    surface = surfaceAlt,
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
                WaterMetricCard(
                    title = uiState.streakTitle,
                    value = "${uiState.remainingMl} ml left",
                    subtitle = uiState.streakMessage,
                    surface = surfaceAlt,
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WaterMetricCard(
    title: String,
    value: String,
    subtitle: String,
    surface: Color,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent)
                )
            }
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WaterInfoChip(
    text: String,
    background: Color,
    color: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = color,
        letterSpacing = 0.8.sp,
    )
}
