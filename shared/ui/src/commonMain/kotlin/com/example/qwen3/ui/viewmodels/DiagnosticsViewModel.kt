package com.example.qwen3.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwen3.core.DeviceProbe
import com.example.qwen3.core.InferenceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DiagnosticsViewModel(
    private val deviceProbe: DeviceProbe? = null,
    private val inferenceEngine: InferenceEngine? = null,
) : ViewModel() {
    private val _deviceInfo = MutableStateFlow(DeviceInfoState())
    val deviceInfo: StateFlow<DeviceInfoState> = _deviceInfo

    private val _modelInfo = MutableStateFlow(ModelInfoState())
    val modelInfo: StateFlow<ModelInfoState> = _modelInfo

    fun loadDiagnostics() {
        viewModelScope.launch {
            try {
                deviceProbe?.let { probe ->
                    _deviceInfo.value = DeviceInfoState(
                        ramMb = "${probe.totalRamMb()} MB",
                        cores = probe.cpuCoreCount(),
                        tier = probe.deviceTier().name,
                        gpuBackend = inferenceEngine?.detectGpuBackend() ?: "cpu",
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

data class DeviceInfoState(
    val ramMb: String = "N/A",
    val cores: Int = 0,
    val tier: String = "UNKNOWN",
    val gpuBackend: String = "cpu",
)

data class ModelInfoState(
    val currentModel: String = "None",
    val contextSize: Int = 0,
    val decodeThreads: Int = 0,
    val batchThreads: Int = 0,
    val batchSize: Int = 0,
)
