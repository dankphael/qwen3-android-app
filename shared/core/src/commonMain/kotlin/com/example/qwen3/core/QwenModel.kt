package com.example.qwen3.core

import kotlinx.atomicfu.atomic

/**
 * Orchestrates inference for a single loaded model: builds ChatML prompts
 * incrementally (only evaluating new messages since the last turn), runs the
 * pull-based streaming loop, and detects context overflow.
 *
 * Lifted from the Android QwenModel; platform dependencies (Context/filesDir/
 * SharedPreferences) are replaced by injected [engine], [paths], and [systemPrompt].
 */
class QwenModel(
    val metadata: ModelMetadata,
    private val engine: InferenceEngine,
    private val paths: PathProvider,
    private val systemPromptProvider: () -> String,
) {
    private val generating = atomic(false)
    private var cachedMessageCount = 0
    private var loaded = false

    val isReady: Boolean get() = loaded

    val modelPath: String
        get() = paths.modelsDir().trimEnd('/') + "/" + metadata.filename

    fun load(tierOrdinal: Int = 1, nGpuLayers: Int = 0): Boolean {
        engine.init()
        if (!engine.loadModel(modelPath, nGpuLayers)) return false
        if (!engine.createContext(metadata.nCtx, tierOrdinal)) {
            engine.freeModel()
            return false
        }
        cachedMessageCount = 0
        loaded = true
        return true
    }

    /**
     * Generate a streaming response for [messages]. Emits each token text via
     * [onToken]. Returns the full raw response (including any <think> block).
     */
    fun generateStreaming(messages: List<ChatMessage>, onToken: (String) -> Unit): String {
        if (!loaded) return "[Error: model not loaded]"
        if (!generating.compareAndSet(expect = false, update = true)) {
            return "[Error: generation already in progress]"
        }
        try {
            val isFirstTurn = cachedMessageCount == 0
            val newMessages = if (isFirstTurn) messages else messages.drop(cachedMessageCount)
            val prompt = buildPrompt(newMessages, isFirstTurn)

            val cachedPos = engine.cachePosition()
            val evaluated =
                if (cachedPos > 0 && !isFirstTurn) engine.evaluatePrompt(prompt, cachedPos)
                else engine.evaluatePrompt(prompt)
            if (evaluated < 0) return "[Error: failed to evaluate prompt]"

            val totalCached = engine.cachePosition()
            if (totalCached >= metadata.nCtx) {
                return "[Error: context length exceeded. Please start a new chat.]"
            }

            engine.startGeneration(metadata.temperature)
            val sb = StringBuilder()
            try {
                while (generating.value) {
                    val token = engine.nextToken()
                    if (token.isEmpty()) break
                    sb.append(token)
                    onToken(token)
                }
            } finally {
                engine.endGeneration()
            }
            cachedMessageCount = messages.size + 1 // +1 for the assistant turn just produced
            return sb.toString()
        } finally {
            generating.value = false
        }
    }

    fun cancel() {
        generating.value = false
        engine.cancel()
    }

    fun resetConversation() {
        engine.resetCache()
        cachedMessageCount = 0
    }

    fun contextUsage(): Pair<Int, Int> = engine.cachePosition() to metadata.nCtx

    fun unload() {
        if (!loaded) return
        engine.freeContext()
        engine.freeModel()
        loaded = false
    }

    /** Build a ChatML prompt. The system prompt is prepended only on the first turn. */
    internal fun buildPrompt(messages: List<ChatMessage>, isFirstTurn: Boolean): String {
        val sb = StringBuilder()
        if (isFirstTurn) {
            sb.append("<|im_start|>system\n").append(systemPromptProvider()).append("<|im_end|>\n")
        }
        for (msg in messages) {
            val role = if (msg.isUser) "user" else "assistant"
            sb.append("<|im_start|>").append(role).append("\n")
                .append(msg.content).append("<|im_end|>\n")
        }
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }
}
