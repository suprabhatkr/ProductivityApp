package com.example.productivityapp.navigation

/** Single source of truth for the active Compose navigation routes. */
object AppRoutes {
    const val HOME = "home"
    const val STEPS = "steps"
    const val STEPS_LEGACY = "steps_legacy"
    const val RUN = "run"
    const val RUN_DETAILS = "run/{runId}"
    const val SLEEP = "sleep"
    const val WATER = "water"
    const val SETTINGS = "settings"

    fun runDetails(runId: Long): String = "run/$runId"
}
