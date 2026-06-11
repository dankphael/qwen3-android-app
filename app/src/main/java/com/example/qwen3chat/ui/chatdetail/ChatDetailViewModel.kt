package com.example.qwen3chat.ui.chatdetail

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwen3chat.ChatMessage
import com.example.qwen3chat.GenerationService
import com.example.qwen3chat.MainActivity
import com.example.qwen3chat.QwenModel
import com.example.qwen3chat.data.db.AppDatabase
import com.example.qwen3chat.data.entity.MessageEntity
import com.example.qwen3chat.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ChatDetailViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ChatDetailViewModel"
    }

    private val repository: ChatRepository

    private val _chatId = MutableStateFlow<Long?>(null)
    val chatId: StateFlow<Long?> = _chatId.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _isLoadingHistory = MutableStateFlow(true)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()

    /** Streaming token flow — each token as it's generated */
    private val _streamingToken = MutableStateFlow("")
    val streamingToken: StateFlow<String> = _streamingToken.asStateFlow()

    /** Tokens-per-second speed indicator */
    private val _tokensPerSecond = MutableStateFlow(0f)
    val tokensPerSecond: StateFlow<Float> = _tokensPerSecond.asStateFlow()

    /** Context usage info (cached tokens / total) */
    private val _contextUsage = MutableStateFlow("")
    val contextUsage: StateFlow<String> = _contextUsage.asStateFlow()

    /** The shared model instance from MainActivity. Must be set before sending messages. */
    private val _chatTitle = MutableStateFlow("Chat")
    val chatTitle: StateFlow<String> = _chatTitle.asStateFlow()

    /** Model name used for this chat */
    private val _chatModelUsed = MutableStateFlow("Qwen3.5-0.8B")
    val chatModelUsed: StateFlow<String> = _chatModelUsed.asStateFlow()

    private var qwenModel: QwenModel? = null

    /** Speed tracking fields */
    private var generationStartTime: Long = 0
    private var tokenCount: Int = 0

    /** Job that collects GenerationService events */
    private var generationJob: Job? = null

    init {
        val db = AppDatabase.getInstance(application)
        repository = ChatRepository(db.chatDao(), db.messageDao())
    }

    val isModelLoaded: Boolean get() = qwenModel != null && qwenModel!!.isReady

    fun setChatId(id: Long) {
        if (_chatId.value == id) return
        _chatId.value = id
        observeChat(id)
        observeMessages(id)
    }

    private fun observeChat(chatId: Long) {
        viewModelScope.launch {
            repository.getChatById(chatId).collect { chat ->
                chat?.let {
                    _chatTitle.value = it.title
                    // Extract model display name from model key
                    val modelDisplay = try {
                        ModelMetadata.findByKey(it.modelUsed).displayName
                    } catch (e: Exception) {
                        it.modelUsed.ifEmpty { "Qwen3.5-0.8B" }
                    }
                    _chatModelUsed.value = modelDisplay
                }
            }
        }
    }

    fun attachModel(model: QwenModel?) {
        qwenModel = model
        _isModelReady.value = model?.isReady == true
    }

    private fun observeMessages(chatId: Long) {
        viewModelScope.launch {
            repository.getMessagesByChatId(chatId)
                .distinctUntilChanged()
                .collect { list ->
                    _messages.value = list
                    if (_isLoadingHistory.value) {
                        _isLoadingHistory.value = false
                    }
                    updateContextUsage()
                }
        }
    }

    /**
     * Send a user message, save to Room, start GenerationService for background generation.
     */
    fun sendMessage(text: String, onResponseStart: () -> Unit, onError: (String) -> Unit) {
        val currentChatId = _chatId.value
        if (currentChatId == null) {
            onError("No active chat")
            return
        }

        val model = qwenModel
        if (model == null || !model.isReady) {
            onError("Model not loaded")
            return
        }
        if (_isGenerating.value) {
            onError("Generation already in progress")
            return
        }

        viewModelScope.launch {
            try {
                // Check if this is the first user message (for auto-title)
                val currentMessages = _messages.value
                val isFirstMessage = currentMessages.none { it.role == "user" }

                // Save user message to Room
                repository.saveMessageAndAutoTitle(
                    chatId = currentChatId,
                    text = text,
                    role = "user",
                    isFirstMessage = isFirstMessage
                )

                _isGenerating.value = true
                onResponseStart()

                // Initialize speed tracking
                generationStartTime = System.currentTimeMillis()
                tokenCount = 0

                // Build chat message list for serialization
                val chatMessages = mutableListOf<ChatMessage>()
                for (msg in _messages.value) {
                    chatMessages.add(
                        ChatMessage(
                            content = msg.content,
                            isUser = msg.role == "user",
                            hasThinking = !msg.thinking.isNullOrEmpty(),
                            thinkingContent = msg.thinking ?: ""
                        )
                    )
                }
                // Guard against Flow race — add the message if not yet reflected
                if (chatMessages.none { it.content == text && it.isUser }) {
                    chatMessages.add(ChatMessage(content = text, isUser = true))
                }

                // Serialize to JSON for service intent
                val jsonArray = JSONArray()
                for (msg in chatMessages) {
                    val obj = JSONObject()
                    obj.put("content", msg.content)
                    obj.put("isUser", msg.isUser)
                    jsonArray.put(obj)
                }

                // Start collecting GenerationService events BEFORE starting the service
                generationJob?.cancel()
                generationJob = viewModelScope.launch {
                    GenerationService.events.collect { event ->
                        when (event) {
                            is GenerationService.GenerationEvent.Token -> {
                                if (event.chatId == _chatId.value) {
                                    _streamingToken.value = event.token
                                    _tokensPerSecond.value = event.speed
                                }
                            }
                            is GenerationService.GenerationEvent.Complete -> {
                                if (event.chatId == _chatId.value) {
                                    onResponseComplete()
                                }
                            }
                            is GenerationService.GenerationEvent.Error -> {
                                if (event.chatId == _chatId.value) {
                                    onResponseComplete()
                                    onError(event.error)
                                }
                            }
                        }
                    }
                }

                // Start the foreground service for generation
                val intent = Intent(getApplication(), GenerationService::class.java).apply {
                    action = GenerationService.ACTION_START
                    putExtra(GenerationService.EXTRA_CHAT_ID, currentChatId)
                    putExtra(GenerationService.EXTRA_MESSAGES_JSON, jsonArray.toString())
                }
                ContextCompat.startForegroundService(getApplication(), intent)

            } catch (e: Exception) {
                Log.e(TAG, "Send/generation failed", e)
                onResponseComplete()
                onError(e.message ?: "Failed to generate response")
            }
        }
    }

    /**
     * Cancel an ongoing generation — stops the service and cancels the model.
     */
    fun stopGeneration() {
        if (_isGenerating.value) {
            // Cancel the service
            val intent = Intent(getApplication(), GenerationService::class.java).apply {
                action = GenerationService.ACTION_CANCEL
            }
            getApplication<Application>().startService(intent)

            // Cancel the model directly for immediate stop
            MainActivity.sharedChatViewModel?.qwenModelInstance?.cancel()
        }
    }

    fun regenerate(onError: (String) -> Unit) {
        val currentMessages = _messages.value
        // Find last user message
        val lastUserMessage = currentMessages.lastOrNull { it.role == "user" }
            ?: return onError("No user message to regenerate from")

        val userText = lastUserMessage.content

        viewModelScope.launch {
            try {
                val currentChatId = _chatId.value ?: return@launch onError("No active chat")

                // Delete all messages after (and including) the last assistant message
                val lastAssistantIndex = currentMessages.indexOfLast { it.role == "assistant" }
                if (lastAssistantIndex >= 0) {
                    val messagesToDelete = currentMessages.drop(lastAssistantIndex)
                    for (msg in messagesToDelete) {
                        repository.deleteMessage(msg.id)
                    }
                }

                // Reset KV cache since we deleted assistant messages
                qwenModel?.resetConversation()

                // Re-send the last user message to regenerate response
                sendMessage(userText, onResponseStart = {}, onError = onError)
            } catch (e: Exception) {
                onError(e.message ?: "Regeneration failed")
            }
        }
    }

    fun editAndResend(messageId: Long, newText: String, onError: (String) -> Unit) {
        val currentMessages = _messages.value
        val messageToEdit = currentMessages.find { it.id == messageId }
            ?: return onError("Message not found")

        if (messageToEdit.role != "user") {
            return onError("Can only edit user messages")
        }

        viewModelScope.launch {
            try {
                val currentChatId = _chatId.value ?: return@launch onError("No active chat")

                // Delete all messages after the edited message
                val editIndex = currentMessages.indexOfFirst { it.id == messageId }
                if (editIndex >= 0) {
                    val messagesToDelete = currentMessages.drop(editIndex + 1)
                    for (msg in messagesToDelete) {
                        repository.deleteMessage(msg.id)
                    }
                }

                // Delete the original message and send the new one
                repository.deleteMessage(messageId)

                // Reset KV cache since we deleted messages
                qwenModel?.resetConversation()

                // Send the new text
                sendMessage(newText, onResponseStart = {}, onError = onError)
            } catch (e: Exception) {
                onError(e.message ?: "Edit & resend failed")
            }
        }
    }

    private fun onResponseComplete() {
        _isGenerating.value = false
        _tokensPerSecond.value = 0f
        generationJob?.cancel()
        generationJob = null
        updateContextUsage()
    }

    override fun onCleared() {
        super.onCleared()
        generationJob?.cancel()
        Log.i(TAG, "ChatDetailViewModel cleared")
    }

    private fun updateContextUsage() {
        val model = qwenModel
        if (model != null && model.isReady) {
            val (cached, total) = model.getContextInfo()
            _contextUsage.value = "$cached/$total"
        }
    }

    suspend fun exportChat(): List<MessageEntity>? {
        val chatId = _chatId.value ?: return null
        return repository.getMessagesByChatIdOnce(chatId)
    }
}
