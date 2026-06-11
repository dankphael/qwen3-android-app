package com.example.qwen3.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwen3.core.ModelPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val modelPreferences: ModelPreferences? = null,
) : ViewModel() {
    private val _temperature = MutableStateFlow(0.7f)
    val temperature: StateFlow<Float> = _temperature

    private val _maxTokens = MutableStateFlow(512)
    val maxTokens: StateFlow<Int> = _maxTokens

    private val _gpuEnabled = MutableStateFlow(false)
    val gpuEnabled: StateFlow<Boolean> = _gpuEnabled

    private val _systemPrompt = MutableStateFlow("You are a helpful AI assistant.")
    val systemPrompt: StateFlow<String> = _systemPrompt

    init {
        viewModelScope.launch {
            _temperature.value = modelPreferences?.getTemperature() ?: 0.7f
            _maxTokens.value = modelPreferences?.getMaxTokens() ?: 512
            _gpuEnabled.value = modelPreferences?.isGpuEnabled() ?: false
            _systemPrompt.value = modelPreferences?.getSystemPrompt() ?: "You are a helpful AI assistant."
        }
    }

    fun setTemperature(value: Float) {
        _temperature.value = value
        viewModelScope.launch {
            modelPreferences?.saveTemperature(value)
        }
    }

    fun setMaxTokens(value: Int) {
        _maxTokens.value = value
        viewModelScope.launch {
            modelPreferences?.saveMaxTokens(value)
        }
    }

    fun setGpuEnabled(enabled: Boolean) {
        _gpuEnabled.value = enabled
        viewModelScope.launch {
            modelPreferences?.saveGpuEnabled(enabled)
        }
    }

    fun setSystemPrompt(prompt: String) {
        _systemPrompt.value = prompt
        viewModelScope.launch {
            modelPreferences?.saveSystemPrompt(prompt)
        }
    }
}
