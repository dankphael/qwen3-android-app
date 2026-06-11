package com.example.qwen3chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ModelSelectionActivity : AppCompatActivity() {

    private lateinit var deviceCapability: DeviceCapability
    private lateinit var availableModels: List<ModelMetadata>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_selection)

        deviceCapability = DeviceCapability(this)
        availableModels = deviceCapability.availableModels

        // Request notification permission on Android 13+ before download starts
        requestNotificationPermission()

        setupDeviceInfo()
        setupModelList()
        setupDownloadButton()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    private fun setupDeviceInfo() {
        val tierLabel = when (deviceCapability.tier) {
            DeviceCapability.Tier.LOW -> "Basic"
            DeviceCapability.Tier.MID -> "Standard"
            DeviceCapability.Tier.HIGH -> "Performance"
        }
        val infoText = "Your device: ${deviceCapability.ramMiB}MB RAM, ${deviceCapability.cpuCores} cores\nTier: $tierLabel"
        findViewById<TextView>(R.id.textViewDeviceInfo).text = infoText
    }

    private fun setupModelList() {
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupModels)
        radioGroup.removeAllViews()

        val onSurfaceColor = ContextCompat.getColor(this, R.color.on_surface)
        val onSurfaceVariantColor = ContextCompat.getColor(this, R.color.on_surface_variant)

        availableModels.forEachIndexed { index, model ->
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 16, 0, 16)
                layoutParams = RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT
                )
            }

            // Radio button with model name
            val radio = RadioButton(this).apply {
                id = View.generateViewId()
                text = model.displayName
                textSize = 18f
                setTextColor(onSurfaceColor)
                isChecked = (index == availableModels.size - 1) // Pre-select largest available
                tag = model.key
                buttonTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this@ModelSelectionActivity, R.color.primary)
                )
            }

            // Model details line
            val sizeGB = String.format("%.1f", model.fileSizeBytes / (1024.0 * 1024 * 1024))
            val details = TextView(this).apply {
                text = "$sizeGB GB  |  ${model.description}  |  Context: ${model.nCtx} tokens"
                textSize = 13f
                setTextColor(onSurfaceVariantColor)
                setPadding(48, 4, 0, 0) // indent to align with radio text
            }

            container.addView(radio)
            container.addView(details)
            radioGroup.addView(container)

            // Add divider (except after last)
            if (index < availableModels.size - 1) {
                val divider = View(this).apply {
                    setBackgroundColor(ContextCompat.getColor(this@ModelSelectionActivity, R.color.outline_variant))
                    alpha = 0.5f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                    )
                }
                radioGroup.addView(divider)
            }
        }
    }

    private fun setupDownloadButton() {
        val button = findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonDownload)
        button.setOnClickListener {
            val radioGroup = findViewById<RadioGroup>(R.id.radioGroupModels)
            val selectedId = radioGroup.findViewById<RadioButton>(radioGroup.checkedRadioButtonId)

            if (selectedId == null) {
                return@setOnClickListener
            }

            val modelKey = selectedId.tag as String
            val selectedModel = ModelMetadata.findByKey(modelKey)

            // Check compatibility warnings
            val warnings = mutableListOf<String>()

            // Check if model meets minimum requirements
            if (deviceCapability.ramMiB < selectedModel.minRamMiB) {
                warnings.add("Warning: Model requires ${selectedModel.minRamMiB}MB RAM, device has ${deviceCapability.ramMiB}MB")
            }
            if (deviceCapability.cpuCores < selectedModel.minCores) {
                warnings.add("Warning: Model requires ${selectedModel.minCores} cores, device has ${deviceCapability.cpuCores}")
            }

            // Show warnings as toasts
            for (warning in warnings) {
                android.widget.Toast.makeText(this, warning, android.widget.Toast.LENGTH_LONG).show()
            }

            // Persist selection
            ModelPreferences(this).apply {
                saveSelectedModelKey(modelKey)
                markFirstLaunchDone()
            }

            // Return to MainActivity with selected model
            val resultIntent = Intent().apply {
                putExtra(EXTRA_SELECTED_MODEL_KEY, modelKey)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    companion object {
        const val EXTRA_SELECTED_MODEL_KEY = "selected_model_key"
        const val REQUEST_CODE = 1001
        private const val REQUEST_NOTIFICATION_PERMISSION = 2001
    }
}
