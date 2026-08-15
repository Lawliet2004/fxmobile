package com.fxsound.mobile

import com.fxsound.mobile.core.model.EqBandTables
import org.junit.Assert.assertEquals
import org.junit.Test

class EqBandTablesTest {

    @Test
    fun remapKeepsNearestGain() {
        val fromFreq = listOf(60f, 1000f, 12000f)
        val fromGain = listOf(4f, 0f, -3f)
        val toFreq = EqBandTables.TEN
        val remapped = EqBandTables.remapGains(fromFreq, fromGain, toFreq)
        assertEquals(10, remapped.size)
        assertEquals(4f, remapped[0], 0.01f)
        assertEquals(0f, remapped[5], 0.01f)
        assertEquals(-3f, remapped[9], 0.01f)
    }
}
