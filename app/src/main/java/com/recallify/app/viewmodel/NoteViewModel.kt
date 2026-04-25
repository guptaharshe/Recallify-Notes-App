package com.recallify.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recallify.app.BuildConfig
import com.recallify.app.data.local.datastore.ThemeDataStore
import com.recallify.app.data.local.entity.NoteEntity
import com.recallify.app.data.remote.AiService
import com.recallify.app.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class NoteViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    // Using Retrofit-based AiService which is compatible with Android 7.0+
    private val aiService = AiService(BuildConfig.OPENROUTER_API_KEY)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _aiResult = MutableStateFlow<String?>(null)
    val aiResult = _aiResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var recentlyDeletedNote: NoteEntity? = null

    val notes = repository.getAllNotes()

    val filteredNotes = combine(
        repository.getAllNotes(),
        searchQuery
    ) { notes, query ->
        val filtered = if (query.isBlank()) {
            notes
        } else {
            notes.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true)
            }
        }
        filtered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.timestamp })
    }

    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            val note = NoteEntity(
                title = title,
                content = content
            )
            repository.insertNote(note)
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            recentlyDeletedNote = note
            repository.deleteNote(note)
        }
    }

    fun undoDelete() {
        recentlyDeletedNote?.let {
            viewModelScope.launch {
                repository.insertNote(it)
                recentlyDeletedNote = null
            }
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun summarizeNote(content: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = aiService.summarize(content)
            result.onSuccess {
                _aiResult.value = it
            }.onFailure { e ->
                _error.value = "Error: ${e.localizedMessage ?: "Unknown error"}"
            }
            _isLoading.value = false
        }
    }

    fun generateQuiz(content: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = aiService.generateQuiz(content)
            result.onSuccess {
                _aiResult.value = it
            }.onFailure { e ->
                _error.value = "Error: ${e.localizedMessage ?: "Unknown error"}"
            }
            _isLoading.value = false
        }
    }

    fun explainLikeImFive(content: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = aiService.explainLikeImFive(content)
            result.onSuccess {
                _aiResult.value = it
            }.onFailure { e ->
                _error.value = "Error: ${e.localizedMessage ?: "Unknown error"}"
            }
            _isLoading.value = false
        }
    }

    fun clearAiResult() {
        _aiResult.value = null
        _error.value = null
    }

    fun loadTheme(context: Context) {
        viewModelScope.launch {
            ThemeDataStore.getTheme(context).collect {
                _isDarkMode.value = it
            }
        }
    }

    fun toggleTheme(context: Context) {
        viewModelScope.launch {
            val newValue = !_isDarkMode.value
            ThemeDataStore.saveTheme(context, newValue)
        }
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }
}
