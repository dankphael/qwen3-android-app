package com.example.qwen3chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private var qwenModel: QwenModel? = null

    /** Messages list — survives rotation. Activity manages adapter notifications. */
    val messages = mutableListOf<ChatMessage>()

    private val _isLoadingModel = MutableStateFlow(false)
    val isLoadingModel: StateFlow<Boolean> = _isLoadingModel.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    val isReady: Boolean get() = qwenModel?.isReady == true
    val hasModel: Boolean get() = qwenModel != null

    /** Expose QwenModel for sharing with ChatDetailViewModel. */
    val qwenModelInstance: QwenModel? get() = qwenModel

    /**
     * Load a model from previously downloaded file.
     * Launches on IO dispatcher internally. Calls onReady or onError when done.
     */
    fun loadModel(metadata: ModelMetadata, onReady: () -> Unit, onError: (String) -> Unit) {
        if (_isLoadingModel.value) return
        _isLoadingModel.value = true
        viewModelScope.launch {
            try {
                val model = QwenModel(getApplication(), metadata)
                val tier = DeviceCapability(getApplication()).tier.ordinal

                // Detect GPU and decide loading strategy
                val gpuBackend = try { LlamaEngine.nativeDetectGpuBackend() } catch (_: Exception) { "cpu" }
                val gpuName = try { LlamaEngine.nativeGetGpuName() } catch (_: Exception) { "Unknown" }
                val gpuMem = try { LlamaEngine.nativeGetGpuMemory() } catch (_: Exception) { 0L }
                Log.i(TAG, "GPU detection: backend=$gpuBackend, name=$gpuName, mem=${gpuMem}MB")

                withContext(Dispatchers.IO) {
                    if (gpuBackend == "opencl" && gpuMem > 512) {
                        // Offload all layers to GPU if we have enough GPU memory
                        val nGpuLayers = metadata.totalLayers  // offload everything
                        Log.i(TAG, "Loading model with OpenCL GPU offload ($nGpuLayers layers)")
                        model.loadModelGpu(tier, nGpuLayers)
                    } else {
                        Log.i(TAG, "Loading model on CPU (GPU: $gpuBackend, ${gpuMem}MB)")
                        model.loadModel(tier)
                    }
                }
                qwenModel = model
                onReady()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load model", e)
                onError(e.message ?: "Unknown error")
            } finally {
                _isLoadingModel.value = false
            }
        }
    }

    /**
     * Generate a response. Does NOT touch the messages list —
     * the caller handles parsing and adding to [messages].
     * Returns the raw model output string.
     */
    fun generate(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val model = qwenModel
        if (model == null) { onError("Model not loaded"); return }
        if (!model.isReady) { onError("Model not ready"); return }
        if (_isGenerating.value) { onError("Generation already in progress"); return }

        _isGenerating.value = true
        viewModelScope.launch {
            try {
                val raw = withContext(Dispatchers.IO) {
                    model.generate(messages)
                }
                onSuccess(raw)
            } catch (e: Exception) {
                Log.e(TAG, "Generation failed", e)
                onError(e.message ?: "Generation failed")
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /** Cancel ongoing generation. */
    fun cancelGeneration() {
        qwenModel?.cancel()
    }

    /** Unload the model, freeing native memory. */
    fun unloadModel() {
        qwenModel?.close()
        qwenModel = null
        _isGenerating.value = false
        Log.i(TAG, "Model unloaded")
    }

    override fun onCleared() {
        super.onCleared()
        qwenModel?.close()
        qwenModel = null
        Log.i(TAG, "ViewModel cleared, model closed")
    }
}