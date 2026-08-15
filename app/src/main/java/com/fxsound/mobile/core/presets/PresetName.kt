package com.fxsound.mobile.core.presets

object PresetName {
    private val illegalChars = charArrayOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')

    data class Result(val ok: Boolean, val message: String? = null, val sanitized: String = "")

    fun validate(raw: String, existingNames: Collection<String>, currentName: String? = null): Result {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return Result(false, "Preset name cannot be empty.")
        }
        if (trimmed.length > 64) {
            return Result(false, "Preset name must be 64 characters or fewer.")
        }
        if (trimmed.any { it in illegalChars }) {
            return Result(false, "Preset name cannot contain \\ / : * ? \" < > |")
        }
        val clash = existingNames.any {
            it.equals(trimmed, ignoreCase = true) && (currentName == null || !it.equals(currentName, ignoreCase = true))
        }
        if (clash) {
            return Result(false, "A preset with this name already exists.")
        }
        return Result(true, sanitized = trimmed)
    }
}
