package com.example.aio_prodapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Table and column names match spec section 3.2 exactly.
// Entities are named *Entity so they never clash with the UI models in NotesScreen/TasksScreen.

@Entity(tableName = "note")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    @ColumnInfo(defaultValue = "''") val description: String = "",
    /** UTC-midnight epoch ms exactly as returned by the Material 3 DatePicker. Format in UTC only. */
    @ColumnInfo(name = "reminder_date") val reminderDate: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(tableName = "task")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    @ColumnInfo(defaultValue = "''") val description: String = "",
    /** UTC-midnight epoch ms (date only). This is the date shown on the task card. */
    @ColumnInfo(name = "reminder_date") val reminderDate: Long? = null,
    @ColumnInfo(defaultValue = "'MEDIUM'") val priority: TaskPriority = TaskPriority.MEDIUM,
    /** The checkbox. false = Active, true = Done. */
    @ColumnInfo(name = "is_done", defaultValue = "0") val isDone: Boolean = false,
    @ColumnInfo(defaultValue = "''") val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /** Set only while isDone = true. */
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null
)

@Entity(
    tableName = "subtask",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["task_id"], name = "idx_subtask_task")]
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "task_id") val taskId: Int,
    val title: String,
    @ColumnInfo(name = "is_done", defaultValue = "0") val isDone: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

/** Append-only history. Powers Sessions, Tasks Done, the weekly chart and the streak. */
@Entity(
    tableName = "activity_log",
    indices = [Index(value = ["occurred_at"], name = "idx_activity_time")]
)
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: ActivityType,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    /** Task id for TASK_COMPLETED. */
    @ColumnInfo(name = "ref_id") val refId: Int? = null,
    /** The "Current Task" text for focus sessions. */
    val detail: String? = null
)

/** History shown by the bell icon. */
@Entity(
    tableName = "notification_history",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["note_id"], name = "idx_notif_note"),
        Index(value = ["task_id"], name = "idx_notif_task")
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: NotificationType,
    val title: String,
    @ColumnInfo(defaultValue = "''") val body: String = "",
    @ColumnInfo(name = "note_id") val noteId: Int? = null,
    @ColumnInfo(name = "task_id") val taskId: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "is_read", defaultValue = "0") val isRead: Boolean = false
)

/** Single row, id is always 1. */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "display_name", defaultValue = "''") val displayName: String = ""
)
