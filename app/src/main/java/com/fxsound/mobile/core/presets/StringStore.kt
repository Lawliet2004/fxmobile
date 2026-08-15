package com.fxsound.mobile.core.presets

import android.content.SharedPreferences

interface StringStore {
    fun getString(key: String, default: String? = null): String?
    fun putString(key: String, value: String?)
    fun getStringSet(key: String): Set<String>
    fun putStringSet(key: String, values: Set<String>)
}

class PrefsStringStore(private val prefs: SharedPreferences) : StringStore {
    override fun getString(key: String, default: String?) = prefs.getString(key, default)
    override fun putString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
    }
    override fun getStringSet(key: String) = prefs.getStringSet(key, emptySet()) ?: emptySet()
    override fun putStringSet(key: String, values: Set<String>) {
        prefs.edit().putStringSet(key, values).apply()
    }
}
