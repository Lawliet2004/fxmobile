package com.fxsound.mobile.core.presets

import com.fxsound.mobile.core.model.Preset
import org.json.JSONArray
import org.json.JSONObject

object PresetPack {
    fun encode(presets: List<Preset>): String {
        val root = JSONObject()
        root.put("version", 2)
        val arr = JSONArray()
        presets.filter { !it.isBuiltIn }.forEach { arr.put(JSONObject(it.toJson())) }
        root.put("presets", arr)
        return root.toString()
    }

    fun decode(json: String): List<Preset> {
        val trimmed = json.trimStart()
        // Legacy v1 form: a bare JSON array of presets (no wrapping object).
        if (trimmed.startsWith("[")) {
            val arr = JSONArray(json)
            return (0 until arr.length()).mapNotNull { i ->
                Preset.fromJson(arr.getJSONObject(i).toString())?.copy(isBuiltIn = false)
            }
        }
        val root = JSONObject(json)
        if (root.has("presets")) {
            val arr = root.getJSONArray("presets")
            return (0 until arr.length()).mapNotNull { i ->
                Preset.fromJson(arr.getJSONObject(i).toString())?.copy(isBuiltIn = false)
            }
        }
        return Preset.fromJson(json)?.let { listOf(it.copy(isBuiltIn = false)) } ?: emptyList()
    }
}
