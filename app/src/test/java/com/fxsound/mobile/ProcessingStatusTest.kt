package com.fxsound.mobile

import com.fxsound.mobile.core.model.ProcessingMode
import com.fxsound.mobile.core.runtime.ProcessingStatus
import com.fxsound.mobile.core.runtime.ProcessingStatusResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class ProcessingStatusTest {

    @Test
    fun fourProcessingStates() {
        assertEquals(
            ProcessingStatus.INTERNAL_IDLE,
            ProcessingStatusResolver.resolve(ProcessingMode.INTERNAL_ENGINE, false, false)
        )
        assertEquals(
            ProcessingStatus.PROCESSING_FILE,
            ProcessingStatusResolver.resolve(ProcessingMode.INTERNAL_ENGINE, true, false)
        )
        assertEquals(
            ProcessingStatus.SESSION_ATTACHED,
            ProcessingStatusResolver.resolve(ProcessingMode.PLAYER_SESSION, false, true)
        )
        assertEquals(
            ProcessingStatus.SESSION_WAITING,
            ProcessingStatusResolver.resolve(ProcessingMode.PLAYER_SESSION, false, false)
        )
    }

    @Test
    fun statusCopyMatchesHomeBanner() {
        assertEquals(
            "Enhancement applies to audio you open here.",
            ProcessingStatusResolver.message(ProcessingStatus.INTERNAL_IDLE)
        )
        assertEquals(
            "Processing this file.",
            ProcessingStatusResolver.message(ProcessingStatus.PROCESSING_FILE)
        )
        assertEquals(
            "Limited EQ on Session 7.",
            ProcessingStatusResolver.message(ProcessingStatus.SESSION_ATTACHED, "Session 7")
        )
        assertEquals(
            "Waiting for a player that publishes a session. YouTube usually will not.",
            ProcessingStatusResolver.message(ProcessingStatus.SESSION_WAITING)
        )
    }
}