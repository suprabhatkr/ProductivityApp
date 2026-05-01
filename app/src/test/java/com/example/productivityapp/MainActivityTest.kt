package com.example.productivityapp

import com.example.productivityapp.data.model.UserProfile
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
}
