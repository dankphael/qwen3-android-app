package com.example.qwen3.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * iOS implementation of ModelDownloader using URLSession background download.
 *
 * Advantages over foreground download:
 * - Survives backgrounding (OS manages download)
 * - Better battery efficiency
 * - Can resume even after app termination
 *
 * Limitations vs Android:
 * - Cannot track real-time progress (updated periodically)
 * - No direct control over pause/resume (OS manages)
 *
 * Implementation TODOs:
 * - Wire URLSessionDownloadDelegate to observe progress
 * - Store download task identifiers for resume
 * - Restore background downloads on app launch
 */
class IosModelDownloader(
    private val pathProvider: PathProvider,
) : ModelDownloader {
    private val modelDir = pathProvider.modelsDir()
    private val _downloadState = MutableStateFlow(DownloadState.IDLE)
    override val downloadState: StateFlow<DownloadState> = _downloadState

    private val _progress = MutableStateFlow(0)
    override val progress: StateFlow<Int> = _progress

    private val _speed = MutableStateFlow(0L)
    override val speed: StateFlow<Long> = _speed

    private val _timeRemaining = MutableStateFlow(0L)
    override val timeRemaining: StateFlow<Long> = _timeRemaining

    override suspend fun downloadModel(metadata: ModelMetadata): Boolean {
        if (isModelAvailable(metadata)) {
            _downloadState.value = DownloadState.COMPLETE
            _progress.value = 100
            return true
        }

        return try {
            _downloadState.value = DownloadState.DOWNLOADING
            _progress.value = 0

            // TODO(ios): Use URLSession.shared.downloadTask(from: URL) to download
            // - Set background configuration: URLSessionConfiguration.background
            // - Store task identifier for resume after app termination
            // - Implement URLSessionDownloadDelegate to track progress
            // - Save file to modelDir + metadata.filename
            // - Verify SHA256 on completion

            _downloadState.value = DownloadState.COMPLETE
            _progress.value = 100
            true
        } catch (e: Exception) {
            _downloadState.value = DownloadState.ERROR
            e.printStackTrace()
            false
        }
    }

    override suspend fun cancelDownload() {
        _downloadState.value = DownloadState.IDLE
        // TODO(ios): Cancel URLSession download task
    }

    override suspend fun pauseDownload() {
        _downloadState.value = DownloadState.PAUSED
        // TODO(ios): URLSession handles pause/resume automatically via Reachability
    }

    override suspend fun resumeDownload(): Boolean {
        if (_downloadState.value == DownloadState.PAUSED) {
            _downloadState.value = DownloadState.DOWNLOADING
            // TODO(ios): Resume by creating new downloadTask with resumeData
            return true
        }
        return false
    }

    override suspend fun isModelAvailable(metadata: ModelMetadata): Boolean {
        val file = File(modelDir, metadata.filename)
        return file.exists() && file.length() == metadata.fileSizeBytes
    }

    override suspend fun verifyModel(metadata: ModelMetadata): Boolean {
        if (metadata.sha256 == null) return true

        val file = File(modelDir, metadata.filename)
        if (!file.exists()) return false

        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val computed = digest.digest().joinToString("") { "%02x".format(it) }
            computed.equals(metadata.sha256, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteModel(metadata: ModelMetadata) {
        val file = File(modelDir, metadata.filename)
        file.delete()
    }

    override suspend fun availableDiskSpace(): Long {
        return try {
            // TODO(ios): Use FileManager.default.attributesOfFileSystemForPath
            0L
        } catch (e: Exception) {
            0L
        }
    }
}
