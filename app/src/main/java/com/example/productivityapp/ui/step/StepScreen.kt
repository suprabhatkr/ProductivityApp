package com.example.productivityapp.ui.step

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * Step counter screen
 * - Top: pinned circular ring showing progress (doesn't scroll)
 * - Below: scrollable area with analysis, day-line (sparkline), past days
 */
@Composable
fun StepScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current

    // Placeholder data — later replace with real ViewModel/state
    val stepsToday = 6423
    val goal = 10000
    val progress = (stepsToday.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val calories = (stepsToday * 0.04)
    val dayLine = listOf(200, 400, 1200, 800, 900, 1500, 1423)

    val ringHeight = 300.dp

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Steps", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Scrollable content starts below the pinned ring — use padding top equal ring height
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = ringHeight)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Today\'s Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Steps: $stepsToday / $goal")
                            Text(String.format("Avg pace: %d steps/hour", (stepsToday / 12)))
                            Text(String.format(Locale.US, "Calories: %.0f kcal", calories))
                        }
                    }
                }

                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Today\'s Line", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            DaySparkline(points = dayLine, modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp))
                        }
                    }
                }

                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Past days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Example list of past days
                val past = listOf("Mon — 8,200", "Tue — 9,100", "Wed — 6,400", "Thu — 10,200", "Fri — 7,350")
                items(past) { day ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(day)
                            Button(onClick = {}) { Text("Details") }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            // Pinned circular ring
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(ringHeight)
                .align(Alignment.TopCenter), contentAlignment = Alignment.Center) {
                StepRing(steps = stepsToday, goal = goal, progress = progress)
            }
        }
    }
}

@Composable
private fun StepRing(steps: Int, goal: Int, progress: Float) {
    val size = 220.dp
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = 20f, cap = StrokeCap.Round)
            val radius = min(size.toPx(), size.toPx()) / 2f
            // background ring
            drawArc(color = Color.LightGray, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
            // progress
            drawArc(color = Color(0xFF6A1B9A), startAngle = -90f, sweepAngle = 360f * progress, useCenter = false, style = stroke)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${steps}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("of $goal", style = MaterialTheme.typography.bodySmall)
        }
    }
}

import java.util.Locale
@Composable
private fun DaySparkline(points: List<Int>, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Canvas(modifier = modifier.padding(8.dp)) {
            if (points.isEmpty()) return@Canvas
            val w = size.width
            val h = size.height
            val maxV = (points.maxOrNull() ?: 1).toFloat()
            val stepX = w / (points.size - 1).coerceAtLeast(1)
            var prevX = 0f
            var prevY = h - (points[0] / maxV) * h
            for (i in points.indices) {
                val x = i * stepX
                val y = h - (points[i] / maxV) * h
                drawLine(color = Color(0xFF3F51B5), start = Offset(prevX, prevY), end = Offset(x, y), strokeWidth = 4f)
                prevX = x
                prevY = y
            }
        }
    }
}

