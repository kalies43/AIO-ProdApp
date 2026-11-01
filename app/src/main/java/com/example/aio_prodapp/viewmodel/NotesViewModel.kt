package com.example.aio_prodapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aio_prodapp.data.NoteEntity
import com.example.aio_prodapp.data.NoteRepository
import com.example.aio_prodapp.reminders.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class NotesUiState(
    val notes: List<NoteEntity> = emptyList(),   // newest first
    val noteCount: Int = 0,                       // "N notes"
    val reminderCount: Int = 0,                   // "M reminders"
    val isLoaded: Boolean = false
)

class NotesViewModel(private val repo: NoteRepository) : ViewModel() {

    val uiState: StateFlow<NotesUiState> = combine(
        repo.notes, repo.noteCount, repo.reminderCount
    ) { notes, count, reminders ->
        NotesUiState(notes, count, reminders, isLoaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    /** id == null adds a new note; otherwise edits that note. onResult(false) = keep the dialog open. */
    fun saveNote(
        context: Context? = null,
        id: Int?,
        title: String,
        description: String,
        reminderDate: Long?,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (title.isBlank()) { onResult(false); return }
        launchWrite(onResult) {
            val noteId = if (id == null) {
                repo.addNote(title, description, reminderDate)
            } else {
                repo.updateNote(id, title, description, reminderDate)
                id
            }
            context?.let { ReminderScheduler.scheduleNoteReminder(it, noteId, reminderDate) }
        }
    }

    fun deleteNote(
        context: Context? = null,
        id: Int,
        onResult: (Boolean) -> Unit = {}
    ) = launchWrite(onResult) {
        repo.deleteNote(id)
        context?.let { ReminderScheduler.cancelNoteReminder(it, id) }
    }

    /** For pendingNoteId: look the note up, then open its Edit dialog. */
    suspend fun getNote(id: Int): NoteEntity? = repo.getNote(id)
}
