package com.example.qwen3chat.ui.diagnostics

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.qwen3chat.LlamaEngine
import com.example.qwen3chat.MainActivity
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.concurrent.atomic.AtomicBoolean

class DiagnosticViewModel : ViewModel() {

    data class DeviceInfo(
        val deviceModel: String,
        val chipset: String,
        val androidVersion: String,
        val totalRamMB: Long
    )

    data class CpuInfo(
        val coreCount: Int,
        val freqsMHz: String,
        val governor: String,
        val usagePercent: String,
        val thermalTempC: Int
    )

    data class GpuInfo(
        val gpuType: String,
        val utilizationPercent: Int,
        val frequencyMHz: String
    )

    data class MemoryInfo(
        val appHeapUsedMB: Long,
        val appHeapMaxMB: Long,
        val nativeHeapMB: Long,
        val deviceRamUsedGB: Float,
        val deviceRamTotalGB: Float
    )

    data class ModelInfo(
        val generationState: String,
        val kvCacheUsed: Int,
        val kvCacheTotal: Int,
        val decodeThreads: Int,
        val batchThreads: Int,
        val batchSize: Int
    )

    private val _deviceInfo = MutableLiveData<DeviceInfo>()
    val deviceInfo: LiveData<DeviceInfo> = _deviceInfo

    private val _cpuInfo = MutableLiveData<CpuInfo>()
    val cpuInfo: LiveData<CpuInfo> = _cpuInfo

    private val _gpuInfo = MutableLiveData<GpuInfo>()
    val gpuInfo: LiveData<GpuInfo> = _gpuInfo

    private val _memoryInfo = MutableLiveData<MemoryInfo>()
    val memoryInfo: LiveData<MemoryInfo> = _memoryInfo

    private val _modelInfo = MutableLiveData<ModelInfo>()
    val modelInfo: LiveData<ModelInfo> = _modelInfo

    private val handler = Handler(Looper.getMainLooper())
    private var isPolling = false
    private var lastCpuStats: Array<LongArray>? = null

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!isPolling) return
            refreshAll()
            handler.postDelayed(this, 500)
        }
    }

    fun startPolling() {
        if (isPolling) return
        isPolling = true
        handler.post(pollRunnable)
    }

    fun stopPolling() {
        isPolling = false
        handler.removeCallbacks(pollRunnable)
    }

    private fun refreshAll() {
        _deviceInfo.postValue(readDeviceInfo())
        _cpuInfo.postValue(readCpuInfo())
        _gpuInfo.postValue(readGpuInfo())
        _memoryInfo.postValue(readMemoryInfo())
        _modelInfo.postValue(readModelInfo())
    }

    private fun readDeviceInfo(): DeviceInfo {
        val totalRam = try {
            // Read from /proc/meminfo
            val reader = BufferedReader(FileReader("/proc/meminfo"))
            val line = reader.readLine()
            reader.close()
            val memTotalKB = line.filter { it.isDigit() }.toLongOrNull() ?: 0L
            memTotalKB / 1024
        } catch (e: Exception) { 0L }

        return DeviceInfo(
            deviceModel = Build.MODEL ?: "Unknown",
            chipset = readChipset(),
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
            totalRamMB = totalRam
        )
    }

    private fun readChipset(): String {
        return try {
            val reader = BufferedReader(FileReader("/proc/cpuinfo"))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.contains("Hardware", ignoreCase = true)) {
                    reader.close()
                    return line!!.substringAfter(":").trim()
                }
            }
            reader.close()
            Build.HARDWARE ?: "Unknown"
        } catch (e: Exception) {
            Build.HARDWARE ?: "Unknown"
        }
    }

    private fun readCpuInfo(): CpuInfo {
        val coreCount = Runtime.getRuntime().availableProcessors()
        val freqs = readCpuFreqs(coreCount)
        val governors = readCpuGovernors(coreCount)
        val usage = readCpuUsage(coreCount)
        val thermal = readThermalTemp()

        return CpuInfo(
            coreCount = coreCount,
            freqsMHz = freqs.joinToString(" ") { "${it / 1000}" },
            governor = governors.firstOrNull() ?: "unknown",
            usagePercent = usage.joinToString(" ") { "$it%" },
            thermalTempC = thermal
        )
    }

    private fun readCpuFreqs(coreCount: Int): List<Long> {
        val freqs = mutableListOf<Long>()
        for (i in 0 until coreCount) {
            try {
                val reader = BufferedReader(FileReader("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq"))
                val freq = reader.readLine().trim().toLongOrNull() ?: 0L
                reader.close()
                freqs.add(freq)
            } catch (e: Exception) {
                freqs.add(0L)
            }
        }
        return freqs
    }

    private fun readCpuGovernors(coreCount: Int): List<String> {
        val governors = mutableListOf<String>()
        for (i in 0 until coreCount) {
            try {
                val reader = BufferedReader(FileReader("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_governor"))
                governors.add(reader.readLine().trim())
                reader.close()
            } catch (e: Exception) {
                governors.add("unknown")
            }
        }
        return governors
    }

    private fun readCpuUsage(coreCount: Int): List<Int> {
        return try {
            val reader = BufferedReader(FileReader("/proc/stat"))
            val usages = mutableListOf<Int>()
            // Skip aggregate "cpu" line
            reader.readLine()
            for (i in 0 until coreCount) {
                val line = reader.readLine() ?: break
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 5) {
                    val user = parts[1].toLongOrNull() ?: 0
                    val nice = parts[2].toLongOrNull() ?: 0
                    val system = parts[3].toLongOrNull() ?: 0
                    val idle = parts[4].toLongOrNull() ?: 0
                    val total = user + nice + system + idle
                    val active = user + nice + system
                    val percent = if (total > 0) ((active * 100) / total).toInt() else 0
                    usages.add(percent)
                } else {
                    usages.add(0)
                }
            }
            reader.close()
            usages
        } catch (e: Exception) {
            List(coreCount) { 0 }
        }
    }

    private fun readThermalTemp(): Int {
        return try {
            val thermalDir = File("/sys/class/thermal")
            val zones = thermalDir.listFiles { _, name -> name.startsWith("thermal_zone") } ?: return 0
            var maxTemp = 0
            for (zone in zones) {
                try {
                    val tempFile = File(zone, "temp")
                    if (tempFile.exists()) {
                        val temp = tempFile.readText().trim().toIntOrNull() ?: 0
                        if (temp > maxTemp) maxTemp = temp
                    }
                } catch (_: Exception) {}
            }
            if (maxTemp > 1000) maxTemp / 1000 else maxTemp
        } catch (e: Exception) {
            0
        }
    }

    private fun readGpuInfo(): GpuInfo {
        val gpuType = readGpuType()
        val (util, freq) = readGpuStats()
        return GpuInfo(
            gpuType = gpuType,
            utilizationPercent = util,
            frequencyMHz = freq
        )
    }

    private fun readGpuType(): String {
        // Try common GPU sysfs paths
        val gpuPaths = listOf(
            "/sys/class/kgsl/kgsl-3d0/gpu_model",
            "/sys/class/misc/mali0/device/gpuinfo/gpu_name",
            "/sys/class/gpufreq/gpufreq/gpu_type",
            "/sys/class/drm/card0/device/gpu_type"
        )
        for (path in gpuPaths) {
            try {
                val file = File(path)
                if (file.exists()) {
                    return file.readText().trim()
                }
            } catch (_: Exception) {}
        }
        return "Unknown"
    }

    private fun readGpuStats(): Pair<Int, String> {
        // Try to read GPU utilization and frequency
        var util = 0
        var freq = "N/A"
        try {
            val utilFile = File("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage")
            if (utilFile.exists()) {
                util = utilFile.readText().trim().filter { it.isDigit() }.toIntOrNull() ?: 0
            }
        } catch (_: Exception) {}
        try {
            val freqFile = File("/sys/class/kgsl/kgsl-3d0/gpuclk")
            if (freqFile.exists()) {
                val freqHz = freqFile.readText().trim().toLongOrNull() ?: 0
                freq = if (freqHz > 1000000) "${freqHz / 1000000}MHz" else "${freqHz / 1000}MHz"
            }
        } catch (_: Exception) {}
        return Pair(util, freq)
    }

    private fun readMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val appHeapUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val appHeapMax = runtime.maxMemory() / (1024 * 1024)
        val nativeHeap = android.os.Debug.getNativeHeapAllocatedSize() / (1024 * 1024)

        val (ramUsedGB, ramTotalGB) = try {
            val reader = BufferedReader(FileReader("/proc/meminfo"))
            var totalKB = 0L
            var availableKB = 0L
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.startsWith("MemTotal:")) totalKB = line!!.filter { c -> c.isDigit() }.toLongOrNull() ?: 0L
                if (line!!.startsWith("MemAvailable:")) availableKB = line!!.filter { c -> c.isDigit() }.toLongOrNull() ?: 0L
            }
            reader.close()
            val totalGB = totalKB / 1048576f
            val usedGB = (totalKB - availableKB) / 1048576f
            Pair(usedGB, totalGB)
        } catch (e: Exception) {
            Pair(0f, 0f)
        }

        return MemoryInfo(
            appHeapUsedMB = appHeapUsed,
            appHeapMaxMB = appHeapMax,
            nativeHeapMB = nativeHeap,
            deviceRamUsedGB = ramUsedGB,
            deviceRamTotalGB = ramTotalGB
        )
    }

    private fun readModelInfo(): ModelInfo {
        val model = MainActivity.sharedChatViewModel?.qwenModelInstance
        val isGenerating = model?.let {
            try {
                val field = it::class.java.getDeclaredField("generating")
                field.isAccessible = true
                (field.get(it) as? AtomicBoolean)?.get() ?: false
            } catch (e: Exception) { false }
        } ?: false

        val kvCacheUsed = try { LlamaEngine.nativeGetCachePosition() } catch (e: Exception) { 0 }
        val kvCacheTotal = model?.metadata?.nCtx ?: 0

        return ModelInfo(
            generationState = if (isGenerating) "Generating" else "Idle",
            kvCacheUsed = kvCacheUsed,
            kvCacheTotal = kvCacheTotal,
            decodeThreads = LlamaEngine.nativeGetDecodeThreads(),
            batchThreads = LlamaEngine.nativeGetBatchThreads(),
            batchSize = LlamaEngine.nativeGetBatchSize()
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
