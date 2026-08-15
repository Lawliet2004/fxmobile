package com.fxsound.mobile.core.runtime

import com.fxsound.mobile.core.model.ProcessingMode

enum class ProcessingStatus {
    INTERNAL_IDLE,
    PROCESSING_FILE,
    SESSION_ATTACHED,
    SESSION_WAITING
}

object ProcessingStatusResolver {

    const val MSG_INTERNAL_IDLE = "Enhancement applies to audio you open here."
    const val MSG_PROCESSING_FILE = "Processing this file."
    const val MSG_SESSION_WAITING =
        "Waiting for a player that publishes a session. YouTube usually will not."

    fun resolve(
        mode: ProcessingMode,
        isPlaying: Boolean,
        sessionAttached: Boolean
    ): ProcessingStatus {
        if (mode == ProcessingMode.PLAYER_SESSION) {
            return if (sessionAttached) {
                ProcessingStatus.SESSION_ATTACHED
            } else {
                ProcessingStatus.SESSION_WAITING
            }
        }
        return if (isPlaying) ProcessingStatus.PROCESSING_FILE else ProcessingStatus.INTERNAL_IDLE
    }

    fun message(status: ProcessingStatus, sessionLabel: String = ""): String {
        return when (status) {
            ProcessingStatus.INTERNAL_IDLE -> MSG_INTERNAL_IDLE
            ProcessingStatus.PROCESSING_FILE -> MSG_PROCESSING_FILE
            ProcessingStatus.SESSION_ATTACHED -> {
                val label = sessionLabel.ifBlank { "session" }
                "Limited EQ on $label."
            }
            ProcessingStatus.SESSION_WAITING -> MSG_SESSION_WAITING
        }
    }
}
