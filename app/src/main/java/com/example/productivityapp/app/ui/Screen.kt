package com.example.productivityapp.app.ui

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object WaterIntake : Screen("water_intake")
}
