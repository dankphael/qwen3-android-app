package com.example.qwen3.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Orchestrates background generation lifecycle. On Android: foreground service with
 * wake lock + notification. On iOS: in-process coroutine with BGTask grace window.
 *
 * Clients call startGeneration(chatId, userMessage) and listen to status/progress flows.
 * The controller handles pausing/resuming on app backgrounding and checkpointing partial
 * responses to the database.
 */
interface GenerationController {
    // Observable status: IDLE, GENERATING, PAUSED, ERROR
    val status: StateFlow<GenerationStatus>

    // Current generated tokens count (for progress display)
    val tokenCount: StateFlow<Int>

    // Accumulated response text (streamed in real-time)
    val responseText: StateFlow<String>

    // Error message if generation fails
    val error: StateFlow<String?>

    /**
     * Start or resume generation for a chat message.
     * - Saves user message to the chat
     * - Creates assistant message placeholder
     * - Starts inference in background
     */
    suspend fun startGeneration(
        chatId: Long,
        userMessage: String,
    )

    /**
     * Cancel ongoing generation. Graceful: finishes current token,
     * then returns. Partial response is saved to DB.
     */
    suspend fun cancel()

    /**
     * Pause generation (Android: pause service, iOS: pause coroutine).
     * Can be resumed via startGeneration.
     */
    suspend fun pause()

    /**
     * Resume paused generation.
     */
    suspend fun resume()
}

enum class GenerationStatus {
    IDLE,         // No generation in progress
    GENERATING,   // Token loop running
    PAUSED,       // Paused by user or app backgrounding
    ERROR,        // Failed; see error() for message
}
