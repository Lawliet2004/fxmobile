package com.fxsound.mobile

import com.fxsound.mobile.ui.navigation.TAB_TRANSITION_MS
import org.junit.Assert.assertTrue
import org.junit.Test

class TabTransitionTest {

    @Test
    fun fadeAndSlideDurationIsShort() {
        assertTrue(TAB_TRANSITION_MS in 120..160)
    }
}