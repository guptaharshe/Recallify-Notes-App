package com.recallify.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recallify.app.data.local.datastore.ThemeDataStore
import com.recallify.app.data.local.entity.NoteEntity
import com.recallify.app.data.repository.NoteRepository
import com.recallify.app.utils.ReminderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NoteViewModel(
    private val repository: NoteRepository,
    private val reminderManager: ReminderManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode = _isDarkMode.asStateFlow()

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

    suspend fun getNoteById(id: Int): NoteEntity? {
        return repository.getAllNotes().first().find { it.id == id }
    }

    fun addNote(title: String, content: String, reminderTime: Long? = null, color: Int = 0xFFFFFFFF.toInt()) {
        viewModelScope.launch {
            val note = NoteEntity(
                title = title,
                content = content,
                reminderTime = reminderTime,
                color = color
            )
            val id = repository.insertNote(note).toInt()
            if (reminderTime != null) {
                reminderManager.scheduleReminder(note.copy(id = id))
            }
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note)
            if (note.reminderTime != null) {
                reminderManager.scheduleReminder(note)
            } else {
                reminderManager.cancelReminder(note.id)
            }
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            recentlyDeletedNote = note
            reminderManager.cancelReminder(note.id)
            repository.deleteNote(note)
        }
    }

    fun undoDelete() {
        recentlyDeletedNote?.let {
            viewModelScope.launch {
                val id = repository.insertNote(it).toInt()
                if (it.reminderTime != null) {
                    reminderManager.scheduleReminder(it.copy(id = id))
                }
                recentlyDeletedNote = null
            }
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned))
        }
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
