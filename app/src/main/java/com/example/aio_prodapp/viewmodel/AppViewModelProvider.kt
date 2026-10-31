package com.example.aio_prodapp.viewmodel

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.aio_prodapp.AioApplication
import com.example.aio_prodapp.data.ActivityDao
import com.example.aio_prodapp.data.ActivityLogEntity
import com.example.aio_prodapp.data.DayCount
import com.example.aio_prodapp.data.NoteDao
import com.example.aio_prodapp.data.NoteEntity
import com.example.aio_prodapp.data.NoteRepository
import com.example.aio_prodapp.data.NotificationDao
import com.example.aio_prodapp.data.NotificationEntity
import com.example.aio_prodapp.data.ProfileDao
import com.example.aio_prodapp.data.ProfileRepository
import com.example.aio_prodapp.data.SubtaskDao
import com.example.aio_prodapp.data.SubtaskEntity
import com.example.aio_prodapp.data.TaskDao
import com.example.aio_prodapp.data.TaskEntity
import com.example.aio_prodapp.data.TaskPriority
import com.example.aio_prodapp.data.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private object FakeNoteDao : NoteDao {
    override fun observeAll(): Flow<List<NoteEntity>> = flowOf(emptyList())
    override fun observeCount(): Flow<Int> = flowOf(0)
    override fun observeReminderCount(): Flow<Int> = flowOf(0)
    override suspend fun getById(id: Int): NoteEntity? = null
    override suspend fun getFirstWithReminder(): NoteEntity? = null
    override suspend fun insert(note: NoteEntity): Long = 0L
    override suspend fun updateDetails(id: Int, title: String, description: String, reminderDate: Long?) {}
    override suspend fun deleteById(id: Int) {}
}

private object FakeTaskDao : TaskDao {
    override fun observeAll(): Flow<List<TaskEntity>> = flowOf(emptyList())
    override fun observeActive(): Flow<List<TaskEntity>> = flowOf(emptyList())
    override fun observeDone(): Flow<List<TaskEntity>> = flowOf(emptyList())
    override fun observeCount(): Flow<Int> = flowOf(0)
    override suspend fun getById(id: Int): TaskEntity? = null
    override suspend fun getFirstWithReminder(): TaskEntity? = null
    override suspend fun insert(task: TaskEntity): Long = 0L
    override suspend fun updateDetails(id: Int, title: String, description: String, reminderDate: Long?, priority: TaskPriority, notes: String) {}
    override suspend fun setDone(id: Int, isDone: Boolean, completedAt: Long?) {}
    override suspend fun deleteById(id: Int) {}
}

private object FakeSubtaskDao : SubtaskDao {
    override fun observeForTask(taskId: Int): Flow<List<SubtaskEntity>> = flowOf(emptyList())
    override suspend fun insert(subtask: SubtaskEntity): Long = 0L
    override suspend fun insertAll(subtasks: List<SubtaskEntity>) {}
    override suspend fun setDone(id: Int, isDone: Boolean) {}
    override suspend fun deleteById(id: Int) {}
}

private object FakeActivityDao : ActivityDao {
    override suspend fun insert(entry: ActivityLogEntity): Long = 0L
    override suspend fun deleteTaskCompletion(taskId: Int) {}
    override fun observeSessionCount(): Flow<Int> = flowOf(0)
    override fun observeTasksDoneCount(): Flow<Int> = flowOf(0)
    override fun observeFocusSessionsSince(startMs: Long): Flow<Int> = flowOf(0)
    override fun observeWeekCounts(weekStartMs: Long): Flow<List<DayCount>> = flowOf(emptyList())
    override fun observeActiveDays(): Flow<List<String>> = flowOf(emptyList())
}

private class FakeNotificationDao : NotificationDao() {
    override fun observeAll(): Flow<List<NotificationEntity>> = flowOf(emptyList())
    override fun observeHasUnread(): Flow<Boolean> = flowOf(false)
    override suspend fun getById(id: Int): NotificationEntity? = null
    override suspend fun insert(entry: NotificationEntity): Long = 0L
    override suspend fun markAllRead() {}
    override suspend fun deleteAll() {}
    override suspend fun trimToNewest(keep: Int) {}
}

private object FakeProfileDao : ProfileDao {
    override fun observe(): Flow<UserProfileEntity?> = flowOf(UserProfileEntity(1, "Guest"))
    override suspend fun upsert(profile: UserProfileEntity) {}
}

/**
 * Factory for instantiating ViewModels in real app or Compose previews safely.
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val app = aioApplication()
            val repo = app?.container?.noteRepository ?: NoteRepository(FakeNoteDao)
            NotesViewModel(repo)
        }
        initializer {
            val app = aioApplication()
            if (app != null) {
                TasksViewModel(app.container.taskRepository)
            } else {
                TasksViewModel(appRequired("TasksViewModel"))
            }
        }
        initializer {
            val app = aioApplication()
            if (app != null) {
                FocusViewModel(app.container.focusRepository)
            } else {
                FocusViewModel(appRequired("FocusViewModel"))
            }
        }
        initializer {
            val app = aioApplication()
            val repo = app?.container?.profileRepository ?: ProfileRepository(FakeProfileDao, FakeNoteDao, FakeActivityDao)
            ProfileViewModel(repo)
        }
        initializer {
            val app = aioApplication()
            if (app != null) {
                NotificationsViewModel(app.container.notificationRepository)
            } else {
                NotificationsViewModel(appRequired("NotificationsViewModel"))
            }
        }
    }
}

private fun CreationExtras.aioApplication(): AioApplication? =
    this[APPLICATION_KEY] as? AioApplication

private fun appRequired(vmName: String): Nothing =
    error("$vmName requires a running AioApplication context.")
