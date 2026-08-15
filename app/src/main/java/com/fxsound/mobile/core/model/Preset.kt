package com.fxsound.mobile.core.model

import org.json.JSONObject

data class Preset(
    val id: String,
    val name: String,
    val isBuiltIn: Boolean = false,
    val parameters: DspParameters
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("id", id)
        root.put("name", name)
        root.put("isBuiltIn", isBuiltIn)
        root.put("parameters", parameters.toJsonObject())
        return root.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): Preset? {
            return try {
                val root = JSONObject(jsonStr)
                val id = root.getString("id")
                val name = root.getString("name")
                val isBuiltIn = root.optBoolean("isBuiltIn", false)
                val params = DspParameters.fromJsonObject(root.getJSONObject("parameters"))
                Preset(id = id, name = name, isBuiltIn = isBuiltIn, parameters = params)
            } catch (_: Exception) {
                null
            }
        }
    }
}
