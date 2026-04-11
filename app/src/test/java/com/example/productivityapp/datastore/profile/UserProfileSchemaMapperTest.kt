package com.example.productivityapp.datastore.profile

import com.example.productivityapp.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserProfileSchemaMapperTest {

    @Test
    fun fromLegacy_preservesNullabilityAndDefaults() {
        val mapped = UserProfileSchemaMapper.fromLegacy(
            LegacyProfileSnapshot(
                displayName = "  ",
                weightKgRaw = null,
                heightCm = null,
                strideLengthMetersRaw = null,
                preferredUnits = null,
                dailyStepGoal = null,
                dailyWaterGoalMl = null,
            )
        )

        assertNull(mapped.profile.displayName)
        assertNull(mapped.profile.weightKg)
        assertNull(mapped.profile.heightCm)
        assertEquals(0.78, mapped.profile.strideLengthMeters, 0.0)
        assertEquals("metric", mapped.profile.preferredUnits)
        assertEquals(10000, mapped.profile.dailyStepGoal)
        assertEquals(2000, mapped.profile.dailyWaterGoalMl)
        assertEquals(ProfileMigrationState.NONE, mapped.migrationState)
    }

    @Test
    fun fromLegacy_handlesMalformedNumericValues() {
        val mapped = UserProfileSchemaMapper.fromLegacy(
            LegacyProfileSnapshot(
                displayName = "Alex",
                weightKgRaw = "oops",
                heightCm = -4,
                strideLengthMetersRaw = "bad",
                preferredUnits = "imperial",
                dailyStepGoal = -99,
                dailyWaterGoalMl = 0,
            )
        )

        assertEquals("Alex", mapped.profile.displayName)
        assertNull(mapped.profile.weightKg)
        assertNull(mapped.profile.heightCm)
        assertEquals(0.78, mapped.profile.strideLengthMeters, 0.0)
        assertEquals("imperial", mapped.profile.preferredUnits)
        assertEquals(10000, mapped.profile.dailyStepGoal)
        assertEquals(2000, mapped.profile.dailyWaterGoalMl)
    }

    @Test
    fun protoRoundTrip_preservesProfileValues() {
        val original = SecureStoredUserProfile(
            profile = UserProfile(
                displayName = "Mina",
                weightKg = 63.5,
                heightCm = 171,
                strideLengthMeters = 0.82,
                preferredUnits = "metric",
                dailyStepGoal = 12000,
                dailyWaterGoalMl = 2500,
            ),
            schemaVersion = 1,
            migrationState = ProfileMigrationState.MIGRATING,
            migratedAtEpochMs = 1234L,
            lastWriteEpochMs = 5678L,
        )

        val restored = UserProfileSchemaMapper.fromProto(UserProfileSchemaMapper.toProto(original))

        assertEquals(original, restored)
    }
}

