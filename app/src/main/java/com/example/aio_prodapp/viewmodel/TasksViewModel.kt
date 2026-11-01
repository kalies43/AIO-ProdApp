package com.example.aio_prodapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aio_prodapp.data.SubtaskEntity
import com.example.aio_prodapp.data.TaskEntity
import com.example.aio_prodapp.data.TaskFilter
import com.example.aio_prodapp.data.TaskPriority
import com.example.aio_prodapp.data.TaskRepository
import com.example.aio_prodapp.reminders.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class TasksUiState(
    /** Already filtered by the selected tab AND the search text. */
    val tasks: List<TaskEntity> = emptyList(),
    /** false -> "No tasks yet."; true with an empty [tasks] -> "No tasks match." */
    val hasAnyTasks: Boolean = false,
    val isLoaded: Boolean = false
)

class TasksViewModel(private val repo: TaskRepository) : ViewModel() {

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun setFilter(value: TaskFilter) = _filter.update { value }
    fun setQuery(value: String) = _query.update { value }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TasksUiState> = combine(
        _filter.flatMapLatest { repo.observeTasks(it) },
        _query,
        repo.taskCount
    ) { tasks, query, total ->
        val needle = query.trim()
        TasksUiState(
            tasks = if (needle.isEmpty()) tasks else tasks.filter { it.title.contains(needle, ignoreCase = true) },
            hasAnyTasks = total > 0,
            isLoaded = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    /**
     * id == null creates a task (with [newSubtasks] held in memory by the dialog until now);
     * otherwise edits the task's own fields. onResult gets the task id, or null if the write failed.
     */
    fun saveTask(
        context: Context? = null,
        id: Int?,
        title: String,
        description: String,
        reminderDate: Long?,
        priority: TaskPriority,
        notes: String,
        newSubtasks: List<String> = emptyList(),
        onResult: (Int?) -> Unit = {}
    ) {
        if (title.isBlank()) { onResult(null); return }
        launchWriteForId(onResult) {
            val savedId = if (id == null) {
                repo.addTask(title, description, reminderDate, priority, notes, newSubtasks)
            } else {
                repo.updateTask(id, title, description, reminderDate, priority, notes)
                id
            }
            context?.let { ReminderScheduler.scheduleTaskReminder(it, savedId, reminderDate) }
            savedId
        }
    }

    /** The card checkbox. Moves the task between Active and Done and writes / removes the log row. */
    fun setTaskDone(
        context: Context? = null,
        taskId: Int,
        done: Boolean,
        onResult: (Boolean) -> Unit = {}
    ) = launchWrite(onResult) {
        val updatedTask = repo.setDone(taskId, done)
        context?.let { ctx ->
            if (done) {
                ReminderScheduler.cancelTaskReminder(ctx, taskId)
            } else if (updatedTask?.reminderDate != null) {
                ReminderScheduler.scheduleTaskReminder(ctx, taskId, updatedTask.reminderDate)
            }
        }
    }

    fun deleteTask(
        context: Context? = null,
        taskId: Int,
        onResult: (Boolean) -> Unit = {}
    ) = launchWrite(onResult) {
        repo.deleteTask(taskId)
        context?.let { ReminderScheduler.cancelTaskReminder(it, taskId) }
    }

    /** For pendingTaskId: look the task up, then open its Task Details dialog. */
    suspend fun getTask(id: Int): TaskEntity? = repo.getTask(id)

    // ---- Subtasks of an existing task: every change saves immediately (no Edit needed) ----

    fun subtasks(taskId: Int): Flow<List<SubtaskEntity>> = repo.observeSubtasks(taskId)

    fun addSubtask(taskId: Int, title: String) = launchWrite({}) { repo.addSubtask(taskId, title) }

    fun setSubtaskDone(subtaskId: Int, done: Boolean) = launchWrite({}) { repo.setSubtaskDone(subtaskId, done) }

    fun deleteSubtask(subtaskId: Int) = launchWrite({}) { repo.deleteSubtask(subtaskId) }
}
