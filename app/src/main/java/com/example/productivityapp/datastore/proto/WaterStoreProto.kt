package com.example.productivityapp.datastore.proto

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.InvalidProtocolBufferException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class WaterStoreProto private constructor(
    val daysMap: Map<String, WaterDayProto>,
) {

    fun writeTo(output: OutputStream) {
        val codedOutput = CodedOutputStream.newInstance(output)
        // write each map entry as a length-delimited submessage on field 1
        for ((key, day) in daysMap) {
            val entryBaos = ByteArrayOutputStream()
            val entryCoded = CodedOutputStream.newInstance(entryBaos)
            entryCoded.writeString(1, key)
            val dayBaos = ByteArrayOutputStream()
            day.writeTo(dayBaos)
            entryCoded.writeByteArray(2, dayBaos.toByteArray())
            entryCoded.flush()
            codedOutput.writeByteArray(1, entryBaos.toByteArray())
        }
        codedOutput.flush()
    }

    fun toBuilder(): Builder = Builder().apply {
        daysMap.forEach { (k, v) -> putDays(k, v) }
    }

    class Builder {
        private val daysMutable: MutableMap<String, WaterDayProto> = mutableMapOf()

        fun putDays(key: String, value: WaterDayProto) = apply { daysMutable[key] = value }

        fun build(): WaterStoreProto = WaterStoreProto(daysMap = daysMutable.toMap())
    }

    companion object {
        @JvmStatic
        fun newBuilder(): Builder = Builder()

        @JvmStatic
        @Throws(InvalidProtocolBufferException::class)
        fun parseFrom(input: InputStream): WaterStoreProto {
            try {
                val codedInput = CodedInputStream.newInstance(input)
                val map: MutableMap<String, WaterDayProto> = mutableMapOf()
                var done = false
                while (!done) {
                    when (val tag = codedInput.readTag()) {
                        0 -> done = true
                        10 -> {
                            // read a length-delimited map entry message
                            val bytes = codedInput.readByteArray()
                            val entryInput = CodedInputStream.newInstance(ByteArrayInputStream(bytes))
                            var key: String = ""
                            var day: WaterDayProto? = null
                            var innerDone = false
                            while (!innerDone) {
                                when (val innerTag = entryInput.readTag()) {
                                    0 -> innerDone = true
                                    10 -> key = entryInput.readString()
                                    18 -> {
                                        val dayBytes = entryInput.readByteArray()
                                        day = WaterDayProto.parseFrom(ByteArrayInputStream(dayBytes))
                                    }
                                    else -> if (!entryInput.skipField(innerTag)) innerDone = true
                                }
                            }
                            if (key.isNotEmpty() && day != null) map[key] = day
                        }
                        else -> if (!codedInput.skipField(tag)) done = true
                    }
                }
                return WaterStoreProto(daysMap = map.toMap())
            } catch (ioException: IOException) {
                throw InvalidProtocolBufferException(ioException)
            }
        }

        fun parseFrom(bytes: ByteArray): WaterStoreProto = parseFrom(ByteArrayInputStream(bytes))
    }
}

