package com.example.qwen3chat

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log

class DeviceCapability(private val context: Context) {

    enum class Tier { LOW, MID, HIGH }

    val ramMiB: Long
    val cpuCores: Int
    val tier: Tier

    init {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        ramMiB = memInfo.totalMem / (1024 * 1024)

        cpuCores = Runtime.getRuntime().availableProcessors().let { cores ->
            if (cores < 1) countCpuFiles() else cores
        }

        tier = classifyTier()
        Log.i(TAG, "Device: ${ramMiB}MB RAM, $cpuCores cores, tier=$tier")
    }

    val availableModels: List<ModelMetadata>
        get() = when (tier) {
            Tier.LOW -> ModelMetadata.ALL.filter { it.key == "qwen3.5-0.8b" }
            Tier.MID -> ModelMetadata.ALL.filter { it.key in listOf("qwen3.5-0.8b", "qwen3.5-2b") }
            Tier.HIGH -> ModelMetadata.ALL
        }

    fun canRunModel(metadata: ModelMetadata): Boolean {
        return ramMiB >= metadata.minRamMiB && cpuCores >= metadata.minCpuCores
    }

    private fun classifyTier(): Tier {
        // HIGH: 6.5GB+ RAM AND 8+ cores
        if (ramMiB > 6500 && cpuCores >= 8) return Tier.HIGH
        // MID: 4.5GB+ RAM AND 4+ cores
        if (ramMiB >= 4500 && cpuCores >= 4) return Tier.MID
        // LOW: everything else
        return Tier.LOW
    }

    private fun countCpuFiles(): Int {
        return try {
            val cpuDir = java.io.File("/sys/devices/system/cpu")
            cpuDir.listFiles { file -> file.name.matches(Regex("cpu[0-9]+")) }?.size ?: 4
        } catch (e: Exception) {
            4 // safe fallback
        }
    }

    companion object {
        private const val TAG = "DeviceCapability"
    }
}
