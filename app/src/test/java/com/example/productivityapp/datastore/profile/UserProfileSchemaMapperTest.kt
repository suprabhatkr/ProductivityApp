package com.example.productivityapp.datastore.profile

import com.example.productivityapp.data.model.AvatarConfig
import com.example.productivityapp.data.model.AvatarDefaults
import com.example.productivityapp.data.model.AvatarPresentation
import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.datastore.profile.proto.UserProfileProto
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
        assertNull(mapped.profile.ageYears)
        assertNull(mapped.profile.gender)
        assertEquals(0.78, mapped.profile.strideLengthMeters, 0.0)
        assertEquals("metric", mapped.profile.preferredUnits)
        assertEquals(10000, mapped.profile.dailyStepGoal)
        assertEquals(2000, mapped.profile.dailyWaterGoalMl)
        assertEquals(480, mapped.profile.nightlySleepGoalMinutes)
        assertEquals(1320, mapped.profile.typicalBedtimeMinutes)
        assertEquals(420, mapped.profile.typicalWakeTimeMinutes)
        assertEquals(30, mapped.profile.sleepDetectionBufferMinutes)
        assertEquals(AvatarConfig(), mapped.profile.avatar)
        assertEquals(ProfileMigrationState.NONE, mapped.migrationState)
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
                nightlySleepGoalMinutes = 460,
                typicalBedtimeMinutes = 1365,
                typicalWakeTimeMinutes = 405,
                sleepDetectionBufferMinutes = 35,
                ageYears = 27,
                gender = "Female",
                avatar = AvatarConfig(avatarId = "female_08"),
            ),
            schemaVersion = SecureStoredUserProfile.CURRENT_SCHEMA_VERSION,
            migrationState = ProfileMigrationState.MIGRATING,
            migratedAtEpochMs = 1234L,
            lastWriteEpochMs = 5678L,
        )

        val restored = UserProfileSchemaMapper.fromProto(UserProfileSchemaMapper.toProto(original))

        assertEquals(original, restored)
    }

    @Test
    fun fromProto_missingAvatarFields_defaultsAvatarConfig() {
        val proto = UserProfileSchemaMapper.toProto(
            SecureStoredUserProfile(
                profile = UserProfile(displayName = "Casey"),
            )
        ).toBuilder()
            .clearAvatarId()
            .clearAvatarSkinTone()
            .clearAvatarPresentation()
            .clearAvatarHairStyle()
            .clearAvatarGlassesStyle()
            .clearAvatarHatStyle()
            .build()

        val restored = UserProfileSchemaMapper.fromProto(proto)

        assertEquals(AvatarConfig(), restored.profile.avatar)
    }

    @Test
    fun fromProto_legacyPresentationMapsToMatchingAvatarSet() {
        val proto = UserProfileProto.newBuilder()
            .setStrideLengthMeters(0.78)
            .setPreferredUnits("metric")
            .setDailyStepGoal(10000)
            .setDailyWaterGoalMl(2000)
            .setNightlySleepGoalMinutes(480)
            .setTypicalBedtimeMinutes(1320)
            .setTypicalWakeTimeMinutes(420)
            .setSleepDetectionBufferMinutes(30)
            .setAvatarPresentation(AvatarPresentation.FEMININE.storageValue)
            .build()

        val restored = UserProfileSchemaMapper.fromProto(proto)

        assertEquals(AvatarDefaults.femaleAvatarIds.first(), restored.profile.avatar.avatarId)
    }
}
