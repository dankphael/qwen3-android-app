package com.example.qwen3.core

/**
 * In-memory representation of a chat message used by the inference layer and UI.
 * Platform-independent.
 */
data class ChatMessage(
    val id: Long = 0,
    val content: String,
    val isUser: Boolean,
    val hasThinking: Boolean = false,
    val thinkingContent: String = "",
    val thinkingExpanded: Boolean = false,
    val timestamp: Long = 0,
)
