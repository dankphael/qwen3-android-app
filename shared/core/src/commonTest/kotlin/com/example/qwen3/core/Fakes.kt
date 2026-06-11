package com.example.qwen3.core

/** Minimal in-memory fakes for testing pure orchestration logic. */
class FakeEngine(private val tokens: List<String> = emptyList()) : InferenceEngine {
    private var idx = 0
    override fun init() {}
    override fun loadModel(path: String, nGpuLayers: Int): Boolean = true
    override fun createContext(nCtx: Int, deviceTier: Int): Boolean = true
    override fun evaluatePrompt(prompt: String, startPos: Int): Int = 1
    override fun cachePosition(): Int = idx
    override fun resetCache() { idx = 0 }
    override fun detectGpuBackend(): String = "cpu"
    override fun startGeneration(temperature: Float) { idx = 0 }
    override fun nextToken(): String = if (idx < tokens.size) tokens[idx++] else ""
    override fun endGeneration() {}
    override fun cancel() {}
    override fun freeContext() {}
    override fun freeModel() {}
    override fun cleanup() {}
}

class FakePaths : PathProvider {
    override fun modelsDir(): String = "/tmp/models"
    override fun notesDir(): String = "/tmp/notes"
    override fun databasePath(): String = "/tmp/qwen3.db"
}
