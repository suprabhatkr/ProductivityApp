package com.example.productivityapp

// android.os.Bundle will be referenced explicitly to avoid ambiguous import issues
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.productivityapp.app.ui.home.HomeScreen as AppHomeScreen
import com.example.productivityapp.app.ui.water.WaterIntakeScreen as AppWaterScreen
import com.example.productivityapp.ui.run.RunScreen
import com.example.productivityapp.ui.sleep.SleepScreen
import com.example.productivityapp.ui.steps.StepScreen
import com.example.productivityapp.ui.settings.SettingsScreen
import com.example.productivityapp.app.viewmodel.WaterViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.productivityapp.ui.theme.ProductivityAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ProductivityAppTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        val waterVm: WaterViewModel = viewModel()
                        AppHomeScreen(
                            onNavigateToWater = { navController.navigate("water") },
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
                    composable("settings") { SettingsScreen() }
                }
            }
        }
    }
}
