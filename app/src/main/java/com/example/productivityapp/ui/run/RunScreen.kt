package com.example.productivityapp.ui.run

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
import com.example.productivityapp.viewmodel.RunViewModelFactory
import android.Manifest
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.example.productivityapp.service.RunTrackingService
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.provider.Settings
import android.net.Uri
import com.google.accompanist.permissions.PermissionStatus
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RunScreen() {
    val context = LocalContext.current
    val repo = RepositoryProvider.provideRunRepository(context)
    val vm: com.example.productivityapp.viewmodel.RunViewModel = viewModel(factory = RunViewModelFactory(repo))
    val runs = vm.runs.collectAsState()

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val backgroundPermissionState = rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    var running by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Run Tracker")
            Text("Past runs: ${runs.value.size}")

            when (val fg = locationPermissionState.status) {
                is PermissionStatus.Granted -> {
                    Column {
                        Button(onClick = {
                            val intent = Intent(context, RunTrackingService::class.java)
                            if (!running) {
                                intent.action = RunTrackingService.ACTION_START
                                context.startForegroundService(intent)
                                running = true
                            } else {
                                intent.action = RunTrackingService.ACTION_STOP
                                context.startService(intent)
                                running = false
                            }
                        }, modifier = Modifier.padding(top = 8.dp)) {
                            Text(if (!running) "Start Run" else "Stop Run")
                        }

                        // background permission prompt (optional)
                        when (val bg = backgroundPermissionState.status) {
                            is PermissionStatus.Granted -> {
                                // nothing
                            }
                            is PermissionStatus.Denied -> {
                                if (bg.shouldShowRationale) {
                                    AlertDialog(
                                        onDismissRequest = {},
                                        confirmButton = {
                                            TextButton(onClick = { backgroundPermissionState.launchPermissionRequest() }) {
                                                Text("Allow Background")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = {}) { Text("Later") }
                                        },
                                        title = { Text("Background location") },
                                        text = { Text("To continue tracking runs while the app is backgrounded, allow background location access.") }
                                    )
                                } else {
                                    // show a small action to open settings or request
                                    Column {
                                        Button(onClick = { backgroundPermissionState.launchPermissionRequest() }, modifier = Modifier.padding(top = 8.dp)) {
                                            Text("Request background location")
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                            context.startActivity(intent)
                                        }) {
                                            Text("Open app settings")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    Button(onClick = { locationPermissionState.launchPermissionRequest() }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Grant Location Permission")
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Show map for latest run (live update)
            val latest = runs.value.firstOrNull()
            if (latest != null) {
                androidx.compose.foundation.layout.Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()) {
                    RunMapView(polylineEncoded = latest.polyline, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

