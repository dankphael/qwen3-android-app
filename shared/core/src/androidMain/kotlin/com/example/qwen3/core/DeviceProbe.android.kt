package com.example.qwen3.core

import android.app.ActivityManager
import android.content.Context

/** Android device probe using ActivityManager and the JVM runtime. */
class AndroidDeviceProbe(private val context: Context) : DeviceProbe {
    override fun totalRamMiB(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem / (1024 * 1024)
    }

    override fun cpuCores(): Int =
        Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
}
