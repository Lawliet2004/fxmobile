package com.fxsound.mobile

import com.fxsound.mobile.ui.navigation.TAB_TRANSITION_MS
import com.fxsound.mobile.ui.theme.FxMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TabTransitionTest {
    @Test
    fun transitionDurationIsSnappy() {
        assertTrue(TAB_TRANSITION_MS in 180..260)
        assertEquals(FxMotion.TAB_MS, TAB_TRANSITION_MS)
    }

    @Test
    fun appleSpringTokensStayInRange() {
        assertEquals(0.85f, FxMotion.SNAPPY_DAMPING, 0.001f)
        assertEquals(400f, FxMotion.SNAPPY_STIFFNESS, 0.1f)
        assertEquals(0.9f, FxMotion.SMOOTH_DAMPING, 0.001f)
        assertTrue(FxMotion.PRESS_SCALE in 0.95f..0.99f)
    }
}
