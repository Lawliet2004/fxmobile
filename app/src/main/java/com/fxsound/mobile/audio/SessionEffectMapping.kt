package com.fxsound.mobile.audio

import com.fxsound.mobile.core.model.DspParameters
import kotlin.math.roundToInt

object SessionEffectMapping {
    fun bassInputGainDb(bass: Float): Float = (bass.coerceIn(0f, 10f) / 10f * 6f)

    fun dynamicGainDb(dynamicBoost: Float): Float = (dynamicBoost.coerceIn(0f, 10f) / 10f * 4f)

    fun combinedInputGainDb(params: DspParameters): Float {
        val bass = bassInputGainDb(params.bass)
        val master = params.masterGainDb.coerceIn(0f, 12f)
        val dyn = dynamicGainDb(params.dynamicBoost)
        return (bass + master + dyn).coerceIn(-12f, 12f)
    }

    fun loudnessEnhancerMb(params: DspParameters): Int {
        val fromMaster = params.masterGainDb.coerceIn(0f, 20f) * 100f
        val fromDynamic = params.dynamicBoost.coerceIn(0f, 10f) / 10f * 1000f
        return (fromMaster + fromDynamic).roundToInt().coerceIn(0, 3000)
    }

    fun shouldEnable(params: DspParameters): Boolean {
        return params.powerEnabled && (
            params.eqEnabled ||
                params.bass > 0.01f ||
                params.dynamicBoost > 0.01f ||
                params.masterGainDb > 0.01f
            )
    }

    fun eqGainAt(params: DspParameters, index: Int): Float {
        return params.eqGains.getOrElse(index) { 0f }.coerceIn(-12f, 12f)
    }

    fun eqFreqAt(params: DspParameters, index: Int): Float {
        return params.eqFrequencies.getOrElse(index) { 1000f }
    }

    fun bassBoostStrength(bass: Float): Short {
        return (bass.coerceIn(0f, 10f) / 10f * 1000f).toInt().coerceIn(0, 1000).toShort()
    }

    fun equalizerMillibel(gainDb: Float, minMb: Int, maxMb: Int): Short {
        return (gainDb * 100f).roundToInt().coerceIn(minMb, maxMb).toShort()
    }
}
