package com.fxsound.mobile

import com.fxsound.mobile.core.presets.StringStore

class MemoryStringStore : StringStore {
    private val strings = mutableMapOf<String, String?>()
    private val sets = mutableMapOf<String, Set<String>>()

    override fun getString(key: String, default: String?) = if (strings.containsKey(key)) strings[key] else default
    override fun putString(key: String, value: String?) {
        strings[key] = value
    }
    override fun getStringSet(key: String) = sets[key] ?: emptySet()
    override fun putStringSet(key: String, values: Set<String>) {
        sets[key] = values.toSet()
    }
}
