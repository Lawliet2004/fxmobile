package com.fxsound.mobile

import com.fxsound.mobile.core.presets.BuiltInPresets
import com.fxsound.mobile.core.presets.PresetCompare
import org.junit.Assert.assertEquals
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
    fun defaultIsFlatAndMasterGainZero() {
        BuiltInPresets.ALL.forEach { preset ->
            assertEquals(0.0f, preset.parameters.masterGainDb, 0.001f)
        }
        assertEquals(0.0f, BuiltInPresets.DEFAULT.parameters.dynamicBoost, 0.001f)
        assertEquals(0.0f, BuiltInPresets.FLAT.parameters.dynamicBoost, 0.001f)
        assertEquals(0.0f, BuiltInPresets.DEFAULT.parameters.bass, 0.001f)
        BuiltInPresets.DEFAULT.parameters.eqGains.forEach { assertEquals(0.0f, it, 0.001f) }
    }

    @Test
    fun musicAndMoviesMatchFacTable() {
        val music = BuiltInPresets.MUSIC.parameters
        assertEquals(3.9f, music.clarity, 0.001f)
        assertEquals(2.8f, music.surround, 0.001f)
        assertEquals(2.8f, music.ambience, 0.001f)
        assertEquals(1.6f, music.dynamicBoost, 0.001f)
        assertEquals(4.7f, music.bass, 0.001f)
        assertEquals(listOf(0f, 2f, 2f, 1f, 0f, 0f, 0f, -1f, 0f, 2f), music.eqGains)

        val movies = BuiltInPresets.MOVIES.parameters
        assertEquals(4.7f, movies.clarity, 0.001f)
        assertEquals(3.9f, movies.surround, 0.001f)
        assertEquals(0.0f, movies.ambience, 0.001f)
        assertEquals(6.7f, movies.dynamicBoost, 0.001f)
        assertEquals(2.8f, movies.bass, 0.001f)
    }

    @Test
    fun compareTogglesFlatVersusActive() {
        val active = BuiltInPresets.VOLUME_BOOST
        assertEquals(BuiltInPresets.FLAT.id, PresetCompare.target(true, active).id)
        assertEquals(0.0f, PresetCompare.target(true, active).parameters.dynamicBoost, 0.001f)
        assertEquals(active.id, PresetCompare.target(false, active).id)
        assertEquals(8.1f, PresetCompare.target(false, active).parameters.dynamicBoost, 0.001f)
    }
}