package com.fxsound.mobile

import com.fxsound.mobile.core.model.DspParameters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DspParameterValidationTest {

    @Test
    fun testDefaultParameterRanges() {
        val params = DspParameters()

        assertTrue(params.bass in 0.0f..10.0f)
        assertTrue(params.clarity in 0.0f..10.0f)
        assertTrue(params.ambience in 0.0f..10.0f)
        assertTrue(params.surround in 0.0f..10.0f)
        assertTrue(params.dynamicBoost in 0.0f..10.0f)
        assertTrue(params.volumeLeveling in 0.0f..4.0f)
        assertTrue(params.masterGainDb in -20.0f..20.0f)
        assertTrue(params.balance in -20.0f..20.0f)
        assertEquals(10, params.eqBandCount)
        assertEquals(10, params.eqGains.size)
        assertEquals(DspParameters.FLAT, params)
    }

    @Test
    fun testFlatParameters() {
        val flat = DspParameters.FLAT

        assertEquals(0.0f, flat.bass, 0.001f)
        assertEquals(0.0f, flat.clarity, 0.001f)
        assertEquals(0.0f, flat.ambience, 0.001f)
        assertEquals(0.0f, flat.surround, 0.001f)
        assertEquals(0.0f, flat.dynamicBoost, 0.001f)
        assertEquals(0.0f, flat.volumeLeveling, 0.001f)
        assertEquals(0.0f, flat.masterGainDb, 0.001f)
        assertEquals(0.0f, flat.balance, 0.001f)
        flat.eqGains.forEach {
            assertEquals(0.0f, it, 0.001f)
        }
    }
}
