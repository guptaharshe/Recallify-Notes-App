package com.recallify.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.recallify.app.data.repository.NoteRepository
import com.recallify.app.utils.ReminderManager

class NoteViewModelFactory(
    private val repository: NoteRepository,
    private val reminderManager: ReminderManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository, reminderManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
