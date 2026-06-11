package com.example.qwen3.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwen3.core.ModelMetadata
import com.example.qwen3.core.ModelPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ModelSelectionViewModel(
    private val modelPreferences: ModelPreferences? = null,
) : ViewModel() {
    private val _availableModels = MutableStateFlow<List<ModelItemState>>(emptyList())
    val availableModels: StateFlow<List<ModelItemState>> = _availableModels

    private val _selectedModel = MutableStateFlow<String>("")
    val selectedModel: StateFlow<String> = _selectedModel

    init {
        _availableModels.value = ModelMetadata.ALL.map { model ->
            ModelItemState(
                name = model.name,
                sizeGb = model.sizeGb,
                minRamMb = model.minRamMiB,
                minCpuCores = model.minCpuCores,
            )
        }

        viewModelScope.launch {
            _selectedModel.value = modelPreferences?.getSelectedModelKey() ?: ModelMetadata.ALL.firstOrNull()?.name ?: ""
        }
    }

    fun selectModel(modelName: String) {
        viewModelScope.launch {
            _selectedModel.value = modelName
            modelPreferences?.saveSelectedModelKey(modelName)
        }
    }
}

data class ModelItemState(
    val name: String,
    val sizeGb: Double,
    val minRamMb: Int,
    val minCpuCores: Int,
)
