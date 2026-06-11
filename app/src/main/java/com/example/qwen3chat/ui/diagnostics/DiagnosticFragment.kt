package com.example.qwen3chat.ui.diagnostics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.qwen3chat.databinding.FragmentDiagnosticBinding

class DiagnosticFragment : Fragment() {

    private var _binding: FragmentDiagnosticBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DiagnosticViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDiagnosticBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.deviceInfo.observe(viewLifecycleOwner) { info ->
            binding.textDeviceModel.text = "Model: ${info.deviceModel}"
            binding.textChipset.text = "Chipset: ${info.chipset}"
            binding.textAndroidVersion.text = "Android ${info.androidVersion}"
            binding.textDeviceRam.text = "RAM: ${info.totalRamMB} MB"
        }

        viewModel.cpuInfo.observe(viewLifecycleOwner) { info ->
            binding.textCpuCores.text = "Cores: ${info.coreCount}"
            binding.textCpuFreqs.text = "Freqs: ${info.freqsMHz}"
            binding.textCpuGovernor.text = "Governor: ${info.governor}"
            binding.textCpuUsage.text = "Usage: ${info.usagePercent}"
            binding.textThermal.text = "Thermal: ${info.thermalTempC}°C"
        }

        viewModel.gpuInfo.observe(viewLifecycleOwner) { info ->
            binding.textGpuType.text = "Type: ${info.gpuType}"
            binding.textGpuUtil.text = "Util: ${info.utilizationPercent}%"
            binding.textGpuFreq.text = "Freq: ${info.frequencyMHz}"
        }

        viewModel.memoryInfo.observe(viewLifecycleOwner) { info ->
            binding.textAppHeap.text = "App Heap: ${info.appHeapUsedMB}MB / ${info.appHeapMaxMB}MB"
            binding.textNativeHeap.text = "Native Heap: ${info.nativeHeapMB}MB"
            binding.textRamUsage.text = "Device RAM: ${info.deviceRamUsedGB}GB / ${info.deviceRamTotalGB}GB"
        }

        viewModel.modelInfo.observe(viewLifecycleOwner) { info ->
            binding.textGenerationState.text = "Status: ${info.generationState}"
            binding.textKvCache.text = "KV Cache: ${info.kvCacheUsed}/${info.kvCacheTotal}"
            binding.textDecodeThreads.text = "Decode Threads: ${info.decodeThreads}"
            binding.textBatchThreads.text = "Batch Threads: ${info.batchThreads}"
            binding.textBatchSize.text = "Batch Size: ${info.batchSize}"
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startPolling()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPolling()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
