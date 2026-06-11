package com.example.qwen3.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwen3.core.ChatMessage
import com.example.qwen3.core.ChatRepository
import com.example.qwen3.core.ModelPreferences
import com.example.qwen3.core.QwenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatDetailViewModel(
    private val chatIdStr: String,
    private val chatRepository: ChatRepository? = null,
    private val qwenModel: QwenModel? = null,
    private val modelPreferences: ModelPreferences? = null,
) : ViewModel() {
    // Convert string chatId to Long for repository
    private val chatId: Long get() = chatIdStr.toLongOrNull() ?: 0L

    private val _messages = MutableStateFlow<List<ChatMessageState>>(emptyList())
    val messages: StateFlow<List<ChatMessageState>> = _messages

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _currentModel = MutableStateFlow("None")
    val currentModel: StateFlow<String> = _currentModel

    private var assistantMessageId: String? = null

    fun loadMessages() {
        viewModelScope.launch {
            try {
                val selectedModel = modelPreferences?.getSelectedModelKey() ?: "None"
                _currentModel.value = selectedModel

                chatRepository?.getMessagesByChatId(chatId)?.collect { messages ->
                    _messages.value = messages.map { message ->
                        ChatMessageState(
                            id = message.id.toString(),
                            content = message.content,
                            isUser = message.role == "user",
                            thinking = null,
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                // Save user message
                chatRepository?.saveMessageAndAutoTitle(
                    chatId = chatId,
                    text = text,
                    role = "user",
                    isFirstMessage = _messages.value.isEmpty(),
                )

                // Create assistant message placeholder
                assistantMessageId = (System.currentTimeMillis() + 1).toString()
                val assistantMessage = ChatMessageState(
                    id = assistantMessageId!!,
                    content = "",
                    isUser = false,
                    thinking = null,
                )

                // Start with empty message
                appendMessage(assistantMessage)

                // Generate response using QwenModel with streaming
                qwenModel?.let { model ->
                    try {
                        // Build chat history for prompt
                        val currentMessages = _messages.value
                            .filter { !it.isUser || it.content.isNotBlank() }
                            .map { msg ->
                                ChatMessage(
                                    id = msg.id,
                                    content = msg.content,
                                    isUser = msg.isUser,
                                    thinking = msg.thinking,
                                )
                            }

                        val responseText = model.generateStreaming(
                            messages = currentMessages,
                            onToken = { token ->
                                // Update UI with streaming tokens
                                updateAssistantMessage(token)
                            },
                        )

                        // Save final message
                        assistantMessageId?.let { _ ->
                            val finalContent = _messages.value
                                .firstOrNull { it.id == assistantMessageId }?.content ?: ""
                            if (finalContent.isNotBlank()) {
                                chatRepository?.saveMessageAndAutoTitle(
                                    chatId = chatId,
                                    text = finalContent,
                                    role = "assistant",
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } finally {
                _isGenerating.value = false
                assistantMessageId = null
            }
        }
    }

    private fun appendMessage(message: ChatMessageState) {
        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(message)
        _messages.value = currentMessages
    }

    private fun updateAssistantMessage(token: String) {
        assistantMessageId?.let { msgId ->
            val currentMessages = _messages.value.toMutableList()
            val index = currentMessages.indexOfFirst { it.id == msgId }
            if (index >= 0) {
                val msg = currentMessages[index]
                currentMessages[index] = msg.copy(content = msg.content + token)
                _messages.value = currentMessages
            }
        }
    }
}

data class ChatMessageState(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val thinking: String? = null,
)
