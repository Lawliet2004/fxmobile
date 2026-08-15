package com.fxsound.mobile.core.model


data class AudioTelemetry(
    val isPlaying: Boolean = false,
    val audioBackend: String = "AAudio",
    val sampleRate: Int = 48000,
    val channelCount: Int = 2,
    val bufferSizeFrames: Int = 256,
    val burstFrames: Int = 128,
    val estimatedLatencyMs: Float = 5.33f,
    val dspCpuLoadPercent: Float = 0.0f,
    val peakLeft: Float = 0.0f,
    val peakRight: Float = 0.0f,
    val rmsLeft: Float = 0.0f,
    val rmsRight: Float = 0.0f,
    val isClipping: Boolean = false,
    val activeOutputDevice: String = "Built-in Speaker",
    val isBluetooth: Boolean = false,
    val mediaTitle: String? = null,
    val mediaPositionMs: Long = 0L,
    val mediaDurationMs: Long = 0L,
    val sourceType: Int = 3,
    val lastError: String? = null,
    val processingMode: ProcessingMode = ProcessingMode.INTERNAL_ENGINE,
    val sessionAttached: Boolean = false,
    val sessionLabel: String = ""
)
