package com.example.productivityapp

import com.example.productivityapp.data.model.AppThemePreference
import com.example.productivityapp.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityTest {
    @Test
    fun requiresMandatoryProfileSetup_returnsTrueWhenNameMissing() {
        assertTrue(requiresMandatoryProfileSetup(UserProfile(displayName = "", ageYears = 28)))
    }

    @Test
    fun requiresMandatoryProfileSetup_returnsTrueWhenAgeMissing() {
        assertTrue(requiresMandatoryProfileSetup(UserProfile(displayName = "Alex", ageYears = null)))
    }

    @Test
    fun requiresMandatoryProfileSetup_returnsFalseWhenNameAndAgeExist() {
        assertFalse(requiresMandatoryProfileSetup(UserProfile(displayName = "Alex", ageYears = 28)))
    }

    @Test
    fun shouldUseDarkTheme_returnsSystemChoiceWhenPreferenceIsSystem() {
        assertEquals(true, shouldUseDarkTheme(AppThemePreference.SYSTEM, systemDarkTheme = true))
        assertEquals(false, shouldUseDarkTheme(AppThemePreference.SYSTEM, systemDarkTheme = false))
    }

    @Test
    fun shouldUseDarkTheme_returnsFalseWhenPreferenceIsLight() {
        assertFalse(shouldUseDarkTheme(AppThemePreference.LIGHT, systemDarkTheme = true))
    }

    @Test
    fun shouldUseDarkTheme_returnsTrueWhenPreferenceIsDark() {
        assertTrue(shouldUseDarkTheme(AppThemePreference.DARK, systemDarkTheme = false))
    }
}
