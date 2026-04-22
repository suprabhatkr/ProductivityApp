package com.example.productivityapp.datastore.profile.proto

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.InvalidProtocolBufferException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class UserProfileProto private constructor(
    private val displayNameValue: String?,
    private val weightKgValue: Double?,
    private val heightCmValue: Int?,
    val strideLengthMeters: Double,
    val preferredUnits: String,
    val dailyStepGoal: Int,
    val dailyWaterGoalMl: Int,
    val nightlySleepGoalMinutes: Int,
    val typicalBedtimeMinutes: Int,
    val typicalWakeTimeMinutes: Int,
    val sleepDetectionBufferMinutes: Int,
    val schemaVersion: Int,
    val migrationState: MigrationStateProto,
    val migratedAtEpochMs: Long,
    val lastWriteEpochMs: Long,
) {
    val displayName: String
        get() = displayNameValue.orEmpty()

    val weightKg: Double
        get() = weightKgValue ?: 0.0

    val heightCm: Int
        get() = heightCmValue ?: 0

    fun hasDisplayName(): Boolean = displayNameValue != null

    fun hasWeightKg(): Boolean = weightKgValue != null

    fun hasHeightCm(): Boolean = heightCmValue != null

    fun writeTo(output: OutputStream) {
        val codedOutput = CodedOutputStream.newInstance(output)
        displayNameValue?.let {
            codedOutput.writeString(1, it)
        }
        weightKgValue?.let {
            codedOutput.writeDouble(2, it)
        }
        heightCmValue?.let {
            codedOutput.writeInt32(3, it)
        }
        codedOutput.writeDouble(4, strideLengthMeters)
        codedOutput.writeString(5, preferredUnits)
        codedOutput.writeInt32(6, dailyStepGoal)
        codedOutput.writeInt32(7, dailyWaterGoalMl)
        codedOutput.writeInt32(8, schemaVersion)
        codedOutput.writeEnum(9, migrationState.getNumber())
        codedOutput.writeInt64(10, migratedAtEpochMs)
        codedOutput.writeInt64(11, lastWriteEpochMs)
        codedOutput.writeInt32(12, nightlySleepGoalMinutes)
        codedOutput.writeInt32(13, typicalBedtimeMinutes)
        codedOutput.writeInt32(14, typicalWakeTimeMinutes)
        codedOutput.writeInt32(15, sleepDetectionBufferMinutes)
        codedOutput.flush()
    }

    fun toBuilder(): Builder = Builder()
        .apply {
            displayNameValue?.let(::setDisplayName)
            weightKgValue?.let(::setWeightKg)
            heightCmValue?.let(::setHeightCm)
            setStrideLengthMeters(strideLengthMeters)
            setPreferredUnits(preferredUnits)
            setDailyStepGoal(dailyStepGoal)
            setDailyWaterGoalMl(dailyWaterGoalMl)
            setNightlySleepGoalMinutes(nightlySleepGoalMinutes)
            setTypicalBedtimeMinutes(typicalBedtimeMinutes)
            setTypicalWakeTimeMinutes(typicalWakeTimeMinutes)
            setSleepDetectionBufferMinutes(sleepDetectionBufferMinutes)
            setSchemaVersion(schemaVersion)
            setMigrationState(migrationState)
            setMigratedAtEpochMs(migratedAtEpochMs)
            setLastWriteEpochMs(lastWriteEpochMs)
        }

    class Builder {
        private var displayName: String? = null
        private var weightKg: Double? = null
        private var heightCm: Int? = null
        private var strideLengthMeters: Double = 0.0
        private var preferredUnits: String = ""
        private var dailyStepGoal: Int = 0
        private var dailyWaterGoalMl: Int = 0
        private var nightlySleepGoalMinutes: Int = 0
        private var typicalBedtimeMinutes: Int = 0
        private var typicalWakeTimeMinutes: Int = 0
        private var sleepDetectionBufferMinutes: Int = 0
        private var schemaVersion: Int = 0
        private var migrationState: MigrationStateProto = MigrationStateProto.MIGRATION_STATE_NONE
        private var migratedAtEpochMs: Long = 0L
        private var lastWriteEpochMs: Long = 0L

        fun setDisplayName(value: String) = apply { displayName = value }

        fun clearDisplayName() = apply { displayName = null }

        fun setWeightKg(value: Double) = apply { weightKg = value }

        fun clearWeightKg() = apply { weightKg = null }

        fun setHeightCm(value: Int) = apply { heightCm = value }

        fun clearHeightCm() = apply { heightCm = null }

        fun setStrideLengthMeters(value: Double) = apply { strideLengthMeters = value }

        fun setPreferredUnits(value: String) = apply { preferredUnits = value }

        fun setDailyStepGoal(value: Int) = apply { dailyStepGoal = value }

        fun setDailyWaterGoalMl(value: Int) = apply { dailyWaterGoalMl = value }

        fun setNightlySleepGoalMinutes(value: Int) = apply { nightlySleepGoalMinutes = value }

        fun setTypicalBedtimeMinutes(value: Int) = apply { typicalBedtimeMinutes = value }

        fun setTypicalWakeTimeMinutes(value: Int) = apply { typicalWakeTimeMinutes = value }

        fun setSleepDetectionBufferMinutes(value: Int) = apply { sleepDetectionBufferMinutes = value }

        fun setSchemaVersion(value: Int) = apply { schemaVersion = value }

        fun setMigrationState(value: MigrationStateProto) = apply { migrationState = value }

        fun setMigratedAtEpochMs(value: Long) = apply { migratedAtEpochMs = value }

        fun setLastWriteEpochMs(value: Long) = apply { lastWriteEpochMs = value }

        fun build(): UserProfileProto = UserProfileProto(
            displayNameValue = displayName,
            weightKgValue = weightKg,
            heightCmValue = heightCm,
            strideLengthMeters = strideLengthMeters,
            preferredUnits = preferredUnits,
            dailyStepGoal = dailyStepGoal,
            dailyWaterGoalMl = dailyWaterGoalMl,
            nightlySleepGoalMinutes = nightlySleepGoalMinutes,
            typicalBedtimeMinutes = typicalBedtimeMinutes,
            typicalWakeTimeMinutes = typicalWakeTimeMinutes,
            sleepDetectionBufferMinutes = sleepDetectionBufferMinutes,
            schemaVersion = schemaVersion,
            migrationState = migrationState,
            migratedAtEpochMs = migratedAtEpochMs,
            lastWriteEpochMs = lastWriteEpochMs,
        )
    }

    companion object {
        @JvmStatic
        fun newBuilder(): Builder = Builder()

        @JvmStatic
        @Throws(InvalidProtocolBufferException::class)
        fun parseFrom(input: InputStream): UserProfileProto {
            try {
                val codedInput = CodedInputStream.newInstance(input)
                val builder = newBuilder()
                var done = false
                while (!done) {
                    when (val tag = codedInput.readTag()) {
                        0 -> done = true
                        10 -> builder.setDisplayName(codedInput.readString())
                        17 -> builder.setWeightKg(codedInput.readDouble())
                        24 -> builder.setHeightCm(codedInput.readInt32())
                        33 -> builder.setStrideLengthMeters(codedInput.readDouble())
                        42 -> builder.setPreferredUnits(codedInput.readString())
                        48 -> builder.setDailyStepGoal(codedInput.readInt32())
                        56 -> builder.setDailyWaterGoalMl(codedInput.readInt32())
                        64 -> builder.setSchemaVersion(codedInput.readInt32())
                        72 -> builder.setMigrationState(MigrationStateProto.forNumber(codedInput.readEnum()))
                        80 -> builder.setMigratedAtEpochMs(codedInput.readInt64())
                        88 -> builder.setLastWriteEpochMs(codedInput.readInt64())
                        96 -> builder.setNightlySleepGoalMinutes(codedInput.readInt32())
                        104 -> builder.setTypicalBedtimeMinutes(codedInput.readInt32())
                        112 -> builder.setTypicalWakeTimeMinutes(codedInput.readInt32())
                        120 -> builder.setSleepDetectionBufferMinutes(codedInput.readInt32())
                        else -> if (!codedInput.skipField(tag)) {
                            done = true
                        }
                    }
                }
                return builder.build()
            } catch (ioException: IOException) {
                throw InvalidProtocolBufferException(ioException)
            }
        }
    }
}
