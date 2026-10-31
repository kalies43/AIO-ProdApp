package com.example.aio_prodapp.data

/** Stored in the `task.priority` column as TEXT (the enum name). */
enum class TaskPriority(val label: String) {
    TOP("Top priority"),
    MEDIUM("Medium priority"),
    LOW("Low priority");

    companion object {
        /** Unknown / corrupt values fall back to MEDIUM instead of crashing. */
        fun fromName(name: String?): TaskPriority = entries.firstOrNull { it.name == name } ?: MEDIUM
    }
}

/** Tabs on the Tasks screen. Not stored. */
enum class TaskFilter { ALL, ACTIVE, DONE }

/** Stored in `activity_log.type`. */
enum class ActivityType { TASK_COMPLETED, FOCUS_SESSION_COMPLETED }

/** Stored in `notification_history.type`. */
enum class NotificationType { NOTE_REMINDER, TASK_REMINDER, FOCUS_COMPLETE }
