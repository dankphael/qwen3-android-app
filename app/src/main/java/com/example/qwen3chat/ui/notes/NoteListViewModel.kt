package com.example.qwen3chat.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwen3chat.data.db.AppDatabase
import com.example.qwen3chat.data.entity.NoteEntity
import com.example.qwen3chat.data.repository.NoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class NoteSortMode { MODIFIED, NAME, CREATED }

class NoteListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository

    private val _notes = MutableStateFlow<List<NoteEntity>>(emptyList())
    val notes: StateFlow<List<NoteEntity>> = _notes.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sortMode = MutableStateFlow(NoteSortMode.MODIFIED)
    val sortMode: StateFlow<NoteSortMode> = _sortMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = NoteRepository(application, db.noteDao())
        loadNotes()
    }

    private fun loadNotes() {
        viewModelScope.launch {
            _sortMode.combine(_searchQuery) { sort, query ->
                Pair(sort, query)
            }.flatMapLatest { (sort, query) ->
                if (query.isBlank()) {
                    when (sort) {
                        NoteSortMode.MODIFIED -> repository.getAllNotes()
                        NoteSortMode.NAME -> repository.getAllNotesSortedByName()
                        NoteSortMode.CREATED -> repository.getAllNotesSortedByCreated()
                    }
                } else {
                    repository.searchNotes(query)
                }
            }.collect { list ->
                _notes.value = list
                _isLoading.value = false
            }
        }
    }

    fun setSortMode(mode: NoteSortMode) {
        _sortMode.value = mode
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createNote(title: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val initialContent = "# $title\n\n"
            val id = repository.createNote(title, initialContent)
            onCreated(id)
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    fun togglePin(noteId: Long) {
        viewModelScope.launch {
            repository.togglePin(noteId)
        }
    }

    fun renameNote(noteId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameNote(noteId, newTitle)
        }
    }
}
