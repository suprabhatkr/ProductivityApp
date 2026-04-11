package com.example.productivityapp.datastore

import com.example.productivityapp.app.data.model.WaterEntry
import com.example.productivityapp.datastore.proto.WaterEntryProto
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun WaterEntry.toProto(): WaterEntryProto =
    WaterEntryProto.newBuilder()
        .setId(this.id)
        .setAmountMl(this.amountMl)
        .setTimestampEpochMs(this.timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        .build()

fun WaterEntryProto.toDomain(): WaterEntry {
    val millis = this.timestampEpochMs
    val ldt = try {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
    } catch (_: Exception) {
        LocalDateTime.now()
    }
    return WaterEntry(id = this.id, amountMl = this.amountMl, timestamp = ldt)
}

