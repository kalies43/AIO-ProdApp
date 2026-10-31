package com.example.aio_prodapp.debug

import androidx.room.withTransaction
import com.example.aio_prodapp.data.ActivityLogEntity
import com.example.aio_prodapp.data.ActivityType
import com.example.aio_prodapp.data.AppDatabase
import com.example.aio_prodapp.data.NoteEntity
import com.example.aio_prodapp.data.NotificationRepository
import com.example.aio_prodapp.data.NotificationType
import com.example.aio_prodapp.data.SubtaskEntity
import com.example.aio_prodapp.data.TaskEntity
import com.example.aio_prodapp.data.TaskPriority
import com.example.aio_prodapp.data.UserProfileEntity
import com.example.aio_prodapp.data.startOfTodayMs
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Debug-only actions behind the debug panel. Every function takes the database so tests can use an in-memory one. */
object DebugData {
    private const val MINUTE = 60_000L

    // ---- Fire test reminders ------------------------------------------------------------

    suspend fun fireTestNoteReminder(db: AppDatabase): String {
        val note = db.noteDao().getFirstWithReminder()
            ?: return "No note has a reminder yet. Tap \"Seed sample data\" first."
        NotificationRepository(db.notificationDao()).add(
            type = NotificationType.NOTE_REMINDER,
            title = note.title,
            body = preview(note.description),
            noteId = note.id
        )
        // TODO(Phase 7): call the real reminder path here so a system notification is posted too.
        return "History row added for note \"${note.title}\". The bell should show an unread dot."
    }

    suspend fun fireTestTaskReminder(db: AppDatabase): String {
        val task = db.taskDao().getFirstWithReminder()
            ?: return "No unchecked task has a reminder yet. Tap \"Seed sample data\" first."
        NotificationRepository(db.notificationDao()).add(
            type = NotificationType.TASK_REMINDER,
            title = task.title,
            body = preview(task.description),
            taskId = task.id
        )
        // TODO(Phase 7): call the real reminder path here so a system notification is posted too.
        return "History row added for task \"${task.title}\". The bell should show an unread dot."
    }

    private fun preview(description: String): String =
        description.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.take(2).joinToString("\n")

    // ---- Seed sample data ---------------------------------------------------------------

    /**
     * Inserts: 3 notes (2 with reminders), 4 tasks (1 checked, 2 with reminders, 1 with two subtasks)
     * and activity rows on 3 consecutive days ending today, so the streak is 3 and the flame shows.
     * Tapping it twice inserts everything twice; use "Clear all data" first for a clean slate.
     */
    suspend fun seed(db: AppDatabase) {
        val noteDao = db.noteDao()
        val taskDao = db.taskDao()
        val subtaskDao = db.subtaskDao()
        val activityDao = db.activityDao()
        val now = System.currentTimeMillis()

        db.withTransaction {
            // Notes (reminder dates are UTC-midnight, like the Material 3 DatePicker returns)
            noteDao.insert(
                NoteEntity(
                    title = "Weekly Review",
                    description = "Reflect on what went well, what to improve, and set intentions for next week.",
                    reminderDate = utcDate(3),
                    createdAt = now - 3 * MINUTE
                )
            )
            noteDao.insert(
                NoteEntity(
                    title = "Book List",
                    description = "Atomic Habits, Deep Work, The Power of Now, Digital Minimalism.",
                    createdAt = now - 2 * MINUTE
                )
            )
            noteDao.insert(
                NoteEntity(
                    title = "Project Ideas",
                    description = "Habit tracker\nBudget planner\nStudy timer",
                    reminderDate = utcDate(6),
                    createdAt = now - MINUTE
                )
            )

            // Tasks
            val finishId = taskDao.insert(
                TaskEntity(
                    title = "Finish Assignment",
                    description = "Networking Assignment (Oct 29)\n1. What is a network?\n2. Why is network topology important?",
                    reminderDate = utcDate(2),
                    priority = TaskPriority.TOP,
                    createdAt = now - 4 * MINUTE
                )
            ).toInt()
            subtaskDao.insertAll(
                listOf(
                    SubtaskEntity(taskId = finishId, title = "What is a network?", createdAt = now),
                    SubtaskEntity(taskId = finishId, title = "Why is network topology important?", createdAt = now)
                )
            )
            taskDao.insert(
                TaskEntity(
                    title = "Workout- Legs",
                    reminderDate = utcDate(5),
                    priority = TaskPriority.MEDIUM,
                    createdAt = now - 3 * MINUTE
                )
            )
            taskDao.insert(
                TaskEntity(title = "Laundry", priority = TaskPriority.LOW, createdAt = now - 2 * MINUTE)
            )
            val reviewId = taskDao.insert(
                TaskEntity(
                    title = "Review for Midterms",
                    isDone = true,
                    completedAt = dayTime(0, 0),
                    createdAt = now - 5 * MINUTE
                )
            ).toInt()

            // Activity: today 3, yesterday 5, two days ago 2 (varied heights, streak = 3).
            // The day before that stays empty so the streak is exactly 3.
            activityDao.insert(task(dayTime(0, 0), reviewId))   // the checked task above
            activityDao.insert(focus(dayTime(0, 1)))
            activityDao.insert(focus(dayTime(0, 2)))

            repeat(2) { activityDao.insert(task(dayTime(1, it), null)) }
            repeat(3) { activityDao.insert(focus(dayTime(1, it + 2))) }

            activityDao.insert(task(dayTime(2, 0), null))
            activityDao.insert(focus(dayTime(2, 1)))
        }
    }

    /** Date-only value in the same form the date picker stores: UTC midnight of that calendar day. */
    private fun utcDate(daysFromToday: Long): Long =
        LocalDate.now().plusDays(daysFromToday).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    /** A real instant on the local day [daysAgo] days back. Today's rows sit just after local midnight so they are never in the future. */
    private fun dayTime(daysAgo: Int, slot: Int): Long {
        if (daysAgo == 0) return startOfTodayMs() + (slot + 1) * 1_000L
        val zone = ZoneId.systemDefault()
        return LocalDate.now(zone).minusDays(daysAgo.toLong())
            .atTime(9 + slot, 0).atZone(zone).toInstant().toEpochMilli()
    }

    private fun task(at: Long, taskId: Int?) =
        ActivityLogEntity(type = ActivityType.TASK_COMPLETED, occurredAt = at, refId = taskId)

    private fun focus(at: Long) =
        ActivityLogEntity(type = ActivityType.FOCUS_SESSION_COMPLETED, occurredAt = at, detail = "Deep Work Session")

    // ---- Clear all data -----------------------------------------------------------------

    suspend fun clear(db: AppDatabase) {
        withContext(Dispatchers.IO) {
            db.clearAllTables()
            // keep the single profile row so the app is in the same state as a fresh install
            db.profileDao().upsert(UserProfileEntity(id = 1, displayName = ""))
        }
        // TODO(Phase 7): also cancel every scheduled reminder job (WorkManager).
        // TODO(Phase 5): also clear the saved Focus timer state in SharedPreferences.
    }
}
