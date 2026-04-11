package com.example.productivityapp.datastore.proto

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.InvalidProtocolBufferException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class WaterEntryProto private constructor(
    val id: Long,
    val amountMl: Int,
    val timestampEpochMs: Long,
) {

    fun writeTo(output: OutputStream) {
        val codedOutput = CodedOutputStream.newInstance(output)
        codedOutput.writeInt64(1, id)
        codedOutput.writeInt32(2, amountMl)
        codedOutput.writeInt64(3, timestampEpochMs)
        codedOutput.flush()
    }

    fun toBuilder(): Builder = Builder()
        .apply {
            setId(id)
            setAmountMl(amountMl)
            setTimestampEpochMs(timestampEpochMs)
        }

    class Builder {
        private var id: Long = 0L
        private var amountMl: Int = 0
        private var timestampEpochMs: Long = 0L

        fun setId(value: Long) = apply { id = value }
        fun setAmountMl(value: Int) = apply { amountMl = value }
        fun setTimestampEpochMs(value: Long) = apply { timestampEpochMs = value }

        fun build(): WaterEntryProto = WaterEntryProto(id = id, amountMl = amountMl, timestampEpochMs = timestampEpochMs)
    }

    companion object {
        @JvmStatic
        fun newBuilder(): Builder = Builder()

        @JvmStatic
        @Throws(InvalidProtocolBufferException::class)
        fun parseFrom(input: InputStream): WaterEntryProto {
            try {
                val codedInput = CodedInputStream.newInstance(input)
                var id: Long = 0L
                var amountMl: Int = 0
                var ts: Long = 0L
                var done = false
                while (!done) {
                    when (val tag = codedInput.readTag()) {
                        0 -> done = true
                        8 -> id = codedInput.readInt64()
                        16 -> amountMl = codedInput.readInt32()
                        24 -> ts = codedInput.readInt64()
                        else -> if (!codedInput.skipField(tag)) done = true
                    }
                }
                return WaterEntryProto(id = id, amountMl = amountMl, timestampEpochMs = ts)
            } catch (ioException: IOException) {
                throw InvalidProtocolBufferException(ioException)
            }
        }

        // helper to parse from raw bytes
        fun parseFrom(bytes: ByteArray): WaterEntryProto = parseFrom(ByteArrayInputStream(bytes))
    }
}

