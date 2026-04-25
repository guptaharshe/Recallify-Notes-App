package com.recallify.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.recallify.app.data.local.dao.NoteDao
import com.recallify.app.data.local.entity.NoteEntity

@Database(entities = [NoteEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
