package com.fxsound.mobile.core.presets

import com.fxsound.mobile.core.model.Preset

object PresetCompare {
    fun target(comparing: Boolean, active: Preset): Preset {
        return if (comparing) BuiltInPresets.FLAT else active
    }
}
