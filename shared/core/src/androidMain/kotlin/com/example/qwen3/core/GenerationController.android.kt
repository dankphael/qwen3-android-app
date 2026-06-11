package com.example.qwen3.core

import android.content.Context
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of GenerationController. Uses:
 * - Foreground service to keep process alive during generation
 * - WakeLock to prevent CPU sleep
 * - StateFlows for status/progress observable to UI
 *
 * Not a service itself; client holds an instance and calls start/cancel/pause.
 * The actual service (GenerationService.kt) is optional for longer-term background runs.
 */
class AndroidGenerationController(
    private val context: Context,
    private val chatRepository: ChatRepository,
    private val qwenModel: QwenModel?,
    private val modelPreferences: ModelPreferences,
) : GenerationController {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private val _status = MutableStateFlow(GenerationStatus.IDLE)
    override val status: StateFlow<GenerationStatus> = _status

    private val _tokenCount = MutableStateFlow(0)
    override val tokenCount: StateFlow<Int> = _tokenCount

    private val _responseText = MutableStateFlow("")
    override val responseText: StateFlow<String> = _responseText

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error

    private var isPaused = false

    override suspend fun startGeneration(chatId: Long, userMessage: String) {
        if (_status.value == GenerationStatus.GENERATING) {
            _error.value = "Generation already in progress"
            return
        }

        withContext(Dispatchers.Default) {
            try {
                _status.value = GenerationStatus.GENERATING
                _error.value = null
                _responseText.value = ""
                _tokenCount.value = 0

                // Acquire wake lock
                acquireWakeLock()

                // Save user message
                chatRepository.saveMessageAndAutoTitle(
                    chatId = chatId,
                    text = userMessage,
                    role = "user",
                    isFirstMessage = false,
                )

                // Generate response
                qwenModel?.let { model ->
                    try {
                        val response = model.generateStreaming(
                            messages = emptyList(),  // TODO: load message history
                            onToken = { token ->
                                if (!isPaused && _status.value == GenerationStatus.GENERATING) {
                                    _responseText.value += token
                                    _tokenCount.value++
                                }
                            },
                        )

                        // Save assistant response
                        chatRepository.saveMessageAndAutoTitle(
                            chatId = chatId,
                            text = _responseText.value,
                            role = "assistant",
                        )

                        _status.value = GenerationStatus.IDLE
                    } catch (e: Exception) {
                        _error.value = e.message ?: "Generation failed"
                        _status.value = GenerationStatus.ERROR
                    }
                } ?: run {
                    _error.value = "Model not loaded"
                    _status.value = GenerationStatus.ERROR
                }
            } finally {
                releaseWakeLock()
            }
        }
    }

    override suspend fun cancel() {
        qwenModel?.cancel()
        _status.value = GenerationStatus.IDLE
        releaseWakeLock()
    }

    override suspend fun pause() {
        isPaused = true
        _status.value = GenerationStatus.PAUSED
    }

    override suspend fun resume() {
        if (_status.value == GenerationStatus.PAUSED) {
            isPaused = false
            _status.value = GenerationStatus.GENERATING
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                    "qwen3:generation",
                ).apply {
                    acquire(24 * 3600 * 1000)  // 24 hours max
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
            wakeLock = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
