package com.example.qwen3chat.data.repository

import androidx.annotation.WorkerThread
import com.example.qwen3chat.data.dao.ChatDao
import com.example.qwen3chat.data.dao.MessageDao
import com.example.qwen3chat.data.entity.ChatEntity
import com.example.qwen3chat.data.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {

    fun getAllChats(): Flow<List<ChatEntity>> = chatDao.getAllChats()

    fun getChatById(chatId: Long): Flow<ChatEntity?> = chatDao.getChatById(chatId)

    suspend fun getChatByIdOnce(chatId: Long): ChatEntity? = chatDao.getChatByIdOnce(chatId)

    fun getMessagesByChatId(chatId: Long): Flow<List<MessageEntity>> =
        messageDao.getMessagesByChatId(chatId)

    suspend fun getMessagesByChatIdOnce(chatId: Long): List<MessageEntity> =
        messageDao.getMessagesByChatIdOnce(chatId)

    suspend fun createChat(modelUsed: String): Long {
        val chat = ChatEntity(
            title = "New Chat",
            modelUsed = modelUsed,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return chatDao.insertChat(chat)
    }

    /**
     * Save a message and update the chat title from the first user message.
     * Both operations run in the same transaction.
     */
    @WorkerThread
    suspend fun saveMessageAndAutoTitle(
        chatId: Long,
        text: String,
        role: String,
        thinking: String? = null,
        isFirstMessage: Boolean = false
    ): Long {
        val message = MessageEntity(
            chatId = chatId,
            role = role,
            content = text,
            thinking = thinking,
            timestamp = System.currentTimeMillis()
        )
        val messageId = messageDao.insertMessage(message)
        chatDao.touchChat(chatId)

        if (isFirstMessage && role == "user") {
            val title = generateTitle(text)
            chatDao.updateTitle(chatId, title)
        }

        return messageId
    }

    suspend fun deleteChat(chatId: Long) {
        chatDao.deleteChatById(chatId)
    }

    suspend fun renameChat(chatId: Long, title: String) {
        chatDao.updateTitle(chatId, title)
    }

    companion object {
        fun generateTitle(firstMessage: String): String {
            val cleaned = firstMessage.trim()
            if (cleaned.isEmpty()) return "New Chat"
            if (cleaned.length <= 50) return cleaned
            val truncated = cleaned.take(50)
            val lastSpace = truncated.lastIndexOf(' ')
            return if (lastSpace > 20) truncated.take(lastSpace) + "…" else truncated + "…"
        }
    }
}