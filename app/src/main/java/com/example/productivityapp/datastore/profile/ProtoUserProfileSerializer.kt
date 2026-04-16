package com.example.productivityapp.datastore.profile

import androidx.datastore.core.Serializer
import com.example.productivityapp.datastore.profile.proto.MigrationStateProto
import com.example.productivityapp.datastore.profile.proto.UserProfileProto
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object ProtoUserProfileSerializer : Serializer<UserProfileProto> {
    override val defaultValue: UserProfileProto = UserProfileProto.newBuilder()
        .setStrideLengthMeters(0.78)
        .setPreferredUnits("metric")
        .setDailyStepGoal(10000)
        .setDailyWaterGoalMl(2000)
        .setNightlySleepGoalMinutes(480)
        .setTypicalBedtimeMinutes(1320)
        .setTypicalWakeTimeMinutes(420)
        .setSleepDetectionBufferMinutes(30)
        .setSchemaVersion(SecureStoredUserProfile.CURRENT_SCHEMA_VERSION)
        .setMigrationState(MigrationStateProto.MIGRATION_STATE_NONE)
        .build()

    override suspend fun readFrom(input: InputStream): UserProfileProto {
        return try {
            UserProfileProto.parseFrom(input)
        } catch (_: InvalidProtocolBufferException) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: UserProfileProto, output: OutputStream) {
        t.writeTo(output)
    }
}
