package com.example.productivityapp.ui.steps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.viewmodel.StepViewModelFactory
import com.example.productivityapp.service.StepCounterService
import android.content.Intent
import android.Manifest
import android.provider.Settings
import android.net.Uri
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StepScreen() {
    val context = LocalContext.current
    val repo = RepositoryProvider.provideStepRepository(context)
    val vm: com.example.productivityapp.viewmodel.StepViewModel = viewModel(factory = StepViewModelFactory(repo))
    val steps = vm.steps.collectAsState()

    var running by remember { mutableStateOf(false) }
    val permissionState = rememberPermissionState(Manifest.permission.ACTIVITY_RECOGNITION)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Steps")
            Text("Today: ${steps.value}")
            Button(onClick = { vm.addManualSteps(100) }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Add 100 steps (manual)")
            }

            when (val status = permissionState.status) {
                is PermissionStatus.Granted -> {
                    Button(onClick = {
                        val intent = Intent(context, StepCounterService::class.java)
                        if (!running) {
                            intent.action = StepCounterService.ACTION_START
                            context.startForegroundService(intent)
                            running = true
                        } else {
                            intent.action = StepCounterService.ACTION_STOP
                            context.startService(intent)
                            running = false
                        }
                    }, modifier = Modifier.padding(top = 8.dp)) {
                        Text(if (!running) "Start Step Service" else "Stop Step Service")
                    }
                }
                is PermissionStatus.Denied -> {
                    // show rationale if needed
                    if (status.shouldShowRationale) {
                        AlertDialog(
                            onDismissRequest = {},
                            confirmButton = {
                                TextButton(onClick = { permissionState.launchPermissionRequest() }) {
                                    Text("Allow")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { /* do nothing */ }) {
                                    Text("Cancel")
                                }
                            },
                            title = { Text("Activity recognition") },
                            text = { Text("This app needs activity recognition to count your steps automatically. Please grant the permission.") }
                        )
                    } else {
                        // permanently denied or first time — offer request or open settings
                        Column {
                            Button(onClick = { permissionState.launchPermissionRequest() }, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Grant Activity Recognition")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                // open app settings
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
}

