package com.fxsound.mobile

import com.fxsound.mobile.core.model.DspParameters
import com.fxsound.mobile.core.model.Preset
import com.fxsound.mobile.core.presets.PresetPack
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetPackTest {

    @Test
    fun v2ArrayForm_decodesIntoList() {
        val custom = Preset(
            id = "user_pack_1",
            name = "Pack One",
            isBuiltIn = false,
            parameters = DspParameters(bass = 4f, clarity = 5f)
        )
        val custom2 = Preset(
            id = "user_pack_2",
            name = "Pack Two",
            isBuiltIn = false,
            parameters = DspParameters(bass = 1f, clarity = 2f)
        )

        val v2Json = PresetPack.encode(listOf(custom, custom2))
        val decoded = PresetPack.decode(v2Json)

        assertEquals(2, decoded.size)
        assertEquals("Pack One", decoded[0].name)
        assertEquals("Pack Two", decoded[1].name)
        assertFalse("Decoded presets must not be marked built-in", decoded[0].isBuiltIn)
        assertFalse(decoded[1].isBuiltIn)
    }

    @Test
    fun v2ArrayForm_canBeReEncoded() {
        val custom = Preset(
            id = "user_round_trip",
            name = "Round Trip",
            isBuiltIn = false,
            parameters = DspParameters(bass = 3f)
        )
        val json = PresetPack.encode(listOf(custom))
        // Verify shape: a JSON object with "version"=2 and "presets" array.
        val root = JSONObject(json)
        assertEquals(2, root.getInt("version"))
        val arr = root.getJSONArray("presets")
        assertEquals(1, arr.length())
        val entry = arr.getJSONObject(0)
        assertEquals("user_round_trip", entry.getString("id"))
        assertEquals("Round Trip", entry.getString("name"))
    }

    @Test
    fun v1SinglePresetForm_decodesIntoListOfOne() {
        // v1 used a bare preset JSON object; PresetPack.decode should still
        // recognise it and return a single-element list.
        val v1Json = """
            {"version":1,"id":"user_v1","name":"V1 Single","isBuiltIn":false,
             "parameters":{"bass":2.0,"clarity":0.0,"ambience":0.0,"surround":0.0,
                            "dynamicBoost":0.0,"volumeLeveling":0.0,"masterGainDb":0.0,
                            "balance":0.0,"eqEnabled":true,"eqBandCount":10,
                            "eqGains":[0,0,0,0,0,0,0,0,0,0]}}
        """.trimIndent()

        val decoded = PresetPack.decode(v1Json)
        assertEquals(1, decoded.size)
        assertEquals("V1 Single", decoded[0].name)
        assertFalse(decoded[0].isBuiltIn)
        assertEquals(2.0f, decoded[0].parameters.bass, 0.001f)
    }

    @Test
    fun v2ArrayForm_builtInPresetsAreFilteredFromEncode() {
        val builtIn = Preset(
            id = "builtin_x",
            name = "Built-In X",
            isBuiltIn = true,
            parameters = DspParameters(bass = 0f)
        )
        val custom = Preset(
            id = "user_x",
            name = "User X",
            isBuiltIn = false,
            parameters = DspParameters(bass = 1f)
        )

        val json = PresetPack.encode(listOf(builtIn, custom))
        val decoded = PresetPack.decode(json)

        // encode() filters out built-ins, so only "User X" should appear.
        assertEquals(1, decoded.size)
        assertEquals("User X", decoded[0].name)
        assertFalse(decoded[0].isBuiltIn)
    }

    @Test
    fun v2ArrayForm_handlesEmpty() {
        val json = PresetPack.encode(emptyList())
        val decoded = PresetPack.decode(json)
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun v2ArrayForm_decodedPresetsAreNotMarkedBuiltIn() {
        val original = Preset(
            id = "user_not_built",
            name = "Not Built-In",
            isBuiltIn = false,
            parameters = DspParameters(bass = 2f)
        )
        val json = PresetPack.encode(listOf(original))
        // Mutate the JSON to set isBuiltIn=true on the embedded entry;
        // decode should still reset it to false.
        val root = JSONObject(json)
        val arr = root.getJSONArray("presets")
        arr.getJSONObject(0).put("isBuiltIn", true)
        val tamperedJson = root.toString()

        val decoded = PresetPack.decode(tamperedJson)
        assertEquals(1, decoded.size)
        assertNotNull(decoded[0])
        assertFalse(
            "Decoded user preset must never be marked built-in",
            decoded[0].isBuiltIn
        )
    }
}
