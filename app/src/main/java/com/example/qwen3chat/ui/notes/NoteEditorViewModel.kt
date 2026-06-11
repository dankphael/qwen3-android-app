package com.example.qwen3chat.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwen3chat.data.db.AppDatabase
import com.example.qwen3chat.data.entity.NoteEntity
import com.example.qwen3chat.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoteEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository

    private val _note = MutableStateFlow<NoteEntity?>(null)
    val note: StateFlow<NoteEntity?> = _note.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var noteId: Long = -1
    private var autoSaveJob: kotlinx.coroutines.Job? = null

    init {
        val db = AppDatabase.getInstance(application)
        repository = NoteRepository(application, db.noteDao())
    }

    fun loadNote(id: Long) {
        noteId = id
        viewModelScope.launch {
            val entity = repository.getNoteById(id)
            _note.value = entity
            val text = repository.readNoteContent(id) ?: ""
            _content.value = text
        }
    }

    fun updateContent(newContent: String) {
        _content.value = newContent
        scheduleAutoSave()
    }

    fun updateTitle(newTitle: String) {
        val current = _note.value ?: return
        _note.value = current.copy(title = newTitle)
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            saveNote()
        }
    }

    fun saveNote() {
        val currentNote = _note.value ?: return
        if (noteId <= 0) return
        viewModelScope.launch {
            _isSaving.value = true
            repository.saveNote(noteId, currentNote.title, _content.value)
            _isSaving.value = false
        }
    }

    fun getNoteTitle(): String = _note.value?.title ?: "Note"

    fun getFilePath(): String {
        val entity = _note.value ?: return ""
        val context = getApplication<Application>()
        return com.example.qwen3chat.data.NoteFileStorage.getAbsolutePath(context, entity.filename)
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        if (_content.value.isNotEmpty() && noteId > 0) {
            // Fire-and-forget save on cleanup
        }
    }
}
