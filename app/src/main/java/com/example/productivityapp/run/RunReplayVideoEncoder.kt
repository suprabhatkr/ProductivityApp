package com.example.productivityapp.run

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.max

interface RunReplayVideoEncoder {
    suspend fun encodeVideo(
        outputFile: File,
        config: RunReplayExportConfig,
        frameCount: Int,
        frameProvider: suspend (Int) -> Bitmap,
    ): File
}

class MediaCodecRunReplayVideoEncoder : RunReplayVideoEncoder {
    override suspend fun encodeVideo(
        outputFile: File,
        config: RunReplayExportConfig,
        frameCount: Int,
        frameProvider: suspend (Int) -> Bitmap,
    ): File = withContext(Dispatchers.Default) {
        if (frameCount <= 0) {
            throw RunReplayExportException.VideoEncodingFailed("Replay export requires at least one frame.")
        }

        outputFile.parentFile?.mkdirs()

        val codec = try {
            MediaCodec.createEncoderByType(MIME_TYPE)
        } catch (error: IOException) {
            throw RunReplayExportException.VideoEncodingFailed("Video encoder could not be created.", error)
        }

        var muxer: MediaMuxer? = null
        var startedMuxer = false
        var trackIndex = -1

        try {
            val capabilities = codec.codecInfo.getCapabilitiesForType(MIME_TYPE)
            val colorFormat = selectColorFormat(capabilities.colorFormats)
            val format = MediaFormat.createVideoFormat(MIME_TYPE, config.widthPx, config.heightPx).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
                setInteger(MediaFormat.KEY_BIT_RATE, config.bitrateMbps * 1_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, config.fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = BufferInfo()

            repeat(frameCount) { frameIndex ->
                val bitmap = frameProvider(frameIndex)
                val bufferIndex = dequeueInputBuffer(codec)
                val inputBuffer = codec.getInputBuffer(bufferIndex)
                    ?: throw RunReplayExportException.VideoEncodingFailed("Video encoder input buffer was unavailable.")
                inputBuffer.clear()
                val encodedFrame = bitmap.toYuv420(config.widthPx, config.heightPx, colorFormat)
                inputBuffer.put(encodedFrame)
                val presentationTimeUs = frameIndex * 1_000_000L / config.fps
                codec.queueInputBuffer(
                    bufferIndex,
                    0,
                    encodedFrame.size,
                    presentationTimeUs,
                    0,
                )
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
                drainEncoder(codec, muxer, bufferInfo, trackIndex) { index ->
                    trackIndex = index
                    startedMuxer = true
                }
            }

            val eosIndex = dequeueInputBuffer(codec)
            codec.queueInputBuffer(eosIndex, 0, 0, frameCount * 1_000_000L / config.fps, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            drainEncoder(codec, muxer, bufferInfo, trackIndex, endOfStream = true) { index ->
                trackIndex = index
                startedMuxer = true
            }

            if (!startedMuxer || trackIndex < 0) {
                throw RunReplayExportException.VideoEncodingFailed("Video encoder did not produce a playable replay track.")
            }

            outputFile
        } catch (error: IllegalStateException) {
            throw RunReplayExportException.VideoEncodingFailed("Video encoder failed while building the replay MP4.", error)
        } finally {
            try {
                codec.stop()
            } catch (_: IllegalStateException) {
            }
            codec.release()

            muxer?.let { activeMuxer ->
                try {
                    if (startedMuxer) activeMuxer.stop()
                } catch (_: IllegalStateException) {
                }
                activeMuxer.release()
            }
        }
    }

    private fun dequeueInputBuffer(codec: MediaCodec): Int {
        repeat(20) {
            val index = codec.dequeueInputBuffer(BUFFER_TIMEOUT_US)
            if (index >= 0) return index
        }
        throw RunReplayExportException.VideoEncodingFailed("Video encoder input buffers did not become available in time.")
    }

    private fun drainEncoder(
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: BufferInfo,
        currentTrackIndex: Int,
        endOfStream: Boolean = false,
        onTrackReady: (Int) -> Unit,
    ) {
        var trackIndex = currentTrackIndex
        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, BUFFER_TIMEOUT_US)
            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER && !endOfStream -> return
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER && endOfStream -> continue
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    onTrackReady(trackIndex)
                }

                outputIndex >= 0 -> {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                        ?: throw RunReplayExportException.VideoEncodingFailed("Video encoder output buffer was unavailable.")
                    if (bufferInfo.size > 0) {
                        if (trackIndex < 0) {
                            throw RunReplayExportException.VideoEncodingFailed("Video muxer track was not ready before encoded samples arrived.")
                        }
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return
                }
            }
        }
    }

    private fun selectColorFormat(colorFormats: IntArray): Int {
        return when {
            colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) ->
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) ->
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
            else -> throw RunReplayExportException.VideoEncodingFailed("No supported YUV420 color format was available for replay encoding.")
        }
    }

    private fun Bitmap.toYuv420(width: Int, height: Int, colorFormat: Int): ByteArray {
        val scaledBitmap = if (this.width == width && this.height == height) this else Bitmap.createScaledBitmap(this, width, height, true)
        val argb = IntArray(width * height)
        scaledBitmap.getPixels(argb, 0, width, 0, 0, width, height)
        val ySize = width * height
        val uvSize = ySize / 4
        val yuv = ByteArray(ySize + uvSize * 2)

        var yIndex = 0
        var uIndex = ySize
        var vIndex = ySize + uvSize
        var uvIndex = ySize

        for (row in 0 until height) {
            for (col in 0 until width) {
                val color = argb[row * width + col]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                yuv[yIndex++] = y.coerceToByte()

                if (row % 2 == 0 && col % 2 == 0) {
                    when (colorFormat) {
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar -> {
                            yuv[uIndex++] = u.coerceToByte()
                            yuv[vIndex++] = v.coerceToByte()
                        }

                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar -> {
                            yuv[uvIndex++] = u.coerceToByte()
                            yuv[uvIndex++] = v.coerceToByte()
                        }
                    }
                }
            }
        }

        if (scaledBitmap !== this) {
            scaledBitmap.recycle()
        }
        return yuv
    }

    private fun Int.coerceToByte(): Byte = max(0, minOf(255, this)).toByte()

    private companion object {
        const val MIME_TYPE = "video/avc"
        const val BUFFER_TIMEOUT_US = 10_000L
    }
}
