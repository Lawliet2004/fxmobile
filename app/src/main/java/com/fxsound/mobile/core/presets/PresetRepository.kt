package com.fxsound.mobile.core.presets

import android.content.Context
import com.fxsound.mobile.core.model.DspParameters
import com.fxsound.mobile.core.model.Preset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class PresetRepository(private val store: StringStore) {

    constructor(context: Context) : this(
        PrefsStringStore(context.getSharedPreferences("fxsound_presets", Context.MODE_PRIVATE))
    )

    private val _presets = MutableStateFlow<List<Preset>>(emptyList())
    val presets: StateFlow<List<Preset>> = _presets.asStateFlow()

    private val _activePreset = MutableStateFlow<Preset>(BuiltInPresets.MUSIC)
    val activePreset: StateFlow<Preset> = _activePreset.asStateFlow()

    init {
        loadPresets()
    }

    private fun loadPresets() {
        val userPresetsJson = store.getStringSet("user_presets_json")
        val userPresets = userPresetsJson.mapNotNull { Preset.fromJson(it) }

        val allPresets = mutableListOf<Preset>()
        allPresets.addAll(BuiltInPresets.ALL)
        allPresets.addAll(userPresets)
        _presets.value = allPresets

        val activeId = store.getString("active_preset_id", BuiltInPresets.MUSIC.id)
        val active = allPresets.find { it.id == activeId } ?: BuiltInPresets.MUSIC
        _activePreset.value = active
    }

    fun selectPreset(preset: Preset) {
        _activePreset.value = preset
        store.putString("active_preset_id", preset.id)
    }

    private var undoSnapshot: List<Preset>? = null
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    fun validateName(name: String, currentName: String? = null): PresetName.Result {
        return PresetName.validate(name, _presets.value.map { it.name }, currentName)
    }

    fun saveCustomPreset(name: String, parameters: DspParameters): Preset {
        val result = validateName(name)
        val trimmedName = if (result.ok) result.sanitized else name.trim().take(64).ifEmpty { "Custom Preset" }
        pushUndo()
        val id = "user_" + UUID.randomUUID().toString()
        val newPreset = Preset(
            id = id,
            name = trimmedName,
            isBuiltIn = false,
            parameters = parameters
        )

        val currentList = _presets.value.toMutableList()
        currentList.add(newPreset)
        _presets.value = currentList

        persistUserPresets(currentList.filter { !it.isBuiltIn })
        selectPreset(newPreset)
        return newPreset
    }

    fun overwriteCustomPreset(presetId: String, parameters: DspParameters): Preset? {
        val existing = _presets.value.find { it.id == presetId && !it.isBuiltIn } ?: return null
        pushUndo()
        val updated = existing.copy(parameters = parameters)
        updateCustomPreset(updated)
        return updated
    }

    fun renameCustomPreset(presetId: String, newName: String): Preset? {
        val existing = _presets.value.find { it.id == presetId && !it.isBuiltIn } ?: return null
        val result = validateName(newName, existing.name)
        if (!result.ok) return null
        pushUndo()
        val updated = existing.copy(name = result.sanitized)
        updateCustomPreset(updated)
        return updated
    }

    fun updateCustomPreset(preset: Preset) {
        if (preset.isBuiltIn) return

        val currentList = _presets.value.map {
            if (it.id == preset.id) preset else it
        }
        _presets.value = currentList
        persistUserPresets(currentList.filter { !it.isBuiltIn })

        if (_activePreset.value.id == preset.id) {
            _activePreset.value = preset
        }
    }

    fun deleteCustomPreset(presetId: String) {
        pushUndo()
        val currentList = _presets.value.filter { it.id != presetId }
        _presets.value = currentList
        persistUserPresets(currentList.filter { !it.isBuiltIn })

        if (_activePreset.value.id == presetId) {
            selectPreset(BuiltInPresets.MUSIC)
        }
    }

    fun undoLastMutation(): Boolean {
        val snapshot = undoSnapshot ?: return false
        undoSnapshot = null
        _canUndo.value = false
        _presets.value = snapshot
        persistUserPresets(snapshot.filter { !it.isBuiltIn })
        val activeId = _activePreset.value.id
        val restored = snapshot.find { it.id == activeId } ?: BuiltInPresets.MUSIC
        selectPreset(restored)
        return true
    }

    private fun pushUndo() {
        undoSnapshot = _presets.value.toList()
        _canUndo.value = true
    }

    fun exportUserPresetsJson(): String = PresetPack.encode(_presets.value)

    fun importUserPresetsJson(json: String): Int {
        val incoming = PresetPack.decode(json)
        if (incoming.isEmpty()) return 0
        pushUndo()
        var imported = 0
        val current = _presets.value.toMutableList()
        for (preset in incoming) {
            var candidate = preset.name.trim().ifEmpty { "Imported" }
            var suffix = 1
            while (!PresetName.validate(candidate, current.map { it.name }).ok && suffix < 100) {
                candidate = "${preset.name} ($suffix)"
                suffix++
            }
            val copy = preset.copy(
                id = "user_" + UUID.randomUUID().toString(),
                name = candidate.take(64),
                isBuiltIn = false
            )
            current.add(copy)
            imported++
        }
        _presets.value = current
        persistUserPresets(current.filter { !it.isBuiltIn })
        return imported
    }

    fun isModified(active: Preset, current: DspParameters): Boolean {
        return active.parameters != current
    }

    private fun persistUserPresets(userPresets: List<Preset>) {
        store.putStringSet("user_presets_json", userPresets.map { it.toJson() }.toSet())
    }
}
