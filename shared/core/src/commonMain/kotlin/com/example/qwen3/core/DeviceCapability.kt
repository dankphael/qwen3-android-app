package com.example.qwen3.core

/**
 * Classifies a device into a capability tier from its RAM and CPU core count,
 * and derives which models it can run. The tier logic is pure; the raw RAM/core
 * probes come from the platform via [DeviceProbe].
 */
class DeviceCapability(private val probe: DeviceProbe) {

    enum class Tier { LOW, MID, HIGH }

    val ramMiB: Long get() = probe.totalRamMiB()
    val cpuCores: Int get() = probe.cpuCores()

    val tier: Tier by lazy { classifyTier(ramMiB, cpuCores) }

    val availableModels: List<ModelMetadata>
        get() = when (tier) {
            Tier.LOW -> ModelMetadata.ALL.filter { it.key == "qwen3.5-0.8b" }
            Tier.MID -> ModelMetadata.ALL.filter { it.key in listOf("qwen3.5-0.8b", "qwen3.5-2b") }
            Tier.HIGH -> ModelMetadata.ALL
        }

    fun canRunModel(model: ModelMetadata): Boolean =
        ramMiB >= model.minRamMiB && cpuCores >= model.minCpuCores

    companion object {
        fun classifyTier(ramMiB: Long, cpuCores: Int): Tier {
            // HIGH: 6.5GB+ RAM AND 8+ cores
            if (ramMiB > 6500 && cpuCores >= 8) return Tier.HIGH
            // MID: 4.5GB+ RAM AND 4+ cores
            if (ramMiB >= 4500 && cpuCores >= 4) return Tier.MID
            return Tier.LOW
        }
    }
}

/** Platform probe for raw device specs. */
interface DeviceProbe {
    fun totalRamMiB(): Long
    fun cpuCores(): Int
}
