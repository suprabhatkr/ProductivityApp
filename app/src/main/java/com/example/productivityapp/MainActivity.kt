package com.example.productivityapp

import android.os.Bundle
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
// unused layout imports removed
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
// Modifier not needed in this Activity
import androidx.lifecycle.ViewModelProvider
import com.example.productivityapp.app.ui.Screen
import com.example.productivityapp.app.ui.home.HomeScreen
import com.example.productivityapp.app.ui.water.WaterIntakeScreen
import com.example.productivityapp.app.viewmodel.WaterViewModel
import com.example.productivityapp.ui.theme.ProductivityAppTheme

class MainActivity : ComponentActivity() {
    // Keep the ViewModel as an Activity-level property so lifecycle-aware receivers can call it
    private lateinit var waterViewModel: WaterViewModel

    private val dateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Forward the event to ViewModel to refresh today's data
            if (this@MainActivity::waterViewModel.isInitialized) {
                waterViewModel.refresh()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Obtain ViewModel the conventional way to avoid requiring compose viewmodel helpers
        waterViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[WaterViewModel::class.java]

        setContent {
            ProductivityAppTheme {
                // Simple state-based navigation to avoid pulling in Navigation Compose dependency
                val currentScreenState = remember { mutableStateOf<Screen>(Screen.Home) }

                when (currentScreenState.value) {
                    is Screen.Home -> HomeScreen(
                        onNavigateToWater = { currentScreenState.value = Screen.WaterIntake },
                        waterViewModel = waterViewModel
                    )
                    is Screen.WaterIntake -> WaterIntakeScreen(
                        onBack = { currentScreenState.value = Screen.Home },
                        viewModel = waterViewModel
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Register dynamically while activity (app) is in foreground. This keeps behavior
        // in-process only and avoids any background work when the app is not running.
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        registerReceiver(dateReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(dateReceiver)
        } catch (e: Exception) {
            // Ignore if already unregistered for any reason
        }
    }
}
