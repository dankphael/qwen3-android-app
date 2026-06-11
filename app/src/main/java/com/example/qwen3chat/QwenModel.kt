package com.example.qwen3chat

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class QwenModel(private val context: Context, val metadata: ModelMetadata) {

    companion object {
        private const val TAG = "QwenModel"
        private const val OVERFLOW_THRESHOLD = 0.9f
    }

    private var modelPtr: Long = 0
    private var ctxPtr: Long = 0
    private val generating = AtomicBoolean(false)
    private var cachedMessageCount = 0
    private var systemPrompt: String = ModelPreferences(context).getSystemPrompt()

    val isReady: Boolean get() = ctxPtr != 0L

    fun getModelDownloadUrl(): String = metadata.downloadUrl

    fun isModelDownloaded(): Boolean {
        val modelFile = File(context.filesDir, metadata.filename)
        return modelFile.exists() && modelFile.length() > 0
    }

    fun loadModel(tierOrdinal: Int = 1) {
        val modelFile = File(context.filesDir, metadata.filename)
        if (!modelFile.exists()) {
            throw IllegalStateException("Model file not found. Download it first.")
        }

        LlamaEngine.nativeInit()
        modelPtr = LlamaEngine.nativeLoadModel(modelFile.absolutePath)

        if (modelPtr == 0L) {
            throw RuntimeException("Failed to load model into memory")
        }
        Log.i(TAG, "Model loaded, ptr=$modelPtr")

        ctxPtr = LlamaEngine.nativeCreateContext(modelPtr, metadata.nCtx, tierOrdinal)
        if (ctxPtr == 0L) {
            throw RuntimeException("Failed to create inference context (n_ctx=${metadata.nCtx})")
        }
        Log.i(TAG, "Context created, ptr=$ctxPtr, n_ctx=${metadata.nCtx}, tier=$tierOrdinal")
    }

    /**
     * Load model with GPU offload via OpenCL.
     * @param tierOrdinal Device tier (0=LOW, 1=MID, 2=HIGH)
     * @param nGpuLayers Number of layers to offload to GPU (0 = CPU only)
     */
    fun loadModelGpu(tierOrdinal: Int = 1, nGpuLayers: Int = 32) {
        val modelFile = File(context.filesDir, metadata.filename)
        if (!modelFile.exists()) {
            throw IllegalStateException("Model file not found. Download it first.")
        }

        LlamaEngine.nativeInit()
        modelPtr = if (nGpuLayers > 0) {
            LlamaEngine.nativeLoadModelGpu(modelFile.absolutePath, nGpuLayers)
        } else {
            LlamaEngine.nativeLoadModel(modelFile.absolutePath)
        }

        if (modelPtr == 0L) {
            throw RuntimeException("Failed to load model into memory")
        }
        Log.i(TAG, "Model loaded (GPU: $nGpuLayers layers), ptr=$modelPtr")

        ctxPtr = LlamaEngine.nativeCreateContext(modelPtr, metadata.nCtx, tierOrdinal)
        if (ctxPtr == 0L) {
            throw RuntimeException("Failed to create inference context (n_ctx=${metadata.nCtx})")
        }
        Log.i(TAG, "Context created, ptr=$ctxPtr, n_ctx=${metadata.nCtx}, tier=$tierOrdinal")
    }

    fun generate(messages: List<ChatMessage>): String {
        if (ctxPtr == 0L) {
            return "Model not loaded."
        }

        if (!generating.compareAndSet(false, true)) {
            return "Generation already in progress."
        }

        try {
            // Only evaluate NEW messages since last call
            val isFirstTurn = (cachedMessageCount == 0)
            val newMessages = if (isFirstTurn) {
                messages // first call: evaluate everything
            } else {
                messages.drop(cachedMessageCount)
            }

            val prompt = buildPrompt(newMessages, isFirstTurn)
            val cachedPos = LlamaEngine.nativeGetCachePosition()
            val tokensEvaluated = if (cachedPos > 0 && !isFirstTurn) {
                LlamaEngine.nativeEvaluatePrompt(ctxPtr, prompt, cachedPos)
            } else {
                LlamaEngine.nativeEvaluatePrompt(ctxPtr, prompt)
            }
            if (tokensEvaluated < 0) {
                return "[Error: failed to evaluate prompt]"
            }

            // Context overflow detection
            val totalCached = LlamaEngine.nativeGetCachePosition()
            if (totalCached >= metadata.nCtx) {
                Log.w(TAG, "Context overflow: $totalCached tokens cached, n_ctx=${metadata.nCtx}")
                return "[Error: context length exceeded. The conversation is too long. Please start a new chat.]"
            }

            if (totalCached >= metadata.nCtx * OVERFLOW_THRESHOLD) {
                Log.w(TAG, "Context approaching limit: $totalCached/${metadata.nCtx} tokens " +
                        "(${(totalCached * 100 / metadata.nCtx)}%)")
            }

            // Generate response
            val result = LlamaEngine.nativeGenerate(ctxPtr, metadata.maxTokens, metadata.temperature)

            // Update cached message count after successful generation
            cachedMessageCount = messages.size

            return result
        } finally {
            generating.set(false)
        }
    }

    /**
     * Streaming generate: calls onToken(token) for each token as it's produced.
     * Must be called from a background thread (Dispatchers.IO).
     */
    fun generateStreaming(messages: List<ChatMessage>, onToken: (String) -> Unit): Int {
        if (ctxPtr == 0L) return 0

        if (!generating.compareAndSet(false, true)) return 0

        try {
            // Only evaluate NEW messages since last call
            val isFirstTurn = (cachedMessageCount == 0)
            val newMessages = if (isFirstTurn) {
                messages
            } else {
                messages.drop(cachedMessageCount)
            }

            val prompt = buildPrompt(newMessages, isFirstTurn)
            val cachedPos = LlamaEngine.nativeGetCachePosition()
            val tokensEvaluated = if (cachedPos > 0 && !isFirstTurn) {
                LlamaEngine.nativeEvaluatePrompt(ctxPtr, prompt, cachedPos)
            } else {
                LlamaEngine.nativeEvaluatePrompt(ctxPtr, prompt)
            }
            if (tokensEvaluated < 0) return 0

            val generated = LlamaEngine.generateStreaming(ctxPtr, metadata.maxTokens, metadata.temperature, onToken)

            // Update cached message count after successful generation
            cachedMessageCount = messages.size

            return generated
        } finally {
            generating.set(false)
        }
    }

    private fun buildPrompt(messages: List<ChatMessage>, isFirstTurn: Boolean = true): String {
        val sb = StringBuilder()
        if (isFirstTurn) {
            sb.append("<|im_start|>system\n$systemPrompt<|im_end|>\n")
        }
        for (msg in messages) {
            val role = if (msg.isUser) "user" else "assistant"
            sb.append("<|im_start|>$role\n${msg.content}<|im_end|>\n")
        }
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    fun resetConversation() {
        if (ctxPtr != 0L) {
            LlamaEngine.nativeResetCache(ctxPtr)
            cachedMessageCount = 0
            Log.i(TAG, "Conversation reset: KV cache cleared, message count reset")
        }
    }

    fun cancel() {
        if (generating.get()) {
            LlamaEngine.nativeCancel()
            Log.i(TAG, "Cancellation requested")
        }
    }

    fun getContextInfo(): Pair<Int, Int> {
        val cached = if (ctxPtr != 0L) LlamaEngine.nativeGetCachePosition() else 0
        return Pair(cached, metadata.nCtx)
    }

    fun close() {
        cachedMessageCount = 0

        // Wait briefly for generation to stop
        if (generating.get()) {
            LlamaEngine.nativeCancel()
            val deadline = System.currentTimeMillis() + 3000
            while (generating.get() && System.currentTimeMillis() < deadline) {
                Thread.sleep(50)
            }
        }

        if (ctxPtr != 0L) {
            LlamaEngine.nativeFreeContext(ctxPtr)
            ctxPtr = 0
        }
        if (modelPtr != 0L) {
            LlamaEngine.nativeFreeModel(modelPtr)
            modelPtr = 0
        }
        LlamaEngine.nativeCleanup()
        Log.i(TAG, "Model and context freed")
    }
}
