package com.fxsound.mobile.core.runtime

class ProgressThrottle(
    private val intervalMs: Long = 250L,
    private val nowMs: () -> Long
) {
    private var lastEmitMs: Long = Long.MIN_VALUE / 4

    fun allow(): Boolean {
        val now = nowMs()
        if (now - lastEmitMs >= intervalMs) {
            lastEmitMs = now
            return true
        }
        return false
    }
}
