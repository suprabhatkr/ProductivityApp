package com.example.productivityapp

// android.os.Bundle will be referenced explicitly to avoid ambiguous import issues
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.app.ui.home.HomeScreen as AppHomeScreen
import com.example.productivityapp.app.ui.water.WaterIntakeScreen as AppWaterScreen
import com.example.productivityapp.service.MidnightResetWorker
import com.example.productivityapp.ui.run.RunScreen
import com.example.productivityapp.ui.sleep.SleepScreen
import com.example.productivityapp.ui.steps.StepScreen
import com.example.productivityapp.ui.settings.SettingsScreen
import com.example.productivityapp.app.viewmodel.WaterViewModel
import com.example.productivityapp.ui.theme.ProductivityAppTheme
import com.example.productivityapp.viewmodel.SettingsViewModel
import com.example.productivityapp.viewmodel.SettingsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MidnightResetWorker.schedule(applicationContext)

        setContent {
            ProductivityAppTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        val waterVm: WaterViewModel = viewModel()
                        AppHomeScreen(
                            onNavigateToWater = { navController.navigate("water") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            waterViewModel = waterVm
                        )
                    }

                    composable("steps") { StepScreen() }
                    composable("run") { RunScreen() }
                    composable("sleep") { SleepScreen() }
                    composable("water") {
                        val waterVm: com.example.productivityapp.app.viewmodel.WaterViewModel = viewModel()
                        AppWaterScreen(onBack = { navController.popBackStack() }, viewModel = waterVm)
                    }
                    composable("settings") {
                        val settingsVm: SettingsViewModel = viewModel(
                            factory = SettingsViewModelFactory(
                                RepositoryProvider.provideUserProfileRepository(this@MainActivity)
                            )
                        )
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            viewModel = settingsVm,
                        )
                    }
                }
            }
        }
    }
}
