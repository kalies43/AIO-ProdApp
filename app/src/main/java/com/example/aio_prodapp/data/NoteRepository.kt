package com.example.aio_prodapp.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val notes: Flow<List<NoteEntity>> = noteDao.observeAll()
    val noteCount: Flow<Int> = noteDao.observeCount()
    val reminderCount: Flow<Int> = noteDao.observeReminderCount()

    suspend fun getNote(id: Int): NoteEntity? = noteDao.getById(id)

    /** Returns the new note's id. */
    suspend fun addNote(title: String, description: String, reminderDate: Long?): Int {
        require(title.isNotBlank()) { "Title is required" }
        return noteDao.insert(
            NoteEntity(
                title = title.trim(),
                description = description,
                reminderDate = reminderDate,
                createdAt = System.currentTimeMillis()
            )
        ).toInt()
    }

    suspend fun updateNote(id: Int, title: String, description: String, reminderDate: Long?) {
        require(title.isNotBlank()) { "Title is required" }
        noteDao.updateDetails(id, title.trim(), description, reminderDate)
    }

    suspend fun deleteNote(id: Int) = noteDao.deleteById(id)
}
