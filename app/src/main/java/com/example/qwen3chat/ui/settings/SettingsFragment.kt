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
        setupGpuToggle()
        setupSystemPromptEditor()
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

    private fun setupGpuToggle() {
        val isGpuEnabled = modelPreferences.isGpuEnabled()
        binding.switchGpuEnabled.isChecked = isGpuEnabled

        binding.switchGpuEnabled.setOnCheckedChangeListener { _, isChecked ->
            modelPreferences.saveGpuEnabled(isChecked)
        }
    }

    private fun setupSystemPromptEditor() {
        val currentPrompt = modelPreferences.getSystemPrompt()
        binding.textSystemPromptPreview.text = currentPrompt

        binding.buttonEditSystemPrompt.setOnClickListener {
            showSystemPromptDialog(currentPrompt)
        }
    }

    private fun showSystemPromptDialog(currentPrompt: String) {
        val input = android.widget.EditText(requireContext()).apply {
            setText(currentPrompt)
            setLines(3)
            maxLines = 5
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit System Prompt")
            .setMessage("Customize how the AI behaves (max 200 chars)")
            .setView(input)
            .setNegativeButton("Cancel") { _, _ -> }
            .setNeutralButton("Reset") { _, _ ->
                modelPreferences.resetSystemPrompt()
                binding.textSystemPromptPreview.text = ModelPreferences.DEFAULT_SYSTEM_PROMPT
            }
            .setPositiveButton("Save") { _, _ ->
                val newPrompt = input.text.toString().trim()
                if (newPrompt.isNotEmpty() && newPrompt.length <= 200) {
                    modelPreferences.saveSystemPrompt(newPrompt)
                    binding.textSystemPromptPreview.text = newPrompt
                    android.widget.Toast.makeText(requireContext(), "Prompt updated", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(requireContext(), "Prompt must be 1-200 characters", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .show()
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

        // Build available models string
        val availableModels = deviceCapability.availableModels
        val modelNames = availableModels.joinToString(", ") { model ->
            val sizeGb = String.format("%.1f", model.sizeBytes / (1024f * 1024f * 1024f))
            "${model.displayName} ($sizeGb GB)"
        }

        binding.textDeviceInfo.text =
            "Device: ${deviceCapability.ramMiB} MB RAM, ${deviceCapability.cpuCores} cores\n" +
            "Tier: $tierLabel\n" +
            "Available: $modelNames"
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
