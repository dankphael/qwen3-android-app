package com.example.qwen3.core

import com.russhwolf.settings.Settings

/**
 * User preferences backed by multiplatform-settings.
 *  - Android: SharedPreferencesSettings
 *  - iOS: NSUserDefaultsSettings
 */
class ModelPreferences(private val settings: Settings) {

    fun getSelectedModelKey(): String? = settings.getStringOrNull(KEY_SELECTED_MODEL)
    fun saveSelectedModelKey(key: String) = settings.putString(KEY_SELECTED_MODEL, key)

    fun isFirstLaunchDone(): Boolean = settings.getBoolean(KEY_FIRST_LAUNCH_DONE, false)
    fun markFirstLaunchDone() = settings.putBoolean(KEY_FIRST_LAUNCH_DONE, true)

    fun getTemperature(): Float = settings.getFloat(KEY_TEMPERATURE, 0.7f)
    fun saveTemperature(value: Float) = settings.putFloat(KEY_TEMPERATURE, value)

    fun getMaxTokens(): Int = settings.getInt(KEY_MAX_TOKENS, 1024)
    fun saveMaxTokens(value: Int) = settings.putInt(KEY_MAX_TOKENS, value)

    fun isGpuEnabled(): Boolean = settings.getBoolean(KEY_GPU_ENABLED, true)
    fun saveGpuEnabled(value: Boolean) = settings.putBoolean(KEY_GPU_ENABLED, value)

    fun getSystemPrompt(): String = settings.getString(KEY_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT)
    fun saveSystemPrompt(prompt: String) = settings.putString(KEY_SYSTEM_PROMPT, prompt)
    fun resetSystemPrompt() = settings.remove(KEY_SYSTEM_PROMPT)

    companion object {
        const val DEFAULT_SYSTEM_PROMPT = "You are a helpful AI assistant."

        private const val KEY_SELECTED_MODEL = "selected_model_key"
        private const val KEY_FIRST_LAUNCH_DONE = "first_launch_done"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_MAX_TOKENS = "max_tokens"
        private const val KEY_GPU_ENABLED = "gpu_enabled"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
    }
}
