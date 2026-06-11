package com.example.qwen3.core

/**
 * JNI declarations for the native llama.cpp bridge (llama-jni.cpp, built by the
 * androidApp CMake). Ported from the original Android app; the native library is
 * loaded by the app process.
 */
object LlamaEngine {
    init {
        System.loadLibrary("llama-jni")
    }

    external fun nativeInit()
    external fun nativeLoadModel(path: String): Long
    external fun nativeLoadModelGpu(path: String, nGpuLayers: Int): Long
    external fun nativeCreateContext(modelPtr: Long, nCtx: Int, deviceTier: Int): Long
    external fun nativeEvaluatePrompt(ctxPtr: Long, prompt: String): Int
    external fun nativeEvaluatePromptAt(ctxPtr: Long, prompt: String, startPos: Int): Int
    external fun nativeGetCachePosition(): Int
    external fun nativeResetCache(ctxPtr: Long)
    external fun nativeDetectGpuBackend(): String
    external fun nativeStartGeneration(ctxPtr: Long, temperature: Float)
    external fun nativeGenerateNextToken(ctxPtr: Long): String
    external fun nativeEndGeneration(ctxPtr: Long)
    external fun nativeCancel()
    external fun nativeFreeContext(ctxPtr: Long)
    external fun nativeFreeModel(modelPtr: Long)
    external fun nativeCleanup()
}
