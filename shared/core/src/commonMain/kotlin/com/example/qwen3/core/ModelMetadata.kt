package com.example.qwen3.core

/**
 * Registry of available quantized Qwen GGUF models with their hardware requirements.
 * Pure data — lifted verbatim from the Android app, platform-independent.
 */
data class ModelMetadata(
    val key: String,
    val displayName: String,
    val filename: String,
    val downloadUrl: String,
    val fileSizeBytes: Long,
    val nCtx: Int,
    val minRamMiB: Long,
    val recommendedRamMiB: Long,
    val minCpuCores: Int,
    val description: String,
    val sha256: String? = null,
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val totalLayers: Int = 0,
) {
    companion object {
        val ALL: List<ModelMetadata> = listOf(
            ModelMetadata(
                key = "qwen3.5-0.8b",
                displayName = "Qwen3.5-0.8B",
                filename = "Qwen3.5-0.8B-Q4_K_M.gguf",
                downloadUrl = "https://huggingface.co/unsloth/Qwen3.5-0.8B-GGUF/resolve/main/Qwen3.5-0.8B-Q4_K_M.gguf",
                fileSizeBytes = 524_000_000L,
                nCtx = 2048,
                minRamMiB = 2500,
                recommendedRamMiB = 3500,
                minCpuCores = 4,
                description = "Smallest and fastest. Good for basic Q&A.",
                maxTokens = 1024,
                temperature = 0.7f,
                totalLayers = 28,
            ),
            ModelMetadata(
                key = "qwen3.5-2b",
                displayName = "Qwen3.5-2B",
                filename = "Qwen3.5-2B-Q4_K_M.gguf",
                downloadUrl = "https://huggingface.co/unsloth/Qwen3.5-2B-GGUF/resolve/main/Qwen3.5-2B-Q4_K_M.gguf",
                fileSizeBytes = 1_342_000_000L,
                nCtx = 4096,
                minRamMiB = 4000,
                recommendedRamMiB = 6000,
                minCpuCores = 4,
                description = "Balanced quality and speed.",
                maxTokens = 1024,
                temperature = 0.7f,
                totalLayers = 36,
            ),
            ModelMetadata(
                key = "qwen3.5-4b",
                displayName = "Qwen3.5-4B",
                filename = "Qwen3.5-4B-Q4_K_M.gguf",
                downloadUrl = "https://huggingface.co/unsloth/Qwen3.5-4B-GGUF/resolve/main/Qwen3.5-4B-Q4_K_M.gguf",
                fileSizeBytes = 2_621_000_000L,
                nCtx = 4096,
                minRamMiB = 5500,
                recommendedRamMiB = 8000,
                minCpuCores = 6,
                description = "Highest quality. Needs a capable device.",
                maxTokens = 1024,
                temperature = 0.7f,
                totalLayers = 40,
            ),
        )

        fun findByKey(key: String): ModelMetadata =
            ALL.firstOrNull { it.key == key }
                ?: throw IllegalArgumentException("Unknown model key: $key")
    }
}
