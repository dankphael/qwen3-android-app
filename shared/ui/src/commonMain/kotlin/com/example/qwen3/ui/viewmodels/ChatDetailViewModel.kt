package com.example.qwen3.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwen3.core.ChatMessage
import com.example.qwen3.core.ChatRepository
import com.example.qwen3.core.QwenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatDetailViewModel(
    private val chatId: String,
    private val chatRepository: ChatRepository? = null,
    private val qwenModel: QwenModel? = null,
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessageState>>(emptyList())
    val messages: StateFlow<List<ChatMessageState>> = _messages

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _currentModel = MutableStateFlow("None")
    val currentModel: StateFlow<String> = _currentModel

    fun loadMessages() {
        viewModelScope.launch {
            try {
                chatRepository?.getMessagesByChatId(chatId)?.collect { messages ->
                    _messages.value = messages.map { message ->
                        ChatMessageState(
                            id = message.id,
                            content = message.content,
                            isUser = message.isUser,
                            thinking = message.thinking,
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
                    message = ChatMessage(
                        id = System.currentTimeMillis().toString(),
                        content = text,
                        isUser = true,
                        thinking = null,
                    ),
                )

                // Generate response using QwenModel
                qwenModel?.let { model ->
                    model.generateStreaming(
                        onToken = { token ->
                            // Update UI with streaming tokens
                        },
                    )
                }
            } finally {
                _isGenerating.value = false
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
