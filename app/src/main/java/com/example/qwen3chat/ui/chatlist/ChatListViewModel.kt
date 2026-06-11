package com.example.qwen3chat.ui.chatlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwen3chat.data.db.AppDatabase
import com.example.qwen3chat.data.entity.ChatEntity
import com.example.qwen3chat.data.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository

    private val _chats = MutableStateFlow<List<ChatEntity>>(emptyList())
    val chats: StateFlow<List<ChatEntity>> = _chats.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = ChatRepository(db.chatDao(), db.messageDao())

        viewModelScope.launch {
            repository.getAllChats()
                .distinctUntilChanged()
                .collect { list ->
                    _chats.value = list
                    _isLoading.value = false
                }
        }
    }

    fun createNewChat(modelUsed: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val chatId = repository.createChat(modelUsed)
            onCreated(chatId)
        }
    }

    fun deleteChat(chatId: Long) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
        }
    }

    fun renameChat(chatId: Long, title: String) {
        viewModelScope.launch {
            repository.renameChat(chatId, title)
        }
    }
}