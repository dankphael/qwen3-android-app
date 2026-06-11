package com.example.qwen3.android.di

import android.content.Context
import com.example.qwen3.core.DatabaseDriverFactory
import com.example.qwen3.core.DeviceProbe
import com.example.qwen3.core.InferenceEngine
import com.example.qwen3.core.ModelPreferences
import com.example.qwen3.core.PathProvider
import com.example.qwen3.core.AndroidPathProvider
import com.example.qwen3.core.AndroidDeviceProbe
import com.example.qwen3.core.AndroidDatabaseDriverFactory
import com.example.qwen3.core.AndroidInferenceEngine
import com.example.qwen3.core.AndroidModelPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android platform-specific implementations of core interfaces.
 * Provides PathProvider, DeviceProbe, DatabaseDriverFactory, InferenceEngine, ModelPreferences.
 */
fun androidPlatformModule(): Module = module {
    single<PathProvider> { AndroidPathProvider(androidContext()) }
    single<DeviceProbe> { AndroidDeviceProbe(androidContext()) }
    single<DatabaseDriverFactory> { AndroidDatabaseDriverFactory(androidContext()) }
    single<InferenceEngine> { AndroidInferenceEngine() }
    single<ModelPreferences> { AndroidModelPreferences(androidContext()) }
}
