package com.example.aio_prodapp.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aio_prodapp.data.AppDatabase
import com.example.aio_prodapp.data.NotificationRepository
import com.example.aio_prodapp.data.NotificationType

class ReminderWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val type = inputData.getString("type") ?: return Result.failure()
        val id = inputData.getInt("id", -1)
        if (id == -1) return Result.failure()

        val db = AppDatabase.getInstance(appContext)
        val notifRepo = NotificationRepository(db.notificationDao())

        when (type) {
            "note" -> {
                val note = db.noteDao().getById(id) ?: return Result.success()
                if (note.reminderDate != null) {
                    val body = preview(note.description)
                    // Insert history entry
                    notifRepo.add(
                        type = NotificationType.NOTE_REMINDER,
                        title = note.title,
                        body = body,
                        noteId = note.id
                    )
                    // Post system notification
                    ReminderScheduler.postNoteSystemNotification(appContext, note.id, note.title, body)
                }
            }
            "task" -> {
                val task = db.taskDao().getById(id) ?: return Result.success()
                if (!task.isDone && task.reminderDate != null) {
                    val body = preview(task.description)
                    // Insert history entry
                    notifRepo.add(
                        type = NotificationType.TASK_REMINDER,
                        title = task.title,
                        body = body,
                        taskId = task.id
                    )
                    // Post system notification
                    ReminderScheduler.postTaskSystemNotification(appContext, task.id, task.title, body)
                }
            }
        }

        return Result.success()
    }

    private fun preview(description: String): String =
        description.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.take(2).joinToString("\n")
}
