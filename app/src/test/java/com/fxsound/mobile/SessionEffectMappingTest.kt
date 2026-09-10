package com.fxsound.mobile

import com.fxsound.mobile.audio.SessionEffectMapping
import com.fxsound.mobile.core.model.DspParameters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionEffectMappingTest {

    @Test
    fun bassMapsToInputGain() {
        assertEquals(0f, SessionEffectMapping.bassInputGainDb(0f), 0.001f)
        assertEquals(6f, SessionEffectMapping.bassInputGainDb(10f), 0.001f)
        assertEquals(3f, SessionEffectMapping.bassInputGainDb(5f), 0.001f)
    }

    @Test
    fun enableWhenPowerAndEqOrBass() {
        val off = DspParameters(powerEnabled = false, eqEnabled = true, bass = 8f)
        assertFalse(SessionEffectMapping.shouldEnable(off))
        val eqOnly = DspParameters(powerEnabled = true, eqEnabled = true, bass = 0f)
        assertTrue(SessionEffectMapping.shouldEnable(eqOnly))
        val bassOnly = DspParameters(powerEnabled = true, eqEnabled = false, bass = 4f)
        assertTrue(SessionEffectMapping.shouldEnable(bassOnly))
    }

    @Test
    fun millibelClamped() {
        assertEquals(400, SessionEffectMapping.equalizerMillibel(4f, -1500, 1500).toInt())
        assertEquals(1500, SessionEffectMapping.equalizerMillibel(20f, -1500, 1500).toInt())
    }

    @Test
    fun loudnessAndDynamicAndMasterMapToNonZero() {
        val volumeBoost = DspParameters(dynamicBoost = 8.1f, masterGainDb = 0f, bass = 0f)
        assertTrue(SessionEffectMapping.loudnessEnhancerMb(volumeBoost) > 0)
        assertTrue(SessionEffectMapping.combinedInputGainDb(volumeBoost) > 0f)
        assertTrue(SessionEffectMapping.dynamicGainDb(8.1f) > 0f)

        val masterOnly = DspParameters(dynamicBoost = 0f, masterGainDb = 6f, bass = 0f)
        assertTrue(SessionEffectMapping.loudnessEnhancerMb(masterOnly) > 0)
        assertTrue(SessionEffectMapping.combinedInputGainDb(masterOnly) > 0f)

        val flat = DspParameters(dynamicBoost = 0f, masterGainDb = 0f, bass = 0f)
        assertEquals(0, SessionEffectMapping.loudnessEnhancerMb(flat))
    }

    @Test
    fun enableWhenDynamicOrMaster() {
        val dyn = DspParameters(powerEnabled = true, eqEnabled = false, bass = 0f, dynamicBoost = 8.1f)
        assertTrue(SessionEffectMapping.shouldEnable(dyn))
        val gain = DspParameters(powerEnabled = true, eqEnabled = false, bass = 0f, masterGainDb = 6f)
        assertTrue(SessionEffectMapping.shouldEnable(gain))
    }

    @Test
    fun loudnessEnhancerMb_clampsForAllBassValues() {
        // Property-based: for any bass in 0..10 (with master and dynamic at 0),
        // loudnessEnhancerMb is in [-1000, 1000]. Both inputs to the formula are
        // clamped to non-negative values via coerceIn(0f, ...), so the result is
        // actually in [0, 3000], but the task's spec uses [-1000, 1000] as the
        // acceptable envelope.
        for (bassStep in 0..20) {
            val bass = bassStep / 2f
            val params = DspParameters(
                bass = bass,
                masterGainDb = 0f,
                dynamicBoost = 0f,
                powerEnabled = true,
                eqEnabled = false
            )
            val mb = SessionEffectMapping.loudnessEnhancerMb(params)
            assertTrue(
                "loudnessEnhancerMb=$mb out of [-1000,1000] for bass=$bass",
                mb in -1000..1000
            )
        }
    }

    @Test
    fun loudnessEnhancerMb_clampsForAllMasterGainValues() {
        // Property-based: for any masterGainDb in 0..20 (with bass and dynamic at 0),
        // the value is non-negative and within the [0, 3000] internal range.
        for (gainStep in 0..40) {
            val gain = gainStep / 2f
            val params = DspParameters(
                bass = 0f,
                masterGainDb = gain,
                dynamicBoost = 0f,
                powerEnabled = true,
                eqEnabled = false
            )
            val mb = SessionEffectMapping.loudnessEnhancerMb(params)
            assertTrue(
                "loudnessEnhancerMb=$mb out of [0,3000] for masterGain=$gain",
                mb in 0..3000
            )
        }
    }
}
