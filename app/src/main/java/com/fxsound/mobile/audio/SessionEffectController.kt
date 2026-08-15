package com.fxsound.mobile.audio

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import com.fxsound.mobile.core.model.DspParameters

class SessionEffectController {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var dynamics: DynamicsProcessing? = null
    private var loudness: LoudnessEnhancer? = null
    private var attachedSession: Int? = null

    val isAttached: Boolean get() = attachedSession != null
    val sessionId: Int? get() = attachedSession

    fun attach(sessionId: Int): Boolean {
        if (attachedSession == sessionId && isAttached) return true
        detach()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2,
                    true,
                    10,
                    false,
                    0,
                    false,
                    0,
                    true
                ).build()
                dynamics = DynamicsProcessing(0, sessionId, config).apply {
                    enabled = true
                }
            } else {
                equalizer = Equalizer(0, sessionId).apply { enabled = true }
                bassBoost = BassBoost(0, sessionId).apply { enabled = true }
            }
            try {
                loudness = LoudnessEnhancer(sessionId).apply { enabled = true }
            } catch (_: Exception) {
                loudness = null
            }
            attachedSession = sessionId
            true
        } catch (_: Exception) {
            detach()
            false
        }
    }

    fun apply(params: DspParameters) {
        try {
            applyLoudness(params)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val dyn = dynamics ?: return
                dyn.enabled = SessionEffectMapping.shouldEnable(params)
                try {
                    dyn.setInputGainAllChannelsTo(SessionEffectMapping.combinedInputGainDb(params))
                } catch (_: Exception) {
                }
                val channelCount = dyn.channelCount
                val bandCount = minOf(params.eqBandCount, 10)
                for (ch in 0 until channelCount) {
                    val preEq = try { dyn.getPreEqByChannelIndex(ch) } catch (_: Exception) { null }
                    val n = preEq?.bandCount ?: bandCount
                    for (i in 0 until n) {
                        val freq = SessionEffectMapping.eqFreqAt(params, i)
                        val gain = if (params.eqEnabled) SessionEffectMapping.eqGainAt(params, i) else 0f
                        try {
                            val band = DynamicsProcessing.EqBand(true, freq, gain)
                            dyn.setPreEqBandByChannelIndex(ch, i, band)
                        } catch (_: Exception) {
                        }
                    }
                }
            } else {
                val eq = equalizer ?: return
                eq.enabled = params.powerEnabled && params.eqEnabled
                val n = eq.numberOfBands.toInt()
                val minMb = eq.bandLevelRange[0].toInt()
                val maxMb = eq.bandLevelRange[1].toInt()
                for (i in 0 until n) {
                    val center = eq.getCenterFreq(i.toShort()) / 1000f
                    val nearest = params.eqFrequencies.withIndex().minByOrNull {
                        kotlin.math.abs(it.value - center)
                    }
                    val gain = nearest?.let { params.eqGains.getOrNull(it.index) } ?: 0f
                    eq.setBandLevel(i.toShort(), SessionEffectMapping.equalizerMillibel(gain, minMb, maxMb))
                }
                bassBoost?.let {
                    it.enabled = params.powerEnabled
                    it.setStrength(SessionEffectMapping.bassBoostStrength(params.bass))
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun applyLoudness(params: DspParameters) {
        val enhancer = loudness ?: return
        try {
            val mb = SessionEffectMapping.loudnessEnhancerMb(params)
            enhancer.setTargetGain(mb)
            enhancer.enabled = params.powerEnabled && mb > 0
        } catch (_: Exception) {
        }
    }

    fun detach() {
        try { dynamics?.release() } catch (_: Exception) {}
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { loudness?.release() } catch (_: Exception) {}
        dynamics = null
        equalizer = null
        bassBoost = null
        loudness = null
        attachedSession = null
    }

    companion object {
        const val ACTION_OPEN = AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION
        const val ACTION_CLOSE = AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION
    }
}
