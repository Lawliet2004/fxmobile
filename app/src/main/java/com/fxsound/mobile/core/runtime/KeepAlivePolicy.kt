package com.fxsound.mobile.core.runtime

import com.fxsound.mobile.core.model.ProcessingMode

object KeepAlivePolicy {

    fun shouldKeepAlive(
        powerEnabled: Boolean,
        isPlaying: Boolean,
        mode: ProcessingMode,
        @Suppress("UNUSED_PARAMETER") sessionAttached: Boolean
    ): Boolean {
        if (isPlaying) return true
        return mode == ProcessingMode.PLAYER_SESSION && powerEnabled
    }
}
