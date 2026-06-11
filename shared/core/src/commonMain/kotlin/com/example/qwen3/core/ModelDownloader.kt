package com.example.qwen3.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Downloads GGUF models from HuggingFace or other sources with resumable,
 * platform-optimized transport:
 * - Android: Ktor (okhttp) with Range requests in foreground service
 * - iOS: URLSession background download (survives backgrounding, uses less battery)
 *
 * Clients listen to progress, completion, and error flows.
 */
interface ModelDownloader {
    /**
     * Download state for a model: IDLE, DOWNLOADING, COMPLETE, ERROR, PAUSED
     */
    val downloadState: StateFlow<DownloadState>

    /**
     * Progress: 0..100
     */
    val progress: StateFlow<Int>

    /**
     * Speed in bytes/sec (0 if not downloading)
     */
    val speed: StateFlow<Long>

    /**
     * Estimated time remaining in seconds (0 if complete)
     */
    val timeRemaining: StateFlow<Long>

    /**
     * Start or resume download. If model already exists locally, returns immediately.
     * Downloads to modelDir + metadata.filename.
     *
     * Returns false if download fails and cannot be resumed.
     */
    suspend fun downloadModel(metadata: ModelMetadata): Boolean

    /**
     * Cancel download. On Android: deletes partial file. On iOS: uses URLSession
     * cancellation (may leave partial file for resume later).
     */
    suspend fun cancelDownload()

    /**
     * Pause download (resumable via downloadModel).
     */
    suspend fun pauseDownload()

    /**
     * Resume paused download (if not expired or corrupted).
     */
    suspend fun resumeDownload(): Boolean

    /**
     * Check if model file exists and is valid (SHA256 match or size match).
     */
    suspend fun isModelAvailable(metadata: ModelMetadata): Boolean

    /**
     * Verify SHA256 of a local model file. Returns true if match or no SHA in metadata.
     */
    suspend fun verifyModel(metadata: ModelMetadata): Boolean

    /**
     * Delete model file and cache. Frees disk space.
     */
    suspend fun deleteModel(metadata: ModelMetadata)

    /**
     * Total free disk space in bytes.
     */
    suspend fun availableDiskSpace(): Long
}

enum class DownloadState {
    IDLE,           // Not downloading
    DOWNLOADING,    // Active download
    PAUSED,         // Paused, can resume
    COMPLETE,       // Download finished successfully
    ERROR,          // Download failed
}

data class DownloadProgress(
    val state: DownloadState,
    val progress: Int,         // 0..100
    val bytesDownloaded: Long,
    val bytesTotal: Long,
    val speedBytesPerSec: Long,
    val timeRemainingSeconds: Long,
)
