package com.example.qwen3chat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class ModelDownloadService : Service() {

    companion object {
        private const val TAG = "ModelDownloadService"
        private const val CHANNEL_ID = "model_download"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START = "com.example.qwen3chat.ACTION_START_DOWNLOAD"
        const val ACTION_CANCEL = "com.example.qwen3chat.ACTION_CANCEL_DOWNLOAD"

        const val EXTRA_MODEL_KEY = "model_key"
        const val EXTRA_MODEL_URL = "model_url"
        const val EXTRA_MODEL_FILENAME = "model_filename"
        const val EXTRA_MODEL_SHA256 = "model_sha256"

        // WeakReference to avoid leaking the Activity
        var listener: WeakReference<DownloadListener>? = null

        fun isModelDownloaded(context: Context, filename: String): Boolean {
            val modelFile = File(context.filesDir, filename)
            return modelFile.exists() && modelFile.length() > 0
        }
    }

    interface DownloadListener {
        fun onProgress(downloaded: Long, total: Long)
        fun onComplete()
        fun onError(message: String)
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null
    private var isDownloading = false

    // Model metadata from Intent extras
    private var modelFilename: String = ""
    private var modelDownloadUrl: String = ""

    // Active OkHttp Call for cancellation
    private var activeCall: okhttp3.Call? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                stopDownload()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                if (!isDownloading) {
                    // Read model metadata from Intent extras
                    modelFilename = intent.getStringExtra(EXTRA_MODEL_FILENAME) ?: run {
                        Log.e(TAG, "No model filename provided")
                        stopSelf()
                        return START_NOT_STICKY
                    }
                    modelDownloadUrl = intent.getStringExtra(EXTRA_MODEL_URL) ?: run {
                        Log.e(TAG, "No model URL provided")
                        stopSelf()
                        return START_NOT_STICKY
                    }
                    startForegroundWithNotification()
                    startDownload()
                }
                return START_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Starting download...", 0, 0)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG::DownloadWakeLock").apply {
                acquire(3 * 60 * 60 * 1000L)
            }
            Log.i(TAG, "WakeLock acquired")
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
            Log.i(TAG, "WakeLock released")
        }
        wakeLock = null
    }

    private fun notifyListener(block: (DownloadListener) -> Unit) {
        listener?.get()?.let { block(it) }
    }

    private fun startDownload() {
        isDownloading = true
        serviceScope.launch {
            try {
                downloadModelInternal()
                notifyListener { it.onComplete() }
                updateNotification("Download complete! Tap to open.", 100, 100)
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                notifyListener { it.onError(e.message ?: "Unknown error") }
                updateNotification("Download failed: ${e.message}", 0, 0)
                stopSelf()
            }
        }
    }

    private suspend fun downloadModelInternal() {
        val modelFile = File(filesDir, modelFilename)
        val partialFile = File(filesDir, "$modelFilename.part")

        if (modelFile.exists() && modelFile.length() > 0) {
            Log.i(TAG, "Model already downloaded, skipping")
            return
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .build()

        var response = tryRequest(client, partialFile)

        if (response != null && response.code == 416) {
            Log.w(TAG, "Server rejected resume (416), deleting stale partial and retrying clean")
            response.close()
            partialFile.delete()
            response = tryRequest(client, partialFile)
        }

        if (response == null || !response.isSuccessful) {
            val code = response?.code ?: -1
            response?.close()
            throw RuntimeException("Download failed: HTTP $code")
        }

        val body = response.body ?: throw RuntimeException("Empty response body")
        val isResuming = response.code == 206
        val contentLength = body.contentLength()
        val resumedBytes = if (isResuming) partialFile.length() else 0L
        val totalBytes = if (contentLength > 0) contentLength + resumedBytes else -1L

        if (isResuming) {
            Log.i(TAG, "Resuming from $resumedBytes bytes (${resumedBytes / (1024 * 1024)}MB)")
        }

        withContext(Dispatchers.IO) {
            val progressInterval = 500L
            var lastProgressTime = 0L

            java.io.FileOutputStream(partialFile, isResuming).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloaded = resumedBytes

                body.byteStream().use { input ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastProgressTime > progressInterval) {
                            lastProgressTime = now
                            val progress = if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else -1
                            updateNotification(formatProgress(downloaded, totalBytes, progress), progress, if (totalBytes > 0) 100 else 0)
                            notifyListener { it.onProgress(downloaded, totalBytes) }
                        }
                    }
                }
            }
        }

        partialFile.renameTo(modelFile)
        Log.i(TAG, "Model downloaded successfully (${modelFile.length() / (1024 * 1024)}MB)")
    }

    private fun tryRequest(client: OkHttpClient, partialFile: File): okhttp3.Response? {
        val requestBuilder = Request.Builder().url(modelDownloadUrl)

        if (partialFile.exists() && partialFile.length() > 0) {
            requestBuilder.header("Range", "bytes=${partialFile.length()}-")
            Log.i(TAG, "Requesting resume from ${partialFile.length()} bytes")
        }

        return try {
            val call = client.newCall(requestBuilder.build())
            activeCall = call
            call.execute()
        } catch (e: Exception) {
            Log.e(TAG, "Download request failed", e)
            null
        }
    }

    private fun formatProgress(downloaded: Long, totalBytes: Long, progress: Int): String {
        val downloadedMB = downloaded / (1024 * 1024)
        return if (totalBytes > 0) {
            val totalMB = totalBytes / (1024 * 1024)
            "Downloading model... ${downloadedMB}MB / ${totalMB}MB ($progress%)"
        } else {
            "Downloading model... ${downloadedMB}MB"
        }
    }

    private fun stopDownload() {
        isDownloading = false
        activeCall?.cancel()
        activeCall = null
        serviceScope.cancel()
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Model Download",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows model download progress"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String, progress: Int, max: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Qwen3 SLM")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(max, progress, progress == 0 && max == 0)
            .build()
    }

    private fun updateNotification(text: String, progress: Int, max: Int) {
        val notification = buildNotification(text, progress, max)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        isDownloading = false
        activeCall?.cancel()
        activeCall = null
        serviceScope.cancel()
        releaseWakeLock()
        super.onDestroy()
        Log.i(TAG, "Service destroyed, partial file preserved for resume")
    }
}