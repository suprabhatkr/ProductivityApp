package com.example.productivityapp

// android.os.Bundle will be referenced explicitly to avoid ambiguous import issues
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.productivityapp.app.ui.home.HomeScreen as AppHomeScreen
import com.example.productivityapp.app.ui.water.WaterIntakeScreen as AppWaterScreen
import com.example.productivityapp.app.viewmodel.HomeViewModel
import com.example.productivityapp.app.viewmodel.HomeViewModelFactory
import com.example.productivityapp.app.viewmodel.WaterViewModel
import com.example.productivityapp.app.viewmodel.WaterViewModelFactory
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.ui.debug.MigrationStatusOverlay
import com.example.productivityapp.navigation.AppRoutes
import com.example.productivityapp.service.HealthReminderWorker
import com.example.productivityapp.service.MidnightResetWorker
import com.example.productivityapp.service.SleepMaintenanceWorker
import com.example.productivityapp.ui.run.RunDetailsScreen
import com.example.productivityapp.ui.run.RunScreen
import com.example.productivityapp.ui.sleep.SleepScreen
import com.example.productivityapp.ui.settings.MandatoryProfileSetupDialog
import com.example.productivityapp.ui.settings.SettingsScreen
import com.example.productivityapp.ui.steps.StepScreen
import com.example.productivityapp.ui.theme.ProductivityAppTheme
import com.example.productivityapp.viewmodel.SettingsViewModel
import com.example.productivityapp.viewmodel.SettingsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MidnightResetWorker.schedule(applicationContext)
        SleepMaintenanceWorker.schedule(applicationContext)
        HealthReminderWorker.ensureScheduled(applicationContext)

        setContent {
            ProductivityAppTheme(darkTheme = isSystemInDarkTheme()) {
                val navController = rememberNavController()
                val ctx = LocalContext.current
                val profileRepository = remember { RepositoryProvider.provideUserProfileRepository(this@MainActivity) }
                val profileSetupViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(
                        profileRepository = profileRepository,
                        themeRepository = RepositoryProvider.provideAppThemeRepository(this@MainActivity),
                    )
                )
                val profileSetupState by profileSetupViewModel.uiState.collectAsState()
                val savedProfile by profileRepository.observeUserProfile().collectAsState(initial = UserProfile())
                val requiresProfileSetup = !profileSetupState.isLoading &&
                    requiresMandatoryProfileSetup(savedProfile)

                // Layer UI so debug overlays can be shown on top of app content
                Box(modifier = Modifier.fillMaxSize()) {

                        NavHost(navController = navController, startDestination = AppRoutes.HOME) {
                    composable(AppRoutes.HOME) {
                        val homeVm: HomeViewModel = viewModel(
                            factory = HomeViewModelFactory(
                                waterRepository = RepositoryProvider.provideWaterRepository(this@MainActivity),
                                stepRepository = RepositoryProvider.provideStepRepository(this@MainActivity),
                                runRepository = RepositoryProvider.provideRunRepository(this@MainActivity),
                                sleepRepository = RepositoryProvider.provideSleepRepository(this@MainActivity),
                                userProfileRepository = RepositoryProvider.provideUserProfileRepository(this@MainActivity),
                            )
                        )
                        AppHomeScreen(
                            onNavigateToSteps = { navController.navigate(AppRoutes.STEPS) },
                            onNavigateToStepsLegacy = { navController.navigate(AppRoutes.STEPS_LEGACY) },
                            onNavigateToRun = { navController.navigate(AppRoutes.RUN) },
                            onNavigateToSleep = { navController.navigate(AppRoutes.SLEEP) },
                            onNavigateToWater = { navController.navigate(AppRoutes.WATER) },
                            onNavigateToSettings = { navController.navigate(AppRoutes.SETTINGS) },
                            homeViewModel = homeVm,
                        )
                    }

                    // Show the new ring-style screen at the canonical STEPS route and keep the older UI available
                    composable(AppRoutes.STEPS) { com.example.productivityapp.ui.steps.StepScreen(onBack = { navController.popBackStack() }) }
                    composable(AppRoutes.STEPS_LEGACY) { com.example.productivityapp.ui.step.StepScreen(onBack = { navController.popBackStack() }) }
                    composable(AppRoutes.RUN) {
                        RunScreen(
                            onBack = { navController.popBackStack() },
                            onOpenRunDetails = { runId -> navController.navigate(AppRoutes.runDetails(runId)) },
                        )
                    }
                    composable(AppRoutes.RUN_DETAILS) { backStackEntry ->
                        val runId = backStackEntry.arguments?.getString("runId")?.toLongOrNull()
                        RunDetailsScreen(
                            runId = runId,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(AppRoutes.SLEEP) { SleepScreen(onBack = { navController.popBackStack() }) }
                    composable(AppRoutes.WATER) {
                        val waterVm: com.example.productivityapp.app.viewmodel.WaterViewModel = viewModel(
                            factory = WaterViewModelFactory(
                                RepositoryProvider.provideWaterRepository(this@MainActivity)
                            )
                        )
                        AppWaterScreen(onBack = { navController.popBackStack() }, viewModel = waterVm)
                    }
                    composable(AppRoutes.SETTINGS) {
                        val settingsVm: SettingsViewModel = viewModel(
                            factory = SettingsViewModelFactory(
                                profileRepository = RepositoryProvider.provideUserProfileRepository(this@MainActivity),
                                themeRepository = RepositoryProvider.provideAppThemeRepository(this@MainActivity),
                            )
                        )
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            viewModel = settingsVm,
                        )
                    }
                }
                    if (requiresProfileSetup) {
                        MandatoryProfileSetupDialog(
                            uiState = profileSetupState,
                            onDisplayNameChanged = profileSetupViewModel::updateDisplayName,
                            onAgeChanged = profileSetupViewModel::updateAgeYears,
                            onGenderChanged = profileSetupViewModel::updateGender,
                            onHeightChanged = profileSetupViewModel::updateHeightCm,
                            onWeightChanged = profileSetupViewModel::updateWeightKg,
                            onSave = { profileSetupViewModel.saveSettings() },
                        )
                    }
                    // small debug overlay to show migration/legacy status for manual QA
                    // MigrationStatusOverlay(appContext = ctx)
                }
            }
        }
    }
}

internal fun requiresMandatoryProfileSetup(profile: UserProfile): Boolean {
    return profile.displayName.isNullOrBlank() || profile.ageYears == null
}
