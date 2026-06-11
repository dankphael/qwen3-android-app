package com.example.qwen3.core

import platform.Foundation.NSProcessInfo

/** iOS device probe via NSProcessInfo. */
class IosDeviceProbe : DeviceProbe {
    override fun totalRamMiB(): Long =
        (NSProcessInfo.processInfo.physicalMemory / (1024u * 1024u)).toLong()

    override fun cpuCores(): Int =
        NSProcessInfo.processInfo.processorCount.toInt().coerceAtLeast(1)
}
