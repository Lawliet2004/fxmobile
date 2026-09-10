package com.fxsound.mobile

import com.fxsound.mobile.core.model.EqBandTables
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

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

    @Test
    fun remapGains_emptyFromGains_returnsAllZeros() {
        val fromFreq = listOf(60f, 1000f, 12000f)
        val fromGain = emptyList<Float>()
        val toFreq = EqBandTables.TEN
        val remapped = EqBandTables.remapGains(fromFreq, fromGain, toFreq)
        assertEquals(toFreq.size, remapped.size)
        assertTrue("All values should be 0 when fromG is empty", remapped.all { it == 0f })
    }

    @Test
    fun remapGains_emptyFromFreqs_returnsAllZeros() {
        val fromFreq = emptyList<Float>()
        val fromGain = listOf(1f, 2f, 3f)
        val toFreq = EqBandTables.TEN
        val remapped = EqBandTables.remapGains(fromFreq, fromGain, toFreq)
        assertEquals(toFreq.size, remapped.size)
        assertTrue(remapped.all { it == 0f })
    }

    @Test
    fun remapGains_outputLengthMatchesToFreqs() {
        val rng = Random(0xF5)
        // Try 5..31 bands for fromF, with a fixed toFreqs size.
        val toFreqs = EqBandTables.TEN
        for (bandCount in 5..31) {
            val fromF = List(bandCount) { i -> 20f * Math.pow(2.0, i.toDouble()).toFloat() }
            val fromG = List(bandCount) { rng.nextFloat() * 24f - 12f }
            val remapped = EqBandTables.remapGains(fromF, fromG, toFreqs)
            assertEquals(
                "remapGains output length should match toFreqs size for $bandCount bands",
                toFreqs.size, remapped.size
            )
        }
    }

    @Test
    fun remapGains_outputLengthMatchesToFreqs_emptyGains() {
        val rng = Random(0xBEEF)
        val toFreqs = EqBandTables.TEN
        for (bandCount in 5..31) {
            val fromF = List(bandCount) { i -> 20f * Math.pow(2.0, i.toDouble()).toFloat() }
            // Empty gains -> all zeros
            val fromG = emptyList<Float>()
            val remapped = EqBandTables.remapGains(fromF, fromG, toFreqs)
            assertEquals(toFreqs.size, remapped.size)
            assertTrue(
                "All values must be 0 when fromG is empty (bandCount=$bandCount)",
                remapped.all { it == 0f }
            )
        }
    }

    @Test
    fun remapGains_outputLengthMatchesToFreqs_randomShapes() {
        val rng = Random(42)
        // Try a variety of arbitrary toFreq shapes (length 1..32).
        for (toSize in listOf(1, 5, 7, 10, 15, 20, 25, 31, 32)) {
            val toFreqs = List(toSize) { i -> 20f * Math.pow(1000.0, i.toDouble() / (toSize - 1).coerceAtLeast(1)).toFloat() }
            for (fromSize in 5..31) {
                val fromF = List(fromSize) { i -> 20f * Math.pow(1000.0, i.toDouble() / (fromSize - 1).coerceAtLeast(1)).toFloat() }
                val fromG = List(fromSize) { rng.nextFloat() * 24f - 12f }
                val remapped = EqBandTables.remapGains(fromF, fromG, toFreqs)
                assertEquals(
                    "Output length mismatch: fromSize=$fromSize, toSize=$toSize",
                    toSize, remapped.size
                )
            }
        }
    }
}

