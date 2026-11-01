package com.example.aio_prodapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aio_prodapp.data.AppDatabase
import com.example.aio_prodapp.data.NoteRepository
import com.example.aio_prodapp.data.NotificationRepository
import com.example.aio_prodapp.data.NotificationType
import com.example.aio_prodapp.data.ProfileRepository
import com.example.aio_prodapp.data.TaskFilter
import com.example.aio_prodapp.data.TaskPriority
import com.example.aio_prodapp.data.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the data layer against an in-memory Room database. No UI needed. */
@RunWith(AndroidJUnit4::class)
class DatabaseSmokeTest {
    private lateinit var db: AppDatabase
    private lateinit var notes: NoteRepository
    private lateinit var tasks: TaskRepository
    private lateinit var notifications: NotificationRepository
    private lateinit var profile: ProfileRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        notes = NoteRepository(db.noteDao())
        tasks = TaskRepository(db)
        notifications = NotificationRepository(db.notificationDao())
        profile = ProfileRepository(db.profileDao(), db.noteDao(), db.activityDao())
    }

    @After fun tearDown() = db.close()

    @Test fun noteCountsAndOrder() = runBlocking<Unit> {
        notes.addNote("First", "", null)
        notes.addNote("Second", "body", 1_788_000_000_000L)
        assertEquals(2, notes.noteCount.first())
        assertEquals(1, notes.reminderCount.first())
        assertEquals("Second", notes.notes.first().first().title) // newest first
    }

    @Test fun checkingTaskLogsCompletionAndUncheckingRemovesIt() = runBlocking<Unit> {
        val id = tasks.addTask("Laundry", "", null, TaskPriority.MEDIUM, "")
        assertEquals(0, profile.tasksDoneCount.first())

        tasks.setDone(id, true)
        assertTrue(tasks.getTask(id)!!.isDone)
        assertEquals(1, profile.tasksDoneCount.first())
        assertEquals(1, tasks.observeTasks(TaskFilter.DONE).first().size)
        assertEquals(0, tasks.observeTasks(TaskFilter.ACTIVE).first().size)

        tasks.setDone(id, false)
        assertFalse(tasks.getTask(id)!!.isDone)
        assertNull(tasks.getTask(id)!!.completedAt)
        assertEquals(0, profile.tasksDoneCount.first())
    }

    @Test fun deletingDoneTaskKeepsHistoryAndRemovesSubtasks() = runBlocking<Unit> {
        val id = tasks.addTask("Assignment", "", null, TaskPriority.TOP, "", listOf("Q1", "  ", "Q2"))
        assertEquals(2, tasks.observeSubtasks(id).first().size) // blank subtask ignored

        tasks.setDone(id, true)
        tasks.deleteTask(id)

        assertEquals(1, profile.tasksDoneCount.first())         // log row survives
        assertTrue(tasks.observeSubtasks(id).first().isEmpty()) // cascade
    }

    @Test fun notificationHistoryKeepsNewest100AndNullsDeletedNote() = runBlocking<Unit> {
        val noteId = notes.addNote("Weekly Review", "", null)
        notifications.add(NotificationType.NOTE_REMINDER, "Weekly Review", noteId = noteId)
        notes.deleteNote(noteId)
        assertNull(notifications.entries.first().first().noteId) // ON DELETE SET NULL

        repeat(105) { notifications.add(NotificationType.FOCUS_COMPLETE, "Focus session complete") }
        assertEquals(NotificationRepository.MAX_ENTRIES, notifications.entries.first().size)
        assertTrue(notifications.hasUnread.first())
        notifications.markAllRead()
        assertFalse(notifications.hasUnread.first())
    }

    @Test fun displayNamePersists() = runBlocking<Unit> {
        assertEquals("", profile.displayName.first())
        profile.setDisplayName("  Shannah Grande ")
        assertEquals("Shannah Grande", profile.displayName.first())
    }
}
