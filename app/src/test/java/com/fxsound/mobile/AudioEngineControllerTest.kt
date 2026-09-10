package com.fxsound.mobile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fxsound.mobile.audio.AudioEngineController
import com.fxsound.mobile.core.settings.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class AudioEngineControllerTest {

    private fun controller(): AudioEngineController {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return AudioEngineController(context, SettingsRepository(context))
    }

    @Test
    fun setPowerEnabled_updatesStateAndDoesNotThrow() {
        val audio = controller()
        try {
            assertTrue(audio.parameters.value.powerEnabled)
            audio.setPowerEnabled(false)
            assertFalse(audio.parameters.value.powerEnabled)
            audio.setPowerEnabled(true)
            assertTrue(audio.parameters.value.powerEnabled)
        } finally {
            audio.release()
        }
    }

    @Test
    fun setPowerEnabled_afterReleaseIsIgnored() {
        val audio = controller()
        audio.release()
        audio.setPowerEnabled(false)
        assertTrue(audio.parameters.value.powerEnabled)
    }

    @Test
    fun setBass_ignoresNaNAndInfinity() {
        val audio = controller()
        try {
            audio.setBass(4.0f)
            assertEquals(4.0f, audio.parameters.value.bass, 0.0f)
            audio.setBass(Float.NaN)
            assertEquals(4.0f, audio.parameters.value.bass, 0.0f)
            audio.setBass(Float.POSITIVE_INFINITY)
            assertEquals(4.0f, audio.parameters.value.bass, 0.0f)
        } finally {
            audio.release()
        }
    }

    @Test
    fun setEqBandGain_ignoresNaN() {
        val audio = controller()
        try {
            audio.setEqBandGain(0, 3.0f)
            assertEquals(3.0f, audio.parameters.value.eqGains[0], 0.0f)
            audio.setEqBandGain(0, Float.NaN)
            assertEquals(3.0f, audio.parameters.value.eqGains[0], 0.0f)
        } finally {
            audio.release()
        }
    }
}
