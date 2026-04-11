package com.example.productivityapp.datastore.profile.proto

enum class MigrationStateProto(private val wireValue: Int) {
    MIGRATION_STATE_NONE(0),
    MIGRATION_STATE_MIGRATING(1),
    MIGRATION_STATE_COMPLETE(2),
    MIGRATION_STATE_FAILED(3),
    UNRECOGNIZED(-1),
    ;

    fun getNumber(): Int = wireValue

    companion object {
        fun forNumber(value: Int): MigrationStateProto = when (value) {
            0 -> MIGRATION_STATE_NONE
            1 -> MIGRATION_STATE_MIGRATING
            2 -> MIGRATION_STATE_COMPLETE
            3 -> MIGRATION_STATE_FAILED
            else -> UNRECOGNIZED
        }
    }
}

