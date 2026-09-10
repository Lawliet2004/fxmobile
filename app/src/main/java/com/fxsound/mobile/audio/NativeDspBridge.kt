package com.fxsound.mobile.audio

import android.util.Log

object NativeDspBridge {

    private val librariesLoaded: Boolean by lazy {
        runCatching {
            System.loadLibrary("oboe")
            System.loadLibrary("fxmobile_dsp")
            true
        }.getOrElse { e ->
            Log.w("FxEngine", "Native libraries not loaded: ${e.message}")
            false
        }
    }

    val isAvailable: Boolean
        get() = librariesLoaded

    init {
        // Load on first access to this object, before any external call.
        // Failures are caught above, so pure-JVM unit tests still work.
        librariesLoaded
    }

    external fun createEngine(sampleRate: Float, maxBlockSize: Int): Long
    external fun destroyEngine(enginePtr: Long)

    external fun setPowerEnabled(enginePtr: Long, enabled: Boolean)
    external fun setBass(enginePtr: Long, amount: Float)
    external fun setClarity(enginePtr: Long, amount: Float)
    external fun setAmbience(enginePtr: Long, amount: Float)
    external fun setSurround(enginePtr: Long, amount: Float)
    external fun setDynamicBoost(enginePtr: Long, amount: Float)

    external fun setVolumeLeveling(enginePtr: Long, amount: Float)
    external fun setMasterGainDb(enginePtr: Long, gainDb: Float)
    external fun setBalance(enginePtr: Long, balance: Float)

    external fun setEqEnabled(enginePtr: Long, enabled: Boolean)
    external fun setEqBandCount(enginePtr: Long, count: Int)
    external fun setEqBandGain(enginePtr: Long, bandIndex: Int, gainDb: Float)
    external fun setEqBandFrequency(enginePtr: Long, bandIndex: Int, freq: Float)
    external fun setEqBandQ(enginePtr: Long, bandIndex: Int, q: Float)
    external fun resetAllEqBands(enginePtr: Long)

    external fun getMagnitudeResponse(enginePtr: Long, inFreqs: FloatArray, outDbs: FloatArray, count: Int)
    external fun getMeterData(enginePtr: Long, outMeterValues: FloatArray)

    external fun createAudioStream(enginePtr: Long): Long
    external fun setAudioStreamLowPowerIo(streamPtr: Long, enabled: Boolean)
    external fun destroyAudioStream(streamPtr: Long)
    external fun startAudioStream(streamPtr: Long): Boolean
    external fun stopAudioStream(streamPtr: Long): Boolean
    external fun pauseAudioStream(streamPtr: Long): Boolean
    external fun setAudioStreamSource(streamPtr: Long, sourceType: Int, frequency: Float)
    external fun getAudioStreamTelemetry(streamPtr: Long, outTelemetry: FloatArray)
    external fun writePcmFrames(streamPtr: Long, data: FloatArray, offsetFrames: Int, numFrames: Int): Int
    external fun pcmAvailableWrite(streamPtr: Long): Int
    external fun resetPcmRing(streamPtr: Long)
    external fun getDspBackend(): String
}
