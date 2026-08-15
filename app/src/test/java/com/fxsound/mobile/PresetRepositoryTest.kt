package com.fxsound.mobile

import com.fxsound.mobile.core.model.DspParameters
import com.fxsound.mobile.core.presets.BuiltInPresets
import com.fxsound.mobile.core.presets.PresetPack
import com.fxsound.mobile.core.presets.PresetRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetRepositoryTest {

    private fun repo() = PresetRepository(MemoryStringStore())

    @Test
    fun saveRenameDeleteUndo() {
        val repo = repo()
        val saved = repo.saveCustomPreset("Night Mix", DspParameters(bass = 7f))
        assertTrue(repo.presets.value.any { it.id == saved.id })
        assertEquals("Night Mix", repo.activePreset.value.name)

        val renamed = repo.renameCustomPreset(saved.id, "Late Mix")
        assertEquals("Late Mix", renamed?.name)

        repo.deleteCustomPreset(saved.id)
        assertFalse(repo.presets.value.any { it.id == saved.id })
        assertEquals(BuiltInPresets.MUSIC.id, repo.activePreset.value.id)

        assertTrue(repo.undoLastMutation())
        assertTrue(repo.presets.value.any { it.id == saved.id })
    }

    @Test
    fun exportImportRoundTrip() {
        val repo = repo()
        repo.saveCustomPreset("Export Me", DspParameters(clarity = 6f))
        val json = repo.exportUserPresetsJson()
        val parsed = PresetPack.decode(json)
        assertEquals(1, parsed.size)
        assertEquals("Export Me", parsed[0].name)
        assertEquals(6f, parsed[0].parameters.clarity, 0.001f)

        val other = repo()
        assertEquals(1, other.importUserPresetsJson(json))
        assertTrue(other.presets.value.any { it.name == "Export Me" && !it.isBuiltIn })
    }

    @Test
    fun detectsModifiedPreset() {
        val repo = repo()
        val music = BuiltInPresets.MUSIC
        assertFalse(repo.isModified(music, music.parameters))
        assertTrue(repo.isModified(music, music.parameters.copy(bass = 9f)))
    }
}
