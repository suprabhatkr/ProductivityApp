package com.example.productivityapp.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onOpenSteps: () -> Unit,
    onOpenRun: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWater: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("ProductivityApp — Home")
            Button(onClick = onOpenSteps, modifier = Modifier.padding(top = 8.dp)) { Text("Open Steps") }
            Button(onClick = onOpenRun, modifier = Modifier.padding(top = 8.dp)) { Text("Open Run") }
            Button(onClick = onOpenSleep, modifier = Modifier.padding(top = 8.dp)) { Text("Open Sleep") }
            Button(onClick = onOpenWater, modifier = Modifier.padding(top = 8.dp)) { Text("Open Water") }
            Button(onClick = onOpenSettings, modifier = Modifier.padding(top = 8.dp)) { Text("Settings") }
        }
    }
}

