package com.fxsound.mobile

import com.fxsound.mobile.core.runtime.ProgressThrottle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressThrottleTest {

    @Test
    fun firstCallAlwaysEmits() {
        var now = 1_000L
        val throttle = ProgressThrottle(intervalMs = 250L, nowMs = { now })
        assertTrue(throttle.allow())
    }

    @Test
    fun dropsUpdatesInsideInterval() {
        var now = 1_000L
        val throttle = ProgressThrottle(intervalMs = 250L, nowMs = { now })
        assertTrue(throttle.allow())
        now = 1_200L
        assertFalse(throttle.allow())
        now = 1_249L
        assertFalse(throttle.allow())
        now = 1_250L
        assertTrue(throttle.allow())
    }
}
