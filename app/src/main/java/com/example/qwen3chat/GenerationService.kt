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
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Foreground service that runs model generation in the background.
 * Holds a PARTIAL_WAKE_LOCK and shows a persistent notification
 * so Android doesn't kill generation when the app is backgrounded.
 *
 * Saves completed responses directly to Room DB so generation survives
 * even if the user navigates away from the chat screen.
 *
 * Also emits events via SharedFlow for real-time UI updates while
 * the chat screen is visible.
 */
class GenerationService : Service() {

    companion object {
        private const val TAG = "GenerationService"
        private const val CHANNEL_ID = "generation"
        private const val NOTIFICATION_ID = 100

        // Intent actions
        const val ACTION_START = "com.example.qwen3chat.ACTION_START_GENERATION"
        const val ACTION_CANCEL = "com.example.qwen3chat.ACTION_CANCEL_GENERATION"

        // Intent extras
        const val EXTRA_CHAT_ID = "chat_id"
        const val EXTRA_MESSAGES_JSON = "messages_json"

        /**
         * SharedFlow for same-process event delivery.
         * ChatDetailViewModel collects from this for streaming UI updates.
         */
        private val _events = MutableSharedFlow<GenerationEvent>(extraBufferCapacity = 128)
        val events: SharedFlow<GenerationEvent> = _events
    }

    /** Events emitted by the service during generation. */
    sealed class GenerationEvent {
        data class Token(val chatId: Long, val token: String, val speed: Float) : GenerationEvent()
        data class Complete(val chatId: Long, val response: String) : GenerationEvent()
        data class Error(val chatId: Long, val error: String) : GenerationEvent()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null
    private var isGenerating = false
    private var currentChatId: Long = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                cancelGeneration()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                if (isGenerating) return START_STICKY

                currentChatId = intent.getLongExtra(EXTRA_CHAT_ID, -1)
                val messagesJson = intent.getStringExtra(EXTRA_MESSAGES_JSON)
                if (messagesJson == null || currentChatId == -1L) {
                    Log.e(TAG, "Missing required extras")
                    stopSelf()
                    return START_NOT_STICKY
                }

                startForegroundWithNotification()
                startGeneration(messagesJson)
                return START_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Generating response...", 0, 0, false)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "$TAG::GenerationWakeLock"
            ).apply {
                acquire(10 * 60 * 1000L) // 10-minute max
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

    private fun startGeneration(messagesJson: String) {
        if (isGenerating) return
        isGenerating = true
        acquireWakeLock()

        serviceScope.launch {
            try {
                val model = MainActivity.sharedChatViewModel?.qwenModelInstance
                if (model == null || !model.isReady) {
                    emitError("Model not loaded")
                    return@launch
                }

                // Parse chat messages from JSON
                val jsonArray = JSONArray(messagesJson)
                val messages = mutableListOf<ChatMessage>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    messages.add(
                        ChatMessage(
                            content = obj.getString("content"),
                            isUser = obj.getBoolean("isUser")
                        )
                    )
                }

                Log.i(TAG, "Starting generation for chat $currentChatId with ${messages.size} messages")

                val fullResponse = StringBuilder()
                val startTime = System.currentTimeMillis()
                var tokenCount = 0

                withContext(Dispatchers.IO) {
                    model.generateStreaming(messages) { token ->
                        fullResponse.append(token)
                        tokenCount++

                        val elapsed = System.currentTimeMillis() - startTime
                        val speed = if (elapsed > 0) (tokenCount * 1000f) / elapsed else 0f

                        // Update notification with progress every 20 tokens
                        if (tokenCount % 20 == 0) {
                            updateNotification(
                                "Generating... ${tokenCount} tokens (${"%.1f".format(speed)} tok/s)",
                                tokenCount,
                                model.metadata.maxTokens,
                                true
                            )
                        }

                        // Emit token event for real-time UI (while chat screen is visible)
                        _events.tryEmit(
                            GenerationEvent.Token(
                                chatId = currentChatId,
                                token = token,
                                speed = speed
                            )
                        )
                    }
                }

                Log.i(TAG, "Generation complete: ${tokenCount} tokens, ${fullResponse.length} chars")

                // Save assistant response directly to Room DB — survives navigation
                val responseText = fullResponse.toString()
                saveResponseToRoom(currentChatId, responseText)

                _events.tryEmit(
                    GenerationEvent.Complete(
                        chatId = currentChatId,
                        response = responseText
                    )
                )

                updateNotification("Response generated (${tokenCount} tokens)", 0, 0, false)
            } catch (e: Exception) {
                Log.e(TAG, "Generation failed", e)
                emitError(e.message ?: "Generation failed")
            } finally {
                cleanup()
            }
        }
    }

    /**
     * Save the completed assistant response directly to Room DB.
     * This runs inside the service so it works even if the user navigated away.
     */
    private fun saveResponseToRoom(chatId: Long, response: String) {
        try {
            val db = com.example.qwen3chat.data.db.AppDatabase.getInstance(applicationContext)
            val chatDao = db.chatDao()
            val messageDao = db.messageDao()

            // Use coroutine on the service scope to do the Room write
            serviceScope.launch(Dispatchers.IO) {
                try {
                    // Parse reasoning from the response
                    val parsed = ReasoningParser.parse(response)

                    // Save assistant message
                    val assistantMessage = com.example.qwen3chat.data.entity.MessageEntity(
                        chatId = chatId,
                        role = "assistant",
                        content = parsed.answer,
                        thinking = parsed.thinking,
                        timestamp = System.currentTimeMillis()
                    )
                    messageDao.insertMessage(assistantMessage)

                    // Update chat's lastMessage preview
                    val chat = chatDao.getChatByIdOnce(chatId)
                    if (chat != null) {
                        val preview = parsed.answer.take(80)
                        chatDao.updateChat(chat.copy(lastMessage = preview))
                    }

                    Log.i(TAG, "Assistant message saved to Room for chat $chatId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save response to Room", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to access Room DB", e)
        }
    }

    private fun emitError(message: String) {
        _events.tryEmit(GenerationEvent.Error(chatId = currentChatId, error = message))
        updateNotification("Generation failed", 0, 0, false)
    }

    private fun cancelGeneration() {
        Log.i(TAG, "Cancellation requested")
        MainActivity.sharedChatViewModel?.qwenModelInstance?.cancel()
        cleanup()
    }

    private fun cleanup() {
        isGenerating = false
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // --- Notification helpers ---

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Generation",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when model is generating a response"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(
        text: String,
        progress: Int,
        max: Int,
        ongoing: Boolean
    ): Notification {
        // Tap notification → return to app
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel action button
        val cancelIntent = Intent(this, GenerationService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Qwen3")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(ongoing)
            .setProgress(max, progress, !ongoing)
            .addAction(android.R.drawable.ic_media_pause, "Cancel", cancelPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String, progress: Int, max: Int, ongoing: Boolean) {
        val notification = buildNotification(text, progress, max, ongoing)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        if (isGenerating) {
            MainActivity.sharedChatViewModel?.qwenModelInstance?.cancel()
        }
        isGenerating = false
        serviceScope.cancel()
        releaseWakeLock()
        super.onDestroy()
        Log.i(TAG, "Service destroyed")
    }
}