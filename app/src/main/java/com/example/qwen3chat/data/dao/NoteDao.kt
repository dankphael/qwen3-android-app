package com.example.qwen3chat.data.dao

import androidx.room.*
import com.example.qwen3chat.data.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR snippet LIKE '%' || :query || '%' ORDER BY isPinned DESC, updatedAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folder = :folder ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByFolder(folder: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY title ASC")
    fun getAllNotesSortedByName(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotesSortedByCreated(): Flow<List<NoteEntity>>

    @Insert
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET title = :title, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun updateTitle(noteId: Long, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET snippet = :snippet, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun updateSnippet(noteId: Long, snippet: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :noteId")
    suspend fun togglePin(noteId: Long, isPinned: Boolean)

    @Query("UPDATE notes SET folder = :folder WHERE id = :noteId")
    suspend fun moveToFolder(noteId: Long, folder: String)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("SELECT DISTINCT folder FROM notes WHERE folder != '' ORDER BY folder ASC")
    fun getAllFolders(): Flow<List<String>>
}
