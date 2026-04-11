package com.example.productivityapp.datastore

import androidx.datastore.core.Serializer
import com.example.productivityapp.datastore.proto.WaterStoreProto
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object WaterStoreSerializer : Serializer<WaterStoreProto> {
    override val defaultValue: WaterStoreProto = WaterStoreProto.newBuilder().build()

    override suspend fun readFrom(input: InputStream): WaterStoreProto {
        return try {
            WaterStoreProto.parseFrom(input)
        } catch (_: InvalidProtocolBufferException) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: WaterStoreProto, output: OutputStream) {
        t.writeTo(output)
    }
}

