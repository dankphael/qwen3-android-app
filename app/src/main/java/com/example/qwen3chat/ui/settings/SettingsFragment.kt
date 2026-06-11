package com.example.qwen3chat.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.qwen3chat.R
import com.example.qwen3chat.DeviceCapability
import com.example.qwen3chat.ModelPreferences
import com.example.qwen3chat.ModelSelectionActivity
import com.example.qwen3chat.databinding.FragmentSettingsBinding
import java.text.DecimalFormat

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var modelPreferences: ModelPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        modelPreferences = ModelPreferences(requireContext())

        setupTemperatureSlider()
        setupMaxTokensSlider()
        setupChangeModelButton()
        setupDiagnosticsButton()
        setupDeviceInfo()
        setupAppVersion()
    }

    private fun setupTemperatureSlider() {
        val currentTemp = modelPreferences.getTemperature()
        binding.sliderTemperature.value = currentTemp
        binding.valueTemperature.text = DecimalFormat("0.0").format(currentTemp.toDouble())

        binding.sliderTemperature.addOnChangeListener { _, value, _ ->
            val formatted = DecimalFormat("0.0").format(value.toDouble())
            binding.valueTemperature.text = formatted
        }

        binding.sliderTemperature.addOnSliderTouchListener(object :
            com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                modelPreferences.saveTemperature(slider.value)
            }
        })
    }

    private fun setupMaxTokensSlider() {
        val currentMaxTokens = modelPreferences.getMaxTokens()
        binding.sliderMaxTokens.value = currentMaxTokens.toFloat()
        binding.valueMaxTokens.text = currentMaxTokens.toString()

        binding.sliderMaxTokens.addOnChangeListener { _, value, _ ->
            binding.valueMaxTokens.text = value.toInt().toString()
        }

        binding.sliderMaxTokens.addOnSliderTouchListener(object :
            com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                modelPreferences.saveMaxTokens(slider.value.toInt())
            }
        })
    }

    private fun setupChangeModelButton() {
        binding.buttonChangeModel.setOnClickListener {
            val intent = Intent(requireContext(), ModelSelectionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupDiagnosticsButton() {
        binding.buttonDiagnostics.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_diagnostics)
        }
    }

    private fun setupDeviceInfo() {
        val deviceCapability = DeviceCapability(requireContext())
        val tierLabel = when (deviceCapability.tier) {
            DeviceCapability.Tier.LOW -> "Basic"
            DeviceCapability.Tier.MID -> "Standard"
            DeviceCapability.Tier.HIGH -> "Performance"
        }
        binding.textDeviceInfo.text =
            "Device: ${deviceCapability.ramMiB} MB RAM, ${deviceCapability.cpuCores} cores\nTier: $tierLabel"
    }

    private fun setupAppVersion() {
        val versionName = try {
            val packageInfo = requireContext().packageManager.getPackageInfo(
                requireContext().packageName, 0
            )
            "Version ${packageInfo.versionName}"
        } catch (e: Exception) {
            "Version unknown"
        }
        binding.textAppVersion.text = versionName
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
