package com.example.aio_prodapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Row of the weekly chart query. dow: 0 = Sunday ... 6 = Saturday. */
data class DayCount(val dow: Int, val n: Int)

@Dao
interface NoteDao {
    @Query("SELECT * FROM note ORDER BY created_at DESC, id DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT COUNT(*) FROM note")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM note WHERE reminder_date IS NOT NULL")
    fun observeReminderCount(): Flow<Int>

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun getById(id: Int): NoteEntity?

    /** Debug panel: the note whose reminder is due soonest. */
    @Query("SELECT * FROM note WHERE reminder_date IS NOT NULL ORDER BY reminder_date ASC, id ASC LIMIT 1")
    suspend fun getFirstWithReminder(): NoteEntity?

    @Insert
    suspend fun insert(note: NoteEntity): Long

    /** Edit never touches created_at (spec Feature 1). */
    @Query("UPDATE note SET title = :title, description = :description, reminder_date = :reminderDate WHERE id = :id")
    suspend fun updateDetails(id: Int, title: String, description: String, reminderDate: Long?)

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface TaskDao {
    // All / Active: reminder_date ASC, tasks without a date last.
    @Query("SELECT * FROM task ORDER BY reminder_date IS NULL, reminder_date ASC, id ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE is_done = 0 ORDER BY reminder_date IS NULL, reminder_date ASC, id ASC")
    fun observeActive(): Flow<List<TaskEntity>>

    // Done: most recently completed first.
    @Query("SELECT * FROM task WHERE is_done = 1 ORDER BY completed_at DESC, id DESC")
    fun observeDone(): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM task")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM task WHERE id = :id")
    suspend fun getById(id: Int): TaskEntity?

    /** Debug panel: the unchecked task whose reminder is due soonest. */
    @Query(
        "SELECT * FROM task WHERE reminder_date IS NOT NULL AND is_done = 0 " +
            "ORDER BY reminder_date ASC, id ASC LIMIT 1"
    )
    suspend fun getFirstWithReminder(): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Query(
        "UPDATE task SET title = :title, description = :description, reminder_date = :reminderDate, " +
            "priority = :priority, notes = :notes WHERE id = :id"
    )
    suspend fun updateDetails(
        id: Int,
        title: String,
        description: String,
        reminderDate: Long?,
        priority: TaskPriority,
        notes: String
    )

    @Query("UPDATE task SET is_done = :isDone, completed_at = :completedAt WHERE id = :id")
    suspend fun setDone(id: Int, isDone: Boolean, completedAt: Long?)

    @Query("DELETE FROM task WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtask WHERE task_id = :taskId ORDER BY created_at ASC, id ASC")
    fun observeForTask(taskId: Int): Flow<List<SubtaskEntity>>

    @Insert
    suspend fun insert(subtask: SubtaskEntity): Long

    @Insert
    suspend fun insertAll(subtasks: List<SubtaskEntity>)

    @Query("UPDATE subtask SET is_done = :isDone WHERE id = :id")
    suspend fun setDone(id: Int, isDone: Boolean)

    @Query("DELETE FROM subtask WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface ActivityDao {
    @Insert
    suspend fun insert(entry: ActivityLogEntity): Long

    /** Used when a task is unchecked. Deleting a task never calls this. */
    @Query("DELETE FROM activity_log WHERE type = 'TASK_COMPLETED' AND ref_id = :taskId")
    suspend fun deleteTaskCompletion(taskId: Int)

    @Query("SELECT COUNT(*) FROM activity_log WHERE type = 'FOCUS_SESSION_COMPLETED'")
    fun observeSessionCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM activity_log WHERE type = 'TASK_COMPLETED'")
    fun observeTasksDoneCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM activity_log WHERE type = 'FOCUS_SESSION_COMPLETED' AND occurred_at >= :startMs")
    fun observeFocusSessionsSince(startMs: Long): Flow<Int>

    // Spec 3.3 weekly chart query (CAST so Room reads dow as an Int).
    @Query(
        "SELECT CAST(strftime('%w', occurred_at/1000, 'unixepoch', 'localtime') AS INTEGER) AS dow, COUNT(*) AS n " +
            "FROM activity_log WHERE occurred_at >= :weekStartMs GROUP BY dow"
    )
    fun observeWeekCounts(weekStartMs: Long): Flow<List<DayCount>>

    // Spec 3.3 streak input: distinct local days with progress, newest first ("yyyy-MM-dd").
    @Query("SELECT DISTINCT date(occurred_at/1000, 'unixepoch', 'localtime') AS d FROM activity_log ORDER BY d DESC")
    fun observeActiveDays(): Flow<List<String>>
}

@Dao
abstract class NotificationDao {
    @Query("SELECT * FROM notification_history ORDER BY created_at DESC, id DESC")
    abstract fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM notification_history WHERE is_read = 0)")
    abstract fun observeHasUnread(): Flow<Boolean>

    @Query("SELECT * FROM notification_history WHERE id = :id")
    abstract suspend fun getById(id: Int): NotificationEntity?

    @Insert
    abstract suspend fun insert(entry: NotificationEntity): Long

    @Query("UPDATE notification_history SET is_read = 1 WHERE is_read = 0")
    abstract suspend fun markAllRead()

    @Query("DELETE FROM notification_history")
    abstract suspend fun deleteAll()

    @Query(
        "DELETE FROM notification_history WHERE id NOT IN " +
            "(SELECT id FROM notification_history ORDER BY created_at DESC, id DESC LIMIT :keep)"
    )
    abstract suspend fun trimToNewest(keep: Int)

    /** Insert, then keep only the newest [keep] rows (spec Feature 7: newest 100). */
    @Transaction
    open suspend fun insertAndTrim(entry: NotificationEntity, keep: Int) {
        insert(entry)
        trimToNewest(keep)
    }
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfileEntity?>

    @Upsert
    suspend fun upsert(profile: UserProfileEntity)
}
