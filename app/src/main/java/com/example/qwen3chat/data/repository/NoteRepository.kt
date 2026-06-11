package com.example.qwen3chat.data.repository

import android.content.Context
import com.example.qwen3chat.data.NoteFileStorage
import com.example.qwen3chat.data.dao.NoteDao
import com.example.qwen3chat.data.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val context: Context,
    private val noteDao: NoteDao
) {
    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun getAllNotesSortedByName(): Flow<List<NoteEntity>> = noteDao.getAllNotesSortedByName()

    fun getAllNotesSortedByCreated(): Flow<List<NoteEntity>> = noteDao.getAllNotesSortedByCreated()

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    fun getNotesByFolder(folder: String): Flow<List<NoteEntity>> = noteDao.getNotesByFolder(folder)

    fun getAllFolders(): Flow<List<String>> = noteDao.getAllFolders()

    suspend fun getNoteById(noteId: Long): NoteEntity? = noteDao.getNoteById(noteId)

    suspend fun readNoteContent(noteId: Long): String? {
        val note = noteDao.getNoteById(noteId) ?: return null
        return NoteFileStorage.readNote(context, note.filename)
    }

    suspend fun createNote(title: String, content: String = "", folder: String = ""): Long {
        val filename = NoteFileStorage.sanitizeFilename(title).let {
            NoteFileStorage.ensureMdExtension(it)
        }
        val note = NoteEntity(
            title = title,
            filename = filename,
            folder = folder,
            snippet = content.take(150),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = noteDao.insertNote(note)
        NoteFileStorage.writeNote(context, filename, content)
        return id
    }

    suspend fun saveNote(noteId: Long, title: String, content: String) {
        val note = noteDao.getNoteById(noteId) ?: return
        val newFilename = NoteFileStorage.sanitizeFilename(title).let {
            NoteFileStorage.ensureMdExtension(it)
        }
        if (newFilename != note.filename) {
            NoteFileStorage.renameNoteFile(context, note.filename, newFilename)
        }
        NoteFileStorage.writeNote(context, newFilename, content)
        noteDao.updateTitle(noteId, title)
        noteDao.updateSnippet(noteId, content.take(150))
    }

    suspend fun deleteNote(noteId: Long) {
        val note = noteDao.getNoteById(noteId) ?: return
        NoteFileStorage.deleteNoteFile(context, note.filename)
        noteDao.deleteNoteById(noteId)
    }

    suspend fun renameNote(noteId: Long, newTitle: String) {
        val note = noteDao.getNoteById(noteId) ?: return
        val newFilename = NoteFileStorage.sanitizeFilename(newTitle).let {
            NoteFileStorage.ensureMdExtension(it)
        }
        NoteFileStorage.renameNoteFile(context, note.filename, newFilename)
        noteDao.updateTitle(noteId, newTitle)
    }

    suspend fun togglePin(noteId: Long) {
        val note = noteDao.getNoteById(noteId) ?: return
        noteDao.togglePin(noteId, !note.isPinned)
    }

    suspend fun moveToFolder(noteId: Long, folder: String) {
        noteDao.moveToFolder(noteId, folder)
    }
}