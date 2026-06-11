package com.example.qwen3chat

import android.content.Context
import java.io.File

class ModelPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("model_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SELECTED_MODEL = "selected_model_key"
        private const val KEY_FIRST_LAUNCH_DONE = "first_launch_done"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_MAX_TOKENS = "max_tokens"
        private const val KEY_GPU_ENABLED = "gpu_enabled"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val DEFAULT_SYSTEM_PROMPT = "You are a helpful AI assistant."
    }

    fun getSelectedModelKey(): String? = prefs.getString(KEY_SELECTED_MODEL, null)

    fun saveSelectedModelKey(key: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL, key).apply()
    }

    fun isFirstLaunchDone(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH_DONE, false)

    fun markFirstLaunchDone() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH_DONE, true).apply()
    }

    fun getTemperature(): Float = prefs.getFloat(KEY_TEMPERATURE, 0.7f)

    fun saveTemperature(value: Float) = prefs.edit().putFloat(KEY_TEMPERATURE, value).apply()

    fun getMaxTokens(): Int = prefs.getInt(KEY_MAX_TOKENS, 512)

    fun saveMaxTokens(value: Int) = prefs.edit().putInt(KEY_MAX_TOKENS, value).apply()

    fun isGpuEnabled(): Boolean = prefs.getBoolean(KEY_GPU_ENABLED, true)

    fun saveGpuEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_GPU_ENABLED, value).apply()

    fun getSystemPrompt(): String = prefs.getString(KEY_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT

    fun saveSystemPrompt(prompt: String) = prefs.edit().putString(KEY_SYSTEM_PROMPT, prompt).apply()

    fun resetSystemPrompt() = prefs.edit().remove(KEY_SYSTEM_PROMPT).apply()

    fun migrateFromV1(context: Context): Boolean {
        if (getSelectedModelKey() != null) return false
        val legacyFile = File(context.filesDir, ModelMetadata.LEGACY_FILENAME)
        if (legacyFile.exists() && legacyFile.length() > 0) {
            saveSelectedModelKey(ModelMetadata.LEGACY_MODEL_KEY)
            markFirstLaunchDone()
            return true
        }
        return false
    }
}
