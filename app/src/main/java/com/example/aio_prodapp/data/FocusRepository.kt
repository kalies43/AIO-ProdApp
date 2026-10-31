package com.example.aio_prodapp.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class FocusRepository(private val db: AppDatabase) {
    private val activityDao = db.activityDao()
    private val notificationDao = db.notificationDao()

    fun observeSessionsSince(startMs: Long): Flow<Int> = activityDao.observeFocusSessionsSince(startMs)

    /**
     * Called when a FOCUS timer reaches 0 (spec Feature 3): one activity_log row plus one
     * notification_history row, in a single transaction. Breaks never call this.
     * [occurredAt] defaults to now; pass the stored end time when restoring after process death.
     */
    suspend fun recordFocusCompleted(currentTask: String?, occurredAt: Long = System.currentTimeMillis()) {
        val detail = currentTask?.trim()?.ifBlank { null }
        db.withTransaction {
            activityDao.insert(
                ActivityLogEntity(
                    type = ActivityType.FOCUS_SESSION_COMPLETED,
                    occurredAt = occurredAt,
                    detail = detail
                )
            )
            notificationDao.insertAndTrim(
                NotificationEntity(
                    type = NotificationType.FOCUS_COMPLETE,
                    title = "Focus session complete",
                    body = detail.orEmpty(),
                    createdAt = occurredAt
                ),
                NotificationRepository.MAX_ENTRIES
            )
        }
    }
}
