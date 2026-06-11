package com.example.qwen3.core.di

import com.example.qwen3.core.ChatRepository
import com.example.qwen3.core.DatabaseDriverFactory
import com.example.qwen3.core.DeviceProbe
import com.example.qwen3.core.InferenceEngine
import com.example.qwen3.core.ModelPreferences
import com.example.qwen3.core.PathProvider
import com.example.qwen3.core.QwenModel
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Core KMP module for platform-independent logic, databases, and preferences.
 * Platform-specific implementations (PathProvider, InferenceEngine, DatabaseDriverFactory,
 * DeviceProbe, ModelPreferences) are expected to be provided by platform-specific modules.
 */
fun coreModule(): Module = module {
    // ChatRepository uses DatabaseDriverFactory which is platform-specific
    single {
        ChatRepository(
            driverFactory = get(),
            ioDispatcher = Dispatchers.Default,
        )
    }

    // QwenModel orchestrates inference
    // Note: platform code should bind specific model instances, or instantiate on demand
}
