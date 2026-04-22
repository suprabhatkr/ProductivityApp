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
import com.example.productivityapp.service.SleepMaintenanceWorker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.productivityapp.ui.debug.MigrationStatusOverlay
import com.example.productivityapp.navigation.AppRoutes
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
        SleepMaintenanceWorker.schedule(applicationContext)

        setContent {
            ProductivityAppTheme {
                val navController = rememberNavController()
                val ctx = LocalContext.current

                // Layer UI so debug overlays can be shown on top of app content
                Box(modifier = Modifier.fillMaxSize()) {

                        NavHost(navController = navController, startDestination = AppRoutes.HOME) {
                    composable(AppRoutes.HOME) {
                        val waterVm: WaterViewModel = viewModel()
                        AppHomeScreen(
                            onNavigateToSteps = { navController.navigate(AppRoutes.STEPS) },
                                    onNavigateToStepsLegacy = { navController.navigate(AppRoutes.STEPS_LEGACY) },
                            onNavigateToRun = { navController.navigate(AppRoutes.RUN) },
                            onNavigateToSleep = { navController.navigate(AppRoutes.SLEEP) },
                            onNavigateToWater = { navController.navigate(AppRoutes.WATER) },
                            onNavigateToSettings = { navController.navigate(AppRoutes.SETTINGS) },
                            waterViewModel = waterVm
                        )
                    }

                    // Show the new ring-style screen at the canonical STEPS route and keep the older UI available
                    composable(AppRoutes.STEPS) { com.example.productivityapp.ui.steps.StepScreen(onBack = { navController.popBackStack() }) }
                    composable(AppRoutes.STEPS_LEGACY) { com.example.productivityapp.ui.step.StepScreen(onBack = { navController.popBackStack() }) }
                    composable(AppRoutes.RUN) { RunScreen(onBack = { navController.popBackStack() }) }
                    composable(AppRoutes.SLEEP) { SleepScreen(onBack = { navController.popBackStack() }) }
                    composable(AppRoutes.WATER) {
                        val waterVm: com.example.productivityapp.app.viewmodel.WaterViewModel = viewModel()
                        AppWaterScreen(onBack = { navController.popBackStack() }, viewModel = waterVm)
                    }
                    composable(AppRoutes.SETTINGS) {
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
                    // small debug overlay to show migration/legacy status for manual QA
                    // MigrationStatusOverlay(appContext = ctx)
                }
            }
        }
    }
}
