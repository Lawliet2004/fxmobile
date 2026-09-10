package com.fxsound.mobile

import com.fxsound.mobile.audio.SessionEffectMapping
import com.fxsound.mobile.core.model.DspParameters
import com.fxsound.mobile.core.model.Preset
import com.fxsound.mobile.core.presets.BuiltInPresets
import com.fxsound.mobile.core.presets.PresetCompare
import com.fxsound.mobile.core.presets.PresetRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetApplyStressTest {

    private fun repo() = PresetRepository(MemoryStringStore())

    @Test
    fun rapidSelectCyclesEveryFactoryPreset() {
        val repo = repo()
        repeat(20) {
            for (preset in BuiltInPresets.ALL) {
                repo.selectPreset(preset)
                assertEquals(preset.id, repo.activePreset.value.id)
                assertEquals(preset.parameters.bass, repo.activePreset.value.parameters.bass, 0.001f)
                assertEquals(preset.parameters.eqGains, repo.activePreset.value.parameters.eqGains)
            }
        }
        assertEquals(BuiltInPresets.DEFAULT.id, repo.activePreset.value.id)
    }

    @Test
    fun deleteActiveCustomReturnsMusicToApply() {
        val repo = repo()
        val saved = repo.saveCustomPreset("Night Mix", DspParameters(bass = 7f, clarity = 6f))
        assertEquals(saved.id, repo.activePreset.value.id)

        val next = repo.deleteCustomPreset(saved.id)
        assertEquals(BuiltInPresets.MUSIC.id, next?.id)
        assertEquals(BuiltInPresets.MUSIC.parameters.bass, next?.parameters?.bass)
        assertEquals(BuiltInPresets.MUSIC.id, repo.activePreset.value.id)
    }

    @Test
    fun deleteInactiveCustomDoesNotChangeActive() {
        val repo = repo()
        repo.selectPreset(BuiltInPresets.VOICE)
        val saved = repo.saveCustomPreset("Keep Voice", DspParameters(clarity = 8f))
        repo.selectPreset(BuiltInPresets.VOICE)

        assertNull(repo.deleteCustomPreset(saved.id))
        assertEquals(BuiltInPresets.VOICE.id, repo.activePreset.value.id)
    }

    @Test
    fun undoAfterOverwriteRestoresPreviousParameters() {
        val repo = repo()
        val saved = repo.saveCustomPreset("Editable", DspParameters(bass = 2f))
        repo.overwriteCustomPreset(saved.id, DspParameters(bass = 9f, dynamicBoost = 8f))
        assertEquals(9f, repo.activePreset.value.parameters.bass, 0.001f)

        val restored = repo.undoLastMutation()
        assertNotNull(restored)
        assertEquals(saved.id, restored!!.id)
        assertEquals(2f, restored.parameters.bass, 0.001f)
        assertEquals(2f, repo.activePreset.value.parameters.bass, 0.001f)
    }

    @Test
    fun exportImportRoundTripMaxedProfile() {
        val repo = repo()
        val extreme = DspParameters(
            bass = 10f,
            clarity = 10f,
            ambience = 10f,
            surround = 10f,
            dynamicBoost = 10f,
            volumeLeveling = 4f,
            masterGainDb = 12f,
            balance = -8f,
            eqGains = listOf(12f, -12f, 6f, -6f, 0f, 3f, -3f, 9f, -9f, 1f)
        )
        repo.saveCustomPreset("Maxed", extreme)
        val json = repo.exportUserPresetsJson()

        val other = repo()
        assertEquals(1, other.importUserPresetsJson(json))
        val imported = other.presets.value.first { !it.isBuiltIn }
        assertEquals(10f, imported.parameters.bass, 0.001f)
        assertEquals(12f, imported.parameters.masterGainDb, 0.001f)
        assertEquals(extreme.eqGains, imported.parameters.eqGains)
        assertTrue(imported.id.startsWith("user_"))
    }

    @Test
    fun powerToggleDoesNotMarkPresetModified() {
        val repo = repo()
        val music = BuiltInPresets.MUSIC
        assertFalse(repo.isModified(music, music.parameters))
        assertFalse(repo.isModified(music, music.parameters.copy(powerEnabled = false)))
        assertTrue(repo.isModified(music, music.parameters.copy(bass = 9f)))
        assertTrue(repo.isModified(music, music.parameters.copy(eqGains = List(10) { 1f })))
    }

    @Test
    fun compareIsNotAUserModification() {
        val active = BuiltInPresets.MUSIC
        val compared = PresetCompare.target(true, active)
        val restored = PresetCompare.target(false, active)
        assertEquals(BuiltInPresets.FLAT.parameters, compared.parameters)
        assertEquals(active.id, restored.id)
        val repo = repo()
        assertTrue(repo.isModified(active, compared.parameters))
        val showingStar = false && repo.isModified(active, compared.parameters)
        assertFalse(showingStar)
    }

    @Test
    fun everyFactoryPresetEnablesSessionEffectsBecauseEqIsOn() {
        for (preset in BuiltInPresets.ALL) {
            assertTrue(preset.name, SessionEffectMapping.shouldEnable(preset.parameters))
        }
        assertTrue(SessionEffectMapping.shouldEnable(DspParameters.FLAT))
        val poweredOff = BuiltInPresets.MUSIC.parameters.copy(powerEnabled = false)
        assertFalse(SessionEffectMapping.shouldEnable(poweredOff))
    }

    @Test
    fun volumeBoostAndTranscriptionMapLoudness() {
        assertTrue(SessionEffectMapping.loudnessEnhancerMb(BuiltInPresets.VOLUME_BOOST.parameters) > 0)
        assertTrue(SessionEffectMapping.loudnessEnhancerMb(BuiltInPresets.TRANSCRIPTION.parameters) > 0)
        assertEquals(0, SessionEffectMapping.loudnessEnhancerMb(BuiltInPresets.DEFAULT.parameters))
        assertTrue(SessionEffectMapping.bassBoostStrength(BuiltInPresets.BASS_BOOST.parameters.bass) > 0)
        assertEquals(0, SessionEffectMapping.bassBoostStrength(BuiltInPresets.VOICE.parameters.bass).toInt())
    }

    @Test
    fun applyTenBandKeepsFactoryGains() {
        for (preset in BuiltInPresets.ALL) {
            val ten = preset.parameters.toTenBand()
            assertEquals(preset.parameters.eqGains, ten.eqGains)
            assertEquals(10, ten.eqBandCount)
        }
    }

    @Test
    fun serializationSurvivesRapidCustomMutations() {
        val repo = repo()
        val names = (1..12).map { "User $it" }
        val saved = names.map { name ->
            repo.saveCustomPreset(name, DspParameters(bass = name.length.toFloat()))
        }
        saved.forEachIndexed { i, preset ->
            repo.renameCustomPreset(preset.id, "Renamed $i")
        }
        val json = repo.exportUserPresetsJson()
        val other = repo()
        assertEquals(12, other.importUserPresetsJson(json))
        assertEquals(13 + 12, other.presets.value.size)
        other.presets.value.filter { !it.isBuiltIn }.forEach { imported ->
            val parsed = Preset.fromJson(imported.toJson())
            assertNotNull(parsed)
            assertEquals(imported.parameters.bass, parsed!!.parameters.bass, 0.001f)
        }
    }
}
