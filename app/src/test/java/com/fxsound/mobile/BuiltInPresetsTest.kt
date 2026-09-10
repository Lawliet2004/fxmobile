package com.fxsound.mobile

import com.fxsound.mobile.core.model.DspParameters
import com.fxsound.mobile.core.model.EqBandTables
import com.fxsound.mobile.core.presets.BuiltInPresets
import com.fxsound.mobile.core.presets.PresetCompare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInPresetsTest {

    @Test
    fun factorySetHasThirteenOfficialNames() {
        val names = BuiltInPresets.ALL.map { it.name }
        assertEquals(13, names.size)
        assertEquals(
            listOf(
                "General",
                "Music",
                "Voice",
                "Volume Boost",
                "Gaming",
                "Classic Processing",
                "Light Processing",
                "Bass Boost",
                "Streaming Video",
                "Movies",
                "TV",
                "Transcription",
                "Default"
            ),
            names
        )
        assertEquals(BuiltInPresets.FACTORY_NAMES, names)
        assertEquals(13, BuiltInPresets.ALL.map { it.id }.toSet().size)
    }

    @Test
    fun everyFactoryPresetMatchesFacTableAndStaysInRange() {
        assertFac(
            BuiltInPresets.GENERAL,
            "builtin_general",
            clarity = 3.9f, surround = 1.6f, ambience = 0.0f, dynamic = 4.7f, bass = 4.7f,
            eq = listOf(0f, 0f, 1f, 2f, 0f, -1f, 0f, -1f, -2f, 0f)
        )
        assertFac(
            BuiltInPresets.MUSIC,
            "builtin_music",
            clarity = 3.9f, surround = 2.8f, ambience = 2.8f, dynamic = 1.6f, bass = 4.7f,
            eq = listOf(0f, 2f, 2f, 1f, 0f, 0f, 0f, -1f, 0f, 2f)
        )
        assertFac(
            BuiltInPresets.VOICE,
            "builtin_voice",
            clarity = 5.7f, surround = 0.0f, ambience = 0.0f, dynamic = 7.5f, bass = 0.0f,
            eq = listOf(0f, -4f, -2f, 2f, 4f, 5f, 3f, 3f, 5f, -11f)
        )
        assertFac(
            BuiltInPresets.VOLUME_BOOST,
            "builtin_volume_boost",
            clarity = 2.5f, surround = 1.6f, ambience = 0.0f, dynamic = 8.1f, bass = 2.8f,
            eq = listOf(0f, 3f, 2f, 2f, 0f, 0f, 1f, 1f, 2f, 2f)
        )
        assertFac(
            BuiltInPresets.GAMING,
            "builtin_gaming",
            clarity = 2.8f, surround = 0.0f, ambience = 0.0f, dynamic = 6.7f, bass = 2.8f,
            eq = listOf(0f, 0f, 2f, 2f, 2f, 0f, -1f, -1f, 2f, 2f)
        )
        assertFac(
            BuiltInPresets.CLASSIC,
            "builtin_classic",
            clarity = 4.7f, surround = 2.8f, ambience = 4.7f, dynamic = 4.7f, bass = 5.5f,
            eq = List(10) { 0f }
        )
        assertFac(
            BuiltInPresets.LIGHT,
            "builtin_light",
            clarity = 2.0f, surround = 0.0f, ambience = 2.8f, dynamic = 0.4f, bass = 1.6f,
            eq = listOf(0f, -1f, 1f, 1f, -1f, -1f, -2f, -1f, -1f, 1f)
        )
        assertFac(
            BuiltInPresets.BASS_BOOST,
            "builtin_bass_boost",
            clarity = 2.4f, surround = 2.8f, ambience = 2.8f, dynamic = 1.6f, bass = 5.9f,
            eq = listOf(0f, 3f, 3f, 2f, 1f, -1f, -1f, -1f, -1f, 0f)
        )
        assertFac(
            BuiltInPresets.STREAMING_VIDEO,
            "builtin_streaming_video",
            clarity = 2.8f, surround = 2.8f, ambience = 0.0f, dynamic = 4.3f, bass = 2.8f,
            eq = listOf(0f, 0f, 0f, 0f, 1f, 1f, 1f, -1f, 0f, 2f)
        )
        assertFac(
            BuiltInPresets.MOVIES,
            "builtin_movies",
            clarity = 4.7f, surround = 3.9f, ambience = 0.0f, dynamic = 6.7f, bass = 2.8f,
            eq = listOf(0f, 0f, 2f, 0f, 2f, 2f, 1f, -1f, 0f, 2f)
        )
        assertFac(
            BuiltInPresets.TV,
            "builtin_tv",
            clarity = 3.9f, surround = 3.9f, ambience = 1.6f, dynamic = 4.7f, bass = 3.5f,
            eq = listOf(0f, 0f, 1f, 0f, 1f, 0f, 1f, -1f, -1f, 2f)
        )
        assertFac(
            BuiltInPresets.TRANSCRIPTION,
            "builtin_transcription",
            clarity = 7.9f, surround = 0.0f, ambience = 0.0f, dynamic = 9.1f, bass = 5.9f,
            eq = listOf(0f, -12f, 7f, 2f, -1f, 7f, 0f, 10f, 3f, -12f)
        )
        assertFac(
            BuiltInPresets.DEFAULT,
            "builtin_default",
            clarity = 0.0f, surround = 0.0f, ambience = 0.0f, dynamic = 0.0f, bass = 0.0f,
            eq = List(10) { 0f }
        )
    }

    @Test
    fun volumeBoostIsDynamicNotMasterGain() {
        val preset = BuiltInPresets.VOLUME_BOOST
        assertEquals("Volume Boost", preset.name)
        assertEquals(8.1f, preset.parameters.dynamicBoost, 0.001f)
        assertEquals(0.0f, preset.parameters.masterGainDb, 0.001f)
        assertEquals(2.5f, preset.parameters.clarity, 0.001f)
        assertEquals(1.6f, preset.parameters.surround, 0.001f)
        assertEquals(0.0f, preset.parameters.ambience, 0.001f)
        assertEquals(2.8f, preset.parameters.bass, 0.001f)
    }

    @Test
    fun defaultIsFlatAndConstructorMatches() {
        BuiltInPresets.ALL.forEach { preset ->
            assertEquals(0.0f, preset.parameters.masterGainDb, 0.001f)
            assertEquals(0.0f, preset.parameters.volumeLeveling, 0.001f)
            assertEquals(0.0f, preset.parameters.balance, 0.001f)
        }
        assertEquals(BuiltInPresets.DEFAULT.parameters, BuiltInPresets.FLAT.parameters)
        assertEquals(DspParameters.FLAT, BuiltInPresets.DEFAULT.parameters)
        assertEquals(DspParameters(), BuiltInPresets.DEFAULT.parameters)
        assertNotEquals(BuiltInPresets.DEFAULT.id, BuiltInPresets.FLAT.id)
        BuiltInPresets.DEFAULT.parameters.eqGains.forEach { assertEquals(0.0f, it, 0.001f) }
    }

    @Test
    fun compareTogglesFlatVersusActive() {
        val active = BuiltInPresets.VOLUME_BOOST
        assertEquals(BuiltInPresets.FLAT.id, PresetCompare.target(true, active).id)
        assertEquals(0.0f, PresetCompare.target(true, active).parameters.dynamicBoost, 0.001f)
        assertEquals(active.id, PresetCompare.target(false, active).id)
        assertEquals(8.1f, PresetCompare.target(false, active).parameters.dynamicBoost, 0.001f)
    }

    private fun assertFac(
        preset: com.fxsound.mobile.core.model.Preset,
        id: String,
        clarity: Float,
        surround: Float,
        ambience: Float,
        dynamic: Float,
        bass: Float,
        eq: List<Float>
    ) {
        assertEquals(id, preset.id)
        assertTrue(preset.isBuiltIn)
        val p = preset.parameters
        assertEquals(clarity, p.clarity, 0.001f)
        assertEquals(surround, p.surround, 0.001f)
        assertEquals(ambience, p.ambience, 0.001f)
        assertEquals(dynamic, p.dynamicBoost, 0.001f)
        assertEquals(bass, p.bass, 0.001f)
        assertEquals(eq, p.eqGains)
        assertEquals(10, p.eqBandCount)
        assertEquals(EqBandTables.TEN, p.eqFrequencies)
        assertEquals(EqBandTables.qFactors(), p.eqQFactors)
        assertTrue(p.powerEnabled)
        assertTrue(p.eqEnabled)
        assertTrue(p.bass in 0f..10f)
        assertTrue(p.clarity in 0f..10f)
        assertTrue(p.ambience in 0f..10f)
        assertTrue(p.surround in 0f..10f)
        assertTrue(p.dynamicBoost in 0f..10f)
        p.eqGains.forEach { assertTrue(it in -12f..12f) }
    }
}