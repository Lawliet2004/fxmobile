package com.fxsound.mobile.core.runtime

import android.app.ActivityManager
import android.content.Context

object DevicePerformance {

    const val LOW_POWER_RAM_BYTES = 4L * 1024L * 1024L * 1024L

    fun preferLowPowerIo(isLowRamDevice: Boolean, totalMemBytes: Long): Boolean {
        if (isLowRamDevice) return true
        return totalMemBytes in 1..LOW_POWER_RAM_BYTES
    }

    fun preferLowPowerIo(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        val info = ActivityManager.MemoryInfo()
        return try {
            am.getMemoryInfo(info)
            preferLowPowerIo(am.isLowRamDevice, info.totalMem)
        } catch (_: Exception) {
            false
        }
    }
}
