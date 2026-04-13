package com.example.productivityapp.ui.steps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.productivityapp.ui.theme.StepsAmber
import com.example.productivityapp.ui.theme.StepsAmberDark
import com.example.productivityapp.ui.theme.BlendLight
import com.example.productivityapp.ui.theme.BlendPrimaryDark
import com.example.productivityapp.ui.theme.BlendDarkBackground
import com.example.productivityapp.ui.theme.BlendPrimary
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import kotlin.math.max
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.contentDescription
import com.example.productivityapp.ui.theme.Spacing
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.DirectionsWalk

@Composable
fun StepSegmentIcon(label: String, modifier: Modifier = Modifier) {
    val image = when (label.lowercase(Locale.getDefault())) {
        "morning" -> Icons.Filled.WbSunny
        "afternoon" -> Icons.Filled.BrightnessHigh
        "evening" -> Icons.Filled.NightsStay
        "night" -> Icons.Filled.NightsStay
        else -> Icons.Filled.DirectionsWalk
    }
    Icon(imageVector = image, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = modifier)
}

@Composable
fun StepsHeader(steps: Int, dailyGoal: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.semantics { testTag = "steps_header"; contentDescription = "Steps: $steps; Goal: $dailyGoal" }) {
        Text("Steps", style = MaterialTheme.typography.headlineMedium)
        Text("Today: $steps", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun StepsProgress(steps: Int, dailyGoal: Int, modifier: Modifier = Modifier) {
    val goalProgress = if (dailyGoal > 0) (steps.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f) else 0f

    Card(modifier = modifier.fillMaxWidth().semantics { testTag = "steps_progress"; contentDescription = "Daily goal progress: ${ (goalProgress * 100).toInt() } percent" }) {
        Column(modifier = Modifier.padding(Spacing.large)) {
            Text("Daily goal", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress = goalProgress, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Progress towards daily step goal" })
            Text(
                "$steps / $dailyGoal steps · ${(goalProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


@Composable
fun TodayActivity(segments: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    // Each segment shows: small icon, micro-sparkline, value, label
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cardBg = if (isDark) BlendPrimaryDark else BlendLight
    Card(modifier = modifier.fillMaxWidth().semantics { testTag = "today_activity"; contentDescription = "Activity segments for today" }, colors = CardDefaults.cardColors(containerColor = cardBg)) {
        Column(modifier = Modifier.padding(Spacing.large), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Text("Today's activity", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                segments.forEachIndexed { segIndex, (label, value) ->
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        StepSegmentIcon(label = label, modifier = Modifier.size(28.dp))

                        Spacer(modifier = Modifier.height(6.dp))

                        // micro-sparkline
                        val series = remember(label, value) {
                            // deterministic small series based on value and label index
                            val base = value.coerceAtLeast(0).toFloat()
                            List(6) { i ->
                                // create ascending shape with small variance
                                val fraction = 0.3f + i * 0.14f
                                (base * fraction * (1f + ((segIndex + 1) * 0.03f)))
                            }
                        }

                        Canvas(modifier = Modifier
                            .height(34.dp)
                            .fillMaxWidth()) {
                            val padding = 6f
                            val w = drawContext.size.width.toFloat() - padding * 2
                            val h = drawContext.size.height.toFloat() - padding * 2
                            val max = (series.maxOrNull() ?: 1f).coerceAtLeast(1f)
                            val points = series.mapIndexed { i, v ->
                                val x = padding + (i.toFloat() / (series.size - 1).coerceAtLeast(1).toFloat()) * w
                                val y = padding + (1f - (v / max)) * h
                                androidx.compose.ui.geometry.Offset(x, y)
                            }
                            if (points.size >= 2) {
                                // line
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    reset()
                                    moveTo(points[0].x, points[0].y)
                                    for (p in points.drop(1)) lineTo(p.x, p.y)
                                }
                                drawPath(path = path, color = StepsAmber, style = Stroke(width = 3f, cap = StrokeCap.Round))
                                // fill under curve with soft alpha
                                val fillPath = androidx.compose.ui.graphics.Path().apply {
                                    reset()
                                    moveTo(points[0].x, drawContext.size.height.toFloat() - padding)
                                    for (p in points) lineTo(p.x, p.y)
                                    lineTo(points.last().x, drawContext.size.height.toFloat() - padding)
                                    close()
                                }
                                drawPath(path = fillPath, color = StepsAmber.copy(alpha = 0.12f))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(value.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}


@Composable
fun StepsWeeklyChart(values: List<Int>, dailyGoal: Int, modifier: Modifier = Modifier) {
    // Use the maximum value from the past week (not the goal) as the scale reference
    val maxVal = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    val maxBarHeight = 120.dp // fixed maximum bar height inside the card
    val barWidth = 28.dp // slightly thicker bars
    val gap = 8.dp // gap between columns

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cardBg = if (isDark) BlendPrimaryDark else BlendLight
    Card(modifier = modifier.fillMaxWidth().semantics { testTag = "steps_weekly_chart"; contentDescription = "Steps over the last 7 days" }, colors = CardDefaults.cardColors(containerColor = cardBg)) {
        val selectedIndex = remember { mutableStateOf(-1) }
        val today = LocalDate.now()

        Row(
            modifier = Modifier
                .padding(Spacing.large)
                .height(maxBarHeight + 40.dp), // reserve space for labels below
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            val density = LocalDensity.current

            values.forEachIndexed { index, v ->
                val ratio = if (maxVal > 0) (v.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f) else 0f
                // compute target height relative to fixed maxBarHeight
                val targetHeight = with(density) { (maxBarHeight.toPx() * ratio).toDp() }.coerceAtLeast(8.dp)
                val animHeight by animateDpAsState(targetValue = targetHeight)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedIndex.value = if (selectedIndex.value == index) -1 else index },
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Bar area (fixed height) with bar bottom-aligned
                    Box(
                        modifier = Modifier
                            .height(maxBarHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .height(animHeight)
                                .background(
                                    color = if (index == values.lastIndex) StepsAmberDark else StepsAmber.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Day initial (S M T W T F S)
                    val labelDate = today.minusDays((values.size - 1 - index).toLong())
                    val initial = labelDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).substring(0, 1).uppercase(Locale.ENGLISH)
                    Text(initial, style = MaterialTheme.typography.labelSmall)

                    Spacer(modifier = Modifier.height(4.dp))

                    // Numeric value below the day initial
                    Text((v).toString(), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

