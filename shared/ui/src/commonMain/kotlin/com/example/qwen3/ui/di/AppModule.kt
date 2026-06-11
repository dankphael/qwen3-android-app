package com.example.qwen3.ui.di

import com.example.qwen3.core.ChatRepository
import com.example.qwen3.core.DatabaseDriverFactory
import com.example.qwen3.core.DeviceProbe
import com.example.qwen3.core.InferenceEngine
import com.example.qwen3.core.ModelPreferences
import com.example.qwen3.core.PathProvider
import com.example.qwen3.ui.viewmodels.ChatDetailViewModel
import com.example.qwen3.ui.viewmodels.ChatListViewModel
import com.example.qwen3.ui.viewmodels.DiagnosticsViewModel
import com.example.qwen3.ui.viewmodels.ModelSelectionViewModel
import com.example.qwen3.ui.viewmodels.NotesViewModel
import com.example.qwen3.ui.viewmodels.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appUiModule(): Module = module {
    // ViewModels
    viewModel { ChatListViewModel(get()) }
    viewModel { (chatId: String) -> ChatDetailViewModel(chatId, get(), null) }
    viewModel { ModelSelectionViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { DiagnosticsViewModel(get(), get()) }
    viewModel { NotesViewModel() }

    // These should be provided by the core module or platform-specific module
    // For now, they are expected to be injected from outside
}
