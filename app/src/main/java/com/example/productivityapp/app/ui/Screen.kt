package com.example.productivityapp.app.ui

sealed class Screen {
    object Home : Screen()
    object WaterIntake : Screen()
    object Steps : Screen()
    object Run : Screen()
    object Sleep : Screen()
    object Settings : Screen()
}
