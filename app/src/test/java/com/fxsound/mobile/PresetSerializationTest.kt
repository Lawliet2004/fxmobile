package com.fxsound.mobile

import com.fxsound.mobile.core.model.DspParameters
import com.fxsound.mobile.core.model.Preset
import com.fxsound.mobile.core.presets.BuiltInPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PresetSerializationTest {

    @Test
    fun testBuiltInPresetsSerializationRoundTrip() {
        for (preset in BuiltInPresets.ALL) {
            val json = preset.toJson()
            assertNotNull(json)

            val parsed = Preset.fromJson(json)
            assertNotNull(parsed)
            assertEquals(preset.id, parsed?.id)
            assertEquals(preset.name, parsed?.name)
            assertEquals(preset.isBuiltIn, parsed?.isBuiltIn)
            assertEquals(preset.parameters.bass, parsed!!.parameters.bass, 0.001f)
            assertEquals(preset.parameters.clarity, parsed.parameters.clarity, 0.001f)
            assertEquals(preset.parameters.ambience, parsed.parameters.ambience, 0.001f)
            assertEquals(preset.parameters.surround, parsed.parameters.surround, 0.001f)
            assertEquals(preset.parameters.dynamicBoost, parsed.parameters.dynamicBoost, 0.001f)
            assertEquals(preset.parameters.volumeLeveling, parsed.parameters.volumeLeveling, 0.001f)
            assertEquals(preset.parameters.masterGainDb, parsed.parameters.masterGainDb, 0.001f)
            assertEquals(preset.parameters.eqBandCount, parsed.parameters.eqBandCount)
            assertEquals(preset.parameters.eqGains.size, parsed.parameters.eqGains.size)
            assertEquals(preset.parameters.eqFrequencies.size, parsed!!.parameters.eqFrequencies.size)
            assertEquals(preset.parameters.eqQFactors.size, parsed.parameters.eqQFactors.size)
        }
    }

    @Test
    fun testCustomUserPresetSerialization() {
        val custom = Preset(
            id = "user_custom_1",
            name = "My Custom Profile",
            isBuiltIn = false,
            parameters = DspParameters(
                powerEnabled = true,
                bass = 8.5f,
                clarity = 7.0f,
                ambience = 4.0f,
                surround = 6.5f,
                dynamicBoost = 5.0f,
                volumeLeveling = 2.0f,
                masterGainDb = 3.5f,
                balance = -2.0f,
                eqEnabled = true,
                eqBandCount = 5,
                eqGains = listOf(4.0f, 2.0f, 0.0f, 1.0f, 3.0f),
                eqFrequencies = listOf(60f, 250f, 1000f, 4000f, 12000f),
                eqQFactors = listOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f)
            )
        )

        val json = custom.toJson()
        val parsed = Preset.fromJson(json)

        assertNotNull(parsed)
        assertEquals("user_custom_1", parsed?.id)
        assertEquals("My Custom Profile", parsed?.name)
        assertEquals(false, parsed?.isBuiltIn)
        assertEquals(8.5f, parsed!!.parameters.bass, 0.001f)
        assertEquals(7.0f, parsed.parameters.clarity, 0.001f)
        assertEquals(10, parsed.parameters.eqBandCount)
        assertEquals(10, parsed.parameters.eqGains.size)
        assertEquals(1000f, parsed.parameters.eqFrequencies[5], 0.01f)
        assertEquals(1.414f, parsed.parameters.eqQFactors[0], 0.001f)
    }

    @Test
    fun testLegacyV1PresetWithoutFrequencies() {
        val json = """
            {"version":1,"id":"user_old","name":"Legacy","isBuiltIn":false,
             "parameters":{"bass":1.0,"clarity":2.0,"ambience":0.0,"surround":0.0,
             "dynamicBoost":0.0,"volumeLeveling":0.0,"masterGainDb":0.0,"balance":0.0,
             "eqEnabled":true,"eqBandCount":10,
             "eqGains":[0,0,0,0,0,0,0,0,0,0]}}
        """.trimIndent()
        val parsed = Preset.fromJson(json)
        assertNotNull(parsed)
        assertEquals(10, parsed!!.parameters.eqFrequencies.size)
        assertEquals(10, parsed.parameters.eqQFactors.size)
        assertEquals(31.25f, parsed.parameters.eqFrequencies[0], 0.01f)
    }
}
