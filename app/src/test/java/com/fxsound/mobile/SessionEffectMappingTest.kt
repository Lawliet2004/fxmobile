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
}
