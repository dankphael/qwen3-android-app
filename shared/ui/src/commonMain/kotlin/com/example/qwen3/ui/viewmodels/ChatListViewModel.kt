package com.example.qwen3.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwen3.core.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val chatRepository: ChatRepository? = null,
) : ViewModel() {
    private val _chats = MutableStateFlow<List<ChatItemState>>(emptyList())
    val chats: StateFlow<List<ChatItemState>> = _chats

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadChats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                chatRepository?.getAllChats()?.collect { chats ->
                    _chats.value = chats.map { chat ->
                        ChatItemState(
                            id = chat.id.toString(),
                            title = chat.title,
                            lastMessage = chat.last_message,
                        )
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createNewChat() {
        viewModelScope.launch {
            chatRepository?.createChat("default")
        }
    }
}

data class ChatItemState(
    val id: String,
    val title: String,
    val lastMessage: String,
)
