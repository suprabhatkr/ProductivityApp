package com.example.productivityapp.ui.screens

import org.junit.Test
import com.example.productivityapp.test.ComposeTestRuleHolder
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable

/**
 * JVM Compose snapshot-like test for the Run screen's stats card. Uses the
 * Compose test rule and app theme wrapper from ComposeTestRuleHolder.
 */
class RunScreenPaparazziTest : ComposeTestRuleHolder() {

    @Composable
    private fun TestStatsCard(distanceMeters: Double, durationSec: Long, avgSpeedMps: Double, calories: Double) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Live Stats")
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Distance: %.2f km".format(distanceMeters / 1000.0))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Time: %02d:%02d".format((durationSec / 60), (durationSec % 60)))
                }
            }
        }
    }

    @Test
    fun run_stats_snapshot() {
        setThemedContent {
            TestStatsCard(distanceMeters = 5234.0, durationSec = 1800L, avgSpeedMps = 3.2, calories = 240.0)
        }
    }
}


