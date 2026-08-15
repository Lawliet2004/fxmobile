package com.fxsound.mobile

import com.fxsound.mobile.core.model.ProcessingMode
import com.fxsound.mobile.core.runtime.KeepAlivePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepAlivePolicyTest {

    @Test
    fun idleInternalPowerOnDoesNotKeepAlive() {
        assertFalse(
            KeepAlivePolicy.shouldKeepAlive(
                powerEnabled = true,
                isPlaying = false,
                mode = ProcessingMode.INTERNAL_ENGINE,
                sessionAttached = false
            )
        )
    }

    @Test
    fun playingKeepsAliveRegardlessOfPower() {
        assertTrue(
            KeepAlivePolicy.shouldKeepAlive(
                powerEnabled = false,
                isPlaying = true,
                mode = ProcessingMode.INTERNAL_ENGINE,
                sessionAttached = false
            )
        )
    }

    @Test
    fun sessionModeNeedsPowerEvenWithoutAttachment() {
        assertTrue(
            KeepAlivePolicy.shouldKeepAlive(
                powerEnabled = true,
                isPlaying = false,
                mode = ProcessingMode.PLAYER_SESSION,
                sessionAttached = false
            )
        )
        assertTrue(
            KeepAlivePolicy.shouldKeepAlive(
                powerEnabled = true,
                isPlaying = false,
                mode = ProcessingMode.PLAYER_SESSION,
                sessionAttached = true
            )
        )
        assertFalse(
            KeepAlivePolicy.shouldKeepAlive(
                powerEnabled = false,
                isPlaying = false,
                mode = ProcessingMode.PLAYER_SESSION,
                sessionAttached = true
            )
        )
    }
}
