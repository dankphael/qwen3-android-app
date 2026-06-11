package com.example.qwen3chat

import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.qwen3chat.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferences: ModelPreferences
    private lateinit var deviceCapability: DeviceCapability

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = ModelPreferences(this)
        deviceCapability = DeviceCapability(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        setupModelSelector()
        setupTemperatureSlider()
        setupMaxTokensSlider()
        setupClearConversations()
        setupDiagnosticsButton()
        setupDeviceInfo()
    }

    private fun setupModelSelector() {
        val currentKey = preferences.getSelectedModelKey() ?: "qwen3.5-0.8b"
        val metadata = try { ModelMetadata.findByKey(currentKey) } catch (e: Exception) { ModelMetadata.ALL[0] }
        binding.tvSelectedModel.text = "${metadata.displayName} (${metadata.description})"

        binding.btnChangeModel.setOnClickListener {
            val intent = android.content.Intent(this, ModelSelectionActivity::class.java)
            startActivityForResult(intent, REQUEST_CHANGE_MODEL)
        }
    }

    private fun setupTemperatureSlider() {
        val currentTemp = preferences.getTemperature()
        binding.seekBarTemperature.progress = ((currentTemp - 0.1f) / 1.9f * 100).toInt()
        binding.tvTemperatureValue.text = String.format("%.1f", currentTemp)
        binding.seekBarTemperature.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val temp = 0.1f + (progress / 100f) * 1.9f
                binding.tvTemperatureValue.text = String.format("%.1f", temp)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val temp = 0.1f + ((seekBar?.progress ?: 50) / 100f) * 1.9f
                preferences.saveTemperature(temp)
                Toast.makeText(this@SettingsActivity, "Temperature set to ${String.format("%.1f", temp)}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupMaxTokensSlider() {
        val currentMax = preferences.getMaxTokens()
        binding.seekBarMaxTokens.progress = ((currentMax - 64) / 960f * 100).toInt()
        binding.tvMaxTokensValue.text = currentMax.toString()
        binding.seekBarMaxTokens.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val tokens = 64 + (progress / 100f * 960).toInt()
                binding.tvMaxTokensValue.text = tokens.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val tokens = 64 + (((seekBar?.progress ?: 50) / 100f) * 960).toInt()
                preferences.saveMaxTokens(tokens)
                Toast.makeText(this@SettingsActivity, "Max tokens set to $tokens", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupClearConversations() {
        binding.btnClearConversations.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Clear All Conversations")
                .setMessage("This will permanently delete all chat history. This cannot be undone.")
                .setPositiveButton("Clear All") { _, _ ->
                    val db = com.example.qwen3chat.data.db.AppDatabase.getInstance(applicationContext)
                    kotlinx.coroutines.runBlocking {
                        db.openHelper.writableDatabase.execSQL("DELETE FROM messages")
                        db.openHelper.writableDatabase.execSQL("DELETE FROM chats")
                    }
                    Toast.makeText(this@SettingsActivity, "All conversations cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupDiagnosticsButton() {
        binding.btnDiagnostics.setOnClickListener {
            startActivity(android.content.Intent(this, DiagnosticActivity::class.java))
        }
    }

    private fun setupDeviceInfo() {
        val tierLabel = when (deviceCapability.tier) {
            DeviceCapability.Tier.LOW -> "Basic"
            DeviceCapability.Tier.MID -> "Standard"
            DeviceCapability.Tier.HIGH -> "Performance"
        }
        binding.tvDeviceInfo.text = "RAM: ${deviceCapability.ramMiB}MB | CPU: ${deviceCapability.cpuCores} cores | Tier: $tierLabel"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHANGE_MODEL && resultCode == RESULT_OK) {
            val newKey = data?.getStringExtra(ModelSelectionActivity.EXTRA_SELECTED_MODEL_KEY)
            if (newKey != null) {
                preferences.saveSelectedModelKey(newKey)
                val metadata = ModelMetadata.findByKey(newKey)
                binding.tvSelectedModel.text = "${metadata.displayName} (${metadata.description})"
                Toast.makeText(this, "Model changed. Restart the app to apply.", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CHANGE_MODEL = 3001
    }
}
