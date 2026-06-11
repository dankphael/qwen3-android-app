package com.example.qwen3.core

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.qwen3.core.db.ChatDatabase
import com.example.qwen3.core.db.Chats
import com.example.qwen3.core.db.Messages
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

/**
 * Chat/message persistence over SQLDelight. Auto-title logic lifted verbatim
 * from the Android ChatRepository.
 */
class ChatRepository(
    driverFactory: DatabaseDriverFactory,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val db = ChatDatabase(driverFactory.createDriver())
    private val chatQ = db.chatDatabaseQueries

    fun getAllChats(): Flow<List<Chats>> =
        chatQ.selectAllChats().asFlow().mapToList(ioDispatcher)

    fun getMessagesByChatId(chatId: Long): Flow<List<Messages>> =
        chatQ.selectMessagesByChat(chatId).asFlow().mapToList(ioDispatcher)

    suspend fun getChatById(chatId: Long): Chats? =
        chatQ.selectChatById(chatId).executeAsOneOrNull()

    fun createChat(modelUsed: String): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        return chatQ.transactionWithResult {
            chatQ.insertChat("New Chat", modelUsed, "", now, now)
            chatQ.lastInsertRowId().executeAsOne()
        }
    }

    /** Save a message and, if it's the first user message, derive the chat title. */
    fun saveMessageAndAutoTitle(
        chatId: Long,
        text: String,
        role: String,
        thinking: String? = null,
        isFirstMessage: Boolean = false,
    ): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        return chatQ.transactionWithResult {
            chatQ.insertMessage(chatId, role, text, thinking, now)
            val id = chatQ.lastInsertRowId().executeAsOne()
            chatQ.touchChat(now, chatId)
            if (isFirstMessage && role == "user") {
                chatQ.updateChatTitle(generateTitle(text), now, chatId)
            }
            id
        }
    }

    fun deleteChat(chatId: Long) = chatQ.deleteChatById(chatId)

    fun deleteMessage(messageId: Long) = chatQ.deleteMessageById(messageId)

    fun renameChat(chatId: Long, title: String) {
        chatQ.updateChatTitle(title, Clock.System.now().toEpochMilliseconds(), chatId)
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
