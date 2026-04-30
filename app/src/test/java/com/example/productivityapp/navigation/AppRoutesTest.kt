package com.example.productivityapp.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AppRoutesTest {

    @Test
    fun runDetails_buildsParameterizedRoute() {
        assertEquals("run/42", AppRoutes.runDetails(42L))
        assertEquals("run/{runId}", AppRoutes.RUN_DETAILS)
    }
}
