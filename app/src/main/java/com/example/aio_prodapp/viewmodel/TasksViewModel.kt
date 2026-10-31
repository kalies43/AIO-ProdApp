package com.example.aio_prodapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aio_prodapp.data.SubtaskEntity
import com.example.aio_prodapp.data.TaskEntity
import com.example.aio_prodapp.data.TaskFilter
import com.example.aio_prodapp.data.TaskPriority
import com.example.aio_prodapp.data.TaskRepository
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

    // Kept as separate flows so the search TextField and tab chips update synchronously.
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
            // TODO(Phase 7): schedule / reschedule / cancel this task's reminder job here.
            savedId
        }
    }

    /** The card checkbox. Moves the task between Active and Done and writes / removes the log row. */
    fun setTaskDone(taskId: Int, done: Boolean, onResult: (Boolean) -> Unit = {}) = launchWrite(onResult) {
        repo.setDone(taskId, done)
        // TODO(Phase 7): cancel the reminder when checked; re-schedule when unchecked (date today or later).
    }

    fun deleteTask(taskId: Int, onResult: (Boolean) -> Unit = {}) = launchWrite(onResult) {
        repo.deleteTask(taskId)
        // TODO(Phase 7): cancel this task's reminder job here.
    }

    /** For pendingTaskId: look the task up, then open its Task Details dialog. */
    suspend fun getTask(id: Int): TaskEntity? = repo.getTask(id)

    // ---- Subtasks of an existing task: every change saves immediately (no Edit needed) ----

    fun subtasks(taskId: Int): Flow<List<SubtaskEntity>> = repo.observeSubtasks(taskId)

    fun addSubtask(taskId: Int, title: String) = launchWrite({}) { repo.addSubtask(taskId, title) }

    fun setSubtaskDone(subtaskId: Int, done: Boolean) = launchWrite({}) { repo.setSubtaskDone(subtaskId, done) }

    fun deleteSubtask(subtaskId: Int) = launchWrite({}) { repo.deleteSubtask(subtaskId) }
}
