package com.example.productivityapp.ui.sleep

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.viewmodel.SleepViewModelFactory

@Composable
fun SleepScreen() {
    val context = LocalContext.current
    val repo = RepositoryProvider.provideSleepRepository(context)
    val vm: com.example.productivityapp.viewmodel.SleepViewModel = viewModel(factory = SleepViewModelFactory(repo))
    val sessions = vm.sessions.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sleep Tracker")
            Text("Sessions today: ${sessions.value.size}")
            Button(onClick = { /* start sleep placeholder */ }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Start Sleep")
            }
        }
    }
}

