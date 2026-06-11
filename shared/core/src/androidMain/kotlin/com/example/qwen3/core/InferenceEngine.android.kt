package com.example.qwen3.core

/**
 * Android inference engine: delegates to the JNI bridge (llama-jni.cpp) via
 * [LlamaEngine]. Holds the raw model/context pointers as Long handles, mirroring
 * the proven Android path with zero C++ rewrite.
 */
class AndroidInferenceEngine : InferenceEngine {
    private var modelPtr: Long = 0
    private var ctxPtr: Long = 0

    override fun init() {
        LlamaEngine.nativeInit()
    }

    override fun loadModel(path: String, nGpuLayers: Int): Boolean {
        modelPtr = if (nGpuLayers > 0) {
            LlamaEngine.nativeLoadModelGpu(path, nGpuLayers)
        } else {
            LlamaEngine.nativeLoadModel(path)
        }
        return modelPtr != 0L
    }

    override fun createContext(nCtx: Int, deviceTier: Int): Boolean {
        if (modelPtr == 0L) return false
        ctxPtr = LlamaEngine.nativeCreateContext(modelPtr, nCtx, deviceTier)
        return ctxPtr != 0L
    }

    override fun evaluatePrompt(prompt: String, startPos: Int): Int {
        if (ctxPtr == 0L) return -1
        return if (startPos >= 0) LlamaEngine.nativeEvaluatePromptAt(ctxPtr, prompt, startPos)
        else LlamaEngine.nativeEvaluatePrompt(ctxPtr, prompt)
    }

    override fun cachePosition(): Int = LlamaEngine.nativeGetCachePosition()

    override fun resetCache() {
        if (ctxPtr != 0L) LlamaEngine.nativeResetCache(ctxPtr)
    }

    override fun detectGpuBackend(): String = LlamaEngine.nativeDetectGpuBackend()

    override fun startGeneration(temperature: Float) {
        if (ctxPtr != 0L) LlamaEngine.nativeStartGeneration(ctxPtr, temperature)
    }

    override fun nextToken(): String =
        if (ctxPtr != 0L) LlamaEngine.nativeGenerateNextToken(ctxPtr) else ""

    override fun endGeneration() {
        if (ctxPtr != 0L) LlamaEngine.nativeEndGeneration(ctxPtr)
    }

    override fun cancel() {
        LlamaEngine.nativeCancel()
    }

    override fun freeContext() {
        if (ctxPtr != 0L) {
            LlamaEngine.nativeFreeContext(ctxPtr)
            ctxPtr = 0
        }
    }

    override fun freeModel() {
        if (modelPtr != 0L) {
            LlamaEngine.nativeFreeModel(modelPtr)
            modelPtr = 0
        }
    }

    override fun cleanup() {
        freeContext()
        freeModel()
        LlamaEngine.nativeCleanup()
    }
}
