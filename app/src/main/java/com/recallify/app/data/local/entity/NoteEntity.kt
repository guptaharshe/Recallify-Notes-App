package com.recallify.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val content: String,

    val timestamp: Long = System.currentTimeMillis(),

    val isPinned: Boolean = false,
    
    val reminderTime: Long? = null,
    
    val color: Int = 0xFFFFFFFF.toInt() // Default white
)
