package com.example.qwen3chat

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    // Thinking / reasoning fields
    val hasThinking: Boolean = false,
    val thinkingContent: String = "",
    var thinkingExpanded: Boolean = false,
    // Code blocks extracted from content
    val codeBlocks: List<CodeBlock> = emptyList()
)

data class CodeBlock(
    val language: String,
    val code: String
)