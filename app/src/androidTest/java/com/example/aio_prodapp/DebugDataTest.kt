package com.example.aio_prodapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aio_prodapp.data.AppDatabase
import com.example.aio_prodapp.data.NoteRepository
import com.example.aio_prodapp.data.NotificationRepository
import com.example.aio_prodapp.data.ProfileRepository
import com.example.aio_prodapp.data.TaskFilter
import com.example.aio_prodapp.data.TaskRepository
import com.example.aio_prodapp.debug.DebugData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Debug build only (DebugData lives in src/debug). Uses an in-memory DB, never your real app data. */
@RunWith(AndroidJUnit4::class)
class DebugDataTest {
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

    @Test fun seedMatchesTheSpec() = runBlocking<Unit> {
        DebugData.seed(db)

        assertEquals(3, notes.noteCount.first())
        assertEquals(2, notes.reminderCount.first())

        val all = tasks.observeTasks(TaskFilter.ALL).first()
        assertEquals(4, all.size)
        assertEquals(1, tasks.observeTasks(TaskFilter.DONE).first().size)
        assertEquals(2, all.count { it.reminderDate != null })
        val subtaskTotal = all.sumOf { tasks.observeSubtasks(it.id).first().size }
        assertEquals(2, subtaskTotal)

        assertEquals(3, profile.observeStreak().first()) // flame icon shows from 3
        assertEquals(6, profile.sessionCount.first())
        assertEquals(4, profile.tasksDoneCount.first())
    }

    @Test fun fireTestRemindersWriteHistoryRows() = runBlocking<Unit> {
        DebugData.seed(db)
        DebugData.fireTestNoteReminder(db)
        DebugData.fireTestTaskReminder(db)
        assertEquals(2, notifications.entries.first().size)
        assertTrue(notifications.hasUnread.first())
    }

    @Test fun clearEmptiesEverything() = runBlocking<Unit> {
        DebugData.seed(db)
        DebugData.fireTestNoteReminder(db)
        DebugData.clear(db)

        assertEquals(0, notes.noteCount.first())
        assertEquals(0, tasks.taskCount.first())
        assertEquals(0, profile.sessionCount.first())
        assertEquals(0, profile.tasksDoneCount.first())
        assertEquals(0, profile.observeStreak().first())
        assertTrue(notifications.entries.first().isEmpty())
    }
}
