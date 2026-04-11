package com.example.productivityapp.datastore.proto

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.InvalidProtocolBufferException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class WaterDayProto private constructor(
    val entriesList: List<WaterEntryProto>,
    val totalMl: Int,
) {

    fun writeTo(output: OutputStream) {
        val codedOutput = CodedOutputStream.newInstance(output)
        // write each entry as a length-delimited sub-message (field 1)
        for (entry in entriesList) {
            val baos = ByteArrayOutputStream()
            entry.writeTo(baos)
            codedOutput.writeByteArray(1, baos.toByteArray())
        }
        codedOutput.writeInt32(2, totalMl)
        codedOutput.flush()
    }

    fun toBuilder(): Builder = Builder().apply {
        clearEntries()
        addAllEntries(entriesList)
        setTotalMl(totalMl)
    }

    class Builder {
        private val entriesMutable: MutableList<WaterEntryProto> = mutableListOf()
        var totalMl: Int = 0

        fun addEntries(entry: WaterEntryProto) = apply { entriesMutable.add(entry) }
        fun addAllEntries(entries: List<WaterEntryProto>) = apply { entriesMutable.addAll(entries) }
        fun clearEntries() = apply { entriesMutable.clear() }
        fun setTotalMl(value: Int) = apply { totalMl = value }

        fun build(): WaterDayProto = WaterDayProto(entriesList = entriesMutable.toList(), totalMl = totalMl)
    }

    companion object {
        @JvmStatic
        fun newBuilder(): Builder = Builder()

        @JvmStatic
        @Throws(InvalidProtocolBufferException::class)
        fun parseFrom(input: InputStream): WaterDayProto {
            try {
                val codedInput = CodedInputStream.newInstance(input)
                val entries: MutableList<WaterEntryProto> = mutableListOf()
                var total = 0
                var done = false
                while (!done) {
                    when (val tag = codedInput.readTag()) {
                        0 -> done = true
                        10 -> {
                            // read length-delimited bytes for embedded WaterEntryProto
                            val bytes = codedInput.readByteArray()
                            val entry = WaterEntryProto.parseFrom(ByteArrayInputStream(bytes))
                            entries.add(entry)
                        }
                        16 -> total = codedInput.readInt32()
                        else -> if (!codedInput.skipField(tag)) done = true
                    }
                }
                return WaterDayProto(entriesList = entries.toList(), totalMl = total)
            } catch (ioException: IOException) {
                throw InvalidProtocolBufferException(ioException)
            }
        }

        fun parseFrom(bytes: ByteArray): WaterDayProto = parseFrom(ByteArrayInputStream(bytes))
    }
}

