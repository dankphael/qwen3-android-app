package com.example.qwen3chat.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.qwen3chat.data.dao.ChatDao
import com.example.qwen3chat.data.dao.MessageDao
import com.example.qwen3chat.data.dao.NoteDao
import com.example.qwen3chat.data.entity.ChatEntity
import com.example.qwen3chat.data.entity.MessageEntity
import com.example.qwen3chat.data.entity.NoteEntity

@Database(
    entities = [ChatEntity::class, MessageEntity::class, NoteEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "qwen3_chat_db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}