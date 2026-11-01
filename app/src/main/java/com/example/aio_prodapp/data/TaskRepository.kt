package com.example.aio_prodapp.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val db: AppDatabase) {
    private val taskDao = db.taskDao()
    private val subtaskDao = db.subtaskDao()
    private val activityDao = db.activityDao()

    val taskCount: Flow<Int> = taskDao.observeCount()

    fun observeTasks(filter: TaskFilter): Flow<List<TaskEntity>> = when (filter) {
        TaskFilter.ALL -> taskDao.observeAll()
        TaskFilter.ACTIVE -> taskDao.observeActive()
        TaskFilter.DONE -> taskDao.observeDone()
    }

    fun observeSubtasks(taskId: Int): Flow<List<SubtaskEntity>> = subtaskDao.observeForTask(taskId)

    suspend fun getTask(id: Int): TaskEntity? = taskDao.getById(id)

    /**
     * Create mode: the task and its in-memory subtasks are saved together in one transaction.
     * Returns the new task id.
     */
    suspend fun addTask(
        title: String,
        description: String,
        reminderDate: Long?,
        priority: TaskPriority,
        notes: String,
        subtaskTitles: List<String> = emptyList()
    ): Int {
        require(title.isNotBlank()) { "Title is required" }
        return db.withTransaction {
            val now = System.currentTimeMillis()
            val taskId = taskDao.insert(
                TaskEntity(
                    title = title.trim(),
                    description = description,
                    reminderDate = reminderDate,
                    priority = priority,
                    notes = notes,
                    createdAt = now
                )
            ).toInt()
            val subtasks = subtaskTitles
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { SubtaskEntity(taskId = taskId, title = it, createdAt = now) }
            if (subtasks.isNotEmpty()) subtaskDao.insertAll(subtasks)
            taskId
        }
    }

    /** Edit mode: updates the task's own fields only. Subtasks save immediately, separately. */
    suspend fun updateTask(
        id: Int,
        title: String,
        description: String,
        reminderDate: Long?,
        priority: TaskPriority,
        notes: String
    ) {
        require(title.isNotBlank()) { "Title is required" }
        taskDao.updateDetails(id, title.trim(), description, reminderDate, priority, notes)
    }

    /**
     * The checkbox. Checking sets is_done + completed_at and writes a TASK_COMPLETED log row;
     * unchecking clears them and removes that log row. Both happen in one transaction.
     * Returns the updated task, or null if it no longer exists.
     */
    suspend fun setDone(taskId: Int, done: Boolean): TaskEntity? = db.withTransaction {
        val task = taskDao.getById(taskId) ?: return@withTransaction null
        if (task.isDone == done) return@withTransaction task
        if (done) {
            val now = System.currentTimeMillis()
            taskDao.setDone(taskId, true, now)
            activityDao.insert(
                ActivityLogEntity(
                    type = ActivityType.TASK_COMPLETED,
                    occurredAt = now,
                    refId = taskId
                )
            )
        } else {
            taskDao.setDone(taskId, false, null)
            activityDao.deleteTaskCompletion(taskId)
        }
        taskDao.getById(taskId)
    }

    /** Deleting a task cascades to its subtasks and never touches activity_log. */
    suspend fun deleteTask(id: Int) = taskDao.deleteById(id)

    suspend fun addSubtask(taskId: Int, title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        subtaskDao.insert(
            SubtaskEntity(taskId = taskId, title = clean, createdAt = System.currentTimeMillis())
        )
    }

    suspend fun setSubtaskDone(subtaskId: Int, done: Boolean) = subtaskDao.setDone(subtaskId, done)

    suspend fun deleteSubtask(subtaskId: Int) = subtaskDao.deleteById(subtaskId)
}
