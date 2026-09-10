package com.fxsound.mobile

import com.fxsound.mobile.audio.NativeDspBridge
import java.util.Collections

/**
 * Test double for [NativeDspBridge] that records every invocation in a
 * thread-safe list and returns 0L for engine / stream creation.
 *
 * This is a *stand-in*; it does not actually drive JNI. It exists so
 * audio-layer unit tests can validate the call ordering and argument
 * shape that the [com.fxsound.mobile.audio.AudioEngineController] sends
 * to native code.
 *
 * Note: this is an object (like the real one) so it is drop-in
 * replaceable only for *call-recording* tests. It cannot be passed to
 * the real [NativeDspBridge] static API; tests that want to swap
 * implementations should use a wrapper or an interface. The point of
 * this class is to provide a parallel recorder the controller could
 * be refactored against, plus a reference for what the call sequence
 * should look like in the production controller.
 */
object FakeNativeDspBridge {

    /** All recorded method invocations. */
    val calls: MutableList<Call> = Collections.synchronizedList(mutableListOf())

    /** Reset between tests. */
    fun reset() {
        calls.clear()
    }

    /** Convenience for assertions. */
    fun callNames(): List<String> = synchronized(calls) { calls.map { it.name } }.toList()

    /**
     * Each entry is one recorded invocation. The [args] list is a
     * best-effort capture; arrays are passed by reference but are
     * typically not mutated by tests.
     */
    data class Call(val name: String, val args: List<Any?>)

    // --- Recording helpers (not invoked by production code) ---

    fun record(name: String, vararg args: Any?) {
        calls.add(Call(name, args.toList()))
    }

    // --- Test-only entries that mirror the real bridge shape ---

    fun createEngine(sampleRate: Float, maxBlockSize: Int): Long {
        record("createEngine", sampleRate, maxBlockSize)
        return 0L
    }

    fun destroyEngine(enginePtr: Long) {
        record("destroyEngine", enginePtr)
    }

    fun setPowerEnabled(enginePtr: Long, enabled: Boolean) {
        record("setPowerEnabled", enginePtr, enabled)
    }

    fun setBass(enginePtr: Long, amount: Float) {
        record("setBass", enginePtr, amount)
    }

    fun setClarity(enginePtr: Long, amount: Float) {
        record("setClarity", enginePtr, amount)
    }

    fun setAmbience(enginePtr: Long, amount: Float) {
        record("setAmbience", enginePtr, amount)
    }

    fun setSurround(enginePtr: Long, amount: Float) {
        record("setSurround", enginePtr, amount)
    }

    fun setDynamicBoost(enginePtr: Long, amount: Float) {
        record("setDynamicBoost", enginePtr, amount)
    }

    fun setVolumeLeveling(enginePtr: Long, amount: Float) {
        record("setVolumeLeveling", enginePtr, amount)
    }

    fun setMasterGainDb(enginePtr: Long, gainDb: Float) {
        record("setMasterGainDb", enginePtr, gainDb)
    }

    fun setBalance(enginePtr: Long, balance: Float) {
        record("setBalance", enginePtr, balance)
    }

    fun setEqEnabled(enginePtr: Long, enabled: Boolean) {
        record("setEqEnabled", enginePtr, enabled)
    }

    fun setEqBandCount(enginePtr: Long, count: Int) {
        record("setEqBandCount", enginePtr, count)
    }

    fun setEqBandGain(enginePtr: Long, bandIndex: Int, gainDb: Float) {
        record("setEqBandGain", enginePtr, bandIndex, gainDb)
    }

    fun setEqBandFrequency(enginePtr: Long, bandIndex: Int, freq: Float) {
        record("setEqBandFrequency", enginePtr, bandIndex, freq)
    }

    fun setEqBandQ(enginePtr: Long, bandIndex: Int, q: Float) {
        record("setEqBandQ", enginePtr, bandIndex, q)
    }

    fun resetAllEqBands(enginePtr: Long) {
        record("resetAllEqBands", enginePtr)
    }

    fun getMagnitudeResponse(enginePtr: Long, inFreqs: FloatArray, outDbs: FloatArray, count: Int) {
        record("getMagnitudeResponse", enginePtr, inFreqs.size, outDbs.size, count)
    }

    fun getMeterData(enginePtr: Long, outMeterValues: FloatArray) {
        record("getMeterData", enginePtr, outMeterValues.size)
    }

    fun createAudioStream(enginePtr: Long): Long {
        record("createAudioStream", enginePtr)
        return 0L
    }

    fun setAudioStreamLowPowerIo(streamPtr: Long, enabled: Boolean) {
        record("setAudioStreamLowPowerIo", streamPtr, enabled)
    }

    fun destroyAudioStream(streamPtr: Long) {
        record("destroyAudioStream", streamPtr)
    }

    fun startAudioStream(streamPtr: Long): Boolean {
        record("startAudioStream", streamPtr)
        return true
    }

    fun stopAudioStream(streamPtr: Long): Boolean {
        record("stopAudioStream", streamPtr)
        return true
    }

    fun pauseAudioStream(streamPtr: Long): Boolean {
        record("pauseAudioStream", streamPtr)
        return true
    }

    fun setAudioStreamSource(streamPtr: Long, sourceType: Int, frequency: Float) {
        record("setAudioStreamSource", streamPtr, sourceType, frequency)
    }

    fun getAudioStreamTelemetry(streamPtr: Long, outTelemetry: FloatArray) {
        record("getAudioStreamTelemetry", streamPtr, outTelemetry.size)
    }

    fun writePcmFrames(streamPtr: Long, data: FloatArray, offsetFrames: Int, numFrames: Int): Int {
        record("writePcmFrames", streamPtr, data.size, offsetFrames, numFrames)
        return numFrames
    }

    fun pcmAvailableWrite(streamPtr: Long): Int {
        record("pcmAvailableWrite", streamPtr)
        return 4096
    }

    fun resetPcmRing(streamPtr: Long) {
        record("resetPcmRing", streamPtr)
    }

    fun getDspBackend(): String {
        record("getDspBackend")
        return "FakeDsp"
    }
}
