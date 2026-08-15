package com.fxsound.mobile.core.settings

import android.content.Context
import com.fxsound.mobile.core.model.DspParameters
import com.fxsound.mobile.core.model.ProcessingMode
import org.json.JSONObject

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("fxsound_settings", Context.MODE_PRIVATE)

    fun loadParameters(): DspParameters? {
        val json = prefs.getString(KEY_PARAMS, null) ?: return null
        return try {
            DspParameters.fromJsonObject(JSONObject(json))
        } catch (_: Exception) {
            null
        }
    }

    fun saveParameters(parameters: DspParameters) {
        prefs.edit().putString(KEY_PARAMS, parameters.toJsonObject().toString()).apply()
    }

    fun loadProcessingMode(): ProcessingMode {
        return if (prefs.getString(KEY_MODE, ProcessingMode.INTERNAL_ENGINE.name) == ProcessingMode.PLAYER_SESSION.name) {
            ProcessingMode.PLAYER_SESSION
        } else {
            ProcessingMode.INTERNAL_ENGINE
        }
    }

    fun saveProcessingMode(mode: ProcessingMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun loadLastMediaUri(): String? = prefs.getString(KEY_LAST_URI, null)

    fun saveLastMediaUri(uri: String?) {
        prefs.edit().putString(KEY_LAST_URI, uri).apply()
    }

    companion object {
        private const val KEY_PARAMS = "last_dsp_params"
        private const val KEY_MODE = "processing_mode"
        private const val KEY_LAST_URI = "last_media_uri"
    }
}
