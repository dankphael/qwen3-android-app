package com.example.qwen3.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * iOS implementation of GenerationController.
 *
 * Unlike Android (which uses foreground services), iOS:
 * - Runs in-process coroutine on worker thread
 * - Disables idle timer while generating (isIdleTimerDisabled = true)
 * - On backgrounding: pauses generation and checkpoints response to DB
 * - On foreground resume: resumes generation if still under BGTask grace window
 *
 * No wake lock equivalent; rely on app foreground status.
 */
class IosGenerationController(
    private val chatRepository: ChatRepository,
    private val qwenModel: QwenModel?,
    private val modelPreferences: ModelPreferences,
) : GenerationController {
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

                // TODO(ios): Set UIApplication.shared.isIdleTimerDisabled = true

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
                // TODO(ios): Set UIApplication.shared.isIdleTimerDisabled = false
            }
        }
    }

    override suspend fun cancel() {
        qwenModel?.cancel()
        _status.value = GenerationStatus.IDLE
    }

    override suspend fun pause() {
        isPaused = true
        _status.value = GenerationStatus.PAUSED
        // TODO(ios): Save checkpoint to DB for resume on foreground
    }

    override suspend fun resume() {
        if (_status.value == GenerationStatus.PAUSED) {
            isPaused = false
            _status.value = GenerationStatus.GENERATING
        }
    }
}
