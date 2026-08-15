package com.fxsound.mobile.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.OpenableColumns
import com.fxsound.mobile.core.runtime.ProgressThrottle
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class AudioFileDecoder(
    private val context: Context,
    private val writeFrames: (FloatArray, Int, Int) -> Int,
    private val availableWrite: () -> Int,
    private val onProgress: (positionMs: Long, durationMs: Long) -> Unit,
    private val onEnded: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val running = AtomicBoolean(false)
    private val playing = AtomicBoolean(false)
    private val seekRequested = AtomicBoolean(false)
    @Volatile private var seekToUs: Long = 0L
    @Volatile private var outputSampleRate: Int = 48000
    @Volatile private var durationUs: Long = 0L
    @Volatile private var positionUs: Long = 0L
    @Volatile var title: String = "Audio"
        private set

    private var worker: Thread? = null
    private val progressThrottle = ProgressThrottle(intervalMs = 250L) { SystemClock.elapsedRealtime() }

    val durationMs: Long get() = durationUs / 1000L
    val positionMs: Long get() = positionUs / 1000L

    fun openAndPlay(uri: Uri, outputSr: Int) {
        stop()
        outputSampleRate = if (outputSr > 0) outputSr else 48000
        title = queryDisplayName(uri)
        running.set(true)
        playing.set(true)
        worker = Thread({ decodeLoop(uri) }, "fx-decoder").apply {
            isDaemon = true
            start()
        }
    }

    fun pause() {
        playing.set(false)
    }

    fun resume() {
        if (running.get()) playing.set(true)
    }

    fun seekMs(ms: Long) {
        seekToUs = (ms.coerceAtLeast(0L)) * 1000L
        seekRequested.set(true)
    }

    fun stop() {
        running.set(false)
        playing.set(false)
        worker?.join(500)
        worker = null
    }

    private fun queryDisplayName(uri: Uri): String {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else uri.lastPathSegment
            } ?: uri.lastPathSegment ?: "Audio"
        } catch (_: Exception) {
            uri.lastPathSegment ?: "Audio"
        }
    }

    private fun decodeLoop(uri: Uri) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                extractor.setDataSource(pfd.fileDescriptor)
            } ?: run {
                extractor.setDataSource(context, uri, null)
            }

            val trackIndex = selectAudioTrack(extractor)
            if (trackIndex < 0) {
                onError("No compatible audio track is available.")
                return
            }
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime.isNullOrEmpty()) {
                onError("Audio processing could not be started.")
                return
            }

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var inputEos = false
            var outputEos = false
            var inRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else outputSampleRate
            var channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2
            var pcmEncoding = if (Build.VERSION.SDK_INT >= 24 && format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                format.getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else {
                android.media.AudioFormat.ENCODING_PCM_16BIT
            }

            var resamplePos = 0.0
            var prevL = 0f
            var prevR = 0f
            val stereoScratch = FloatArray(8192 * 2)
            val resampled = FloatArray(16384 * 2)

            while (running.get() && !outputEos) {
                if (!playing.get()) {
                    Thread.sleep(20)
                    continue
                }
                if (seekRequested.getAndSet(false)) {
                    extractor.seekTo(seekToUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    codec.flush()
                    inputEos = false
                    outputEos = false
                    resamplePos = 0.0
                    positionUs = seekToUs
                }

                if (availableWrite() < 512) {
                    Thread.sleep(4)
                    continue
                }

                if (!inputEos) {
                    val inIndex = codec.dequeueInputBuffer(4_000)
                    if (inIndex >= 0) {
                        val inBuf = codec.getInputBuffer(inIndex)
                        if (inBuf != null) {
                            val sampleSize = extractor.readSampleData(inBuf, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEos = true
                            } else {
                                val pts = extractor.sampleTime
                                codec.queueInputBuffer(inIndex, 0, sampleSize, pts, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, 4_000)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFormat = codec.outputFormat
                        if (outFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            inRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (outFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                        if (Build.VERSION.SDK_INT >= 24 && outFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            pcmEncoding = outFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }
                    }
                    outIndex >= 0 -> {
                        val outBuf = codec.getOutputBuffer(outIndex)
                        if (outBuf != null && info.size > 0) {
                            outBuf.position(info.offset)
                            outBuf.limit(info.offset + info.size)
                            val frames = decodeToStereo(outBuf, info.size, channels, pcmEncoding, stereoScratch)
                            val outFrames = resampleStereo(
                                stereoScratch, frames, inRate, outputSampleRate,
                                resampled, resamplePos, prevL, prevR
                            )
                            resamplePos = outFrames.third
                            prevL = outFrames.fourth
                            prevR = outFrames.fifth
                            writeAll(resampled, outFrames.first)
                            if (info.presentationTimeUs > 0) {
                                positionUs = info.presentationTimeUs
                                if (progressThrottle.allow()) {
                                    onProgress(positionMs, durationMs)
                                }
                            }
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputEos = true
                        }
                    }
                }
            }

            if (outputEos && running.get()) {
                onEnded()
            }
        } catch (e: SecurityException) {
            onError("This file could not be opened. Choose the file again.")
        } catch (e: Exception) {
            onError("Audio processing could not be started.")
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
            running.set(false)
            playing.set(false)
        }
    }

    private fun writeAll(data: FloatArray, frames: Int) {
        var written = 0
        while (written < frames && running.get()) {
            val n = writeFrames(data, written, frames - written)
            if (n <= 0) {
                Thread.sleep(4)
            } else {
                written += n
            }
        }
    }

    private fun decodeToStereo(
        buffer: ByteBuffer,
        size: Int,
        channels: Int,
        encoding: Int,
        out: FloatArray
    ): Int {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val ch = channels.coerceAtLeast(1)
        return if (encoding == android.media.AudioFormat.ENCODING_PCM_FLOAT) {
            val samples = size / 4
            val frames = samples / ch
            val maxFrames = min(frames, out.size / 2)
            for (i in 0 until maxFrames) {
                val l = buffer.float
                val r = if (ch > 1) buffer.float else l
                for (c in 2 until ch) buffer.float
                out[i * 2] = l
                out[i * 2 + 1] = r
            }
            maxFrames
        } else {
            val samples = size / 2
            val frames = samples / ch
            val maxFrames = min(frames, out.size / 2)
            for (i in 0 until maxFrames) {
                val l = buffer.short / 32768.0f
                val r = if (ch > 1) buffer.short / 32768.0f else l
                for (c in 2 until ch) buffer.short
                out[i * 2] = l
                out[i * 2 + 1] = r
            }
            maxFrames
        }
    }

    private fun resampleStereo(
        input: FloatArray,
        inFrames: Int,
        inRate: Int,
        outRate: Int,
        output: FloatArray,
        startPos: Double,
        prevL: Float,
        prevR: Float
    ): ResampleResult {
        if (inFrames <= 0) return ResampleResult(0, 0.0, prevL, prevR)
        if (inRate <= 0 || outRate <= 0 || inRate == outRate) {
            val copy = min(inFrames, output.size / 2)
            System.arraycopy(input, 0, output, 0, copy * 2)
            return ResampleResult(copy, 0.0, input[(copy - 1) * 2], input[(copy - 1) * 2 + 1])
        }
        val ratio = inRate.toDouble() / outRate.toDouble()
        var pos = startPos
        var outFrames = 0
        var lastL = prevL
        var lastR = prevR
        while (pos < inFrames - 1 && outFrames < output.size / 2) {
            val i0 = pos.toInt()
            val frac = (pos - i0).toFloat()
            val l0 = if (i0 >= 0) input[i0 * 2] else lastL
            val r0 = if (i0 >= 0) input[i0 * 2 + 1] else lastR
            val l1 = input[(i0 + 1) * 2]
            val r1 = input[(i0 + 1) * 2 + 1]
            output[outFrames * 2] = l0 + (l1 - l0) * frac
            output[outFrames * 2 + 1] = r0 + (r1 - r0) * frac
            outFrames++
            pos += ratio
        }
        val newPos = pos - inFrames
        lastL = input[(inFrames - 1) * 2]
        lastR = input[(inFrames - 1) * 2 + 1]
        return ResampleResult(outFrames, newPos.coerceAtLeast(0.0), lastL, lastR)
    }

    private data class ResampleResult(
        val frames: Int,
        val pos: Double,
        val lastL: Float,
        val lastR: Float
    ) {
        val first get() = frames
        val third get() = pos
        val fourth get() = lastL
        val fifth get() = lastR
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return -1
    }
}
