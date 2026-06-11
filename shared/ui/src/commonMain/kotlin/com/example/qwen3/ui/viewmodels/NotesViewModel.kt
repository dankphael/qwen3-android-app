package com.example.qwen3.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotesViewModel : ViewModel() {
    private val _notes = MutableStateFlow<List<NoteItemState>>(emptyList())
    val notes: StateFlow<List<NoteItemState>> = _notes

    fun loadNotes() {
        viewModelScope.launch {
            try {
                // TODO: Load notes from file storage
                _notes.value = emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createNewNote() {
        viewModelScope.launch {
            // TODO: Create new note
        }
    }

    fun editNote(noteId: String) {
        viewModelScope.launch {
            // TODO: Edit note
        }
    }
}

data class NoteItemState(
    val id: String,
    val title: String,
    val content: String,
)
