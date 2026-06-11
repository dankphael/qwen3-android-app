package com.example.qwen3chat

object LlamaEngine {

    init {
        System.loadLibrary("llama-jni")
    }

    external fun nativeInit()
    external fun nativeLoadModel(path: String): Long
    external fun nativeLoadModelGpu(path: String, nGpuLayers: Int): Long
    external fun nativeCreateContext(modelPtr: Long, nCtx: Int, deviceTier: Int): Long
    external fun nativeCreateContextLegacy(modelPtr: Long, nCtx: Int): Long
    external fun nativeEvaluatePrompt(ctxPtr: Long, prompt: String): Int
    external fun nativeEvaluatePrompt(ctxPtr: Long, prompt: String, startPos: Int): Int
    external fun nativeGenerate(ctxPtr: Long, maxTokens: Int, temperature: Float): String
    external fun nativeGetCachePosition(): Int
    external fun nativeResetCache(ctxPtr: Long)
    external fun nativeCancel()
    external fun nativeFreeContext(ctxPtr: Long)
    external fun nativeFreeModel(modelPtr: Long)
    external fun nativeCleanup()

    // Streaming generation API
    external fun nativeStartGeneration(temperature: Float)
    external fun nativeGenerateNextToken(ctxPtr: Long): String
    external fun nativeEndGeneration()

    // Diagnostic getters
    external fun nativeGetDecodeThreads(): Int
    external fun nativeGetBatchThreads(): Int
    external fun nativeGetBatchSize(): Int

    // GPU detection
    external fun nativeDetectGpuBackend(): String  // returns "opencl", "vulkan", or "cpu"
    external fun nativeGetGpuName(): String
    external fun nativeGetGpuMemory(): Long  // in MB

    /**
     * Streaming generate: calls onToken(token) for each token as it is produced.
     * Runs entirely on the calling thread — call from Dispatchers.IO.
     */
    fun generateStreaming(
        ctxPtr: Long,
        maxTokens: Int,
        temperature: Float,
        onToken: (String) -> Unit
    ): Int {
        nativeStartGeneration(temperature)
        var generated = 0
        try {
            for (i in 0 until maxTokens) {
                val token = nativeGenerateNextToken(ctxPtr)
                if (token.isEmpty()) break
                onToken(token)
                generated++
            }
        } finally {
            nativeEndGeneration()
        }
        return generated
    }
}
