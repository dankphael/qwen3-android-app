package com.example.qwen3.core

/**
 * Cross-platform inference engine over llama.cpp.
 *
 * Streaming is **pull-based** ([nextToken]) so the token callback never has to
 * cross the Kotlin/Native cinterop boundary — the `onToken` lambda stays pure
 * Kotlin inside [QwenModel].
 *
 * Implementations:
 *  - Android: [com.example.qwen3.core.AndroidInferenceEngine] → JNI (llama-jni.cpp).
 *  - iOS:     [com.example.qwen3.core.IosInferenceEngine] → llama.cpp via cinterop (Metal).
 *
 * Modeled as an interface (not expect/actual) so it can be injected and faked in tests.
 */
interface InferenceEngine {
    fun init()
    fun loadModel(path: String, nGpuLayers: Int = 0): Boolean
    fun createContext(nCtx: Int, deviceTier: Int): Boolean
    fun evaluatePrompt(prompt: String, startPos: Int = -1): Int
    fun cachePosition(): Int
    fun resetCache()
    fun detectGpuBackend(): String
    fun startGeneration(temperature: Float)
    fun nextToken(): String
    fun endGeneration()
    fun cancel()
    fun freeContext()
    fun freeModel()
    fun cleanup()
}
