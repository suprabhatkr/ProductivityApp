package com.example.productivityapp.ui.steps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
fun StepsWeeklyChart(values: List<Int>, dailyGoal: Int, modifier: Modifier = Modifier) {
    val maxVal = max(dailyGoal, (values.maxOrNull() ?: 1))

    Card(modifier = modifier.fillMaxWidth().semantics { testTag = "steps_weekly_chart"; contentDescription = "Steps over the last 7 days" }) {
        val selectedIndex = remember { mutableStateOf<Int>(-1) }
        Row(modifier = Modifier.padding(Spacing.large), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            val today = LocalDate.now()
            values.forEachIndexed { index, v ->
                val ratio = if (maxVal > 0) (v.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f) else 0f
                val targetHeight = ((120 * ratio).coerceAtLeast(8f)).dp
                val animHeight by animateDpAsState(targetValue = targetHeight)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedIndex.value = if (selectedIndex.value == index) -1 else index }) {
                    if (selectedIndex.value == index) {
                        val labelDate = today.minusDays((values.size - 1 - index).toLong())
                        val dayLabel = labelDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        Text(dayLabel, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(animHeight)
                            .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text((v).toString(), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}


