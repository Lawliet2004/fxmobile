package com.fxsound.mobile

import com.fxsound.mobile.core.presets.PresetName
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetNameTest {

    @Test
    fun rejectsEmptyAndTooLong() {
        assertFalse(PresetName.validate("   ", listOf("Music")).ok)
        assertFalse(PresetName.validate("x".repeat(65), listOf()).ok)
        assertTrue(PresetName.validate("x".repeat(64), listOf()).ok)
    }

    @Test
    fun rejectsIllegalCharactersAndDuplicates() {
        assertFalse(PresetName.validate("bad:name", listOf()).ok)
        assertFalse(PresetName.validate("Music", listOf("Music", "Movie")).ok)
        assertTrue(PresetName.validate("Music", listOf("Music"), currentName = "Music").ok)
        assertTrue(PresetName.validate("Late Night", listOf("Music")).ok)
    }
}
