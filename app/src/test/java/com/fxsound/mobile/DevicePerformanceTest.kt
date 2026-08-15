package com.fxsound.mobile

import com.fxsound.mobile.core.runtime.DevicePerformance
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePerformanceTest {

    @Test
    fun fourGigabyteDevicesUseLowPowerIo() {
        val fourGb = 4L * 1024L * 1024L * 1024L
        assertTrue(DevicePerformance.preferLowPowerIo(false, fourGb))
        assertTrue(DevicePerformance.preferLowPowerIo(false, fourGb - 1))
        assertTrue(DevicePerformance.preferLowPowerIo(true, 8L * 1024L * 1024L * 1024L))
        assertFalse(DevicePerformance.preferLowPowerIo(false, fourGb + 1))
        assertEqualsThreshold()
    }

    private fun assertEqualsThreshold() {
        assertTrue(DevicePerformance.LOW_POWER_RAM_BYTES == 4L * 1024L * 1024L * 1024L)
    }
}